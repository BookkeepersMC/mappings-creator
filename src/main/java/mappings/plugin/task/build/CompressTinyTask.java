package mappings.plugin.task.build;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskContainer;
import mappings.plugin.constants.Extensions;
import mappings.plugin.constants.Groups;
import mappings.plugin.plugin.MapMinecraftJarsPlugin;
import mappings.plugin.plugin.MappingsBasePlugin;
import mappings.plugin.task.ArtifactFileTask;
import mappings.plugin.task.MappingsArtifactTask;

/**
 * Compresses the input {@link #getMappings() mappings} using GZip.
 *
 * @see MappingsBasePlugin QuiltMappingsBasePlugin's configureEach
 */
public abstract class CompressTinyTask extends DefaultTask implements ArtifactFileTask, MappingsArtifactTask {
    /**
     * {@linkplain TaskContainer#register Registered} by {@link MapMinecraftJarsPlugin}.
     */
    public static final String COMPRESS_TINY_TASK_NAME = "compressTiny";

    public static final String DEFAULT_EXTENSION = Extensions.GZ;

    @InputFile
    public abstract RegularFileProperty getMappings();

    public CompressTinyTask() {
        this.setGroup(Groups.BUILD_MAPPINGS);

        this.getArtifactExtension().convention(DEFAULT_EXTENSION);
    }

    @TaskAction
    public void compressTiny() throws IOException {
        this.getLogger().lifecycle(":compressing tiny mappings");

        try (
            final var outputStream =
                new GZIPOutputStream(new FileOutputStream(this.getArtifactFile().get().getAsFile()));
            final var inputStream = new FileInputStream(this.getMappings().get().getAsFile())
        ) {
            final byte[] buffer = new byte[1024];

            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.finish();
        }
    }
}
