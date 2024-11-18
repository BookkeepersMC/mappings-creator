package mappings.plugin.task.setup;

import org.gradle.api.tasks.TaskContainer;
import mappings.plugin.constants.Extensions;
import mappings.plugin.constants.Groups;
import mappings.plugin.plugin.MapMinecraftJarsPlugin;
import mappings.plugin.task.ExtractSingleZippedFileTask;

public abstract class ExtractTinyMappingsTask extends ExtractSingleZippedFileTask {
    /**
     * {@linkplain TaskContainer#register Registered} by {@link MapMinecraftJarsPlugin}.
     */
    public static final String EXTRACT_TINY_INTERMEDIATE_MAPPINGS_TASK_NAME = "extractTinyIntermediateMappings";

    private static final String TINY_MAPPINGS_PATTERN = "**/*mappings." + Extensions.TINY;

    public ExtractTinyMappingsTask() {
        super(filterable -> filterable.include(TINY_MAPPINGS_PATTERN));

        this.setGroup(Groups.SETUP);
    }
}
