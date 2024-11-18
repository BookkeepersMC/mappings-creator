package mappings.plugin.task;

import org.gradle.api.Task;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import mappings.plugin.plugin.MappingsBasePlugin;

/**
 * A task that creates an artifact whose name uses Quilt Mappings' name and version.
 * <p>
 * Has no effect if the implementing task isn't a subclass of either
 * {@link ArtifactFileTask} or {@link AbstractArchiveTask}.
 *
 * @see MappingsBasePlugin QuiltMappingsBasePlugin's configureEach
 */
public interface MappingsArtifactTask extends Task {
    static boolean isInstance(Task task) {
        return task instanceof MappingsArtifactTask;
    }
}
