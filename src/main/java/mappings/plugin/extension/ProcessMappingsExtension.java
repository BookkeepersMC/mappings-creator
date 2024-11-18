package mappings.plugin.extension;

import mappings.plugin.extension.abstraction.DefaultTaskedExtension;
import mappings.plugin.plugin.ProcessMappingsPlugin;

public abstract class ProcessMappingsExtension extends DefaultTaskedExtension<ProcessMappingsPlugin.Tasks> {
    public static final String NAME = "processMappings";

    public ProcessMappingsExtension(ProcessMappingsPlugin.Tasks tasks) {
        super(tasks);
    }
}
