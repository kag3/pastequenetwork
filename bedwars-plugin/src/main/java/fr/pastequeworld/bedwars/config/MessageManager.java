package fr.pastequeworld.bedwars.config;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.util.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Messages en cache, supporte les placeholders %prefix% et %key%.
 */
public class MessageManager {

    private final BedWarsPlugin plugin;
    private FileConfiguration config;
    private String prefix;

    public MessageManager(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        this.config = YamlConfiguration.loadConfiguration(file);
        this.prefix = ColorUtil.color(config.getString("prefix", "&b&lBedWars &8\u00bb &r"));
    }

    public String getPrefix() { return prefix; }

    public String get(String path) {
        String raw = config.getString(path);
        if (raw == null) return path;
        return ColorUtil.color(raw.replace("%prefix%", prefix));
    }

    public String get(String path, Map<String, String> placeholders) {
        String msg = get(path);
        if (placeholders == null) return msg;
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            msg = msg.replace("%" + e.getKey() + "%", e.getValue());
        }
        return msg;
    }

    public String get(String path, String key1, String val1) {
        Map<String, String> m = new HashMap<String, String>();
        m.put(key1, val1);
        return get(path, m);
    }

    public String get(String path, String k1, String v1, String k2, String v2) {
        Map<String, String> m = new HashMap<String, String>();
        m.put(k1, v1);
        m.put(k2, v2);
        return get(path, m);
    }

    public String getRaw(String path) {
        return config.getString(path, path);
    }
}
