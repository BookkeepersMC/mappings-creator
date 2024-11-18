package mappings.plugin.task.setup;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.jvm.tasks.Jar;
import mappings.plugin.constants.Classifiers;
import mappings.plugin.constants.Groups;
import mappings.plugin.plugin.ProcessMappingsPlugin;

public abstract class SourcesJarTask extends Jar {
    /**
     * {@linkplain TaskContainer#register Registered} by {@link ProcessMappingsPlugin}.
     */
    public static final String SOURCES_JAR_TASK_NAME = "sourcesJar";

    @InputFiles
    public abstract ConfigurableFileCollection getSources();

    public SourcesJarTask() {
        this.setGroup(Groups.SETUP);

        this.getArchiveClassifier().convention(Classifiers.SOURCES);

        this.from(this.getSources());
    }
}
