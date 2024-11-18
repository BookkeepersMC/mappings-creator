package mappings.plugin.task.build;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import mappings.plugin.constants.Groups;

import java.io.IOException;

public abstract class AbstractHashedMergeTask extends AbstractTinyMergeTask {
    @InputFile
    public abstract RegularFileProperty getHashedTinyMappings();

    public AbstractHashedMergeTask() {
        super(Groups.INTERMEDIATE);
    }

    @Override
    public void mergeMappings() throws IOException {
        this.mergeMappings(this.getHashedTinyMappings().get().getAsFile());
    }
}
