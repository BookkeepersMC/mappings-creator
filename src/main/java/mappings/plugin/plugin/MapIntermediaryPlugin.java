package mappings.plugin.plugin;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import mappings.plugin.constants.Classifiers;
import mappings.plugin.constants.Extensions;
import mappings.plugin.extension.MapIntermediaryExtension;
import mappings.plugin.extension.MappingsExtension;
import mappings.plugin.constants.Namespaces;
import mappings.plugin.plugin.abstraction.DefaultExtensionedMappingsProjectPlugin;
import mappings.plugin.task.build.BuildIntermediaryTask;
import mappings.plugin.task.build.IntermediaryMappingsV2JarTask;
import mappings.plugin.task.build.MergeIntermediaryTask;
import mappings.plugin.task.build.MergeTinyV2Task;
import mappings.plugin.task.build.RemoveIntermediaryTask;
import mappings.plugin.task.setup.ExtractTinyIntermediaryMappingsTask;
import mappings.plugin.task.setup.ExtractTinyMappingsTask;
import mappings.plugin.task.setup.IntermediaryDependantTask;

/**
 * {@linkplain TaskContainer#register Registers} tasks related to {@value Namespaces#INTERMEDIARY} mappings.
 * <p>
 * Applies:
 * <ul>
 *     <li> {@link MappingsBasePlugin}
 *     <li> {@link MapV2Plugin}
 * </ul>
 * <p>
 * Additionally:
 * <ul>
 *     <li> {@linkplain  TaskContainer#register registers} the
 *          {@value BuildIntermediaryTask#BUILD_INTERMEDIARY_TASK_NAME} aggregate task
 *     <li> {@linkplain org.gradle.api.tasks.TaskCollection#configureEach configures} {@link IntermediaryDependantTask}s
 *          to run {@link Task#onlyIf(Spec) onlyIf}
 *          the {@value MapIntermediaryPlugin#INTERMEDIARY_MAPPINGS_CONFIGURATION_NAME}
 *          {@link org.gradle.api.artifacts.Configuration Configuration}
 *          successfully {@linkplain Configuration#resolve() resolves}
 * </ul>
 */
public abstract class MapIntermediaryPlugin extends DefaultExtensionedMappingsProjectPlugin<MapIntermediaryExtension> {
    public static final String INTERMEDIARY_MAPPINGS_CONFIGURATION_NAME = Namespaces.INTERMEDIARY;

    @Override
    protected MapIntermediaryExtension applyImpl(@NotNull Project project) {
        final ConfigurationContainer configurations = project.getConfigurations();
        final Configuration intermediaryMappings = configurations.create(INTERMEDIARY_MAPPINGS_CONFIGURATION_NAME);

        // apply required plugins and save their registered objects
        final PluginContainer plugins = project.getPlugins();

        final MappingsExtension quiltExt = plugins.apply(MappingsBasePlugin.class).getExt();

        final MapV2Plugin.Tasks mappingsV2Tasks =
            plugins.apply(MapV2Plugin.class).getExt().getTasks();
        final TaskProvider<MergeTinyV2Task> mergeTinyV2 =
            mappingsV2Tasks.mergeTinyV2();

        // register this plugin's tasks
        final TaskContainer tasks = project.getTasks();

        final var extractTinyIntermediaryMappings = tasks.register(
            ExtractTinyIntermediaryMappingsTask.EXTRACT_TINY_INTERMEDIARY_MAPPINGS_TASK_NAME,
            ExtractTinyIntermediaryMappingsTask.class,
            task -> {
                task.getExtractionDest().convention(this.provideMappingsBuildFile(
                    quiltExt.provideSuffixedMinecraftVersion("-" + Namespaces.INTERMEDIARY + "." + Extensions.TINY)
                ));
            }
        );

        final Provider<RegularFile> intermediaryFile;
        {
            final Property<RegularFile> intermediaryProperty = this.getObjects().fileProperty();
            intermediaryProperty.set(this.provideOptionalFile(intermediaryMappings));

            intermediaryFile = intermediaryProperty;
        }

        tasks.withType(IntermediaryDependantTask.class).configureEach(task -> {
            task.onlyIf(unused -> intermediaryFile.isPresent());
        });

        extractTinyIntermediaryMappings.configure(task -> {
            task.getZippedFile().convention(intermediaryFile);
        });

        final var mergeIntermediary = tasks.register(
            MergeIntermediaryTask.MERGE_INTERMEDIARY_TASK_NAME,
            MergeIntermediaryTask.class,
            task -> {
                task.getInput().convention(
                    extractTinyIntermediaryMappings.flatMap(ExtractTinyMappingsTask::getExtractionDest)
                );

                task.getMergedTinyMappings().convention(mergeTinyV2.flatMap(MergeTinyV2Task::getOutputMappings));

                task.getOutputMappings().convention(this.provideMappingsBuildFile(
                    "mappings-" + Classifiers.INTERMEDIARY + "Merged." + Extensions.TINY
                ));
            }
        );

        final var removeIntermediary = tasks.register(
            RemoveIntermediaryTask.REMOVE_INTERMEDIARY_TASK_NAME,
            RemoveIntermediaryTask.class,
            task -> {
                task.getInput().convention(mergeIntermediary.flatMap(MergeIntermediaryTask::getOutputMappings));

                task.getOutputMappings().convention(
                    this.provideMappingsBuildFile("mappings-" + Classifiers.INTERMEDIARY + "." + Extensions.TINY)
                );
            }
        );

        final var intermediaryV2MappingsJar = tasks.register(
            IntermediaryMappingsV2JarTask.INTERMEDIARY_V_2_MAPPINGS_JAR_TASK_NAME,
            IntermediaryMappingsV2JarTask.class,
            quiltExt.getUnpickVersion()
        );
        intermediaryV2MappingsJar.configure(task -> {
            task.getMappings().convention(removeIntermediary.flatMap(RemoveIntermediaryTask::getOutputMappings));

            task.getArchiveClassifier().convention(Classifiers.INTERMEDIARY_V2);
        });

        final var intermediaryV2MergedMappingsJar = tasks.register(
            IntermediaryMappingsV2JarTask.INTERMEDIARY_V_2_MERGED_MAPPINGS_JAR_TASK_NAME,
            IntermediaryMappingsV2JarTask.class,
            quiltExt.getUnpickVersion()
        );
        intermediaryV2MergedMappingsJar.configure(task -> {
            task.getMappings().convention(mergeIntermediary.flatMap(MergeIntermediaryTask::getOutputMappings));

            task.getArchiveClassifier().convention(Classifiers.INTERMEDIARY_V2_MERGED);
        });

        tasks.register(
            BuildIntermediaryTask.BUILD_INTERMEDIARY_TASK_NAME,
            BuildIntermediaryTask.class,
            task -> {
                task.dependsOn(intermediaryV2MappingsJar, intermediaryV2MergedMappingsJar);
            }
        );

        return project.getExtensions().create(
            MapIntermediaryExtension.NAME, MapIntermediaryExtension.class, intermediaryFile
        );
    }
}
