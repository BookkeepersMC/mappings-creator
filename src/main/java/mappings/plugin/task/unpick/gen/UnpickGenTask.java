package mappings.plugin.task.unpick.gen;

import mappings.plugin.plugin.MappingsPlugin;
import org.gradle.api.Task;

/**
 * A task that outputs unpick files.
 * <p>
 * {@link MappingsPlugin QuiltMappingsPlugin} adds the
 * {@link org.gradle.api.Task#getOutputs() outputs} of all
 * {@code UnpickGenTask}s to
 * {@value mappings.plugin.task.unpick.CombineUnpickDefinitionsTask#COMBINE_UNPICK_DEFINITIONS_TASK_NAME}'s
 * {@link mappings.plugin.task.unpick.CombineUnpickDefinitionsTask#getUnpickDefinitions() unpickDefinitions},
 * so implementing tasks should <i>only</i> output unpick files.
 */
public interface UnpickGenTask extends Task { }
