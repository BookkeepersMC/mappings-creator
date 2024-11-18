package mappings.plugin.extension;

import mappings.plugin.extension.abstraction.DefaultTaskedExtension;
import mappings.plugin.plugin.MapMinecraftJarsPlugin;

public abstract class MapMinecraftJarsExtension extends DefaultTaskedExtension<MapMinecraftJarsPlugin.Tasks> {
    public static final String NAME = "mapMinecraftJars";

    public MapMinecraftJarsExtension(MapMinecraftJarsPlugin.Tasks tasks) {
        super(tasks);
    }
}
