package fr.pastequeworld.bedwars.config;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.game.GameMode;
import fr.pastequeworld.bedwars.team.TeamColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wrapper du config.yml. Centralise tous les accesseurs typ\u00e9s
 * pour eviter les dereferences brutes de chemins YAML dans le reste du code.
 */
public class ConfigManager {

    private final BedWarsPlugin plugin;

    private String serverName;
    private String lobbyWorldName;
    private Location lobbySpawn;
    private final Map<GameMode, ModeDefinition> modes = new LinkedHashMap<GameMode, ModeDefinition>();
    private final Map<GameMode, Location> npcLocations = new LinkedHashMap<GameMode, Location>();
    private final Map<GameMode, String> npcSkins = new LinkedHashMap<GameMode, String>();

    private String schematicFolder;
    private String templateFolder;
    private int startCountdown;
    private int startCountdownQuick;
    private int endDelay;
    private int voidHeight;
    private int arenaSpacing;

    private EventsTimeline timeline;

    private final Map<String, GeneratorSettings> generators = new LinkedHashMap<String, GeneratorSettings>();
    private final Map<TeamColor, TeamAppearance> teamAppearances = new LinkedHashMap<TeamColor, TeamAppearance>();

    private int respawnSeconds;
    private int spawnProtectionSeconds;
    private int bedDestroyRadius;
    private int voidDeathY;
    private int camperWarnRadius;

    private final Map<GameMode, List<String>> mapsPerMode = new LinkedHashMap<GameMode, List<String>>();

    public ConfigManager(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration c = plugin.getConfig();
        this.serverName = c.getString("server-name", "pasteque.world");

        this.lobbyWorldName = c.getString("lobby.world", "bedwars_lobby");
        ensureLobbyWorld();

        World lobby = Bukkit.getWorld(lobbyWorldName);
        this.lobbySpawn = new Location(lobby,
                c.getDouble("lobby.spawn.x"),
                c.getDouble("lobby.spawn.y"),
                c.getDouble("lobby.spawn.z"),
                (float) c.getDouble("lobby.spawn.yaw"),
                (float) c.getDouble("lobby.spawn.pitch"));

        // Modes
        ConfigurationSection modesSection = c.getConfigurationSection("modes");
        if (modesSection != null) {
            for (String key : modesSection.getKeys(false)) {
                GameMode mode = GameMode.fromConfigKey(key);
                if (mode == null) continue;
                ConfigurationSection m = modesSection.getConfigurationSection(key);
                ModeDefinition def = new ModeDefinition(
                        mode,
                        m.getString("display-name"),
                        m.getString("icon", "IRON_SWORD"),
                        m.getInt("teams", 4),
                        m.getInt("players-per-team", 1),
                        m.getInt("min-players-to-start", 2),
                        m.getStringList("lines")
                );
                modes.put(mode, def);

                ConfigurationSection npc = c.getConfigurationSection("lobby.npcs." + key);
                if (npc != null) {
                    Location loc = new Location(lobby,
                            npc.getDouble("x"),
                            npc.getDouble("y"),
                            npc.getDouble("z"),
                            (float) npc.getDouble("yaw", 0),
                            (float) npc.getDouble("pitch", 0));
                    npcLocations.put(mode, loc);
                    npcSkins.put(mode, npc.getString("skin", "MHF_Steve"));
                }
            }
        }

        // Arena
        this.schematicFolder = c.getString("arena.schematic-folder", "schematics");
        this.templateFolder = c.getString("arena.template-folder", "templates");
        this.startCountdown = c.getInt("arena.start-countdown", 20);
        this.startCountdownQuick = c.getInt("arena.start-countdown-quick", 10);
        this.endDelay = c.getInt("arena.end-delay", 10);
        this.voidHeight = c.getInt("arena.void-height", 64);
        this.arenaSpacing = c.getInt("arena.arena-spacing", 500);

        // Events timeline
        this.timeline = new EventsTimeline(
                c.getInt("events.diamond-ii"),
                c.getInt("events.emerald-ii"),
                c.getInt("events.diamond-iii"),
                c.getInt("events.emerald-iii"),
                c.getInt("events.bed-destruction"),
                c.getInt("events.sudden-death"),
                c.getInt("events.game-end"));

        // Generators
        ConfigurationSection gens = c.getConfigurationSection("generators");
        if (gens != null) {
            for (String key : gens.getKeys(false)) {
                ConfigurationSection g = gens.getConfigurationSection(key);
                GeneratorSettings gs = new GeneratorSettings();
                gs.name = key;
                gs.maxItemsGround = g.getInt("max-items-ground", 16);
                if (g.contains("spawn-interval-ticks")) {
                    GeneratorSettings.Tier t = new GeneratorSettings.Tier();
                    t.intervalTicks = g.getInt("spawn-interval-ticks");
                    t.display = "";
                    gs.tiers.add(t);
                }
                if (g.contains("tiers")) {
                    for (Map<?, ?> raw : g.getMapList("tiers")) {
                        GeneratorSettings.Tier t = new GeneratorSettings.Tier();
                        t.intervalTicks = ((Number) raw.get("interval-ticks")).intValue();
                        t.display = (String) raw.get("display");
                        gs.tiers.add(t);
                    }
                }
                gs.drops = g.getStringList("drops");
                generators.put(key, gs);
            }
        }

        // Team appearances
        ConfigurationSection teamsSection = c.getConfigurationSection("teams");
        if (teamsSection != null) {
            for (String key : teamsSection.getKeys(false)) {
                try {
                    TeamColor color = TeamColor.valueOf(key.toUpperCase());
                    ConfigurationSection t = teamsSection.getConfigurationSection(key);
                    TeamAppearance ap = new TeamAppearance();
                    ap.chatColor = t.getString("chat-color", "&f");
                    ap.displayName = t.getString("display-name", key);
                    ap.prefix = t.getString("prefix", key.substring(0, 1));
                    ap.woolData = t.getInt("wool-data", 0);
                    ap.glassData = t.getInt("glass-data", 0);
                    ap.clayData = t.getInt("clay-data", 0);
                    String[] rgb = t.getString("armor-color", "255,255,255").split(",");
                    ap.armorR = Integer.parseInt(rgb[0].trim());
                    ap.armorG = Integer.parseInt(rgb[1].trim());
                    ap.armorB = Integer.parseInt(rgb[2].trim());
                    teamAppearances.put(color, ap);
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // Gameplay
        this.respawnSeconds = c.getInt("gameplay.respawn-seconds", 5);
        this.spawnProtectionSeconds = c.getInt("gameplay.spawn-protection-seconds", 3);
        this.bedDestroyRadius = c.getInt("gameplay.bed-destroy-radius", 3);
        this.voidDeathY = c.getInt("gameplay.void-death-y", 0);
        this.camperWarnRadius = c.getInt("gameplay.camper-warn-radius", 10);

        // Maps per mode
        ConfigurationSection maps = c.getConfigurationSection("maps");
        if (maps != null) {
            for (String key : maps.getKeys(false)) {
                GameMode mode = GameMode.fromConfigKey(key);
                if (mode == null) continue;
                mapsPerMode.put(mode, new ArrayList<String>(maps.getStringList(key)));
            }
        }
    }

    private void ensureLobbyWorld() {
        if (Bukkit.getWorld(lobbyWorldName) != null) return;
        try {
            org.bukkit.WorldCreator creator = new org.bukkit.WorldCreator(lobbyWorldName);
            creator.type(org.bukkit.WorldType.FLAT);
            creator.generateStructures(false);
            creator.createWorld();
            plugin.getLogger().info("Monde de lobby cree: " + lobbyWorldName);
        } catch (Exception e) {
            plugin.getLogger().warning("Impossible de creer le monde de lobby: " + e.getMessage());
        }
    }

    // === Getters ===

    public String getServerName() { return serverName; }
    public String getLobbyWorldName() { return lobbyWorldName; }
    public Location getLobbySpawn() { return lobbySpawn; }
    public Map<GameMode, ModeDefinition> getModes() { return modes; }
    public ModeDefinition getMode(GameMode mode) { return modes.get(mode); }
    public Map<GameMode, Location> getNpcLocations() { return npcLocations; }
    public Map<GameMode, String> getNpcSkins() { return npcSkins; }

    public String getSchematicFolder() { return schematicFolder; }
    public String getTemplateFolder() { return templateFolder; }
    public int getStartCountdown() { return startCountdown; }
    public int getStartCountdownQuick() { return startCountdownQuick; }
    public int getEndDelay() { return endDelay; }
    public int getVoidHeight() { return voidHeight; }
    public int getArenaSpacing() { return arenaSpacing; }
    public EventsTimeline getTimeline() { return timeline; }

    public GeneratorSettings getGenerator(String name) { return generators.get(name); }
    public Map<String, GeneratorSettings> getGenerators() { return generators; }

    public TeamAppearance getTeamAppearance(TeamColor color) { return teamAppearances.get(color); }

    public int getRespawnSeconds() { return respawnSeconds; }
    public int getSpawnProtectionSeconds() { return spawnProtectionSeconds; }
    public int getBedDestroyRadius() { return bedDestroyRadius; }
    public int getVoidDeathY() { return voidDeathY; }
    public int getCamperWarnRadius() { return camperWarnRadius; }

    public List<String> getMapsForMode(GameMode mode) {
        List<String> list = mapsPerMode.get(mode);
        return list == null ? new ArrayList<String>() : list;
    }

    // === Inner data classes ===

    public static class ModeDefinition {
        private final GameMode mode;
        private final String displayName;
        private final String icon;
        private final int teams;
        private final int playersPerTeam;
        private final int minPlayersToStart;
        private final List<String> lines;

        public ModeDefinition(GameMode mode, String displayName, String icon, int teams,
                              int playersPerTeam, int minPlayersToStart, List<String> lines) {
            this.mode = mode;
            this.displayName = displayName;
            this.icon = icon;
            this.teams = teams;
            this.playersPerTeam = playersPerTeam;
            this.minPlayersToStart = minPlayersToStart;
            this.lines = lines == null ? new ArrayList<String>() : lines;
        }

        public GameMode getMode() { return mode; }
        public String getDisplayName() { return displayName; }
        public String getIcon() { return icon; }
        public int getTeams() { return teams; }
        public int getPlayersPerTeam() { return playersPerTeam; }
        public int getMaxPlayers() { return teams * playersPerTeam; }
        public int getMinPlayersToStart() { return minPlayersToStart; }
        public List<String> getLines() { return lines; }
    }

    public static class EventsTimeline {
        public final int diamondII;
        public final int emeraldII;
        public final int diamondIII;
        public final int emeraldIII;
        public final int bedDestruction;
        public final int suddenDeath;
        public final int gameEnd;

        public EventsTimeline(int dii, int eii, int diii, int eiii, int bd, int sd, int ge) {
            this.diamondII = dii;
            this.emeraldII = eii;
            this.diamondIII = diii;
            this.emeraldIII = eiii;
            this.bedDestruction = bd;
            this.suddenDeath = sd;
            this.gameEnd = ge;
        }
    }

    public static class GeneratorSettings {
        public String name;
        public int maxItemsGround;
        public final List<Tier> tiers = new ArrayList<Tier>();
        public List<String> drops = new ArrayList<String>();

        public static class Tier {
            public int intervalTicks;
            public String display;
        }
    }

    public static class TeamAppearance {
        public String chatColor;
        public String displayName;
        public String prefix;
        public int woolData;
        public int glassData;
        public int clayData;
        public int armorR;
        public int armorG;
        public int armorB;
    }
}
