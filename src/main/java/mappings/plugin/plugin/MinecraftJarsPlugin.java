package mappings.plugin.plugin;

import org.gradle.api.Project;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import mappings.plugin.constants.Extensions;
import mappings.plugin.extension.MinecraftJarsExtension;
import mappings.plugin.extension.MappingsExtension;
import mappings.plugin.plugin.abstraction.DefaultExtensionedMappingsProjectPlugin;
import mappings.plugin.task.VersionParserConsumingTask;
import mappings.plugin.task.setup.DownloadMinecraftJarsTask;
import mappings.plugin.task.setup.DownloadMinecraftLibrariesTask;
import mappings.plugin.task.setup.DownloadWantedVersionManifestTask;
import mappings.plugin.task.setup.ExtractServerJarTask;
import mappings.plugin.task.setup.MergeJarsTask;
import mappings.plugin.util.serializable.SerializableVersionEntry;
import mappings.plugin.util.serializable.VersionParser;

/**
 * {@linkplain TaskContainer#register Registers} tasks that download and extract
 * Minecraft's client, server, and library jars.
 * <p>
 * Additionally:
 * <ul>
 *     <li> {@link TaskContainer#register registers} {@value MergeJarsTask#MERGE_JARS_TASK_NAME}
 *          which merges the client and server jars
 *     <li> {@linkplain org.gradle.api.tasks.TaskCollection#configureEach configures} the default value of
 *          {@link VersionParserConsumingTask}s'
 *          {@link VersionParserConsumingTask#getVersionParser versionParser} to
 *          {@value DownloadWantedVersionManifestTask#DOWNLOAD_WANTED_VERSION_MANIFEST_TASK_NAME}'s
 *          {@linkplain DownloadWantedVersionManifestTask#provideVersionParser provided}
 *          {@link VersionParser}
 * </ul>
 */
public abstract class MinecraftJarsPlugin extends DefaultExtensionedMappingsProjectPlugin<MinecraftJarsExtension> {
    @Override
    protected MinecraftJarsExtension applyImpl(@NotNull Project project) {
        final PluginContainer plugins = project.getPlugins();

        final MappingsExtension quiltExt = plugins.apply(MappingsBasePlugin.class).getExt();

        final TaskContainer tasks = project.getTasks();

        {
            final var downloadWantedVersionManifest = tasks.register(
                DownloadWantedVersionManifestTask.DOWNLOAD_WANTED_VERSION_MANIFEST_TASK_NAME,
                DownloadWantedVersionManifestTask.class,
                task -> {
                    task.getManifestVersion().convention(
                        this.getProviders().of(
                            SerializableVersionEntry.Source.class,
                            spec -> spec.parameters(params -> {
                                params.getUrl().set(
                                    "https://piston-meta.mojang.com/mc/game/version_manifest_v2." + Extensions.JSON
                                );

                                params.getVersion().set(quiltExt.getMinecraftVersion());
                            })
                        )
                    );

                    task.getDest().convention(
                        this.provideMinecraftBuildFile(quiltExt.provideSuffixedMinecraftVersion("." + Extensions.JSON))
                    );
                }
            );

            // provideVersionParser is already cached in a property
            tasks.withType(VersionParserConsumingTask.class).configureEach(task -> {
                task.getVersionParser().convention(
                    // versionParser
                    downloadWantedVersionManifest.flatMap(DownloadWantedVersionManifestTask::provideVersionParser)
                );
            });
        }

        final var downloadMinecraftJars = tasks.register(
            DownloadMinecraftJarsTask.DOWNLOAD_MINECRAFT_JARS_TASK_NAME,
            DownloadMinecraftJarsTask.class,
            task -> {
                task.getClientJar().convention(this.provideMinecraftBuildFile(
                    quiltExt.provideSuffixedMinecraftVersion("-client." + Extensions.JAR)
                ));

                task.getServerBootstrapJar().convention(this.provideMinecraftBuildFile(
                    quiltExt.provideSuffixedMinecraftVersion("-server-bootstrap." + Extensions.JAR)
                ));
            }
        );

        final var extractServerJar = tasks.register(
            ExtractServerJarTask.EXTRACT_SERVER_JAR_TASK_NAME,
            ExtractServerJarTask.class,
            task -> {
                task.getZippedFile().convention(
                    downloadMinecraftJars.flatMap(DownloadMinecraftJarsTask::getServerBootstrapJar)
                );

                task.getExtractionDest().convention(
                    this.provideMinecraftBuildFile(quiltExt.provideSuffixedMinecraftVersion("-server." + Extensions.JAR))
                );
            }
        );

        final var mergeJars = tasks.register(
            MergeJarsTask.MERGE_JARS_TASK_NAME,
            MergeJarsTask.class,
            task -> {
                task.getClientJar().convention(downloadMinecraftJars.flatMap(DownloadMinecraftJarsTask::getClientJar));

                task.getServerJar().convention(extractServerJar.flatMap(ExtractServerJarTask::getExtractionDest));

                task.getMergedFile().convention(
                    this.provideMinecraftBuildFile(quiltExt.provideSuffixedMinecraftVersion("-merged." + Extensions.JAR))
                );
            }
        );

        final var downloadMinecraftLibraries = tasks.register(
            DownloadMinecraftLibrariesTask.DOWNLOAD_MINECRAFT_LIBRARIES_TASK_NAME,
            DownloadMinecraftLibrariesTask.class,
            task -> {
                task.getLibrariesDir().convention(this.getMinecraftBuildDir().map(dir -> dir.dir("libraries")));
            }
        );

        return project.getExtensions().create(
            MinecraftJarsExtension.NAME, MinecraftJarsExtension.class,
            new Tasks(mergeJars, downloadMinecraftLibraries)
        );
    }

    public record Tasks(
        TaskProvider<MergeJarsTask> mergeJars,
        TaskProvider<DownloadMinecraftLibrariesTask> downloadMinecraftLibraries
    ) { }
}
