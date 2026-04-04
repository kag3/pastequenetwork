package fr.pasteque.skyblockarena.service;

import fr.pasteque.skyblockarena.PastequeSkyBlockArenaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class EconomyBridge {

    private final PastequeSkyBlockArenaPlugin plugin;

    public EconomyBridge(PastequeSkyBlockArenaPlugin plugin) {
        this.plugin = plugin;
    }

    private File getEconomyFile() {
        return new File(plugin.getDataFolder(), plugin.getConfig().getString("economy.file", "../PastequeSkyblock/economy.yml"));
    }

    private YamlConfiguration loadEconomy() {
        File file = getEconomyFile();
        if (!file.exists()) {
            try {
                if (file.getParentFile() != null && !file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                file.createNewFile();
            } catch (IOException ignored) {
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private String resolveBalancePath(YamlConfiguration config, UUID uuid, String name) {
        List<String> templates = plugin.getConfig().getStringList("economy.balance-paths");
        String fallback = "balances." + uuid.toString();
        for (String template : templates) {
            String path = template.replace("%uuid%", uuid.toString()).replace("%name%", name == null ? uuid.toString() : name);
            if (config.contains(path)) {
                return path;
            }
        }
        if (!templates.isEmpty()) {
            return templates.get(0).replace("%uuid%", uuid.toString()).replace("%name%", name == null ? uuid.toString() : name);
        }
        return fallback;
    }

    public synchronized double getBalance(UUID uuid, String name) {
        YamlConfiguration config = loadEconomy();
        String path = resolveBalancePath(config, uuid, name);
        return config.getDouble(path, 0.0D);
    }

    public synchronized void setBalance(UUID uuid, String name, double amount) {
        File file = getEconomyFile();
        YamlConfiguration config = loadEconomy();
        String path = resolveBalancePath(config, uuid, name);
        config.set(path, Math.max(0.0D, amount));
        try {
            config.save(file);
        } catch (IOException ignored) {
        }
    }

    public synchronized boolean withdraw(UUID uuid, String name, double amount) {
        double balance = getBalance(uuid, name);
        if (balance < amount) {
            return false;
        }
        setBalance(uuid, name, balance - amount);
        return true;
    }

    public synchronized void deposit(UUID uuid, String name, double amount) {
        setBalance(uuid, name, getBalance(uuid, name) + Math.max(0.0D, amount));
    }
}
