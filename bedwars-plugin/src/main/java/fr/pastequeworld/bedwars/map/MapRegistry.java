package fr.pastequeworld.bedwars.map;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.game.GameMode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Charge et expose les {@link MapTemplate}s disponibles sur disque.
 * Scans plugins/PastequeBedWars/maps/*.yml au demarrage.
 *
 * Fournit un selecteur aleatoire par mode, filtre sur les maps qui supportent ce mode.
 */
public class MapRegistry {

    private final BedWarsPlugin plugin;
    private final Map<String, MapTemplate> templates = new LinkedHashMap<String, MapTemplate>();
    private final Random random = new Random();

    public MapRegistry(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
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
                plugin.getLogger().info("Map chargee: " + template.getId() + " ("
                        + template.getSupportedModes().size() + " modes)");
            } catch (IOException e) {
                plugin.getLogger().warning("Erreur chargement map " + file.getName() + ": " + e.getMessage());
            }
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
        // fallback : si pas declarees avec supported-modes, on prend toutes les maps
        if (list.isEmpty()) {
            list = new ArrayList<MapTemplate>(templates.values());
        }
        if (list.isEmpty()) return null;
        Collections.shuffle(list, random);
        return list.get(0);
    }
}
