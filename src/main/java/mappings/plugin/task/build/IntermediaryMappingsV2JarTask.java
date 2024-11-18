package mappings.plugin.task.build;

import org.gradle.api.tasks.TaskContainer;
import mappings.plugin.constants.Constants;
import mappings.plugin.plugin.MapIntermediaryPlugin;
import mappings.plugin.plugin.MapV2Plugin;
import mappings.plugin.plugin.MappingsBasePlugin;
import mappings.plugin.task.setup.IntermediaryDependantTask;

import javax.inject.Inject;

/**
 * Creates a jar file containing {@value Constants#INTERMEDIARY_MAPPINGS_NAME} mappings in Quilt's v2 mapping format.
 *
 * @see MappingsBasePlugin QuiltMappingsBasePlugin's configureEach
 * @see MapIntermediaryPlugin MapIntermediaryPlugin's configureEach
 * @see MapV2Plugin MapV2Plugin's configureEach
 */
public abstract class IntermediaryMappingsV2JarTask extends MappingsV2JarTask implements IntermediaryDependantTask {
    /**
     * {@linkplain TaskContainer#register Registered} by {@link MapIntermediaryPlugin}.
     */
    public static final String INTERMEDIARY_V_2_MAPPINGS_JAR_TASK_NAME = "intermediaryV2MappingsJar";
    /**
     * {@linkplain TaskContainer#register Registered} by {@link MapIntermediaryPlugin}.
     */
    public static final String INTERMEDIARY_V_2_MERGED_MAPPINGS_JAR_TASK_NAME = "intermediaryV2MergedMappingsJar";

    @Inject
    public IntermediaryMappingsV2JarTask(String unpickVersion) {
        super(unpickVersion);
    }
}
