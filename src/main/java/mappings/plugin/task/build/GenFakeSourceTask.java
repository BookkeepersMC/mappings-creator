package mappings.plugin.task.build;

import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskContainer;
import mappings.plugin.plugin.ProcessMappingsPlugin;
import mappings.plugin.task.decompile.DecompileVineflowerTask;

import java.io.IOException;
import java.util.Map;

public abstract class GenFakeSourceTask extends DecompileVineflowerTask {
    /**
     * {@linkplain TaskContainer#register Registered} by {@link ProcessMappingsPlugin}.
     */
    public static final String GEN_FAKE_SOURCE_TASK_NAME = "genFakeSource";

    public GenFakeSourceTask() {
        this.getDecompilerOptions().putAll(Map.of(
            // remove synthetics
            "rsy", "1",
            // decompile generic signatures
            "dgs", "1",
            // pll (Preferred Line Length) is length for line wrapping
            "pll", "99999"
        ));
    }

    @Override
    @TaskAction
    public void decompile() throws IOException {
        super.decompile();

        this.getLogger().lifecycle(":Fake source generated");
    }
}
