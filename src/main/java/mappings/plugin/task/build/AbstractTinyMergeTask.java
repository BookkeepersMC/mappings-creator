package mappings.plugin.task.build;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.VisibleForTesting;
import mappings.plugin.constants.Constants;
import mappings.plugin.constants.Groups;
import mappings.plugin.constants.Namespaces;
import mappings.plugin.mappingio.CompleteInitializersVisitor;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.format.tiny.Tiny2FileWriter;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public abstract class AbstractTinyMergeTask extends DefaultTask {
    @InputFile
    public abstract RegularFileProperty getInput();

    @OutputFile
    public abstract RegularFileProperty getOutputMappings();

    protected final String mergeName;
    protected final String fillName;

    public AbstractTinyMergeTask(String mergeName) {
        this(mergeName, mergeName);
    }

    public AbstractTinyMergeTask(String mergeName, String fillName) {
        this.setGroup(Groups.BUILD_MAPPINGS);

        this.mergeName = mergeName;
        this.fillName = fillName;
    }

    @TaskAction
    public abstract void mergeMappings() throws IOException;

    protected void mergeMappings(File mergeTinyInput) throws IOException {
        final File mappingsTinyInput = this.getInput().get().getAsFile();

        this.getLogger().lifecycle(":merging {} and {}", Constants.MAPPINGS_NAME, this.mergeName);
        mergeMappings(
            mappingsTinyInput.toPath(), mergeTinyInput.toPath(),
            this.getOutputMappings().get().getAsFile().toPath(),
            this::getFirstVisitor, this::getPreWriteVisitor
        );
    }

    protected MappingVisitor getFirstVisitor(MappingVisitor next) {
        return next;
    }

    protected MappingVisitor getPreWriteVisitor(MappingVisitor writer) {
        return writer;
    }

    @VisibleForTesting
    public static void mergeMappings(
        Path mappingsTinyInput, Path mergeTinyInput, Path outputMappings,
        Function<MappingVisitor, MappingVisitor> firstVisitor,
        Function<MappingVisitor, MappingVisitor> preWriteVisitor
    ) throws IOException {
        final MemoryMappingTree tree = new MemoryMappingTree(false); // hashed is the src namespace
        MappingReader.read(mergeTinyInput, MappingFormat.TINY_2_FILE, tree);
        MappingReader.read(mappingsTinyInput, MappingFormat.TINY_2_FILE, tree);
        try (Tiny2FileWriter w = new Tiny2FileWriter(Files.newBufferedWriter(outputMappings), false)) {
            tree.accept(firstVisitor.apply(
                new CompleteInitializersVisitor(
                    new MappingSourceNsSwitch(
                        preWriteVisitor.apply(w),
                        Namespaces.OFFICIAL,
                        // Drop methods not in hashed
                        true
                    )
                )
            ));
        }
    }
}
