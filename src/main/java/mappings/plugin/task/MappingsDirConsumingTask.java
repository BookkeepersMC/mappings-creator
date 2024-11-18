package mappings.plugin.task;

import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputDirectory;
import mappings.plugin.plugin.MappingsBasePlugin;

/**
 * A task that takes a directory containing mappings as input.
 *
 * @see MappingsBasePlugin QuiltMappingsBasePlugin's configureEach
 */
public interface MappingsDirConsumingTask extends Task {
    @InputDirectory
    DirectoryProperty getMappingsDir();
}
