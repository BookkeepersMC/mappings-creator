package mappings.plugin.task.jarmapping;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import mappings.plugin.plugin.MapMinecraftJarsPlugin;
import mappings.plugin.util.JarRemapper;

/**
 * Copies a {@linkplain #getInputJar() Jar} and applies {@linkplain #getMappingsFile() mappings}.
 *
 * @see MapMinecraftJarsPlugin MapMinecraftJarsPlugin's configureEach
 */
public abstract class MapJarTask extends DefaultTask {
    public static final ImmutableMap<String, String> JAVAX_TO_JETBRAINS = ImmutableMap.of(
        "javax/annotation/Nullable", "org/jetbrains/annotations/Nullable",
        "javax/annotation/Nonnull", "org/jetbrains/annotations/NotNull",
        "javax/annotation/concurrent/Immutable", "org/jetbrains/annotations/Unmodifiable"
    );

    @Input
    public abstract MapProperty<String, String> getAdditionalMappings();

    @InputFile
    public abstract RegularFileProperty getInputJar();

    @InputFile
    public abstract RegularFileProperty getMappingsFile();

    @InputDirectory
    public abstract DirectoryProperty getLibrariesDir();

    @OutputFile
    public abstract RegularFileProperty getOutputJar();

    private final String from, to;

    public MapJarTask(String group, String from, String to) {
        this.setGroup(group);
        this.from = from;
        this.to = to;
    }

    @TaskAction
    public void remapJar() {
        this.getLogger().lifecycle(":mapping minecraft from " + this.from + " to " + this.to);

        final Map<String, String> additionalMappings = this.getAdditionalMappings().get();

        JarRemapper.mapJar(
            this.getOutputJar().get().getAsFile(),
            this.getInputJar().get().getAsFile(),
            this.getMappingsFile().get().getAsFile(),
            this.getLibrariesDir().get().getAsFile(),
            this.from, this.to,
            builder -> builder.withMappings(out -> additionalMappings.forEach(out::acceptClass))
        );
    }
}
