package fr.pasteque.skyblock.leaderboard;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.model.Island;
import fr.pasteque.skyblock.util.HologramUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * LeaderboardManager : fournit les tops (money, elo, ile) et gere les
 * hologrammes natifs (ArmorStand invisibles) places au spawn par un admin.
 * Chaque hologramme est rafraichi automatiquement toutes les 30 secondes.
 */
public class LeaderboardManager {

    public static final String HOLOGRAM_TAG = "\u00A7r\u00A70\u00A7lb\u00A7r";

    private final PastequeSkyblockPlugin plugin;
    private final File file;
    /** type -> location (one hologram per type placed in the world) */
    private final Map<String, Location> placements = new HashMap<String, Location>();

    public LeaderboardManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "leaderboards.yml");
    }

    public void load() {
        placements.clear();
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = cfg.getConfigurationSection("placements");
        if (sec == null) return;
        for (String type : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(type);
            if (s == null) continue;
            String world = s.getString("world");
            if (Bukkit.getWorld(world) == null) continue;
            Location loc = new Location(Bukkit.getWorld(world),
                    s.getDouble("x"), s.getDouble("y"), s.getDouble("z"));
            placements.put(type.toLowerCase(), loc);
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, Location> e : placements.entrySet()) {
            Location l = e.getValue();
            String base = "placements." + e.getKey();
            cfg.set(base + ".world", l.getWorld().getName());
            cfg.set(base + ".x", l.getX());
            cfg.set(base + ".y", l.getY());
            cfg.set(base + ".z", l.getZ());
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("[Leaderboard] Impossible de sauvegarder leaderboards.yml: " + e.getMessage());
        }
    }

    public void placeHologram(String type, Location loc) {
        removeHologram(type);
        placements.put(type.toLowerCase(), loc);
        save();
        refreshAll();
    }

    public void removeHologram(String type) {
        Location loc = placements.remove(type.toLowerCase());
        if (loc != null) despawnLines(loc);
        save();
    }

    public Map<String, Location> getPlacements() { return placements; }

    // ===== Data providers =====

    public static class Entry {
        public final String name;
        public final long value;
        public Entry(String name, long value) {
            this.name = name;
            this.value = value;
        }
    }

    public List<Entry> getTopMoney(int count) {
        List<Entry> list = new ArrayList<Entry>();
        try {
            Object raw = plugin.getEconomyManager();
            // Read via reflection-safe path: use dataFile on disk
            java.lang.reflect.Field f = raw.getClass().getDeclaredField("balances");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<UUID, Double> balances = (Map<UUID, Double>) f.get(raw);
            List<Map.Entry<UUID, Double>> sorted = new ArrayList<Map.Entry<UUID, Double>>(balances.entrySet());
            Collections.sort(sorted, new Comparator<Map.Entry<UUID, Double>>() {
                @Override public int compare(Map.Entry<UUID, Double> a, Map.Entry<UUID, Double> b) {
                    return Double.compare(b.getValue(), a.getValue());
                }
            });
            for (int i = 0; i < Math.min(count, sorted.size()); i++) {
                Map.Entry<UUID, Double> e = sorted.get(i);
                String name = Bukkit.getOfflinePlayer(e.getKey()).getName();
                list.add(new Entry(name == null ? "?" : name, (long) (double) e.getValue()));
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[Leaderboard] top money: " + t.getMessage());
        }
        return list;
    }

    public List<Entry> getTopElo(int count) {
        List<Entry> list = new ArrayList<Entry>();
        List<Map.Entry<UUID, Integer>> top = plugin.getEloService().getTopPlayers(count);
        for (Map.Entry<UUID, Integer> e : top) {
            String name = Bukkit.getOfflinePlayer(e.getKey()).getName();
            list.add(new Entry(name == null ? "?" : name, e.getValue()));
        }
        return list;
    }

    public List<Entry> getTopIslands(int count) {
        List<Entry> list = new ArrayList<Entry>();
        List<Island> islands = new ArrayList<Island>(plugin.getIslandManager().getIslands());
        Collections.sort(islands, new Comparator<Island>() {
            @Override public int compare(Island a, Island b) {
                return Integer.compare(
                        plugin.getIslandManager().getLevel(b),
                        plugin.getIslandManager().getLevel(a));
            }
        });
        for (int i = 0; i < Math.min(count, islands.size()); i++) {
            Island island = islands.get(i);
            String ownerName = Bukkit.getOfflinePlayer(island.getOwner()).getName();
            list.add(new Entry(ownerName == null ? "?" : ownerName,
                    plugin.getIslandManager().getLevel(island)));
        }
        return list;
    }

    // ===== Hologram rendering =====

    public void refreshAll() {
        for (Map.Entry<String, Location> e : placements.entrySet()) {
            renderHologram(e.getKey(), e.getValue());
        }
    }

    private void renderHologram(String type, Location base) {
        if (base.getWorld() == null) return;
        despawnLines(base);

        List<String> lines = new ArrayList<String>();
        String header;
        List<Entry> entries;
        String unit;
        if (type.equals("money")) {
            header = "&6&l\u2726 TOP MONEY \u2726";
            entries = getTopMoney(5);
            unit = " Pasteque";
        } else if (type.equals("elo")) {
            header = "&c&l\u265b TOP ELO \u265b";
            entries = getTopElo(5);
            unit = " ELO";
        } else if (type.equals("ile") || type.equals("island")) {
            header = "&a&l\u2766 TOP ILE \u2766";
            entries = getTopIslands(5);
            unit = " niv";
        } else {
            return;
        }
        lines.add(header);
        lines.add("&8&m----------------");
        if (entries.isEmpty()) {
            lines.add("&7Aucun joueur classe");
        } else {
            String[] ranks = { "&6&l1.", "&e&l2.", "&f&l3.", "&7&l4.", "&7&l5." };
            for (int i = 0; i < entries.size(); i++) {
                Entry e = entries.get(i);
                lines.add(ranks[i] + " &f" + e.name + " &8- &e" + e.value + unit);
            }
        }
        lines.add("&8&m----------------");

        // Lines are spawned from top to bottom, spacing 0.28
        for (int i = 0; i < lines.size(); i++) {
            Location lineLoc = base.clone().add(0, -0.28 * i, 0);
            HologramUtil.spawnLine(lineLoc, lines.get(i), HOLOGRAM_TAG);
        }
    }

    private void despawnLines(Location base) {
        HologramUtil.removeLinesNear(base, HOLOGRAM_TAG);
    }

    public void startAutoRefresh() {
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override public void run() {
                refreshAll();
            }
        }, 20L * 10L, 20L * 30L);
    }
}
