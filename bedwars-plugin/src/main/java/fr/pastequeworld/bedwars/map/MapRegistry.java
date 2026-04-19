package fr.pastequeworld.bedwars.map;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.game.GameMode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Charge et expose les {@link MapTemplate}s disponibles sur disque.
 * - Scan plugins/PastequeBedWars/maps/*.yml pour les templates explicites.
 * - Auto-enregistre les templates "world-folder" trouves dans
 *   plugins/PastequeBedWars/maps-worlds/<id>/ ou <server-root>/<id>/ et qui
 *   correspondent aux conventions (mapbedwars, mapbedwars2..5). Ces templates
 *   sont en mode "auto-detect" : leurs mecanismes sont scannes au chargement
 *   de l'arene.
 *
 * Selecteur aleatoire par mode, filtre sur les maps qui supportent ce mode.
 */
public class MapRegistry {

    private static final String[] DEFAULT_WORLD_MAPS = new String[] {
            "mapbedwars", "mapbedwars2", "mapbedwars3", "mapbedwars4", "mapbedwars5"
    };

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
        loadAutoWorldFolderTemplates();
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
     * Pour chaque nom conventionnel de map-monde (mapbedwars, mapbedwars2, ...),
     * si un dossier template existe et qu'aucun YAML correspondant n'a deja ete
     * enregistre, on cree un MapTemplate minimal en mode auto-detect, supportant
     * tous les modes de jeu. Les mecanismes seront detectes au runtime.
     */
    private void loadAutoWorldFolderTemplates() {
        for (String id : DEFAULT_WORLD_MAPS) {
            if (templates.containsKey(id)) continue;
            File folder = worldTemplateLoader.resolveTemplateFolder(id);
            if (folder == null) continue;

            MapTemplate t = new MapTemplate(id);
            t.setDisplayName(id);
            t.setWorldTemplate(id);
            t.setAutoDetect(true);
            t.setSupportedModes(new ArrayList<GameMode>(Arrays.asList(
                    GameMode.SOLO, GameMode.DUO, GameMode.TEAMS)));
            templates.put(id, t);
            plugin.getLogger().info("Map auto-enregistree (dossier monde): " + id
                    + " -> " + folder.getAbsolutePath());
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
