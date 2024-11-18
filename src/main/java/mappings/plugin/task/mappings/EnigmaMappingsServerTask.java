package mappings.plugin.task.mappings;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.options.Option;
import org.quiltmc.enigma.network.DedicatedEnigmaServer;
import mappings.plugin.plugin.EnigmaMappingsPlugin;
import mappings.plugin.plugin.MappingsBasePlugin;

import java.util.ArrayList;
import java.util.List;

import static mappings.plugin.util.ProviderUtil.toOptional;

/**
 * Starts a {@link DedicatedEnigmaServer}.
 * <p>
 * Optional inputs will be passed as command line args when starting the server if present.
 * <p>
 * Additional args can be specified when invoking the task using the
 * {@linkplain JavaExec#setArgsString(String) --args} option.
 *
 * @see MappingsBasePlugin QuiltMappingsBasePlugin's configureEach
 * @see EnigmaMappingsPlugin EnigmaMappingsPlugin's configureEach[es]
 */
public abstract class EnigmaMappingsServerTask extends AbstractEnigmaMappingsTask {
	/**
	 * {@linkplain TaskContainer#register Registered} by {@link EnigmaMappingsPlugin}.
	 */
	public static final String MAPPINGS_SERVER_TASK_NAME = "mappingsServer";
	/**
	 * {@linkplain TaskContainer#register Registered} by {@link EnigmaMappingsPlugin}.
	 */
	public static final String MAPPINGS_UNPICKED_SERVER_TASK_NAME = "mappingsUnpickedServer";

	public static final String PORT_OPTION = "port";
	public static final String PASSWORD_OPTION = "password";
	public static final String LOG_OPTION = "log";

    @Optional
	@Option(option = PORT_OPTION, description = "The port the Enigma server will run on.")
	@Input
	public abstract Property<String> getPort();

	@Optional
	@Option(option = PASSWORD_OPTION, description = "The password for the Enigma server.")
	@Input
	public abstract Property<String> getPassword();

	@Optional
	@Option(option = LOG_OPTION, description = "The path the Enigma server will write its log to.")
	@OutputFile
	public abstract RegularFileProperty getLog();

	public EnigmaMappingsServerTask() {
		this.getMainClass().set(DedicatedEnigmaServer.class.getName());
		this.getMainClass().finalizeValue();

		this.getArgumentProviders().add(() -> {
			final List<String> optionalArgs = new ArrayList<>();

			toOptional(this.getPort()).ifPresent(port -> {
				optionalArgs.add("-" + PORT_OPTION);
				optionalArgs.add(port);
			});

			toOptional(this.getPassword()).ifPresent(password -> {
				optionalArgs.add("-" + PASSWORD_OPTION);
				optionalArgs.add(password);
			});

			toOptional(this.getLog().getAsFile()).ifPresent(log -> {
				optionalArgs.add("-" + LOG_OPTION);
				optionalArgs.add(log.getAbsolutePath());
			});

			return optionalArgs;
		});
	}
}
