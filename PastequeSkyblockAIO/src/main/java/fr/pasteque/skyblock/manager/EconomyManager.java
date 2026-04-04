package fr.pasteque.skyblock.manager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Centralized economy manager -- single source of truth for ALL economy
 * operations across the entire plugin. Replaces the three separate economy
 * bridges that existed before.
 */
public class EconomyManager {
    private final Plugin plugin;
    private final DataFile dataFile;
    private final Map<UUID, Double> balances = new HashMap<UUID, Double>();
    private final DecimalFormat formatter = new DecimalFormat("#,##0.##");

    public EconomyManager(Plugin plugin) {
        this.plugin = plugin;
        this.dataFile = new DataFile(plugin, "economy.yml");
        load();
    }

    public synchronized void load() {
        balances.clear();
        ConfigurationSection players = dataFile.getConfig().getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            balances.put(UUID.fromString(key), players.getDouble(key));
        }
    }

    public synchronized void save() {
        dataFile.getConfig().set("players", null);
        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            dataFile.getConfig().set("players." + entry.getKey().toString(), entry.getValue());
        }
        dataFile.save();
    }

    /**
     * Saves economy data asynchronously using a BukkitRunnable.
     */
    public void saveAsync() {
        new BukkitRunnable() {
            @Override
            public void run() {
                save();
            }
        }.runTaskAsynchronously(plugin);
    }

    public synchronized double getBalance(UUID uuid) {
        if (!balances.containsKey(uuid)) {
            balances.put(uuid, plugin.getConfig().getDouble("economy.starting-balance", 0.0D));
        }
        return balances.get(uuid);
    }

    public synchronized void setBalance(UUID uuid, double amount) {
        balances.put(uuid, Math.max(0, amount));
    }

    public synchronized void add(UUID uuid, double amount) {
        setBalance(uuid, getBalance(uuid) + amount);
    }

    public synchronized boolean take(UUID uuid, double amount) {
        if (getBalance(uuid) < amount) {
            return false;
        }
        setBalance(uuid, getBalance(uuid) - amount);
        return true;
    }

    /**
     * Returns a formatted string representation of the given amount
     * including the currency name, e.g. "1,250.50 Pasteque".
     */
    public String format(double amount) {
        return formatter.format(amount) + " " + getCurrencyName();
    }

    public String getCurrencyName() {
        return plugin.getConfig().getString("economy.currency-name", "Pasteque");
    }
}
