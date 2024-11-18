package mappings.plugin.task.setup;

import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.bundling.Jar;
import mappings.plugin.constants.Classifiers;
import mappings.plugin.constants.Groups;
import mappings.plugin.plugin.ProcessMappingsPlugin;

public abstract class JavadocJarTask extends Jar {
    /**
     * {@linkplain TaskContainer#register Registered} by {@link ProcessMappingsPlugin}.
     */
    public static final String JAVADOC_JAR_TASK_NAME = "javadocJar";

    public JavadocJarTask() {
        this.setGroup(Groups.JAVADOC_GENERATION);

        this.getArchiveClassifier().convention(Classifiers.JAVADOC);
    }
}
