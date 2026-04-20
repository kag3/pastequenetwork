package fr.pastequeworld.bedwars.map;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.game.GameMode;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Charge les MapTemplate disponibles.
 *
 * Sources (par ordre de priorite) :
 *   1. plugins/PastequeBedWars/maps/*.yml     -> templates explicites YAML
 *   2. plugins/PastequeBedWars/schematics/*.schematic -> auto-enregistre
 *      chaque .schematic comme une map autonome en mode auto-detect (tous
 *      les mecanismes sont detectes apres le paste dans l'arene).
 *
 * La schematic reservee "Spawn.schematic" (petit lobby pattern) n'est pas
 * utilisable comme map de jeu : elle est skip.
 */
public class MapRegistry {

    /** Noms reserves : ces schematics ne sont PAS des maps jouables. */
    private static final List<String> RESERVED_SCHEMATICS = Arrays.asList(
            "spawn", "bedwarslobby", "bedwarslobbyy", "lobby"
    );

    private final BedWarsPlugin plugin;
    private final Map<String, MapTemplate> templates = new LinkedHashMap<String, MapTemplate>();
    private final WorldTemplateLoader worldTemplateLoader;
    private final Random random = new Random();

    public MapRegistry(BedWarsPlugin plugin) {
        this.plugin = plugin;
        this.worldTemplateLoader = new WorldTemplateLoader(plugin);
    }

    public WorldTemplateLoader getWorldTemplateLoader() { return worldTemplateLoader; }

    public void load() {
        templates.clear();
        loadYamlTemplates();
        loadSchematicTemplates();
        plugin.getLogger().info("MapRegistry : " + templates.size() + " map(s) chargee(s) : " + templates.keySet());
    }

    private void loadYamlTemplates() {
        File mapsDir = new File(plugin.getDataFolder(), "maps");
        if (!mapsDir.exists() && !mapsDir.mkdirs()) {
            plugin.getLogger().warning("Impossible de creer le dossier maps/");
            return;
        }
        File[] files = mapsDir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (!file.getName().toLowerCase().endsWith(".yml")) continue;
            try {
                MapTemplate template = MapTemplate.loadFromFile(file);
                templates.put(template.getId(), template);
                plugin.getLogger().info("Map chargee (YAML): " + template.getId());
            } catch (IOException e) {
                plugin.getLogger().warning("Erreur chargement map " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Pour chaque .schematic trouve dans plugins/PastequeBedWars/schematics/
     * et qui n'est pas reserve, cree un MapTemplate schematic-based + auto-detect.
     * Supporte tous les modes de jeu (SOLO, DUO, TEAMS) car toutes les maps
     * Hypixel 4-teams fournissent 4 lits -> compatibles avec 4x1, 4x2 et 4x4.
     */
    private void loadSchematicTemplates() {
        File folder = new File(plugin.getDataFolder(), plugin.getConfigManager().getSchematicFolder());
        if (!folder.isDirectory()) return;
        File[] files = folder.listFiles();
        if (files == null) return;

        // Offset par defaut : haut de world, centre sur 0,0
        // (l'auto-detect s'appuiera sur le bbox reel du schematic)
        Vector defaultPasteOffset = new Vector(0, 64, 0);

        for (File file : files) {
            String name = file.getName();
            if (!name.toLowerCase(Locale.ROOT).endsWith(".schematic")) continue;

            String idRaw = name.substring(0, name.length() - ".schematic".length());
            String key = idRaw.toLowerCase(Locale.ROOT);
            if (RESERVED_SCHEMATICS.contains(key)) {
                plugin.getLogger().info("Schematic reservee, skip: " + name);
                continue;
            }
            String id = "bw_" + key;
            if (templates.containsKey(id)) continue;

            MapTemplate t = new MapTemplate(id);
            t.setDisplayName(idRaw);
            t.setSchematic(name);
            t.setAutoDetect(true);
            t.setPasteOffset(defaultPasteOffset);
            t.setSupportedModes(new ArrayList<GameMode>(Arrays.asList(
                    GameMode.SOLO, GameMode.DUO, GameMode.TEAMS)));
            templates.put(id, t);
            plugin.getLogger().info("Map chargee (schematic): " + id + " -> " + name);
        }
    }

    public MapTemplate get(String id) { return templates.get(id); }

    public List<String> getAllMapIds() { return new ArrayList<String>(templates.keySet()); }

    public List<MapTemplate> getMapsForMode(GameMode mode) {
        List<MapTemplate> list = new ArrayList<MapTemplate>();
        for (MapTemplate t : templates.values()) {
            if (t.getSupportedModes().contains(mode)) list.add(t);
        }
        return list;
    }

    public MapTemplate pickRandomForMode(GameMode mode) {
        List<MapTemplate> list = getMapsForMode(mode);
        if (list.isEmpty()) list = new ArrayList<MapTemplate>(templates.values());
        if (list.isEmpty()) return null;
        Collections.shuffle(list, random);
        return list.get(0);
    }
}
