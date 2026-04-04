package fr.pasteque.skyblock.manager;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ChallengeManager {
    private final PastequeSkyblockPlugin plugin;
    private final DataFile dataFile;
    private final Map<String, Set<UUID>> completed = new HashMap<String, Set<UUID>>();

    public ChallengeManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new DataFile(plugin, "challenges.yml");
        load();
    }

    public void load() {
        completed.clear();
        ConfigurationSection section = dataFile.getConfig().getConfigurationSection("completed");
        if (section == null) {
            return;
        }
        for (String challengeId : section.getKeys(false)) {
            List<String> values = section.getStringList(challengeId);
            Set<UUID> uuids = new HashSet<UUID>();
            for (String value : values) {
                uuids.add(UUID.fromString(value));
            }
            completed.put(challengeId, uuids);
        }
    }

    public void save() {
        dataFile.getConfig().set("completed", null);
        for (Map.Entry<String, Set<UUID>> entry : completed.entrySet()) {
            List<String> values = new ArrayList<String>();
            for (UUID uuid : entry.getValue()) {
                values.add(uuid.toString());
            }
            dataFile.getConfig().set("completed." + entry.getKey(), values);
        }
        dataFile.save();
    }

    public Set<String> getChallengeIds() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("challenges");
        return section == null ? Collections.<String>emptySet() : section.getKeys(false);
    }

    public boolean isCompleted(String challengeId, UUID uuid) {
        return completed.containsKey(challengeId) && completed.get(challengeId).contains(uuid);
    }

    public boolean complete(Player player, String challengeId) {
        if (isCompleted(challengeId, player.getUniqueId())) {
            return false;
        }
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("challenges." + challengeId);
        if (section == null) {
            return false;
        }
        Material material = Material.matchMaterial(section.getString("material", "STONE"));
        int amount = section.getInt("amount", 1);
        if (material == null || !hasItems(player, material, amount)) {
            return false;
        }
        removeItems(player, material, amount);
        double reward = section.getDouble("reward", 0.0D);
        plugin.getEconomyManager().add(player.getUniqueId(), reward);
        if (!completed.containsKey(challengeId)) {
            completed.put(challengeId, new HashSet<UUID>());
        }
        completed.get(challengeId).add(player.getUniqueId());
        save();
        return true;
    }

    private boolean hasItems(Player player, Material material, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
                if (count >= amount) {
                    return true;
                }
            }
        }
        return false;
    }

    private void removeItems(Player player, Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != material) {
                continue;
            }
            if (item.getAmount() <= remaining) {
                remaining -= item.getAmount();
                contents[i] = null;
            } else {
                item.setAmount(item.getAmount() - remaining);
                remaining = 0;
            }
            if (remaining <= 0) {
                break;
            }
        }
        player.getInventory().setContents(contents);
        player.updateInventory();
    }
}
