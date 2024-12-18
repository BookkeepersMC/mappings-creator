package mappings.plugin.task.build;

import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.adapter.MappingDstNsReorder;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskContainer;
import org.jetbrains.annotations.VisibleForTesting;
import mappings.plugin.constants.Namespaces;
import mappings.plugin.mappingio.DoubleNsCompleterVisitor;
import mappings.plugin.mappingio.UnmappedNameRemoverVisitor;
import mappings.plugin.plugin.MapIntermediaryPlugin;
import mappings.plugin.task.setup.IntermediaryDependantTask;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * @see MapIntermediaryPlugin MapIntermediaryPlugin's configureEach
 */
public abstract class MergeIntermediaryTask extends AbstractTinyMergeTask implements IntermediaryDependantTask {
    /**
     * {@linkplain TaskContainer#register Registered} by {@link MapIntermediaryPlugin}.
     */
    public static final String MERGE_INTERMEDIARY_TASK_NAME = "mergeIntermediary";

    @InputFile
    public abstract RegularFileProperty getMergedTinyMappings();

    public MergeIntermediaryTask() {
        super(Namespaces.INTERMEDIARY, Namespaces.INTERMEDIATE);
    }

    @Override
    public void mergeMappings() throws IOException {
        this.mergeMappings(this.getMergedTinyMappings().get().getAsFile());
    }

    @Override
    protected MappingVisitor getFirstVisitor(MappingVisitor next) {
        return firstVisitor(next);
    }

    @Override
    protected MappingVisitor getPreWriteVisitor(MappingVisitor writer) {
        return preWriteVisitor(writer);
    }

    @VisibleForTesting
    public static void mergeMappings(
        Path intermediaryMappings, Path mergeTinyV2Output, Path outputMappings
    ) throws IOException {
        AbstractTinyMergeTask.mergeMappings(intermediaryMappings, mergeTinyV2Output, outputMappings,
            MergeIntermediaryTask::firstVisitor,
            MergeIntermediaryTask::preWriteVisitor
        );
    }

    private static MappingVisitor firstVisitor(MappingVisitor next) {
        // Copy unobfuscated names to the named namespace, since intermediary would override them
        return new DoubleNsCompleterVisitor(
            // Fix bug when intermediary doesn't have a mapping but hashed does
            // (i.e. `net/minecraft/client/main/Main$2`)
            new DoubleNsCompleterVisitor(
                new UnmappedNameRemoverVisitor(next, Namespaces.NAMED, Namespaces.INTERMEDIATE),
                // Copy names from `official` to `named` if `intermediary` is empty
                Namespaces.NAMED,
                Namespaces.INTERMEDIARY,
                Namespaces.OFFICIAL
            ),
            // Copy names from `official` to `named` if `hashed` is empty
            Namespaces.NAMED,
            Namespaces.INTERMEDIATE,
            Namespaces.OFFICIAL
        );
    }

    private static MappingVisitor preWriteVisitor(MappingVisitor writer) {
        return new MappingDstNsReorder(
            writer,
            // Remove hashed namespace
            List.of(Namespaces.INTERMEDIARY, Namespaces.NAMED)
        );
    }
}
