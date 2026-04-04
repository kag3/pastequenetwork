package fr.pasteque.skyblock.guard;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.guard.model.SanctionEntry;
import fr.pasteque.skyblock.guard.model.SanctionType;
import fr.pasteque.skyblock.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class SanctionService {

    private final PastequeSkyblockPlugin plugin;
    private final File file;
    private final FileConfiguration config;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE);

    public SanctionService(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "sanctions.yml");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Impossible de créer sanctions.yml", e);
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder sanctions.yml");
        }
    }

    public synchronized void applySanction(String targetName, UUID targetUuid, String staffName, SanctionType type, String reason, long durationMs) {
        String key = resolveKey(targetName, targetUuid);
        long now = System.currentTimeMillis();
        long expiresAt = durationMs <= 0L ? 0L : now + durationMs;
        String id = String.valueOf(now);
        String base = "players." + key;
        config.set(base + ".uuid", targetUuid != null ? targetUuid.toString() : key);
        config.set(base + ".name", targetName);

        String historyBase = base + ".history." + id;
        config.set(historyBase + ".type", type.name());
        config.set(historyBase + ".target", targetName);
        config.set(historyBase + ".staff", staffName);
        config.set(historyBase + ".reason", reason);
        config.set(historyBase + ".createdAt", now);
        config.set(historyBase + ".expiresAt", expiresAt);
        config.set(historyBase + ".active", true);

        if (type == SanctionType.BAN || type == SanctionType.TEMPBAN) {
            setActive(base, "ban", id);
        }
        if (type == SanctionType.MUTE || type == SanctionType.TEMPMUTE || type == SanctionType.AUTOMUTE) {
            setActive(base, "mute", id);
        }
        save();
    }

    private void setActive(String base, String field, String id) {
        config.set(base + ".active." + field, id);
    }

    public synchronized SanctionEntry getActiveBan(String targetName) {
        return getActiveSanction(targetName, "ban");
    }

    public synchronized SanctionEntry getActiveMute(String targetName) {
        return getActiveSanction(targetName, "mute");
    }

    private SanctionEntry getActiveSanction(String targetName, String field) {
        String key = resolveKeyByName(targetName);
        if (key == null) {
            return null;
        }
        String id = config.getString("players." + key + ".active." + field);
        if (id == null) {
            return null;
        }
        SanctionEntry entry = readHistoryEntry(key, id);
        if (entry == null) {
            config.set("players." + key + ".active." + field, null);
            save();
            return null;
        }
        if (entry.isExpired()) {
            config.set("players." + key + ".history." + id + ".active", false);
            config.set("players." + key + ".active." + field, null);
            save();
            return null;
        }
        return entry;
    }

    public synchronized void unban(String targetName, String staffName) {
        clearActive(targetName, "ban", staffName);
    }

    public synchronized void unmute(String targetName, String staffName) {
        clearActive(targetName, "mute", staffName);
    }

    private void clearActive(String targetName, String field, String staffName) {
        String key = resolveKeyByName(targetName);
        if (key == null) {
            return;
        }
        String id = config.getString("players." + key + ".active." + field);
        if (id != null) {
            config.set("players." + key + ".history." + id + ".active", false);
            config.set("players." + key + ".history." + id + ".liftedBy", staffName);
            config.set("players." + key + ".history." + id + ".liftedAt", System.currentTimeMillis());
        }
        config.set("players." + key + ".active." + field, null);
        save();
    }

    public synchronized List<SanctionEntry> getHistory(String targetName) {
        String key = resolveKeyByName(targetName);
        if (key == null) {
            return Collections.emptyList();
        }
        ConfigurationSection section = config.getConfigurationSection("players." + key + ".history");
        if (section == null) {
            return Collections.emptyList();
        }
        List<SanctionEntry> entries = new ArrayList<SanctionEntry>();
        for (String id : section.getKeys(false)) {
            SanctionEntry entry = readHistoryEntry(key, id);
            if (entry != null) {
                entries.add(entry);
            }
        }
        Collections.sort(entries, new Comparator<SanctionEntry>() {
            @Override
            public int compare(SanctionEntry a, SanctionEntry b) {
                return Long.compare(b.getCreatedAt(), a.getCreatedAt());
            }
        });
        return entries;
    }

    private SanctionEntry readHistoryEntry(String key, String id) {
        String base = "players." + key + ".history." + id;
        String typeRaw = config.getString(base + ".type");
        if (typeRaw == null) {
            return null;
        }
        SanctionType type = SanctionType.valueOf(typeRaw);
        return new SanctionEntry(
                id,
                type,
                config.getString(base + ".target", key),
                config.getString(base + ".staff", "Console"),
                config.getString(base + ".reason", "Aucune raison"),
                config.getLong(base + ".createdAt"),
                config.getLong(base + ".expiresAt"),
                config.getBoolean(base + ".active")
        );
    }

    public String buildBanScreen(SanctionEntry sanction) {
        return plugin.getGuardPrefix()
                + plugin.color(plugin.getConfig().getString("ban-screen.line1", "&dVous êtes banni de ce serveur."))
                + "\n"
                + plugin.color(plugin.getConfig().getString("ban-screen.line2", "&fRaison : &d%reason%").replace("%reason%", sanction.getReason()))
                + "\n"
                + plugin.color(plugin.getConfig().getString("ban-screen.line3", "&fStaff : &d%staff%").replace("%staff%", sanction.getStaffName()))
                + "\n"
                + plugin.color(plugin.getConfig().getString("ban-screen.line4", "&fFin : &d%expires%")
                .replace("%expires%", sanction.isPermanent() ? "Permanent" : dateFormat.format(new Date(sanction.getExpiresAt()))));
    }

    public String buildKickScreen(String staffName, String reason) {
        return plugin.getGuardPrefix()
                + plugin.color(plugin.getConfig().getString("kick-screen.line1", "&dVous avez été expulsé du serveur."))
                + "\n"
                + plugin.color(plugin.getConfig().getString("kick-screen.line2", "&fRaison : &d%reason%").replace("%reason%", reason))
                + "\n"
                + plugin.color(plugin.getConfig().getString("kick-screen.line3", "&fStaff : &d%staff%").replace("%staff%", staffName));
    }

    public String buildMuteMessage(SanctionEntry sanction) {
        return plugin.getGuardPrefix() + plugin.color(plugin.getConfig().getString("mute-message", "&fVous ne pouvez pas parler actuellement. &fRaison : &d%reason% &f| Fin : &d%expires%")
                .replace("%reason%", sanction.getReason())
                .replace("%expires%", sanction.isPermanent() ? "Permanent" : TimeUtil.formatRemaining(sanction.getExpiresAt())));
    }

    public void disconnectIfOnline(String targetName, String message) {
        Player player = Bukkit.getPlayerExact(targetName);
        if (player != null && player.isOnline()) {
            player.kickPlayer(message);
        }
    }

    public String resolveLastKnownName(String input) {
        Player online = Bukkit.getPlayerExact(input);
        if (online != null) {
            return online.getName();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(input);
        return offline != null && offline.getName() != null ? offline.getName() : input;
    }

    /**
     * Resolves the UUID-based storage key for a player.
     * Uses UUID as key to prevent data loss when players change their name.
     */
    private String resolveKey(String name, UUID uuid) {
        if (uuid != null) {
            return uuid.toString();
        }
        // Fallback: try to resolve UUID from server
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online.getUniqueId().toString();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline != null && offline.getUniqueId() != null) {
            return offline.getUniqueId().toString();
        }
        // Last resort: use lowercase name
        return name.toLowerCase(Locale.ROOT);
    }

    /**
     * Resolves the UUID-based storage key by player name lookup.
     * Searches existing entries for a matching name, or resolves UUID from server.
     */
    private String resolveKeyByName(String name) {
        // First try to find by UUID from server
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            String uuidKey = online.getUniqueId().toString();
            if (config.contains("players." + uuidKey)) {
                return uuidKey;
            }
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline != null && offline.getUniqueId() != null) {
            String uuidKey = offline.getUniqueId().toString();
            if (config.contains("players." + uuidKey)) {
                return uuidKey;
            }
        }
        // Search all entries for matching name (migration support)
        ConfigurationSection players = config.getConfigurationSection("players");
        if (players != null) {
            for (String key : players.getKeys(false)) {
                String storedName = config.getString("players." + key + ".name");
                if (storedName != null && storedName.equalsIgnoreCase(name)) {
                    return key;
                }
            }
        }
        return null;
    }
}
