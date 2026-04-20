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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detection automatique + robuste des mecanismes d'une map de BedWars.
 *
 * Specificite 1.9.4 : TOUS les BED_BLOCK ont le meme data (direction/etat, pas
 * de couleur comme en 1.12+). La seule maniere fiable d'associer un lit a une
 * equipe est la LAINE COLOREE adjacente (convention Hypixel classique).
 *
 * Pipeline :
 *   1. Scan plein du monde (chunks charges a la volee) :
 *      - BED_BLOCK  -> candidates de lit (tete+pied)
 *      - WOOL       -> liste de laines colorees
 *      - DIAMOND_BLOCK / EMERALD_BLOCK -> spots publics
 *      - IRON_BLOCK / GOLD_BLOCK       -> gens d'equipe
 *      - ENDER_STONE / OBSIDIAN        -> villageois
 *   2. Merge des paires de lits (tete+pied) en une seule position.
 *   3. Association lit<->couleur via laine la plus proche (rayon 4 blocs).
 *      Si pas de laine : fallback round-robin sur TeamColor.firstN.
 *   4. Clustering des diamond/emerald blocks voisins (merge <3 blocs).
 *   5. Attribution iron/gold/shop/upgrade au team le plus proche.
 *   6. Fallback deterministe pour tout ce qui manque.
 *   7. Cache YAML : les resultats sont persistes dans maps-cache/<id>.yml
 *      et rechargees au prochain boot (scan une seule fois, demarrage instant).
 */
public class MapAutoDetector {

    private final BedWarsPlugin plugin;

    public MapAutoDetector(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Si un cache existe pour ce template, le charge. Sinon scan le monde,
     * peuple le template et ecrit le cache.
     */
    public void detect(World world, MapTemplate template, Location center, int radius) {
        File cacheFile = cacheFileFor(template.getId());
        if (cacheFile.isFile() && loadCache(cacheFile, template)) {
            plugin.getLogger().info("Map '" + template.getId() + "' chargee depuis le cache ("
                    + template.getBedLocations().size() + " teams).");
            return;
        }

        scanWorld(world, template, center, radius);
        try {
            saveCache(cacheFile, template);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible d'ecrire le cache " + cacheFile.getName() + ": " + e.getMessage());
        }
    }

    // === SCAN ===

    private void scanWorld(World world, MapTemplate template, Location center, int radius) {
        int minX = (center == null ? 0 : center.getBlockX()) - radius;
        int maxX = (center == null ? 0 : center.getBlockX()) + radius;
        int minZ = (center == null ? 0 : center.getBlockZ()) - radius;
        int maxZ = (center == null ? 0 : center.getBlockZ()) + radius;
        int cxMin = minX >> 4, cxMax = maxX >> 4;
        int czMin = minZ >> 4, czMax = maxZ >> 4;

        List<Vector> beds = new ArrayList<Vector>();
        Map<Integer, List<Vector>> woolByColor = new HashMap<Integer, List<Vector>>();
        List<Vector> ironBlocks = new ArrayList<Vector>();
        List<Vector> goldBlocks = new ArrayList<Vector>();
        List<Vector> diamondBlocks = new ArrayList<Vector>();
        List<Vector> emeraldBlocks = new ArrayList<Vector>();
        List<Vector> enderStones = new ArrayList<Vector>();
        List<Vector> obsidians = new ArrayList<Vector>();

        for (int cx = cxMin; cx <= cxMax; cx++) {
            for (int cz = czMin; cz <= czMax; cz++) {
                Chunk chunk = world.getChunkAt(cx, cz);
                if (!chunk.isLoaded() && !chunk.load(false)) continue;
                for (int lx = 0; lx < 16; lx++) {
                    for (int lz = 0; lz < 16; lz++) {
                        int wx = (cx << 4) + lx;
                        int wz = (cz << 4) + lz;
                        for (int y = 10; y <= 150; y++) {
                            Block b = world.getBlockAt(wx, y, wz);
                            Material type = b.getType();
                            if (type == Material.AIR) continue;

                            if (type == Material.BED_BLOCK || type == Material.BED) {
                                // Ne garder que la tete du lit (bit 0x8)
                                if ((b.getData() & 0x8) != 0) {
                                    beds.add(new Vector(wx, y, wz));
                                }
                            } else if (type == Material.WOOL) {
                                int data = b.getData() & 0xF;
                                List<Vector> list = woolByColor.get(data);
                                if (list == null) { list = new ArrayList<Vector>(); woolByColor.put(data, list); }
                                list.add(new Vector(wx, y, wz));
                            } else if (type == Material.DIAMOND_BLOCK) {
                                diamondBlocks.add(new Vector(wx, y, wz));
                            } else if (type == Material.EMERALD_BLOCK) {
                                emeraldBlocks.add(new Vector(wx, y, wz));
                            } else if (type == Material.IRON_BLOCK) {
                                ironBlocks.add(new Vector(wx, y, wz));
                            } else if (type == Material.GOLD_BLOCK) {
                                goldBlocks.add(new Vector(wx, y, wz));
                            } else if (type == Material.ENDER_STONE) {
                                enderStones.add(new Vector(wx, y, wz));
                            } else if (type == Material.OBSIDIAN) {
                                obsidians.add(new Vector(wx, y, wz));
                            }
                        }
                    }
                }
            }
        }

        // Si aucun lit trouve en gardant seulement la tete, elargis a tous les beds.
        if (beds.isEmpty()) {
            for (int cx = cxMin; cx <= cxMax; cx++) {
                for (int cz = czMin; cz <= czMax; cz++) {
                    Chunk chunk = world.getChunkAt(cx, cz);
                    if (!chunk.isLoaded()) continue;
                    for (int lx = 0; lx < 16; lx++)
                        for (int lz = 0; lz < 16; lz++)
                            for (int y = 10; y <= 150; y++) {
                                Block b = world.getBlockAt((cx << 4) + lx, y, (cz << 4) + lz);
                                if (b.getType() == Material.BED_BLOCK || b.getType() == Material.BED) {
                                    beds.add(new Vector((cx << 4) + lx, y, (cz << 4) + lz));
                                }
                            }
                }
            }
            // Deduplique les paires tete/pied (<=1.5 blocs)
            beds = mergeClose(beds, 1.5);
        }

        // Clustering diamond/emerald (blocs adjacents = un seul spot)
        List<Vector> diamondSpots = mergeClose(diamondBlocks, 3.0);
        List<Vector> emeraldSpots = mergeClose(emeraldBlocks, 3.0);

        // Centre de la map : moyenne des spots publics, fallback origine
        Vector mapCenter;
        if (!diamondSpots.isEmpty() || !emeraldSpots.isEmpty()) {
            double sx = 0, sz = 0; int n = 0;
            for (Vector v : diamondSpots) { sx += v.getX(); sz += v.getZ(); n++; }
            for (Vector v : emeraldSpots) { sx += v.getX(); sz += v.getZ(); n++; }
            mapCenter = new Vector(sx / n, 64, sz / n);
        } else {
            mapCenter = new Vector(0, 64, 0);
        }

        // Attribution lit -> couleur via laine la plus proche
        Map<TeamColor, Vector> bedByColor = assignBedColors(beds, woolByColor);

        // Attribution iron/gold/shop/upgrade au lit le plus proche
        Map<TeamColor, Vector> ironByColor = assignToNearestBed(ironBlocks, bedByColor);
        Map<TeamColor, Vector> goldByColor = assignToNearestBed(goldBlocks, bedByColor);
        Map<TeamColor, Vector> shopByColor = assignToNearestBed(enderStones, bedByColor);
        Map<TeamColor, Vector> upgradeByColor = assignToNearestBed(obsidians, bedByColor);

        // Remplissage du template
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
            template.getBedLocations().put(color, bed);
            template.getSpawnLocations().put(color, findSpawnNear(world, bed, mapCenter));

            Vector iron = ironByColor.get(color);
            Vector gold = goldByColor.get(color);
            Vector shop = shopByColor.get(color);
            Vector upg  = upgradeByColor.get(color);

            // Fallback agencement Hypixel. Axe (forward, right) oriente vers le
            // centre de la map, perpendiculaire pour right :
            //   bed (arriere) -> spawn (+1 fwd) -> iron (+3 fwd, milieu de l'ile)
            //                                   -> gold (+3 fwd, +1 right)
            //   shops sur les bords (+4 fwd, +/-3 right)
            Vector fwd = unitXZToCenter(bed, mapCenter);
            Vector right = new Vector(-fwd.getZ(), 0, fwd.getX());
            if (iron == null) iron = hypixelOffset(bed, fwd, 3, right, 0, 1);
            if (gold == null) gold = hypixelOffset(bed, fwd, 3, right, 1, 1);
            if (shop == null) shop = hypixelOffset(bed, fwd, 4, right, -3, 1);
            if (upg  == null) upg  = hypixelOffset(bed, fwd, 4, right,  3, 1);

            template.getIronGenLocations().put(color, iron);
            template.getGoldGenLocations().put(color, gold);
            template.getShopLocations().put(color, shop);
            template.getUpgradeLocations().put(color, upg);
        }

        template.getDiamondGenLocations().addAll(diamondSpots);
        template.getEmeraldGenLocations().addAll(emeraldSpots);

        // Fallback si aucun diamant/emeraude trouve : positionne selon convention
        // Hypixel (4 diamants aux 4 points cardinaux depuis le centre, 2 emeraudes
        // a +/- 5 blocs X du centre au niveau du centre de la map).
        injectDiamondEmeraldFallback(template, bedByColor, mapCenter);

        Vector qs = template.getQueueSpawn();
        if (qs == null || (qs.getX() == 0 && qs.getY() == 0 && qs.getZ() == 0)) {
            template.setQueueSpawn(new Vector(mapCenter.getX(), mapCenter.getY() + 40, mapCenter.getZ()));
        }

        plugin.getLogger().info(String.format(
                "Auto-detect '%s' : %d teams, %d lit(s), %d diamant, %d emeraude, centre=(%d,%d)",
                template.getId(), bedByColor.size(), beds.size(),
                diamondSpots.size(), emeraldSpots.size(),
                (int) mapCenter.getX(), (int) mapCenter.getZ()));
    }

    // === Association lit / couleur via laine adjacente ===

    private Map<TeamColor, Vector> assignBedColors(List<Vector> beds, Map<Integer, List<Vector>> woolByColor) {
        Map<TeamColor, Vector> result = new EnumMap<TeamColor, Vector>(TeamColor.class);
        List<Vector> unassigned = new ArrayList<Vector>();

        for (Vector bed : beds) {
            TeamColor best = findClosestWoolColor(bed, woolByColor, 5.0);
            if (best != null && !result.containsKey(best)) {
                result.put(best, bed);
            } else {
                unassigned.add(bed);
            }
        }

        // Fallback : les lits sans laine associee recuperent les couleurs restantes
        // dans l'ordre standard (RED, BLUE, GREEN, YELLOW, ...)
        if (!unassigned.isEmpty()) {
            TeamColor[] order = { TeamColor.RED, TeamColor.BLUE, TeamColor.GREEN, TeamColor.YELLOW,
                    TeamColor.AQUA, TeamColor.WHITE, TeamColor.PINK, TeamColor.GRAY };
            int idx = 0;
            for (Vector bed : unassigned) {
                while (idx < order.length && result.containsKey(order[idx])) idx++;
                if (idx >= order.length) break;
                result.put(order[idx++], bed);
            }
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
        return bestData == null ? null : woolDataToTeam(bestData);
    }

    /**
     * Data-values de la laine (1.9.4) vers TeamColor du plugin.
     *   0 white, 1 orange, 2 magenta, 3 lightblue, 4 yellow, 5 lime, 6 pink,
     *   7 gray, 8 lightgray, 9 cyan, 10 purple, 11 blue, 12 brown, 13 green,
     *   14 red, 15 black.
     */
    private TeamColor woolDataToTeam(int data) {
        switch (data) {
            case 0:  return TeamColor.WHITE;
            case 3:  return TeamColor.AQUA;     // light blue
            case 4:  return TeamColor.YELLOW;
            case 5:  return TeamColor.GREEN;    // lime
            case 6:  return TeamColor.PINK;
            case 7:  return TeamColor.GRAY;
            case 8:  return TeamColor.GRAY;     // light gray
            case 9:  return TeamColor.AQUA;     // cyan
            case 11: return TeamColor.BLUE;
            case 13: return TeamColor.GREEN;
            case 14: return TeamColor.RED;
            case 15: return TeamColor.GRAY;     // black -> gray
            case 1:  return TeamColor.YELLOW;   // orange -> yellow
            case 2:  return TeamColor.PINK;     // magenta -> pink
            case 10: return TeamColor.BLUE;     // purple -> blue
            case 12: return TeamColor.RED;      // brown -> red
            default: return null;
        }
    }

    // === Attribution d'un ensemble de blocs au lit le plus proche ===

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
            // Garde le plus haut (le bloc sur plateforme, pas au sol)
            Vector existing = result.get(nearest);
            if (existing == null || c.getY() > existing.getY()) {
                result.put(nearest, c);
            }
        }
        return result;
    }

    // === Utilitaires ===

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
                    // moyenne
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

    /**
     * Applique un offset style Hypixel a partir du lit : (forward * fDist + right * rDist + upY).
     * Les axes forward/right sont unitaires et XZ-plan. Retourne une position entiere centree.
     */
    private Vector hypixelOffset(Vector bed, Vector fwd, double fDist, Vector right, double rDist, double upY) {
        double x = bed.getX() + fwd.getX() * fDist + right.getX() * rDist + 0.5;
        double z = bed.getZ() + fwd.getZ() * fDist + right.getZ() * rDist + 0.5;
        double y = bed.getY() + upY;
        return new Vector(x, y, z);
    }

    /**
     * Si aucun diamant ou emeraude n'a ete detecte, injecte des spots par defaut
     * a l'agencement Hypixel : 4 diamants a NSWE du centre, 2 emeraudes centrees.
     * La distance des diamants est calculee comme 2/3 de la distance moyenne
     * bed<->centre pour coller a la topologie de la map.
     */
    private void injectDiamondEmeraldFallback(MapTemplate template,
                                              Map<TeamColor, Vector> bedByColor,
                                              Vector mapCenter) {
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
            template.getDiamondGenLocations().add(new Vector(mapCenter.getX() + diamondR, y, mapCenter.getZ()));
            template.getDiamondGenLocations().add(new Vector(mapCenter.getX() - diamondR, y, mapCenter.getZ()));
            template.getDiamondGenLocations().add(new Vector(mapCenter.getX(), y, mapCenter.getZ() + diamondR));
            template.getDiamondGenLocations().add(new Vector(mapCenter.getX(), y, mapCenter.getZ() - diamondR));
        }
        if (template.getEmeraldGenLocations().isEmpty()) {
            template.getEmeraldGenLocations().add(new Vector(mapCenter.getX() + 4, y, mapCenter.getZ()));
            template.getEmeraldGenLocations().add(new Vector(mapCenter.getX() - 4, y, mapCenter.getZ()));
        }
    }

    private Vector unitXZToCenter(Vector from, Vector mapCenter) {
        double dx = mapCenter.getX() - from.getX();
        double dz = mapCenter.getZ() - from.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.01) return new Vector(1, 0, 0);
        return new Vector(dx / len, 0, dz / len);
    }

    private Vector findSpawnNear(World world, Vector bed, Vector mapCenter) {
        Vector dir = unitXZToCenter(bed, mapCenter);
        double sx = bed.getX() + dir.getX() * 3 + 0.5;
        double sz = bed.getZ() + dir.getZ() * 3 + 0.5;
        int startY = (int) bed.getY();
        for (int y = startY + 2; y >= startY - 5; y--) {
            Block b = world.getBlockAt((int) Math.floor(sx), y, (int) Math.floor(sz));
            if (b.getType() != Material.AIR) {
                return new Vector(sx, y + 1, sz);
            }
        }
        return new Vector(sx, bed.getY() + 1, sz);
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
