package mappings.plugin.task.setup;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.jvm.tasks.Jar;
import mappings.plugin.constants.Classifiers;
import mappings.plugin.constants.Groups;
import mappings.plugin.plugin.MapV2Plugin;

public abstract class ConstantsJarTask extends Jar {
    /**
     * {@linkplain TaskContainer#register Registered} by {@link MapV2Plugin}.
     */
    public static final String CONSTANTS_JAR_TASK_NAME = "constantsJar";

    @InputFiles
    public abstract ConfigurableFileCollection getConstants();

    public ConstantsJarTask() {
        this.setGroup(Groups.SETUP);

        this.getArchiveClassifier().convention(Classifiers.CONSTANTS);

        this.from(this.getConstants());
    }
}
