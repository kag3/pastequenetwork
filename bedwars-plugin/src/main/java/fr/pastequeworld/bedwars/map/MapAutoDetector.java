package fr.pastequeworld.bedwars.map;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.team.TeamColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Auto-detect robuste des mecanismes BedWars dans une schematic Hypixel 4-teams.
 *
 * Apres paste de la schematic a {@code pasteOrigin} :
 *   1. Scan de la zone (air skippe) :
 *      - BED_BLOCK (8 blocs = 4 lits tete+pied)
 *      - DIAMOND_BLOCK (4 ilots) / EMERALD_BLOCK (2 spots)
 *      - WOOL colore (fallback couleur si present, cas Swashbuckle)
 *      - IRON / GOLD / ENDER_STONE / OBSIDIAN (optionnels)
 *   2. Merge des paires tete+pied du lit.
 *   3. Assignation team <-> lit :
 *      a. Si la laine adjacente resout une couleur unique par lit : on l'utilise.
 *      b. Sinon : orientation cardinale (angle autour du centre de la map) ->
 *         ordre RED / BLUE / GREEN / YELLOW a partir de +X rotationnel.
 *   4. Placement Hypixel des PNJ/generateurs : relatif au lit, oriente vers le
 *      centre (forward) et perpendiculaire (right). Distances standards Hypixel.
 *   5. Toutes les positions sont ecrites RELATIVES a {@code pasteOrigin} afin
 *      qu'elles soient reutilisables entre plusieurs arenes (meme template).
 *   6. Cache YAML dans maps-cache/<id>.yml : scan une seule fois, boot instant
 *      au reuse.
 */
public class MapAutoDetector {

    private final BedWarsPlugin plugin;

    public MapAutoDetector(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Compat : {@code pasteOrigin = (0,0,0)} (mode world-folder). */
    public void detect(World world, MapTemplate template, Location center, int radius) {
        detect(world, template, center, radius, new Vector(0, 0, 0));
    }

    /**
     * Detecte les mecanismes. Les positions ecrites dans le template sont
     * RELATIVES a {@code pasteOrigin} : {@code Arena.materialize(rel)} produit
     * la Location absolue dans le monde courant.
     */
    public void detect(World world, MapTemplate template, Location center, int radius, Vector pasteOrigin) {
        File cacheFile = cacheFileFor(template.getId());
        if (cacheFile.isFile() && loadCache(cacheFile, template)) {
            plugin.getLogger().info("Map '" + template.getId() + "' chargee depuis cache ("
                    + template.getBedLocations().size() + " teams).");
            return;
        }

        scanWorld(world, template, center, radius, pasteOrigin);
        try {
            saveCache(cacheFile, template);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible d'ecrire le cache " + cacheFile.getName() + ": " + e.getMessage());
        }
    }

    // === SCAN ===

    private void scanWorld(World world, MapTemplate template, Location center, int radius, Vector pasteOrigin) {
        int cx0 = (center.getBlockX() - radius) >> 4;
        int cx1 = (center.getBlockX() + radius) >> 4;
        int cz0 = (center.getBlockZ() - radius) >> 4;
        int cz1 = (center.getBlockZ() + radius) >> 4;
        int yMin = Math.max(0, center.getBlockY() - 40);
        int yMax = Math.min(255, center.getBlockY() + 80);

        List<Vector> beds = new ArrayList<Vector>();
        Map<Integer, List<Vector>> woolByColor = new HashMap<Integer, List<Vector>>();
        List<Vector> ironBlocks = new ArrayList<Vector>();
        List<Vector> goldBlocks = new ArrayList<Vector>();
        List<Vector> diamondBlocks = new ArrayList<Vector>();
        List<Vector> emeraldBlocks = new ArrayList<Vector>();
        List<Vector> enderStones = new ArrayList<Vector>();
        List<Vector> obsidians = new ArrayList<Vector>();

        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                Chunk chunk = world.getChunkAt(cx, cz);
                if (!chunk.isLoaded() && !chunk.load(false)) continue;
                for (int lx = 0; lx < 16; lx++) {
                    for (int lz = 0; lz < 16; lz++) {
                        int wx = (cx << 4) + lx;
                        int wz = (cz << 4) + lz;
                        for (int y = yMin; y <= yMax; y++) {
                            Block b = world.getBlockAt(wx, y, wz);
                            Material type = b.getType();
                            if (type == Material.AIR) continue;

                            Vector pos = new Vector(wx, y, wz);
                            if (type == Material.BED_BLOCK || type == Material.BED) {
                                beds.add(pos);
                            } else if (type == Material.WOOL) {
                                int data = b.getData() & 0xF;
                                List<Vector> list = woolByColor.get(data);
                                if (list == null) { list = new ArrayList<Vector>(); woolByColor.put(data, list); }
                                list.add(pos);
                            } else if (type == Material.DIAMOND_BLOCK) {
                                diamondBlocks.add(pos);
                            } else if (type == Material.EMERALD_BLOCK) {
                                emeraldBlocks.add(pos);
                            } else if (type == Material.IRON_BLOCK) {
                                ironBlocks.add(pos);
                            } else if (type == Material.GOLD_BLOCK) {
                                goldBlocks.add(pos);
                            } else if (type == Material.ENDER_STONE) {
                                enderStones.add(pos);
                            } else if (type == Material.OBSIDIAN) {
                                obsidians.add(pos);
                            }
                        }
                    }
                }
            }
        }

        // Merge pairs tete+pied du lit (adjacents XZ, meme Y)
        List<Vector> mergedBeds = mergeClose(beds, 1.5);

        // Cluster diamonds / emeralds (blocs adjacents = un seul spot)
        List<Vector> diamondSpots = mergeClose(diamondBlocks, 3.0);
        List<Vector> emeraldSpots = mergeClose(emeraldBlocks, 3.0);

        // Centre de la map : moyenne des lits (robuste car toujours 4-symetrique)
        Vector mapCenter;
        if (!mergedBeds.isEmpty()) {
            double sx = 0, sz = 0, sy = 0;
            for (Vector v : mergedBeds) { sx += v.getX(); sy += v.getY(); sz += v.getZ(); }
            int n = mergedBeds.size();
            mapCenter = new Vector(sx / n, sy / n, sz / n);
        } else if (!diamondSpots.isEmpty() || !emeraldSpots.isEmpty()) {
            double sx = 0, sz = 0, sy = 0; int n = 0;
            for (Vector v : diamondSpots) { sx += v.getX(); sy += v.getY(); sz += v.getZ(); n++; }
            for (Vector v : emeraldSpots) { sx += v.getX(); sy += v.getY(); sz += v.getZ(); n++; }
            mapCenter = new Vector(sx / n, sy / n, sz / n);
        } else {
            mapCenter = new Vector(center.getX(), center.getY(), center.getZ());
        }

        // Assignation lit <-> couleur : priorite wool adjacente, fallback cardinal
        Map<TeamColor, Vector> bedByColor = assignBedColorsCardinal(mergedBeds, woolByColor, mapCenter);

        // Assignation iron/gold/shop/upgrade : bloc present le plus proche du lit
        Map<TeamColor, Vector> ironByColor = assignToNearestBed(ironBlocks, bedByColor);
        Map<TeamColor, Vector> goldByColor = assignToNearestBed(goldBlocks, bedByColor);
        Map<TeamColor, Vector> shopByColor = assignToNearestBed(enderStones, bedByColor);
        Map<TeamColor, Vector> upgradeByColor = assignToNearestBed(obsidians, bedByColor);

        // Remplissage du template (positions RELATIVES au pasteOrigin)
        template.getBedLocations().clear();
        template.getSpawnLocations().clear();
        template.getShopLocations().clear();
        template.getUpgradeLocations().clear();
        template.getIronGenLocations().clear();
        template.getGoldGenLocations().clear();
        template.getDiamondGenLocations().clear();
        template.getEmeraldGenLocations().clear();

        for (Map.Entry<TeamColor, Vector> e : bedByColor.entrySet()) {
            TeamColor color = e.getKey();
            Vector bed = e.getValue();

            Vector fwd = unitXZ(mapCenter.getX() - bed.getX(), mapCenter.getZ() - bed.getZ());
            Vector right = new Vector(-fwd.getZ(), 0, fwd.getX());

            // Spawn : 3 blocs devant le lit, meme Y (le sol est deja la)
            Vector spawn = hypixelOffset(bed, fwd, 3, right, 0, 1);
            // Generators iron/gold : 4 blocs devant, gold decale de +1 sur la droite
            Vector iron = ironByColor.get(color);
            Vector gold = goldByColor.get(color);
            if (iron == null) iron = hypixelOffset(bed, fwd, 4, right, 0, 1);
            if (gold == null) gold = hypixelOffset(bed, fwd, 4, right, 2, 1);
            // Shops/upgrades : 5 blocs devant le lit, decales
            Vector shop = shopByColor.get(color);
            Vector upg  = upgradeByColor.get(color);
            if (shop == null) shop = hypixelOffset(bed, fwd, 5, right, -3, 1);
            if (upg  == null) upg  = hypixelOffset(bed, fwd, 5, right,  3, 1);

            template.getBedLocations().put(color, relative(bed, pasteOrigin));
            template.getSpawnLocations().put(color, relative(spawn, pasteOrigin));
            template.getIronGenLocations().put(color, relative(iron, pasteOrigin));
            template.getGoldGenLocations().put(color, relative(gold, pasteOrigin));
            template.getShopLocations().put(color, relative(shop, pasteOrigin));
            template.getUpgradeLocations().put(color, relative(upg, pasteOrigin));
        }

        for (Vector v : diamondSpots) template.getDiamondGenLocations().add(relative(v, pasteOrigin));
        for (Vector v : emeraldSpots) template.getEmeraldGenLocations().add(relative(v, pasteOrigin));

        // Fallback diamond/emerald si aucune detection (peu probable mais garde-fou)
        injectDiamondEmeraldFallback(template, bedByColor, mapCenter, pasteOrigin);

        // Queue spawn : 40 blocs au-dessus du centre
        template.setQueueSpawn(new Vector(
                mapCenter.getX() - pasteOrigin.getX(),
                mapCenter.getY() - pasteOrigin.getY() + 40,
                mapCenter.getZ() - pasteOrigin.getZ()));

        plugin.getLogger().info(String.format(
                "Auto-detect '%s' : %d teams, %d lit(s) brut, %d diamant, %d emeraude, centre=(%d,%d,%d)",
                template.getId(), bedByColor.size(), beds.size(),
                diamondSpots.size(), emeraldSpots.size(),
                (int) mapCenter.getX(), (int) mapCenter.getY(), (int) mapCenter.getZ()));
    }

    // === Assignation lit <-> couleur (cardinal + wool fallback) ===

    /**
     * Priorite 1 : essaie la laine adjacente au lit. Si chaque lit resout une
     * couleur UNIQUE (pas de doublon), on garde ce mapping.
     * Priorite 2 : fallback orientation cardinale autour du centre de la map,
     * ordre stable RED / BLUE / GREEN / YELLOW (angle 0,90,180,270 a partir de +X).
     */
    private Map<TeamColor, Vector> assignBedColorsCardinal(List<Vector> beds,
                                                           Map<Integer, List<Vector>> woolByColor,
                                                           Vector mapCenter) {
        // Tentative wool-color
        Map<TeamColor, Vector> fromWool = tryAssignByWool(beds, woolByColor);
        if (fromWool != null && fromWool.size() == beds.size()) {
            return fromWool;
        }

        // Fallback cardinal : trie les lits par angle autour du centre
        List<Vector> sorted = new ArrayList<Vector>(beds);
        Collections.sort(sorted, new Comparator<Vector>() {
            @Override
            public int compare(Vector a, Vector b) {
                double aa = Math.atan2(a.getZ() - mapCenter.getZ(), a.getX() - mapCenter.getX());
                double ab = Math.atan2(b.getZ() - mapCenter.getZ(), b.getX() - mapCenter.getX());
                return Double.compare(aa, ab);
            }
        });
        Map<TeamColor, Vector> result = new EnumMap<TeamColor, Vector>(TeamColor.class);
        TeamColor[] order = { TeamColor.RED, TeamColor.BLUE, TeamColor.GREEN, TeamColor.YELLOW,
                TeamColor.AQUA, TeamColor.WHITE, TeamColor.PINK, TeamColor.GRAY };
        for (int i = 0; i < sorted.size() && i < order.length; i++) {
            result.put(order[i], sorted.get(i));
        }
        return result;
    }

    private Map<TeamColor, Vector> tryAssignByWool(List<Vector> beds, Map<Integer, List<Vector>> woolByColor) {
        Map<TeamColor, Vector> result = new EnumMap<TeamColor, Vector>(TeamColor.class);
        for (Vector bed : beds) {
            TeamColor c = findClosestWoolColor(bed, woolByColor, 5.0);
            if (c == null) return null;
            if (result.containsKey(c)) return null; // collision
            result.put(c, bed);
        }
        return result;
    }

    private TeamColor findClosestWoolColor(Vector bed, Map<Integer, List<Vector>> woolByColor, double maxDist) {
        double best = maxDist * maxDist;
        Integer bestData = null;
        for (Map.Entry<Integer, List<Vector>> e : woolByColor.entrySet()) {
            for (Vector w : e.getValue()) {
                double dx = w.getX() - bed.getX();
                double dy = w.getY() - bed.getY();
                double dz = w.getZ() - bed.getZ();
                double d = dx * dx + dy * dy + dz * dz;
                if (d < best) { best = d; bestData = e.getKey(); }
            }
        }
        if (bestData == null) return null;
        return woolDataToTeam(bestData);
    }

    /** Mapping wool data (1.9) -> TeamColor du plugin. */
    private TeamColor woolDataToTeam(int data) {
        switch (data) {
            case 4:  return TeamColor.YELLOW;   // yellow
            case 5:  return TeamColor.GREEN;    // lime
            case 11: return TeamColor.BLUE;     // blue
            case 14: return TeamColor.RED;      // red
            case 1:  return TeamColor.YELLOW;   // orange -> yellow
            case 13: return TeamColor.GREEN;    // green -> green
            case 10: return TeamColor.BLUE;     // purple -> blue
            case 3:  return TeamColor.AQUA;     // light blue
            case 9:  return TeamColor.AQUA;     // cyan
            case 6:  return TeamColor.PINK;     // pink
            case 2:  return TeamColor.PINK;     // magenta -> pink
            case 0:  return TeamColor.WHITE;    // white
            case 7:
            case 8:
            case 15: return TeamColor.GRAY;     // gris/noir -> gray
            default: return null;
        }
    }

    // === Utilitaires ===

    private Map<TeamColor, Vector> assignToNearestBed(List<Vector> candidates, Map<TeamColor, Vector> beds) {
        Map<TeamColor, Vector> result = new EnumMap<TeamColor, Vector>(TeamColor.class);
        if (beds.isEmpty()) return result;
        for (Vector c : candidates) {
            TeamColor nearest = null;
            double bestDist = Double.MAX_VALUE;
            for (Map.Entry<TeamColor, Vector> b : beds.entrySet()) {
                double dx = b.getValue().getX() - c.getX();
                double dz = b.getValue().getZ() - c.getZ();
                double d = dx * dx + dz * dz;
                if (d < bestDist) { bestDist = d; nearest = b.getKey(); }
            }
            if (nearest == null) continue;
            Vector existing = result.get(nearest);
            if (existing == null || c.getY() > existing.getY()) {
                result.put(nearest, c);
            }
        }
        return result;
    }

    private List<Vector> mergeClose(List<Vector> input, double threshold) {
        List<Vector> result = new ArrayList<Vector>();
        double t2 = threshold * threshold;
        for (Vector v : input) {
            boolean merged = false;
            for (int i = 0; i < result.size(); i++) {
                Vector r = result.get(i);
                double dx = r.getX() - v.getX();
                double dy = r.getY() - v.getY();
                double dz = r.getZ() - v.getZ();
                if (dx * dx + dy * dy + dz * dz <= t2) {
                    result.set(i, new Vector((r.getX() + v.getX()) / 2,
                            (r.getY() + v.getY()) / 2,
                            (r.getZ() + v.getZ()) / 2));
                    merged = true;
                    break;
                }
            }
            if (!merged) result.add(v.clone());
        }
        return result;
    }

    private Vector hypixelOffset(Vector bed, Vector fwd, double fDist, Vector right, double rDist, double upY) {
        double x = bed.getX() + fwd.getX() * fDist + right.getX() * rDist + 0.5;
        double z = bed.getZ() + fwd.getZ() * fDist + right.getZ() * rDist + 0.5;
        double y = bed.getY() + upY;
        return new Vector(x, y, z);
    }

    private Vector unitXZ(double dx, double dz) {
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.01) return new Vector(1, 0, 0);
        return new Vector(dx / len, 0, dz / len);
    }

    private void injectDiamondEmeraldFallback(MapTemplate template,
                                              Map<TeamColor, Vector> bedByColor,
                                              Vector mapCenter,
                                              Vector pasteOrigin) {
        double meanBedDist = 0; int n = 0;
        for (Vector bed : bedByColor.values()) {
            double dx = bed.getX() - mapCenter.getX();
            double dz = bed.getZ() - mapCenter.getZ();
            meanBedDist += Math.sqrt(dx * dx + dz * dz); n++;
        }
        if (n > 0) meanBedDist /= n;
        double diamondR = meanBedDist > 1 ? meanBedDist * 0.55 : 25;
        double y = mapCenter.getY();

        if (template.getDiamondGenLocations().isEmpty()) {
            template.getDiamondGenLocations().add(relative(new Vector(mapCenter.getX() + diamondR, y, mapCenter.getZ()), pasteOrigin));
            template.getDiamondGenLocations().add(relative(new Vector(mapCenter.getX() - diamondR, y, mapCenter.getZ()), pasteOrigin));
            template.getDiamondGenLocations().add(relative(new Vector(mapCenter.getX(), y, mapCenter.getZ() + diamondR), pasteOrigin));
            template.getDiamondGenLocations().add(relative(new Vector(mapCenter.getX(), y, mapCenter.getZ() - diamondR), pasteOrigin));
        }
        if (template.getEmeraldGenLocations().isEmpty()) {
            template.getEmeraldGenLocations().add(relative(new Vector(mapCenter.getX() + 4, y, mapCenter.getZ()), pasteOrigin));
            template.getEmeraldGenLocations().add(relative(new Vector(mapCenter.getX() - 4, y, mapCenter.getZ()), pasteOrigin));
        }
    }

    private Vector relative(Vector worldPos, Vector pasteOrigin) {
        return new Vector(
                worldPos.getX() - pasteOrigin.getX(),
                worldPos.getY() - pasteOrigin.getY(),
                worldPos.getZ() - pasteOrigin.getZ());
    }

    // === Cache YAML ===

    private File cacheFileFor(String templateId) {
        File folder = new File(plugin.getDataFolder(), "maps-cache");
        if (!folder.exists()) //noinspection ResultOfMethodCallIgnored
            folder.mkdirs();
        return new File(folder, templateId + ".yml");
    }

    private boolean loadCache(File file, MapTemplate template) {
        try {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            if (cfg.getKeys(false).isEmpty()) return false;

            template.getBedLocations().clear();
            template.getSpawnLocations().clear();
            template.getShopLocations().clear();
            template.getUpgradeLocations().clear();
            template.getIronGenLocations().clear();
            template.getGoldGenLocations().clear();
            template.getDiamondGenLocations().clear();
            template.getEmeraldGenLocations().clear();

            readTeamMap(cfg, "beds", template.getBedLocations());
            readTeamMap(cfg, "spawns", template.getSpawnLocations());
            readTeamMap(cfg, "shops", template.getShopLocations());
            readTeamMap(cfg, "upgrades", template.getUpgradeLocations());
            readTeamMap(cfg, "iron", template.getIronGenLocations());
            readTeamMap(cfg, "gold", template.getGoldGenLocations());
            readVectorList(cfg, "diamond", template.getDiamondGenLocations());
            readVectorList(cfg, "emerald", template.getEmeraldGenLocations());

            if (cfg.isConfigurationSection("queue-spawn")) {
                template.setQueueSpawn(new Vector(
                        cfg.getDouble("queue-spawn.x"),
                        cfg.getDouble("queue-spawn.y"),
                        cfg.getDouble("queue-spawn.z")));
            }
            return !template.getBedLocations().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private void readTeamMap(FileConfiguration cfg, String path, Map<TeamColor, Vector> target) {
        if (!cfg.isConfigurationSection(path)) return;
        for (String key : cfg.getConfigurationSection(path).getKeys(false)) {
            try {
                TeamColor color = TeamColor.valueOf(key.toUpperCase());
                target.put(color, new Vector(
                        cfg.getDouble(path + "." + key + ".x"),
                        cfg.getDouble(path + "." + key + ".y"),
                        cfg.getDouble(path + "." + key + ".z")));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void readVectorList(FileConfiguration cfg, String path, List<Vector> target) {
        List<Map<?, ?>> list = cfg.getMapList(path);
        for (Map<?, ?> raw : list) {
            target.add(new Vector(
                    ((Number) raw.get("x")).doubleValue(),
                    ((Number) raw.get("y")).doubleValue(),
                    ((Number) raw.get("z")).doubleValue()));
        }
    }

    private void saveCache(File file, MapTemplate template) throws IOException {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("id", template.getId());
        writeTeamMap(cfg, "beds", template.getBedLocations());
        writeTeamMap(cfg, "spawns", template.getSpawnLocations());
        writeTeamMap(cfg, "shops", template.getShopLocations());
        writeTeamMap(cfg, "upgrades", template.getUpgradeLocations());
        writeTeamMap(cfg, "iron", template.getIronGenLocations());
        writeTeamMap(cfg, "gold", template.getGoldGenLocations());
        writeVectorList(cfg, "diamond", template.getDiamondGenLocations());
        writeVectorList(cfg, "emerald", template.getEmeraldGenLocations());
        Vector qs = template.getQueueSpawn();
        if (qs != null) {
            cfg.set("queue-spawn.x", qs.getX());
            cfg.set("queue-spawn.y", qs.getY());
            cfg.set("queue-spawn.z", qs.getZ());
        }
        cfg.save(file);
    }

    private void writeTeamMap(YamlConfiguration cfg, String path, Map<TeamColor, Vector> src) {
        for (Map.Entry<TeamColor, Vector> e : src.entrySet()) {
            String base = path + "." + e.getKey().name();
            cfg.set(base + ".x", e.getValue().getX());
            cfg.set(base + ".y", e.getValue().getY());
            cfg.set(base + ".z", e.getValue().getZ());
        }
    }

    private void writeVectorList(YamlConfiguration cfg, String path, List<Vector> src) {
        List<Map<String, Double>> list = new ArrayList<Map<String, Double>>();
        for (Vector v : src) {
            Map<String, Double> m = new HashMap<String, Double>();
            m.put("x", v.getX());
            m.put("y", v.getY());
            m.put("z", v.getZ());
            list.add(m);
        }
        cfg.set(path, list);
    }
}
