package mappings.plugin.task.mappings;

import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.Internal;
import mappings.plugin.plugin.MappingsBasePlugin;
import mappings.plugin.task.MappingsDirConsumingTask;

/**
 * A task that outputs mappings to the {@linkplain #getMappingsDir() mappings directory}.
 * <p>
 * All tasks that output to the mappings directory should implement this interface so that
 * {@link MappingsBasePlugin} adds their outputs to the inputs of
 * {@link MappingsDirConsumingTask MappingsDirConsumingTask}s.
 * <p>
 * An implementing task should <i>only</i> output to files within {@link #getMappingsDir() mappingsDir} and should
 * <b>not</b> output to the whole directory unless it is an {@link org.gradle.api.tasks.UntrackedTask @UntrackedTask}
 * whose output is not intended for consumption by other tasks.
 *
 * @see MappingsBasePlugin QuiltMappingsBasePlugin's configureEach
 */
public interface MappingsDirOutputtingTask extends Task {
    @Internal(
        """
        This is only used to resolve relative output paths against.
        A task should not add the whole directory to its output unless the task is
        untracked and its output is not intended for consumption by other tasks.
        """
    )
    DirectoryProperty getMappingsDir();
}
