package mappings.plugin.task.jarmapping;

import org.gradle.api.tasks.TaskContainer;
import mappings.plugin.constants.Groups;
import mappings.plugin.constants.Namespaces;
import mappings.plugin.plugin.MapMinecraftJarsPlugin;
import mappings.plugin.plugin.MapV2Plugin;

/**
 * @see MapMinecraftJarsPlugin MapMinecraftJarsPlugin's configureEach
 */
public abstract class MapNamedJarTask extends MapJarTask {
    /**
     * {@linkplain TaskContainer#register Registered} by {@link MapV2Plugin}.
     */
    public static final String MAP_NAMED_JAR_TASK_NAME = "mapNamedJar";

    public MapNamedJarTask() {
        super(Groups.MAP_JAR, Namespaces.INTERMEDIATE, Namespaces.NAMED);

        this.getAdditionalMappings().putAll(JAVAX_TO_JETBRAINS);
    }
}
