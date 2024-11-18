package mappings.plugin.task.build;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.jvm.tasks.Jar;
import mappings.plugin.constants.Extensions;
import mappings.plugin.constants.Groups;
import mappings.plugin.plugin.MapMinecraftJarsPlugin;
import mappings.plugin.plugin.MappingsBasePlugin;
import mappings.plugin.task.MappingsArtifactTask;

/**
 * Creates a jar file with the input {@link #getMappings() mappings} located at {@value JAR_MAPPINGS_PATH}.
 *
 * @see MappingsBasePlugin QuiltMappingsBasePlugin's configureEach
 */
public abstract class TinyJarTask extends Jar implements MappingsArtifactTask {
    /**
     * {@linkplain TaskContainer#register Registered} by {@link MapMinecraftJarsPlugin}.
     */
    public static final String TINY_JAR_TASK_NAME = "tinyJar";

    public static final String JAR_MAPPINGS_PATH = "mappings/mappings." + Extensions.TINY;

    @InputFile
    public abstract RegularFileProperty getMappings();

    public TinyJarTask() {
        this.setGroup(Groups.BUILD_MAPPINGS);

        this.from(this.getMappings()).rename(original -> JAR_MAPPINGS_PATH);
    }
}
