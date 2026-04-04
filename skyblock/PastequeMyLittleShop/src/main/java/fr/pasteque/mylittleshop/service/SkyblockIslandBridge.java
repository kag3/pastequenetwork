package fr.pasteque.mylittleshop.service;

import fr.pasteque.mylittleshop.PastequeMyLittleShopPlugin;
import fr.pasteque.mylittleshop.model.IslandArea;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SkyblockIslandBridge {

    private final PastequeMyLittleShopPlugin plugin;

    public SkyblockIslandBridge(PastequeMyLittleShopPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isOnOwnedSoloIsland(UUID player, Location location) {
        IslandArea island = findIslandAt(location);
        return island != null && island.getOwner().equals(player);
    }

    public IslandArea findIslandAt(Location location) {
        if (location == null || location.getWorld() == null) return null;
        if (!location.getWorld().getName().equalsIgnoreCase(plugin.getConfig().getString("skyblock.island-world-name", "pasteque_skyblock_islands"))) {
            return null;
        }
        File file = resolveIslandsFile();
        if (file == null) return null;
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("islands");
        if (section == null) return null;
        int size = plugin.getConfig().getInt("skyblock.island-size", 320);
        int half = size / 2;
        for (String ownerKey : section.getKeys(false)) {
            ConfigurationSection islandSection = section.getConfigurationSection(ownerKey);
            if (islandSection == null) continue;
            int centerX = islandSection.getInt("centerX");
            int centerZ = islandSection.getInt("centerZ");
            if (Math.abs(location.getBlockX() - centerX) <= half && Math.abs(location.getBlockZ() - centerZ) <= half) {
                try {
                    return new IslandArea(UUID.fromString(ownerKey), location.getWorld().getName(), centerX, centerZ);
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private File resolveIslandsFile() {
        String configuredPath = plugin.getConfig().getString("skyblock.islands-file", "PastequeSkyblock/islands.yml");
        File pluginsFolder = plugin.getDataFolder().getParentFile();
        List<File> candidates = new ArrayList<File>();
        candidates.add(new File(pluginsFolder, configuredPath));
        if (configuredPath.startsWith("../")) {
            candidates.add(new File(pluginsFolder, configuredPath.substring(3)));
        }
        candidates.add(new File(pluginsFolder, "PastequeSkyblock/islands.yml"));
        candidates.add(new File(pluginsFolder, "PastequeSkyBlock/islands.yml"));
        for (File candidate : candidates) {
            if (candidate.exists()) return candidate;
        }
        return null;
    }
}
