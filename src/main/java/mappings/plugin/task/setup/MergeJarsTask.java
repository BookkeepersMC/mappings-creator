package mappings.plugin.task.setup;

import java.io.IOException;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import mappings.plugin.constants.Groups;
import mappings.plugin.plugin.MinecraftJarsPlugin;

import net.fabricmc.stitch.merge.JarMerger;

public abstract class MergeJarsTask extends DefaultTask {
    /**
     * {@linkplain org.gradle.api.tasks.TaskContainer#register Registered} by
     * {@link MinecraftJarsPlugin MinecraftJarsPlugin}.
     */
    public static final String MERGE_JARS_TASK_NAME = "mergeJars";

    @InputFile
    public abstract RegularFileProperty getClientJar();

    @InputFile
    public abstract RegularFileProperty getServerJar();

    @OutputFile
    public abstract RegularFileProperty getMergedFile();

    public MergeJarsTask() {
        this.setGroup(Groups.SETUP);
    }

    @TaskAction
    public void mergeJars() throws IOException {
        this.getLogger().lifecycle(":merging jars");

        if (this.getMergedFile().get().getAsFile().exists()) {
            return;
        }

        try (JarMerger jarMerger = new JarMerger(
            this.getClientJar().get().getAsFile(),
            this.getServerJar().get().getAsFile(),
            this.getMergedFile().get().getAsFile()
        )) {
            jarMerger.merge();
        }
    }
}
