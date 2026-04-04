package fr.pasteque.mylittleshop.economy;

import fr.pasteque.mylittleshop.PastequeMyLittleShopPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PastequeEconomyBridge {

    private final PastequeMyLittleShopPlugin plugin;
    private final DecimalFormat decimalFormat = new DecimalFormat("0.##");

    public PastequeEconomyBridge(PastequeMyLittleShopPlugin plugin) {
        this.plugin = plugin;
    }

    public double getBalance(UUID uuid) {
        File economyFile = resolveEconomyFile();
        if (economyFile == null) return 0D;
        FileConfiguration config = YamlConfiguration.loadConfiguration(economyFile);
        return config.getDouble(resolveBalancePath(config, uuid), 0D);
    }

    public boolean take(UUID uuid, double amount) {
        if (amount <= 0D) return true;
        File economyFile = resolveEconomyFile();
        if (economyFile == null) return false;
        FileConfiguration config = YamlConfiguration.loadConfiguration(economyFile);
        String path = resolveBalancePath(config, uuid);
        double current = config.getDouble(path, 0D);
        if (current < amount) return false;
        config.set(path, current - amount);
        return save(config, economyFile);
    }

    public boolean add(UUID uuid, double amount) {
        if (amount <= 0D) return true;
        File economyFile = resolveEconomyFile();
        if (economyFile == null) return false;
        FileConfiguration config = YamlConfiguration.loadConfiguration(economyFile);
        String path = resolveBalancePath(config, uuid);
        double current = config.getDouble(path, 0D);
        config.set(path, current + amount);
        return save(config, economyFile);
    }

    public String format(double value) {
        return decimalFormat.format(value);
    }

    private boolean save(FileConfiguration configuration, File file) {
        try {
            configuration.save(file);
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible d'enregistrer economy.yml : " + e.getMessage());
            return false;
        }
    }

    private File resolveEconomyFile() {
        String configuredPath = plugin.getConfig().getString("economy.file", "PastequeSkyblock/economy.yml");
        File pluginsFolder = plugin.getDataFolder().getParentFile();
        List<File> candidates = new ArrayList<File>();
        candidates.add(new File(pluginsFolder, configuredPath));
        if (configuredPath.startsWith("../")) {
            candidates.add(new File(pluginsFolder, configuredPath.substring(3)));
        } else {
            candidates.add(new File(pluginsFolder, "../" + configuredPath));
        }
        candidates.add(new File(pluginsFolder, "PastequeSkyblock/economy.yml"));
        candidates.add(new File(pluginsFolder, "PastequeSkyBlock/economy.yml"));
        for (File candidate : candidates) {
            if (candidate.exists()) {
                return candidate;
            }
        }
        return null;
    }

    private String resolveBalancePath(FileConfiguration config, UUID uuid) {
        String uuidString = uuid.toString();
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        String name = offlinePlayer != null && offlinePlayer.getName() != null ? offlinePlayer.getName() : uuidString;
        List<String> candidates = plugin.getConfig().getStringList("economy.balance-paths");
        if (candidates.isEmpty()) {
            candidates.add("players.%uuid%");
            candidates.add("balances.%uuid%");
        }
        String fallback = null;
        for (String candidate : candidates) {
            String path = candidate.replace("%uuid%", uuidString).replace("%name%", name);
            if (fallback == null) fallback = path;
            if (config.contains(path)) return path;
        }
        return fallback == null ? "players." + uuidString : fallback;
    }
}
