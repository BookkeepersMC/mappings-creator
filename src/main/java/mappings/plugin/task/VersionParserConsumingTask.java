package mappings.plugin.task;

import org.gradle.api.Task;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import mappings.plugin.plugin.MinecraftJarsPlugin;
import mappings.plugin.util.serializable.VersionParser;

/**
 * A task that takes {@link VersionParser} as input.
 *
 * @see MinecraftJarsPlugin MinecraftJarsPlugin's configureEach
 */
public interface VersionParserConsumingTask extends Task {
    @Input
    Property<VersionParser> getVersionParser();
}
