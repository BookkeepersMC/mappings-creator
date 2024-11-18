package mappings.plugin.task.build;

import java.io.IOException;
import java.nio.file.Path;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskContainer;
import org.quiltmc.enigma.command.MapSpecializedMethodsCommand;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingParseException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.VisibleForTesting;
import mappings.plugin.constants.Groups;
import mappings.plugin.constants.Namespaces;
import mappings.plugin.plugin.MapMinecraftJarsPlugin;
import mappings.plugin.plugin.MappingsBasePlugin;
import mappings.plugin.task.MappingsDirConsumingTask;
import mappings.plugin.util.ProviderUtil;

/**
 * @see MappingsBasePlugin QuiltMappingsBasePlugin's configureEach
 */
public abstract class BuildMappingsTinyTask extends DefaultTask implements MappingsDirConsumingTask {
    /**
     * {@linkplain TaskContainer#register Registered} by {@link MapMinecraftJarsPlugin}.
     */
    public static final String BUILD_MAPPINGS_TINY_TASK_NAME = "buildMappingsTiny";

    @InputFile
    public abstract RegularFileProperty getPerVersionMappingsJar();

    @OutputFile
    public abstract RegularFileProperty getOutputMappings();

    public BuildMappingsTinyTask() {
        this.setGroup(Groups.BUILD_MAPPINGS);
    }

    @TaskAction
    public void execute() throws IOException, MappingParseException {
        this.getLogger().lifecycle(":generating tiny mappings");

        buildMappingsTiny(
            ProviderUtil.getPath(this.getPerVersionMappingsJar()),
            ProviderUtil.getPath(this.getMappingsDir()),
            ProviderUtil.getPath(this.getOutputMappings())
        );
    }

    @VisibleForTesting
    public static void buildMappingsTiny(
        Path perVersionMappingsJar, Path mappings, Path outputMappings
    ) throws IOException, MappingParseException {
        MapSpecializedMethodsCommand.run(
                perVersionMappingsJar,
                mappings,
                outputMappings,
                Namespaces.INTERMEDIATE,
                Namespaces.NAMED
        );
    }
}
