package fr.pasteque.skyblock.daily;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.util.HologramUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Gere le systeme de DailyReward complet :
 * - commande /dailychest give : donne un ender chest special
 * - pose de l'ender chest special : enregistre sa position + spawn holograms
 * - click droit sur l'ender chest : ouvre la GUI calendrier
 * - sauvegarde de la progression par joueur (streak) dans daily-players.yml
 * - sauvegarde des emplacements dans daily-chests.yml
 */
@SuppressWarnings("deprecation")
public class DailyRewardManager {

    public static final String HOLO_TAG = "\u00A7r\u00A70\u00A7dchest\u00A7r";
    public static final String CHEST_ITEM_TAG = "\u00A7r\u00A70\u00A7dailychest\u00A7r";

    private final PastequeSkyblockPlugin plugin;
    private final File chestsFile;
    private final File playersFile;

    /** block locations of registered daily chests */
    private final Set<String> chestLocations = new HashSet<String>();
    /** player -> claimed day (year*1000 + day_of_year) */
    private final Map<UUID, Long> lastClaimDay = new HashMap<UUID, Long>();
    /** player -> current streak (consecutive days) */
    private final Map<UUID, Integer> streaks = new HashMap<UUID, Integer>();

    public DailyRewardManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.chestsFile = new File(plugin.getDataFolder(), "daily-chests.yml");
        this.playersFile = new File(plugin.getDataFolder(), "daily-players.yml");
    }

    public void load() {
        chestLocations.clear();
        lastClaimDay.clear();
        streaks.clear();
        if (chestsFile.exists()) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(chestsFile);
            List<String> list = cfg.getStringList("chests");
            if (list != null) chestLocations.addAll(list);
        }
        if (playersFile.exists()) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(playersFile);
            ConfigurationSection sec = cfg.getConfigurationSection("players");
            if (sec != null) {
                for (String k : sec.getKeys(false)) {
                    try {
                        UUID id = UUID.fromString(k);
                        lastClaimDay.put(id, sec.getLong(k + ".lastDay", -1L));
                        streaks.put(id, sec.getInt(k + ".streak", 0));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
        // Delayed respawn of holograms (after worlds loaded)
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override public void run() { respawnAllHolograms(); }
        }, 40L);
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("chests", new ArrayList<String>(chestLocations));
        try { cfg.save(chestsFile); } catch (IOException e) {
            plugin.getLogger().severe("[Daily] daily-chests.yml: " + e.getMessage());
        }
        YamlConfiguration pCfg = new YamlConfiguration();
        for (Map.Entry<UUID, Long> e : lastClaimDay.entrySet()) {
            String base = "players." + e.getKey().toString();
            pCfg.set(base + ".lastDay", e.getValue());
            pCfg.set(base + ".streak", streaks.get(e.getKey()) == null ? 0 : streaks.get(e.getKey()));
        }
        try { pCfg.save(playersFile); } catch (IOException e) {
            plugin.getLogger().severe("[Daily] daily-players.yml: " + e.getMessage());
        }
    }

    // ===== Item creation =====

    public ItemStack createDailyChestItem() {
        ItemStack item = new ItemStack(Material.ENDER_CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(PastequeSkyblockPlugin.color("&6&l\u2726 Coffre de Recompense Quotidienne \u2726"));
        List<String> lore = new ArrayList<String>();
        lore.add(PastequeSkyblockPlugin.color("&7Placez ce coffre ou vous voulez."));
        lore.add(PastequeSkyblockPlugin.color("&7Il sera instantanement configure"));
        lore.add(PastequeSkyblockPlugin.color("&7avec un hologramme au-dessus."));
        lore.add("");
        lore.add(PastequeSkyblockPlugin.color("&8\u258E &7Fonctionnalites"));
        lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Calendrier 7 jours"));
        lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Progression sauvegardee"));
        lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Reclamation une fois par 24h"));
        lore.add("");
        lore.add(PastequeSkyblockPlugin.color(CHEST_ITEM_TAG));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isDailyChestItem(ItemStack item) {
        if (item == null || item.getType() != Material.ENDER_CHEST || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        for (String line : meta.getLore()) {
            if (line.contains("dailychest")) return true;
        }
        return false;
    }

    // ===== Chest placement / removal =====

    public static String keyOf(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    public boolean isDailyChestBlock(Location loc) {
        return chestLocations.contains(keyOf(loc));
    }

    public void registerChest(Location loc) {
        chestLocations.add(keyOf(loc));
        save();
        spawnHologram(loc);
    }

    public void unregisterChest(Location loc) {
        chestLocations.remove(keyOf(loc));
        save();
        despawnHologram(loc);
    }

    // ===== Holograms =====

    private void spawnHologram(Location chestLoc) {
        World w = chestLoc.getWorld();
        if (w == null) return;
        despawnHologram(chestLoc);
        String[] lines = {
                "&6&l\u2726 DAILY REWARD \u2726",
                "&eClic droit pour reclamer",
                "&7Reset chaque 24h"
        };
        Location base = chestLoc.clone().add(0.5, 1.8, 0.5);
        for (int i = 0; i < lines.length; i++) {
            Location lineLoc = base.clone().add(0, -0.28 * i, 0);
            HologramUtil.spawnLine(lineLoc, lines[i], HOLO_TAG);
        }
    }

    private void despawnHologram(Location chestLoc) {
        World w = chestLoc.getWorld();
        if (w == null) return;
        Location base = chestLoc.clone().add(0.5, 1.8, 0.5);
        HologramUtil.removeLinesNear(base, HOLO_TAG);
    }

    public void respawnAllHolograms() {
        for (String key : chestLocations) {
            String[] parts = key.split(",");
            if (parts.length < 4) continue;
            World w = Bukkit.getWorld(parts[0]);
            if (w == null) continue;
            try {
                Location loc = new Location(w, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                if (w.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                    spawnHologram(loc);
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    // ===== Claim logic =====

    public static long currentDayKey() {
        Calendar c = Calendar.getInstance();
        return (c.get(Calendar.YEAR) * 1000L) + c.get(Calendar.DAY_OF_YEAR);
    }

    public boolean canClaim(UUID uuid) {
        Long last = lastClaimDay.get(uuid);
        return last == null || last != currentDayKey();
    }

    public int getStreak(UUID uuid) {
        Integer s = streaks.get(uuid);
        return s == null ? 0 : s;
    }

    public long getLastClaimDay(UUID uuid) {
        Long v = lastClaimDay.get(uuid);
        return v == null ? -1L : v;
    }

    /**
     * Reclame la recompense. Retourne le numero de jour dans le cycle (1..7) si reussi, 0 si deja reclame.
     */
    public int claim(Player player) {
        UUID id = player.getUniqueId();
        long today = currentDayKey();
        Long last = lastClaimDay.get(id);
        if (last != null && last == today) {
            return 0;
        }
        int streak = getStreak(id);
        // If last claim was yesterday (today - 1), streak continues; otherwise reset
        if (last != null && last == today - 1) {
            streak = Math.min(streak + 1, 7);
        } else {
            streak = 1;
        }
        if (streak > 7) streak = 1; // cycle restart after 7
        streaks.put(id, streak);
        lastClaimDay.put(id, today);
        save();
        // Give reward
        double[] rewards = { 200, 400, 700, 1000, 1500, 2000, 5000 };
        double reward = rewards[streak - 1];
        plugin.getEconomyManager().add(id, reward);
        plugin.getEconomyManager().save();
        player.sendMessage(PastequeSkyblockPlugin.color(
                "&6&l>> &aRecompense quotidienne reclamee ! &7Jour &e" + streak + "&7/&e7 &8- &a+"
                        + ((long) reward) + " Pasteque"));
        // Bonus jackpot item on day 7
        if (streak == 7) {
            ItemStack jackpot = new ItemStack(Material.NETHER_STAR, 1);
            ItemMeta meta = jackpot.getItemMeta();
            meta.setDisplayName(PastequeSkyblockPlugin.color("&6&l\u2605 Jackpot Hebdomadaire \u2605"));
            jackpot.setItemMeta(meta);
            player.getInventory().addItem(jackpot);
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "&6&l\u2605 BONUS ! &eVous avez recu une &6Nether Star &ejackpot !"));
        }
        return streak;
    }

    public double getRewardForDay(int day) {
        double[] rewards = { 200, 400, 700, 1000, 1500, 2000, 5000 };
        if (day < 1 || day > 7) return 0;
        return rewards[day - 1];
    }
}
