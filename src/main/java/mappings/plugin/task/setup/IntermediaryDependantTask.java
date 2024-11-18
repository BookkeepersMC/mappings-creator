package mappings.plugin.task.setup;

import org.gradle.api.Task;
import mappings.plugin.constants.Namespaces;
import mappings.plugin.plugin.MapIntermediaryPlugin;

/**
 * A task that depends on {@value Namespaces#INTERMEDIARY} mappings for the current Minecraft version.
 *
 * @see MapIntermediaryPlugin MapIntermediaryPlugin's configureEach
 */
public interface IntermediaryDependantTask extends Task { }
