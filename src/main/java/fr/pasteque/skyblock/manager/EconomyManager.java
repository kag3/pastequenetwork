package fr.pasteque.skyblock.manager;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyManager {
    private final PastequeSkyblockPlugin plugin;
    private final DataFile dataFile;
    private final Map<UUID, Double> balances = new HashMap<UUID, Double>();

    public EconomyManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new DataFile(plugin, "economy.yml");
        load();
    }

    public void load() {
        balances.clear();
        ConfigurationSection players = dataFile.getConfig().getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            balances.put(UUID.fromString(key), players.getDouble(key));
        }
    }

    public void save() {
        dataFile.getConfig().set("players", null);
        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            dataFile.getConfig().set("players." + entry.getKey().toString(), entry.getValue());
        }
        dataFile.save();
    }

    public double getBalance(UUID uuid) {
        if (!balances.containsKey(uuid)) {
            balances.put(uuid, plugin.getConfig().getDouble("economy.starting-balance", 0.0D));
        }
        return balances.get(uuid);
    }

    public void setBalance(UUID uuid, double amount) {
        balances.put(uuid, Math.max(0, amount));
    }

    public void add(UUID uuid, double amount) {
        setBalance(uuid, getBalance(uuid) + amount);
    }

    public boolean take(UUID uuid, double amount) {
        if (getBalance(uuid) < amount) {
            return false;
        }
        setBalance(uuid, getBalance(uuid) - amount);
        return true;
    }

    public String getCurrencyName() {
        return plugin.getConfig().getString("economy.currency-name", "Pasteque");
    }
}
