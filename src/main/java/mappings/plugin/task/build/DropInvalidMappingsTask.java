package mappings.plugin.task.build;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskContainer;
import org.quiltmc.enigma.command.DropInvalidMappingsCommand;
import org.gradle.api.tasks.TaskAction;
import mappings.plugin.constants.Groups;
import mappings.plugin.plugin.MapMinecraftJarsPlugin;
import mappings.plugin.plugin.MappingsBasePlugin;
import mappings.plugin.task.MappingsDirConsumingTask;

/**
 * Removes any invalid mappings found in the passed {@link #getMappingsDir() mappingsDir}.
 * <p>
 * Invalid mappings are usually the result of differences between Minecraft versions.
 *
 * @see MappingsBasePlugin QuiltMappingsBasePlugin's configureEach
 */
public abstract class DropInvalidMappingsTask extends DefaultTask implements MappingsDirConsumingTask {
    /**
     * {@linkplain TaskContainer#register Registered} by {@link MapMinecraftJarsPlugin}.
     */
    public static final String DROP_INVALID_MAPPINGS_TASK_NAME = "dropInvalidMappings";

    @InputFile
    public abstract RegularFileProperty getPerVersionMappingsJar();

    public DropInvalidMappingsTask() {
        this.setGroup(Groups.BUILD_MAPPINGS);
    }

    @TaskAction
    public void dropInvalidMappings() {
        this.getLogger().info(":dropping invalid mappings");

        final String[] args = new String[]{
            this.getPerVersionMappingsJar().get().getAsFile().getAbsolutePath(),
            this.getMappingsDir().get().getAsFile().getAbsolutePath()
        };

        try {
            new DropInvalidMappingsCommand().run(args);
        } catch (Exception e) {
            throw new GradleException("Failed to drop mappings", e);
        }
    }
}
