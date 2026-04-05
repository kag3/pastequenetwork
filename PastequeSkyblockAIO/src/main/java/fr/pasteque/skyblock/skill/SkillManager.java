package fr.pasteque.skyblock.skill;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.skill.model.PlayerSkills;
import fr.pasteque.skyblock.skill.model.SkillType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkillManager {

    private final PastequeSkyblockPlugin plugin;
    private final File file;
    private final HashMap<UUID, PlayerSkills> cache = new HashMap<UUID, PlayerSkills>();

    public SkillManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "skills.yml");
    }

    // ── Persistence ──────────────────────────────────────────────────────

    public void load() {
        cache.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = cfg.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String uuidStr : players.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                continue;
            }
            PlayerSkills skills = new PlayerSkills();
            ConfigurationSection section = players.getConfigurationSection(uuidStr);
            if (section == null) {
                continue;
            }
            for (SkillType type : SkillType.values()) {
                String key = type.name().toLowerCase();
                if (section.isConfigurationSection(key)) {
                    ConfigurationSection skillSec = section.getConfigurationSection(key);
                    int xp = skillSec.getInt("xp", 0);
                    int level = skillSec.getInt("level", 0);
                    skills.getXpMap().put(type, xp);
                    skills.setLevel(type, level);
                }
            }
            cache.put(uuid, skills);
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerSkills> entry : cache.entrySet()) {
            String base = "players." + entry.getKey().toString();
            PlayerSkills skills = entry.getValue();
            for (SkillType type : SkillType.values()) {
                String key = base + "." + type.name().toLowerCase();
                cfg.set(key + ".xp", skills.getXp(type));
                cfg.set(key + ".level", skills.getLevel(type));
            }
        }
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder skills.yml: " + e.getMessage());
        }
    }

    // ── Accessors ────────────────────────────────────────────────────────

    public PlayerSkills getSkills(UUID uuid) {
        PlayerSkills skills = cache.get(uuid);
        if (skills == null) {
            skills = new PlayerSkills();
            cache.put(uuid, skills);
        }
        return skills;
    }

    public int getLevel(UUID uuid, SkillType type) {
        return getSkills(uuid).getLevel(type);
    }

    /**
     * Returns the bonus percentage for the given skill (level * 2).
     */
    public int getSkillBonus(UUID uuid, SkillType type) {
        return getLevel(uuid, type) * 2;
    }

    /**
     * Adds XP and sends a level-up message if the player levelled up.
     */
    public void addXp(Player player, SkillType type, int amount) {
        PlayerSkills skills = getSkills(player.getUniqueId());
        boolean levelledUp = skills.addXp(type, amount);
        if (levelledUp) {
            int newLevel = skills.getLevel(type);
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "&6&l>> " + type.getColor() + type.getDisplayName()
                            + " &7niveau &e" + newLevel + " &7atteint ! &a(+" + (newLevel * 2) + "% bonus)"
            ));
        }
    }

    // ── GUI ──────────────────────────────────────────────────────────────

    public void openSkillGui(Player player) {
        PlayerSkills skills = getSkills(player.getUniqueId());
        Inventory inv = SkillGui.create(plugin, skills);
        player.openInventory(inv);
    }

    // ── Getter ───────────────────────────────────────────────────────────

    public PastequeSkyblockPlugin getPlugin() {
        return plugin;
    }
}
