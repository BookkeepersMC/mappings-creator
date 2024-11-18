package mappings.plugin.task.setup;

import mappings.plugin.constants.Extensions;
import mappings.plugin.constants.Groups;
import mappings.plugin.plugin.MinecraftJarsPlugin;
import mappings.plugin.task.ExtractSingleZippedFileTask;

public abstract class ExtractServerJarTask extends ExtractSingleZippedFileTask {
    /**
     * {@linkplain org.gradle.api.tasks.TaskContainer#register Registered} by
     * {@link MinecraftJarsPlugin MinecraftJarsPlugin}.
     */
    public static final String EXTRACT_SERVER_JAR_TASK_NAME = "extractServerJar";

    public static final String SERVER_JAR_PATTERN = "META-INF/versions/*/server-*." + Extensions.JAR;

    public ExtractServerJarTask() {
        super(filterable -> filterable.include(SERVER_JAR_PATTERN));

        this.setGroup(Groups.SETUP);
    }
}
