package mappings.plugin.task.build;

import java.util.Map;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.jvm.tasks.Jar;
import mappings.plugin.constants.Extensions;
import mappings.plugin.constants.Groups;
import mappings.plugin.plugin.MapV2Plugin;
import mappings.plugin.plugin.MappingsBasePlugin;
import mappings.plugin.task.MappingsArtifactTask;

import javax.inject.Inject;

import static mappings.plugin.constants.Constants.UNPICK_NAME;

/**
 * Creates a jar file with Quilt's v2 mapping format.
 *
 * @see MappingsBasePlugin QuiltMappingsBasePlugin's configureEach
 * @see MapV2Plugin MapV2Plugin's configureEach
 */
public abstract class MappingsV2JarTask extends Jar implements MappingsArtifactTask {
    /**
     * {@linkplain TaskContainer#register Registered} by {@link MapV2Plugin}.
     */
    public static final String V2_UNMERGED_MAPPINGS_JAR_TASK_NAME = "v2UnmergedMappingsJar";
    /**
     * {@linkplain TaskContainer#register Registered} by {@link MapV2Plugin}.
     */
    public static final String V2_MERGED_MAPPINGS_JAR_TASK_NAME = "v2MergedMappingsJar";

    private static final String EXTRAS_DIR = "extras/";
    public static final String JAR_UNPICK_META_PATH = EXTRAS_DIR + UNPICK_NAME + "." + Extensions.JSON;
    public static final String JAR_UNPICK_DEFINITION_PATH = EXTRAS_DIR + "definitions." + Extensions.UNPICK;
    public static final String JAR_MAPPINGS_PATH = TinyJarTask.JAR_MAPPINGS_PATH;

    @InputFile
    public abstract RegularFileProperty getUnpickMeta();

    @InputFile
    public abstract RegularFileProperty getUnpickDefinition();

    @InputFile
    public abstract RegularFileProperty getMappings();

    // unpick version can't be a property because it's used when the task is instantiated
    public final String unpickVersion;

    @Inject
    public MappingsV2JarTask(String unpickVersion) {
        this.setGroup(Groups.BUILD_MAPPINGS);

        this.unpickVersion = unpickVersion;

        this.from(this.getUnpickMeta(), copySpec -> {
            copySpec.expand(Map.of("version", this.unpickVersion));

            copySpec.rename(unused -> JAR_UNPICK_META_PATH);
        });

        this.from(this.getUnpickDefinition(), copySpec -> copySpec.rename(unused -> JAR_UNPICK_DEFINITION_PATH));

        this.from(this.getMappings(), copySpec -> copySpec.rename(unused -> JAR_MAPPINGS_PATH));
    }
}
