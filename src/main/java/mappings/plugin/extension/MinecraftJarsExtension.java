package mappings.plugin.extension;

import mappings.plugin.extension.abstraction.DefaultTaskedExtension;
import mappings.plugin.plugin.MinecraftJarsPlugin;

public abstract class MinecraftJarsExtension extends DefaultTaskedExtension<MinecraftJarsPlugin.Tasks> {
    public static final String NAME = "minecraftJars";

    public MinecraftJarsExtension(MinecraftJarsPlugin.Tasks tasks) {
        super(tasks);
    }
}
