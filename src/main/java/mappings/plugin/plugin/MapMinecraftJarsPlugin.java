package mappings.plugin.plugin;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.jetbrains.annotations.NotNull;
import mappings.plugin.constants.Constants;
import mappings.plugin.constants.Classifiers;
import mappings.plugin.constants.Extensions;
import mappings.plugin.extension.MapMinecraftJarsExtension;
import mappings.plugin.extension.MappingsExtension;
import mappings.plugin.constants.Namespaces;
import mappings.plugin.plugin.abstraction.DefaultExtensionedMappingsProjectPlugin;
import mappings.plugin.task.build.AddProposedMappingsTask;
import mappings.plugin.task.build.BuildMappingsTinyTask;
import mappings.plugin.task.build.CompressTinyTask;
import mappings.plugin.task.build.DropInvalidMappingsTask;
import mappings.plugin.task.build.GeneratePackageInfoMappingsTask;
import mappings.plugin.task.build.InvertPerVersionMappingsTask;
import mappings.plugin.task.build.MergeTinyTask;
import mappings.plugin.task.build.TinyJarTask;
import mappings.plugin.task.jarmapping.MapJarTask;
import mappings.plugin.task.jarmapping.MapPerVersionMappingsJarTask;
import mappings.plugin.task.lint.Checker;
import mappings.plugin.task.lint.FindDuplicateMappingFilesTask;
import mappings.plugin.task.lint.MappingLintTask;
import mappings.plugin.task.setup.DownloadMinecraftLibrariesTask;
import mappings.plugin.task.setup.ExtractTinyMappingsTask;
import mappings.plugin.task.setup.MergeJarsTask;

import static mappings.plugin.util.FileUtil.getNameWithExtension;
import static mappings.plugin.util.FileUtil.getPathWithExtension;

/**
 * {@linkplain TaskContainer#register Registers} tasks that map Minecraft jars and verify mappings.
 * <p>
 * Applies:
 * <ul>
 *     <li> {@link MappingsBasePlugin}
 *     <li> {@link MinecraftJarsPlugin}
 * </ul>
 * <p>
 * Additionally:
 * <ul>
 *     <li> creates the {@value INTERMEDIATE_MAPPINGS_CONFIGURATION_NAME} configuration,
 *          to which mappings must be added in order to use the
 *          {@value ExtractTinyMappingsTask#EXTRACT_TINY_INTERMEDIATE_MAPPINGS_TASK_NAME} task
 *     <li> if the {@link JavaPlugin} is applied, {@linkplain org.gradle.api.Task#setEnabled(boolean) disables} the
 *          {@value JavaPlugin#JAR_TASK_NAME} task so its output doesn't collide with the
 *          {@value TinyJarTask#TINY_JAR_TASK_NAME} task's output
 *     <li> {@linkplain TaskCollection#configureEach(Action) configures} {@link MapJarTask}s
 *          {@link MapJarTask#getLibrariesDir() librariesDir}'s default values to
 *          {@value DownloadMinecraftLibrariesTask#DOWNLOAD_MINECRAFT_LIBRARIES_TASK_NAME}'s
 *          {@link DownloadMinecraftLibrariesTask#getLibrariesDir() librariesDir}
 *     <li> if {@link LifecycleBasePlugin} is applied, configures the {@value LifecycleBasePlugin#CHECK_TASK_NAME} task
 *          to {@linkplain Task#dependsOn(Object...) depend on} the
 *          {@value MappingLintTask#MAPPING_LINT_TASK_NAME} task
 * </ul>
 * Note:
 * <ul>
 *     <li> v2 mappings are created by {@link MapV2Plugin} tasks
 *     <li> {@value Namespaces#INTERMEDIARY} mappings are created by {@link MapIntermediaryPlugin} tasks
 * </ul>
 */
public abstract class MapMinecraftJarsPlugin extends
        DefaultExtensionedMappingsProjectPlugin<MapMinecraftJarsExtension> {
    public static final String INTERMEDIATE_MAPPINGS_CONFIGURATION_NAME = Namespaces.INTERMEDIATE;

    @Override
    protected MapMinecraftJarsExtension applyImpl(@NotNull Project project) {
        final Configuration perVersionMappings =
            project.getConfigurations().create(INTERMEDIATE_MAPPINGS_CONFIGURATION_NAME);

        // apply required plugins and save their registered objects
        final PluginContainer plugins = project.getPlugins();

        // configures EnigmaProfileConsumingTasks (insertAutoGeneratedMappings)
        // configures MappingsDirOutputtingTasks (generatePackageInfoMappings)
        // configures MappingsDirConsumingTasks
        //  (buildMappingsTiny, dropInvalidMappings, findDuplicateMappingFiles, mappingLint)
        final MappingsExtension quiltExt = plugins.apply(MappingsBasePlugin.class).getExt();

        final MinecraftJarsPlugin.Tasks minecraftJarsTasks =
            plugins.apply(MinecraftJarsPlugin.class).getExt().getTasks();
        final TaskProvider<DownloadMinecraftLibrariesTask> downloadMinecraftLibraries =
            minecraftJarsTasks.downloadMinecraftLibraries();
        final TaskProvider<MergeJarsTask> mergeJars =
            minecraftJarsTasks.mergeJars();

        // register this plugin's tasks
        final TaskContainer tasks = project.getTasks();

        final var extractTinyPerVersionMappings = tasks.register(
            ExtractTinyMappingsTask.EXTRACT_TINY_INTERMEDIATE_MAPPINGS_TASK_NAME,
            ExtractTinyMappingsTask.class,
            task -> {
                task.getZippedFile().convention(this.provideRequiredFile(perVersionMappings));

                task.getExtractionDest().convention(this.provideMappingsBuildFile(
                    quiltExt.provideSuffixedMinecraftVersion("-" + Classifiers.INTERMEDIATE + "." + Extensions.TINY)
                ));
            }
        );

        final var invertPerVersionMappings = tasks.register(
            InvertPerVersionMappingsTask.INVERT_INTERMEDIATE_MAPPINGS_TASK_NAME,
            InvertPerVersionMappingsTask.class,
            task -> {
                task.getInput().convention(
                    extractTinyPerVersionMappings.flatMap(ExtractTinyMappingsTask::getExtractionDest)
                );

                task.getInvertedTinyFile().convention(
                    task.getInput()
                        .map(input -> getPathWithExtension(input, "-inverted." + Extensions.TINY))
                        .map(this.getProjectDir()::file)
                );
            }
        );

        tasks.withType(MapJarTask.class).configureEach(task -> {
            task.getLibrariesDir().convention(
                downloadMinecraftLibraries
                    .flatMap(DownloadMinecraftLibrariesTask::getLibrariesDir)
            );
        });

        final var mapPerVersionMappingsJar = tasks.register(
            MapPerVersionMappingsJarTask.MAP_INTERMEDIATE_MAPPINGS_JAR_TASK_NAME,
            MapPerVersionMappingsJarTask.class,
            task -> {
                task.getInputJar().convention(
                    mergeJars.flatMap(MergeJarsTask::getMergedFile)
                );

                task.getMappingsFile().convention(
                    extractTinyPerVersionMappings.flatMap(ExtractTinyMappingsTask::getExtractionDest)
                );

                task.getOutputJar().convention(
                    this.provideMappedMinecraftBuildFile(
                        quiltExt.provideSuffixedMinecraftVersion("-" + Classifiers.INTERMEDIATE + "." + Extensions.JAR)
                    )
                );
            }
        );

        final var buildMappingsTiny = tasks.register(
            BuildMappingsTinyTask.BUILD_MAPPINGS_TINY_TASK_NAME,
            BuildMappingsTinyTask.class,
            task -> {
                task.getPerVersionMappingsJar().convention(
                    mapPerVersionMappingsJar.flatMap(MapPerVersionMappingsJarTask::getOutputJar)
                );

                task.getOutputMappings().convention(
                    this.provideMappingsBuildFile(Constants.MAPPINGS_NAME + "." + Extensions.TINY)
                );
            }
        );

        final var insertAutoGeneratedMappings = tasks.register(
            AddProposedMappingsTask.INSERT_AUTO_GENERATED_MAPPINGS_TASK_NAME,
            AddProposedMappingsTask.class,
            task -> {
                task.getInputJar().convention(
                    mapPerVersionMappingsJar.flatMap(MapPerVersionMappingsJarTask::getOutputJar)
                );

                task.getInputMappings().convention(buildMappingsTiny.flatMap(BuildMappingsTinyTask::getOutputMappings));

                task.getOutputMappings().convention(
                    this.getMappingsBuildDir().zip(task.getInputMappings(), (dir, input) ->
                        dir.file(getNameWithExtension(input, "-inserted." + Extensions.TINY))
                    )
                );

                task.getPreprocessedMappings().convention(
                    this.getTempDir().zip(task.getInputMappings(), (dir, input) ->
                        dir.file(getNameWithExtension(input, "-preprocessed." + Extensions.TINY))
                    )
                );

                task.getProcessedMappings().convention(
                    this.getTempDir().zip(task.getInputMappings(), (dir, input) ->
                        dir.file(getNameWithExtension(input, "-processed." + Extensions.TINY))
                    )
                );
            }
        );

        final var mergeTiny = tasks.register(
            MergeTinyTask.MERGE_TINY_TASK_NAME,
            MergeTinyTask.class,
            task -> {
                task.getInput().convention(buildMappingsTiny.flatMap(BuildMappingsTinyTask::getOutputMappings));

                task.getHashedTinyMappings().convention(
                    invertPerVersionMappings.flatMap(InvertPerVersionMappingsTask::getInvertedTinyFile)
                );

                task.getOutputMappings().convention(this.provideMappingsBuildFile("mappings." + Extensions.TINY));
            }
        );

        tasks.register(
            TinyJarTask.TINY_JAR_TASK_NAME,
            TinyJarTask.class,
            task -> {
                task.getMappings().convention(mergeTiny.flatMap(MergeTinyTask::getOutputMappings));
            }
        );

        plugins.withType(JavaPlugin.class, java -> {
            // Its artifact collides with the `tinyJar` one, just disable it since it isn't used either way
            tasks.named(JavaPlugin.JAR_TASK_NAME).configure(task -> task.setEnabled(false));
        });

        tasks.register(
            CompressTinyTask.COMPRESS_TINY_TASK_NAME,
            CompressTinyTask.class,
            task -> {
                task.getMappings().convention(mergeTiny.flatMap(MergeTinyTask::getOutputMappings));

                task.getArtifactBaseName().convention(Constants.MAPPINGS_NAME);

                task.getArtifactVersion().convention(quiltExt.getMappingsVersion());

                task.getArtifactClassifier().convention(Classifiers.TINY);
            }
        );

        tasks.register(
            GeneratePackageInfoMappingsTask.GENERATE_PACKAGE_INFO_MAPPINGS_TASK_NAME,
            GeneratePackageInfoMappingsTask.class,
            task -> {
                task.getInputJar().convention(
                    mapPerVersionMappingsJar.flatMap(MapPerVersionMappingsJarTask::getOutputJar)
                );
            }
        );

        tasks.register(
            DropInvalidMappingsTask.DROP_INVALID_MAPPINGS_TASK_NAME,
            DropInvalidMappingsTask.class,
            task -> {
                task.getPerVersionMappingsJar().convention(
                    mapPerVersionMappingsJar.flatMap(MapPerVersionMappingsJarTask::getOutputJar)
                );
            }
        );

        final var findDuplicateMappingFiles = tasks.register(
            FindDuplicateMappingFilesTask.FIND_DUPLICATE_MAPPING_FILES_TASK_NAME,
            FindDuplicateMappingFilesTask.class
        );

        final var mappingLint = tasks.register(
            MappingLintTask.MAPPING_LINT_TASK_NAME,
            MappingLintTask.class,
            task -> {
                // this does mappings verification but has no output to depend on
                task.dependsOn(findDuplicateMappingFiles);

                task.getJarFile().convention(
                    mapPerVersionMappingsJar.flatMap(MapPerVersionMappingsJarTask::getOutputJar)
                );

                task.getCheckers().addAll(Checker.DEFAULT_CHECKERS);

                this.provideDefaultError(
                    task.getDictionaryFile(),
                    "No dictionary file specified. A file must be specified to use " + task.getName()
                );
            }
        );

        plugins.withType(LifecycleBasePlugin.class, lifecycle -> {
            tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).configure(task -> task.dependsOn(mappingLint));
        });

        return project.getExtensions().create(
            MapMinecraftJarsExtension.NAME, MapMinecraftJarsExtension.class,
            new Tasks(mapPerVersionMappingsJar, invertPerVersionMappings, insertAutoGeneratedMappings)
        );
    }

    public record Tasks(
        TaskProvider<MapPerVersionMappingsJarTask> mapPerVersionMappingsJar,
        TaskProvider<InvertPerVersionMappingsTask> invertPerVersionMappings,
        TaskProvider<AddProposedMappingsTask> insertAutoGeneratedMappings
    ) { }
}