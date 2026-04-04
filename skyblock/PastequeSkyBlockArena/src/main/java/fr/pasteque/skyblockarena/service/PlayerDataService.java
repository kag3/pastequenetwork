package fr.pasteque.skyblockarena.service;

import fr.pasteque.skyblockarena.PastequeSkyBlockArenaPlugin;
import fr.pasteque.skyblockarena.model.ArenaPlayerData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataService {

    private final PastequeSkyBlockArenaPlugin plugin;
    private final ArenaWorldService arenaWorldService;
    private final Map<UUID, ArenaPlayerData> dataMap = new HashMap<UUID, ArenaPlayerData>();
    private final File file;
    private YamlConfiguration data;

    public PlayerDataService(PastequeSkyBlockArenaPlugin plugin, ArenaWorldService arenaWorldService) {
        this.plugin = plugin;
        this.arenaWorldService = arenaWorldService;
        this.file = new File(plugin.getDataFolder(), "players.yml");
    }

    public void load() {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ignored) {
            }
        }
        data = YamlConfiguration.loadConfiguration(file);
        dataMap.clear();
        ConfigurationSection players = data.getConfigurationSection("players");
        if (players != null) {
            for (String key : players.getKeys(false)) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(key);
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                ConfigurationSection section = players.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                ArenaPlayerData arenaData = new ArenaPlayerData(uuid, section.getString("name", key));
                arenaData.setLevel(section.getInt("level"));
                arenaData.setXp(section.getInt("xp"));
                arenaData.setKills(section.getInt("kills"));
                arenaData.setDeaths(section.getInt("deaths"));
                arenaData.setActivitySeconds(section.getInt("activitySeconds"));
                arenaData.setStreakIntervals(section.getInt("streakIntervals"));
                arenaData.setPendingCombatPenalty(section.getBoolean("pendingCombatPenalty"));
                arenaData.setPendingPenaltyAmount(section.getDouble("pendingPenaltyAmount"));
                arenaData.getPurchasedKits().addAll(section.getStringList("purchasedKits"));
                dataMap.put(uuid, arenaData);
            }
        }
    }

    public void save() {
        if (data == null) {
            data = new YamlConfiguration();
        }
        data.set("players", null);
        for (ArenaPlayerData arenaData : dataMap.values()) {
            String path = "players." + arenaData.getUuid();
            data.set(path + ".name", arenaData.getName());
            data.set(path + ".level", arenaData.getLevel());
            data.set(path + ".xp", arenaData.getXp());
            data.set(path + ".kills", arenaData.getKills());
            data.set(path + ".deaths", arenaData.getDeaths());
            data.set(path + ".activitySeconds", arenaData.getActivitySeconds());
            data.set(path + ".streakIntervals", arenaData.getStreakIntervals());
            data.set(path + ".pendingCombatPenalty", arenaData.isPendingCombatPenalty());
            data.set(path + ".pendingPenaltyAmount", arenaData.getPendingPenaltyAmount());
            data.set(path + ".purchasedKits", arenaData.getPurchasedKits().toArray(new String[0]));
        }
        try {
            data.save(file);
        } catch (IOException ignored) {
        }
    }

    public ArenaPlayerData get(UUID uuid, String name) {
        ArenaPlayerData arenaData = dataMap.get(uuid);
        if (arenaData == null) {
            arenaData = new ArenaPlayerData(uuid, name == null ? uuid.toString() : name);
            dataMap.put(uuid, arenaData);
        }
        if (name != null) {
            arenaData.setName(name);
        }
        return arenaData;
    }

    public ArenaPlayerData get(Player player) {
        return get(player.getUniqueId(), player.getName());
    }

    public void tickSessions() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!arenaWorldService.isArenaWorld(player.getWorld())) {
                continue;
            }
            ArenaPlayerData arenaData = get(player);
            arenaData.addActivitySecond();
        }
    }

    public String getLevelTag(Player player) {
        int level = get(player).getLevel();
        String color;
        if (level >= 90) {
            color = "&5";
        } else if (level >= 70) {
            color = "&d";
        } else if (level >= 45) {
            color = "&2";
        } else if (level >= 20) {
            color = "&f";
        } else {
            color = "&7";
        }
        return plugin.color(color + level);
    }

    public Collection<ArenaPlayerData> all() {
        return Collections.unmodifiableCollection(dataMap.values());
    }

    public String formatRatio(Player player) {
        return new DecimalFormat("0.00").format(get(player).getRatio());
    }
}
