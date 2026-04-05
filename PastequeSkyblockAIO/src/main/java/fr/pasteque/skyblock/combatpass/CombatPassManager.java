package fr.pasteque.skyblock.combatpass;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.combatpass.model.PassTier;
import fr.pasteque.skyblock.combatpass.model.PlayerPassData;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CombatPassManager {

    private final PastequeSkyblockPlugin plugin;
    private final HashMap<UUID, PlayerPassData> cache;
    private final List<PassTier> freeTiers;
    private final List<PassTier> premiumTiers;
    private final File file;

    public CombatPassManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.cache = new HashMap<UUID, PlayerPassData>();
        this.freeTiers = new ArrayList<PassTier>();
        this.premiumTiers = new ArrayList<PassTier>();
        this.file = new File(plugin.getDataFolder(), "combatpass.yml");
        initTiers();
        load();
    }

    // =========================================================================
    //  Tier definitions
    // =========================================================================

    private void initTiers() {
        // Free tiers (30)
        freeTiers.add(new PassTier(1, "&7100$", "GOLD_NUGGET", 1, 100));
        freeTiers.add(new PassTier(2, "&7200$", "GOLD_NUGGET", 2, 200));
        freeTiers.add(new PassTier(3, "&78x Fer", "IRON_INGOT", 8, 0));
        freeTiers.add(new PassTier(4, "&7400$", "GOLD_NUGGET", 4, 400));
        freeTiers.add(new PassTier(5, "&7500$", "GOLD_INGOT", 1, 500));
        freeTiers.add(new PassTier(6, "&716x Fer", "IRON_INGOT", 16, 0));
        freeTiers.add(new PassTier(7, "&7700$", "GOLD_INGOT", 2, 700));
        freeTiers.add(new PassTier(8, "&78x Or", "GOLD_INGOT", 8, 0));
        freeTiers.add(new PassTier(9, "&71000$", "GOLD_INGOT", 3, 1000));
        freeTiers.add(new PassTier(10, "&74x Diamant", "DIAMOND", 4, 0));
        freeTiers.add(new PassTier(11, "&71200$", "GOLD_INGOT", 4, 1200));
        freeTiers.add(new PassTier(12, "&732x Fer", "IRON_INGOT", 32, 0));
        freeTiers.add(new PassTier(13, "&71500$", "GOLD_INGOT", 5, 1500));
        freeTiers.add(new PassTier(14, "&78x Diamant", "DIAMOND", 8, 0));
        freeTiers.add(new PassTier(15, "&72000$", "GOLD_BLOCK", 1, 2000));
        freeTiers.add(new PassTier(16, "&716x Or", "GOLD_INGOT", 16, 0));
        freeTiers.add(new PassTier(17, "&72200$", "GOLD_BLOCK", 2, 2200));
        freeTiers.add(new PassTier(18, "&764x Fer", "IRON_BLOCK", 7, 0));
        freeTiers.add(new PassTier(19, "&72500$", "GOLD_BLOCK", 3, 2500));
        freeTiers.add(new PassTier(20, "&712x Diamant", "DIAMOND", 12, 0));
        freeTiers.add(new PassTier(21, "&73000$", "GOLD_BLOCK", 4, 3000));
        freeTiers.add(new PassTier(22, "&732x Or", "GOLD_INGOT", 32, 0));
        freeTiers.add(new PassTier(23, "&73500$", "GOLD_BLOCK", 5, 3500));
        freeTiers.add(new PassTier(24, "&716x Diamant", "DIAMOND", 16, 0));
        freeTiers.add(new PassTier(25, "&74000$", "DIAMOND", 1, 4000));
        freeTiers.add(new PassTier(26, "&74x Bloc de Fer", "IRON_BLOCK", 4, 0));
        freeTiers.add(new PassTier(27, "&74200$", "DIAMOND", 2, 4200));
        freeTiers.add(new PassTier(28, "&732x Diamant", "DIAMOND", 32, 0));
        freeTiers.add(new PassTier(29, "&74500$", "DIAMOND", 3, 4500));
        freeTiers.add(new PassTier(30, "&e5000$ + 64x Diamant", "DIAMOND_BLOCK", 64, 5000));

        // Premium tiers (30)
        premiumTiers.add(new PassTier(1, "&6500$", "DIAMOND", 1, 500));
        premiumTiers.add(new PassTier(2, "&68x Diamant", "DIAMOND", 8, 0));
        premiumTiers.add(new PassTier(3, "&61000$", "DIAMOND", 2, 1000));
        premiumTiers.add(new PassTier(4, "&64x Bloc de Fer", "IRON_BLOCK", 4, 0));
        premiumTiers.add(new PassTier(5, "&62000$", "EMERALD", 1, 2000));
        premiumTiers.add(new PassTier(6, "&616x Diamant", "DIAMOND", 16, 0));
        premiumTiers.add(new PassTier(7, "&63000$", "EMERALD", 2, 3000));
        premiumTiers.add(new PassTier(8, "&64x Bloc d'Or", "GOLD_BLOCK", 4, 0));
        premiumTiers.add(new PassTier(9, "&64000$", "EMERALD", 3, 4000));
        premiumTiers.add(new PassTier(10, "&64x Bloc de Diamant", "DIAMOND_BLOCK", 4, 0));
        premiumTiers.add(new PassTier(11, "&65000$", "EMERALD", 4, 5000));
        premiumTiers.add(new PassTier(12, "&68x Bloc d'Or", "GOLD_BLOCK", 8, 0));
        premiumTiers.add(new PassTier(13, "&66000$", "EMERALD", 5, 6000));
        premiumTiers.add(new PassTier(14, "&68x Bloc de Diamant", "DIAMOND_BLOCK", 8, 0));
        premiumTiers.add(new PassTier(15, "&68000$", "EMERALD_BLOCK", 1, 8000));
        premiumTiers.add(new PassTier(16, "&616x Bloc d'Or", "GOLD_BLOCK", 16, 0));
        premiumTiers.add(new PassTier(17, "&69000$", "EMERALD_BLOCK", 2, 9000));
        premiumTiers.add(new PassTier(18, "&616x Bloc de Diamant", "DIAMOND_BLOCK", 16, 0));
        premiumTiers.add(new PassTier(19, "&610000$", "EMERALD_BLOCK", 3, 10000));
        premiumTiers.add(new PassTier(20, "&64x Bloc d'Emeraude", "EMERALD_BLOCK", 4, 0));
        premiumTiers.add(new PassTier(21, "&612000$", "EMERALD_BLOCK", 4, 12000));
        premiumTiers.add(new PassTier(22, "&632x Bloc de Diamant", "DIAMOND_BLOCK", 32, 0));
        premiumTiers.add(new PassTier(23, "&614000$", "EMERALD_BLOCK", 5, 14000));
        premiumTiers.add(new PassTier(24, "&68x Bloc d'Emeraude", "EMERALD_BLOCK", 8, 0));
        premiumTiers.add(new PassTier(25, "&616000$", "NETHER_STAR", 1, 16000));
        premiumTiers.add(new PassTier(26, "&616x Bloc d'Emeraude", "EMERALD_BLOCK", 16, 0));
        premiumTiers.add(new PassTier(27, "&618000$", "NETHER_STAR", 2, 18000));
        premiumTiers.add(new PassTier(28, "&632x Bloc d'Emeraude", "EMERALD_BLOCK", 32, 0));
        premiumTiers.add(new PassTier(29, "&620000$", "NETHER_STAR", 3, 20000));
        premiumTiers.add(new PassTier(30, "&c&l50000$ + Etoile du Nether", "NETHER_STAR", 5, 50000));
    }

    // =========================================================================
    //  Persistence
    // =========================================================================

    public void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = config.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                ConfigurationSection sec = players.getConfigurationSection(key);
                if (sec == null) continue;

                PlayerPassData data = new PlayerPassData();
                data.addXp(sec.getInt("xp", 0));
                data.setPremium(sec.getBoolean("premium", false));

                List<Integer> claimed = sec.getIntegerList("claimed");
                for (int t : claimed) {
                    data.claimTier(t);
                }

                cache.put(uuid, data);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerPassData> entry : cache.entrySet()) {
            String path = "players." + entry.getKey().toString();
            PlayerPassData data = entry.getValue();
            config.set(path + ".xp", data.getXp());
            config.set(path + ".premium", data.isPremium());
            config.set(path + ".claimed", new ArrayList<Integer>(data.getClaimedTiers()));
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder combatpass.yml: " + e.getMessage());
        }
    }

    // =========================================================================
    //  Data access
    // =========================================================================

    public PlayerPassData getData(UUID uuid) {
        if (!cache.containsKey(uuid)) {
            cache.put(uuid, new PlayerPassData());
        }
        return cache.get(uuid);
    }

    public List<PassTier> getFreeTiers() {
        return freeTiers;
    }

    public List<PassTier> getPremiumTiers() {
        return premiumTiers;
    }

    public PastequeSkyblockPlugin getPlugin() {
        return plugin;
    }

    // =========================================================================
    //  XP & rewards
    // =========================================================================

    public void addXp(Player player, int amount) {
        PlayerPassData data = getData(player.getUniqueId());
        int oldTier = data.getCurrentTier();
        data.addXp(amount);
        int newTier = data.getCurrentTier();

        if (newTier > oldTier) {
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "&d&lPasse de Combat &7» &aTu as atteint le &epalier " + newTier + "&a ! Ouvre ton passe pour recuperer ta recompense."));
        }
    }

    public boolean claimReward(Player player, int tier, boolean premium) {
        PlayerPassData data = getData(player.getUniqueId());

        // Check tier is unlocked
        if (data.getXp() < tier * 500) {
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "&d&lPasse de Combat &7» &cTu n'as pas encore atteint ce palier !"));
            return false;
        }

        // Premium check
        if (premium && !data.isPremium()) {
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "&d&lPasse de Combat &7» &cTu dois acheter le &6Passe Premium &cpour recuperer cette recompense !"));
            return false;
        }

        // Encode claimed key: positive = free, negative = premium
        int claimKey = premium ? -tier : tier;
        if (data.hasClaimed(claimKey)) {
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "&d&lPasse de Combat &7» &cTu as deja recupere cette recompense !"));
            return false;
        }

        // Get the tier data
        List<PassTier> tierList = premium ? premiumTiers : freeTiers;
        if (tier < 1 || tier > tierList.size()) {
            return false;
        }
        PassTier passTier = tierList.get(tier - 1);

        // Give money
        if (passTier.getMoneyReward() > 0) {
            plugin.getEconomyManager().add(player.getUniqueId(), passTier.getMoneyReward());
        }

        // Give items
        if (passTier.getAmount() > 0 && passTier.getMoneyReward() == 0) {
            Material mat = Material.matchMaterial(passTier.getMaterial());
            if (mat != null) {
                int remaining = passTier.getAmount();
                while (remaining > 0) {
                    int stack = Math.min(remaining, mat.getMaxStackSize());
                    ItemStack item = new ItemStack(mat, stack);
                    HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                    if (!overflow.isEmpty()) {
                        for (ItemStack drop : overflow.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), drop);
                        }
                    }
                    remaining -= stack;
                }
            }
        } else if (passTier.getAmount() > 0 && passTier.getMoneyReward() > 0) {
            // Tiers with both money and items (e.g. tier 30)
            Material mat = Material.matchMaterial(passTier.getMaterial());
            if (mat != null) {
                int remaining = passTier.getAmount();
                while (remaining > 0) {
                    int stack = Math.min(remaining, mat.getMaxStackSize());
                    ItemStack item = new ItemStack(mat, stack);
                    HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                    if (!overflow.isEmpty()) {
                        for (ItemStack drop : overflow.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), drop);
                        }
                    }
                    remaining -= stack;
                }
            }
        }

        data.claimTier(claimKey);

        player.sendMessage(PastequeSkyblockPlugin.color(
                "&d&lPasse de Combat &7» &aRecompense du palier &e" + tier
                        + (premium ? " &6(Premium)" : " &7(Gratuit)")
                        + " &arecuperee !"));
        return true;
    }

    // =========================================================================
    //  GUI
    // =========================================================================

    public void openPassGui(Player player) {
        CombatPassGui.open(this, player, 0);
    }
}
