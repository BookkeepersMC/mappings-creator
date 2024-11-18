package mappings.plugin.extension;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.VersionCatalogsExtension;
import org.gradle.api.artifacts.VersionConstraint;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.quiltmc.enigma.api.EnigmaProfile;
import mappings.plugin.constants.Constants;
import mappings.plugin.plugin.MappingsBasePlugin;
import mappings.plugin.task.EnigmaProfileConsumingTask;
import mappings.plugin.task.MappingsDirConsumingTask;
import mappings.plugin.task.mappings.MappingsDirOutputtingTask;
import mappings.plugin.util.EnigmaProfileService;

import javax.inject.Inject;

public abstract class MappingsExtension {
    /**
     * {@linkplain ExtensionContainer#create Created} by {@link MappingsBasePlugin}.
     */
    public static final String NAME = "mappingsCreator";

    private static final String DEFAULT_CATALOG_NAME = "libs";

    public abstract Property<String> getMinecraftVersion();

    public abstract Property<String> getMappingsVersion();

    private String name = "mappings-project";

    /**
     * @see MappingsDirOutputtingTask
     * @see MappingsDirConsumingTask
     * @see MappingsBasePlugin
     */
    public abstract DirectoryProperty getMappingsDir();

    /**
     * Don't parse this to create an {@link EnigmaProfile}, use the
     * {@value EnigmaProfileService#ENIGMA_PROFILE_SERVICE_NAME} service's
     * {@link EnigmaProfileService#getProfile() profile} instead.
     * <p>
     * This should only be passed to external processes which can't accept an {@link EnigmaProfile} instance.
     *
     * @see EnigmaProfileConsumingTask
     * @see MappingsBasePlugin
     */
    public abstract RegularFileProperty getEnigmaProfileConfig();

    public abstract RegularFileProperty getUnpickMeta();

    private final String unpickVersion;

    public String getUnpickVersion() {
        return this.unpickVersion;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Inject
    public MappingsExtension(Project project) {
        this.unpickVersion = project.getExtensions().getByType(VersionCatalogsExtension.class)
            .named(DEFAULT_CATALOG_NAME)
            .findVersion(Constants.UNPICK_NAME)
            .map(VersionConstraint::getRequiredVersion)
            .orElseThrow(() -> new GradleException(
                """
                Could not find %s version.
                \tAn '%s' version must be specified in the '%s' version catalog,
                \tusually by adding it to 'gradle/%s.versions.toml'.
                """.formatted(Constants.UNPICK_NAME, Constants.UNPICK_NAME, DEFAULT_CATALOG_NAME, DEFAULT_CATALOG_NAME)
            ));
    }

    public Provider<String> provideSuffixedMinecraftVersion(String suffix) {
        return this.getMinecraftVersion().map(version -> version + suffix);
    }
}
