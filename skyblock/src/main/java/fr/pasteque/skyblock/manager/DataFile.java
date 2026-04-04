package fr.pasteque.skyblock.manager;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class DataFile {
    private final PastequeSkyblockPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    public DataFile(PastequeSkyblockPlugin plugin, String name) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), name);
        reload();
    }

    public void reload() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le fichier " + file.getName(), e);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            this.config.save(file);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de sauvegarder " + file.getName(), e);
        }
    }

    public YamlConfiguration getConfig() {
        return config;
    }
}
