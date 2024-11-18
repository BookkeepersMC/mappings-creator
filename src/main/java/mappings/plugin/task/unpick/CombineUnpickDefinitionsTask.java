package mappings.plugin.task.unpick;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

import daomephsta.unpick.constantmappers.datadriven.parser.v2.UnpickV2Reader;
import daomephsta.unpick.constantmappers.datadriven.parser.v2.UnpickV2Writer;
import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkerExecutor;
import org.jetbrains.annotations.VisibleForTesting;
import mappings.plugin.constants.Groups;
import mappings.plugin.plugin.MapV2Plugin;
import mappings.plugin.util.UnpickUtil;

/**
 * Combines many unpick definition files into the {@link #getOutput() output}.
 *
 * @see mappings.plugin.task.unpick.gen.UnpickGenTask UnpickGenTask
 */
public abstract class CombineUnpickDefinitionsTask extends DefaultTask {
    /**
     * {@linkplain TaskContainer#register Registered} by {@link MapV2Plugin}.
     */
    public static final String COMBINE_UNPICK_DEFINITIONS_TASK_NAME = "combineUnpickDefinitions";

    @InputFiles
    public abstract ConfigurableFileCollection getUnpickDefinitions();

    @OutputFile
    public abstract RegularFileProperty getOutput();

    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

    public CombineUnpickDefinitionsTask() {
        this.setGroup(Groups.UNPICK);
    }

    @TaskAction
    public void run() {
        this.getWorkerExecutor().noIsolation().submit(CombineAction.class, parameters -> {
            parameters.getInput().from(this.getUnpickDefinitions());
            parameters.getOutput().set(this.getOutput());
        });
    }

    @VisibleForTesting
    public static void combineUnpickDefinitions(Collection<File> input, Path output) {
        try {
            final UnpickV2Writer writer = new UnpickV2Writer();

            input.stream()
                .filter(file -> file.getName().endsWith(".unpick"))
                // Sort inputs to get reproducible outputs (also for testing)
                .sorted(Comparator.comparing(File::getName))
                .forEach(file -> {
                    try (UnpickV2Reader reader = new UnpickV2Reader(new FileInputStream(file))) {
                        reader.accept(writer);
                    } catch (IOException e) {
                        throw new GradleException("Failed to read unpick definition", e);
                    }
                });

            Files.deleteIfExists(output);

            Files.writeString(output, UnpickUtil.getLfOutput(writer));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public interface CombineParameters extends WorkParameters {
        @InputFiles
        ConfigurableFileCollection getInput();

        @OutputFile
        RegularFileProperty getOutput();
    }

    public abstract static class CombineAction implements WorkAction<CombineParameters> {
        @Inject
        public CombineAction() { }

        @Override
        public void execute() {
            final Set<File> input = this.getParameters().getInput()
                // this is to flatten the contents of directories
                .getAsFileTree()
                .getFiles();
            final Path output = this.getParameters().getOutput().getAsFile().get().toPath();
            combineUnpickDefinitions(input, output);
        }
    }
}
