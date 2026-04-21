package fr.pastequeworld.bedwars.map;

import fr.pastequeworld.bedwars.game.GameMode;
import fr.pastequeworld.bedwars.team.TeamColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Template d'une map persistant sur disque.
 * Lue depuis plugins/PastequeBedWars/maps/<id>.yml.
 *
 * Une fois chargee, toutes les positions sont relatives (Vector) et sont
 * traduites en {@link Location} lors de l'instantiation d'une Arena
 * via {@link MapRegistry#materialize(MapTemplate, org.bukkit.World, Vector)}.
 */
public class MapTemplate {

    private final String id;
    private String displayName;
    private String author;
    private String schematic;
    private String worldTemplate;  // dossier monde (level.dat) a copier; null si on utilise la schematic
    private boolean autoDetect = false;
    private List<GameMode> supportedModes = new ArrayList<GameMode>();

    private Vector pasteOffset = new Vector();  // offset relatif a l'origine de paste

    private Map<TeamColor, Vector> spawnLocations = new LinkedHashMap<TeamColor, Vector>();
    private Map<TeamColor, Vector> bedLocations = new LinkedHashMap<TeamColor, Vector>();
    private Map<TeamColor, Vector> shopLocations = new LinkedHashMap<TeamColor, Vector>();
    private Map<TeamColor, Vector> upgradeLocations = new LinkedHashMap<TeamColor, Vector>();
    private Map<TeamColor, Vector> ironGenLocations = new LinkedHashMap<TeamColor, Vector>();
    private Map<TeamColor, Vector> goldGenLocations = new LinkedHashMap<TeamColor, Vector>();

    private List<Vector> diamondGenLocations = new ArrayList<Vector>();
    private List<Vector> emeraldGenLocations = new ArrayList<Vector>();

    private Vector queueSpawn = new Vector();

    private int buildRadiusMax = 100;
    private int voidY = 0;

    public MapTemplate(String id) {
        this.id = id;
    }

    public static MapTemplate loadFromFile(File file) throws IOException {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String id = cfg.getString("id", file.getName().replace(".yml", ""));
        MapTemplate t = new MapTemplate(id);
        t.displayName = cfg.getString("display-name", id);
        t.author = cfg.getString("author", "");
        t.schematic = cfg.getString("schematic", id + ".schematic");
        t.worldTemplate = cfg.getString("world-template", null);
        t.autoDetect = cfg.getBoolean("auto-detect", false);
        for (String m : cfg.getStringList("modes")) {
            GameMode mode = GameMode.fromConfigKey(m);
            if (mode != null) t.supportedModes.add(mode);
        }

        t.pasteOffset = readVector(cfg.getConfigurationSection("paste-offset"));
        t.queueSpawn = readVector(cfg.getConfigurationSection("queue-spawn"));
        t.buildRadiusMax = cfg.getInt("build-radius-max", 100);
        t.voidY = cfg.getInt("void-y", 0);

        loadTeamSection(cfg.getConfigurationSection("teams.spawns"), t.spawnLocations);
        loadTeamSection(cfg.getConfigurationSection("teams.beds"), t.bedLocations);
        loadTeamSection(cfg.getConfigurationSection("teams.shops"), t.shopLocations);
        loadTeamSection(cfg.getConfigurationSection("teams.upgrades"), t.upgradeLocations);
        loadTeamSection(cfg.getConfigurationSection("teams.iron-generators"), t.ironGenLocations);
        loadTeamSection(cfg.getConfigurationSection("teams.gold-generators"), t.goldGenLocations);

        List<Map<?, ?>> diamond = cfg.getMapList("generators.diamond");
        for (Map<?, ?> raw : diamond) t.diamondGenLocations.add(rawToVector(raw));
        List<Map<?, ?>> emerald = cfg.getMapList("generators.emerald");
        for (Map<?, ?> raw : emerald) t.emeraldGenLocations.add(rawToVector(raw));
        return t;
    }

    private static void loadTeamSection(ConfigurationSection section, Map<TeamColor, Vector> target) {
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            try {
                TeamColor color = TeamColor.valueOf(key.toUpperCase());
                target.put(color, readVector(section.getConfigurationSection(key)));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private static Vector readVector(ConfigurationSection section) {
        if (section == null) return new Vector();
        double yaw = section.getDouble("yaw", 0);
        Vector v = new Vector(section.getDouble("x"), section.getDouble("y"), section.getDouble("z"));
        // encode yaw dans un vector via getX/getY/getZ : on utilise un wrapper ici si besoin
        v = new Vector(v.getX(), v.getY(), v.getZ());
        // stocke yaw via set vars (hack minimal: we repack yaw in a BlockVector? non) -> Simplifier:
        // On va stocker yaw dans une matrice separee via RotatedVector in future. Pour rester simple,
        // ce wrapper ne porte pas le yaw. Le yaw est stocke dans un fichier separe si necessaire.
        return v;
    }

    @SuppressWarnings("unused")
    private static Vector rawToVector(Map<?, ?> raw) {
        double x = ((Number) raw.get("x")).doubleValue();
        double y = ((Number) raw.get("y")).doubleValue();
        double z = ((Number) raw.get("z")).doubleValue();
        return new Vector(x, y, z);
    }

    // === Accesseurs ===

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getAuthor() { return author; }
    public String getSchematic() { return schematic; }
    public String getWorldTemplate() { return worldTemplate; }
    public boolean isAutoDetect() { return autoDetect; }
    public boolean isWorldFolderBased() { return worldTemplate != null && !worldTemplate.isEmpty(); }
    public List<GameMode> getSupportedModes() { return supportedModes; }
    public void setSupportedModes(List<GameMode> modes) { this.supportedModes = modes; }
    public void setDisplayName(String name) { this.displayName = name; }
    public void setWorldTemplate(String wt) { this.worldTemplate = wt; }
    public void setAutoDetect(boolean b) { this.autoDetect = b; }
    public void setQueueSpawn(Vector v) { this.queueSpawn = v; }
    public void setSchematic(String s) { this.schematic = s; }
    public void setPasteOffset(Vector v) { this.pasteOffset = v; }
    public Vector getPasteOffset() { return pasteOffset; }
    public Vector getQueueSpawn() { return queueSpawn; }
    public int getBuildRadiusMax() { return buildRadiusMax; }
    public int getVoidY() { return voidY; }
    public Map<TeamColor, Vector> getSpawnLocations() { return spawnLocations; }
    public Map<TeamColor, Vector> getBedLocations() { return bedLocations; }
    public Map<TeamColor, Vector> getShopLocations() { return shopLocations; }
    public Map<TeamColor, Vector> getUpgradeLocations() { return upgradeLocations; }
    public Map<TeamColor, Vector> getIronGenLocations() { return ironGenLocations; }
    public Map<TeamColor, Vector> getGoldGenLocations() { return goldGenLocations; }
    public List<Vector> getDiamondGenLocations() { return diamondGenLocations; }
    public List<Vector> getEmeraldGenLocations() { return emeraldGenLocations; }
}
