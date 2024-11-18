package mappings.plugin.task.unpick;

import java.io.File;
import java.util.stream.Stream;

import com.google.common.collect.Streams;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskContainer;
import mappings.plugin.constants.Groups;
import mappings.plugin.plugin.MapV2Plugin;

/**
 * Unpicks a jar file using {@link daomephsta.unpick.cli.Main}.
 *
 * @see MapV2Plugin MapV2Plugin's configureEach
 */
public abstract class UnpickJarTask extends JavaExec {
    /**
     * {@linkplain TaskContainer#register Registered} by {@link MapV2Plugin}.
     */
    public static final String UNPICK_HASHED_JAR_TASK_NAME = "unpickHashedJar";

    @InputFile
    public abstract RegularFileProperty getInputFile();

    @InputFile
    public abstract RegularFileProperty getUnpickDefinition();

    @InputFile
    public abstract RegularFileProperty getUnpickConstantsJar();

    @InputFiles
    public abstract ConfigurableFileCollection getDecompileClasspathFiles();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    public UnpickJarTask() {
        this.setGroup(Groups.UNPICK);

        this.getMainClass().set(daomephsta.unpick.cli.Main.class.getName());
        this.getMainClass().finalizeValue();

        this.getArgumentProviders().add(() ->
            Streams
                .concat(
                    Stream
                        .of(
                            this.getInputFile(), this.getOutputFile(),
                            this.getUnpickDefinition(), this.getUnpickConstantsJar()
                        )
                        .map(Provider::get)
                        .map(FileSystemLocation::getAsFile),
                    this.getDecompileClasspathFiles().getAsFileTree().getFiles().stream()
                )
                .map(File::getAbsolutePath)
                .toList()
        );
    }
}
