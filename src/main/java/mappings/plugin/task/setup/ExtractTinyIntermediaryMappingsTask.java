package mappings.plugin.task.setup;

import org.gradle.api.tasks.TaskContainer;
import mappings.plugin.plugin.MapIntermediaryPlugin;

/**
 * @see MapIntermediaryPlugin MapIntermediaryPlugin's configureEach
 */
public abstract class ExtractTinyIntermediaryMappingsTask extends ExtractTinyMappingsTask
        implements IntermediaryDependantTask {
    /**
     * {@linkplain TaskContainer#register Registered} by {@link MapIntermediaryPlugin}.
     */
    public static final String EXTRACT_TINY_INTERMEDIARY_MAPPINGS_TASK_NAME = "extractTinyIntermediaryMappings";
}
