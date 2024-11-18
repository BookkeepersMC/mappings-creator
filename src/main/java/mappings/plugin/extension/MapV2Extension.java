package mappings.plugin.extension;

import mappings.plugin.extension.abstraction.DefaultTaskedExtension;
import mappings.plugin.plugin.MapV2Plugin;

public abstract class MapV2Extension extends DefaultTaskedExtension<MapV2Plugin.Tasks> {
    public static final String NAME = "mapV2";

    public MapV2Extension(MapV2Plugin.Tasks tasks) {
        super(tasks);
    }
}
