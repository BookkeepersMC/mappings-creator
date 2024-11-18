package mappings.plugin.task;

import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.quiltmc.enigma.api.EnigmaProfile;
import mappings.plugin.plugin.MappingsBasePlugin;
import mappings.plugin.util.EnigmaProfileService;

/**
 * A task that takes an {@link EnigmaProfile} as input.
 *
 * @see MappingsBasePlugin QuiltMappingsBasePlugin's configureEach
 */
public interface EnigmaProfileConsumingTask extends Task {
    @Internal("@ServiceReference is @Incubating")
    Property<EnigmaProfileService> getEnigmaProfileService();

    /**
     * Don't parse this to create an {@link EnigmaProfile}, use the one provided by
     * {@link #getEnigmaProfileService() enigmaProfileService} instead.
     * <p>
     * This is exposed so it can be passed to external processes.
     */
    @InputFile
    RegularFileProperty getEnigmaProfileConfig();

    /**
     * Holds any files referenced by {@link #getEnigmaProfileService() enigmaProfileService}'s
     * {@link EnigmaProfileService#getProfile() profile}.
     * <p>
     * {@link EnigmaProfileConsumingTask}s may not access these files directly, but they affect Enigma's behavior,
     * so they must be considered for up-to-date checks.
     */
    @InputFiles
    ConfigurableFileCollection getProfileFileDependencies();
}
