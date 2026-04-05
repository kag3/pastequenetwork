package fr.pasteque.skyblock.arena;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EloService {

    private static final int DEFAULT_ELO = 1000;
    private static final int K_FACTOR = 32;

    private final PastequeSkyblockPlugin plugin;
    private final File file;
    private final HashMap<UUID, Integer> eloRatings = new HashMap<UUID, Integer>();

    public EloService(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "elo.yml");
    }

    // =========================================================================
    //  Persistence
    // =========================================================================

    public void load() {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        eloRatings.clear();
        ConfigurationSection section = config.getConfigurationSection("elo");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    eloRatings.put(uuid, section.getInt(key, DEFAULT_ELO));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Integer> entry : eloRatings.entrySet()) {
            config.set("elo." + entry.getKey().toString(), entry.getValue());
        }
        try {
            config.save(file);
        } catch (IOException ignored) {}
    }

    // =========================================================================
    //  ELO logic
    // =========================================================================

    public int getElo(UUID uuid) {
        if (!eloRatings.containsKey(uuid)) {
            eloRatings.put(uuid, DEFAULT_ELO);
        }
        return eloRatings.get(uuid);
    }

    public void updateElo(UUID winner, UUID loser) {
        int ratingW = getElo(winner);
        int ratingL = getElo(loser);

        double expectedW = 1.0 / (1.0 + Math.pow(10.0, (ratingL - ratingW) / 400.0));
        double expectedL = 1.0 / (1.0 + Math.pow(10.0, (ratingW - ratingL) / 400.0));

        int newW = (int) Math.round(ratingW + K_FACTOR * (1.0 - expectedW));
        int newL = (int) Math.round(ratingL + K_FACTOR * (0.0 - expectedL));

        eloRatings.put(winner, Math.max(0, newW));
        eloRatings.put(loser, Math.max(0, newL));
    }

    // =========================================================================
    //  Rank display
    // =========================================================================

    public String getRank(int elo) {
        if (elo < 800) return "Bronze";
        if (elo < 1200) return "Argent";
        if (elo < 1600) return "Or";
        if (elo < 2000) return "Diamant";
        return "Legende";
    }

    public String getRankColor(int elo) {
        if (elo < 800) return "&6";
        if (elo < 1200) return "&7";
        if (elo < 1600) return "&e";
        if (elo < 2000) return "&b";
        return "&d";
    }

    // =========================================================================
    //  Leaderboard
    // =========================================================================

    public List<Map.Entry<UUID, Integer>> getTopPlayers(int count) {
        List<Map.Entry<UUID, Integer>> entries = new ArrayList<Map.Entry<UUID, Integer>>(eloRatings.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<UUID, Integer>>() {
            @Override
            public int compare(Map.Entry<UUID, Integer> a, Map.Entry<UUID, Integer> b) {
                return b.getValue().compareTo(a.getValue());
            }
        });
        if (entries.size() > count) {
            return entries.subList(0, count);
        }
        return entries;
    }
}
