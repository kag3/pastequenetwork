package fr.pastequeworld.bedwars.map;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.team.TeamColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Detection automatique des mecanismes d'une map de BedWars a partir des
 * blocs "marqueurs" presents sur le monde.
 *
 * Convention (identique aux maps Hypixel et similaires) :
 *   - BED blocks : position de la base de chaque equipe. La couleur du lit
 *     determine la couleur de l'equipe (data value).
 *   - EMERALD_BLOCK : spots d'emeraude (generalement deux au centre et au bord).
 *   - DIAMOND_BLOCK : spots de diamant (generalement 4 sur les cotes).
 *   - IRON_BLOCK (isole dans une base) : position du generateur de fer de base.
 *   - GOLD_BLOCK (isole dans une base) : position du generateur d'or de base.
 *   - BEACON : point de spawn d'equipe (en fallback si absence de plateforme).
 *   - ENDER_STONE : position du villageois shop items.
 *   - OBSIDIAN (colonne isolee) : position du villageois upgrades.
 *
 * Si un ou plusieurs marqueurs sont absents, la detection tombera en fallback
 * sur les conventions:
 *   - IRON/GOLD generateur : juste au-dessus du lit de la team
 *   - Shop: 2 blocs a droite du lit (face au spawn)
 *   - Upgrades: 2 blocs a gauche du lit
 *   - Spawn: 3 blocs devant le lit, tourne vers le centre
 *
 * Les scans sont realises sur les chunks deja charges + chargent a la volee
 * les chunks dans un rayon autour du centre de la map (-200..+200 X/Z par defaut).
 */
public class MapAutoDetector {

    private final BedWarsPlugin plugin;

    public MapAutoDetector(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Scanne le monde pour detecter les mecanismes et peuple le MapTemplate associe.
     * Le template re\u00e7oit toutes ses coordonn\u00e9es en coordonn\u00e9es ABSOLUES du monde
     * (Arena ajoutera worldCorner=0 dans le cas world-folder).
     *
     * @param world  monde deja charge contenant la map
     * @param template template a enrichir
     * @param center centre approximatif (pour limiter le scan). Peut etre null : utilise (0,0,0).
     * @param radius rayon XZ en blocs
     */
    public void detect(World world, MapTemplate template, Location center, int radius) {
        int minX = (center == null ? 0 : center.getBlockX()) - radius;
        int maxX = (center == null ? 0 : center.getBlockX()) + radius;
        int minZ = (center == null ? 0 : center.getBlockZ()) - radius;
        int maxZ = (center == null ? 0 : center.getBlockZ()) + radius;
        int cxMin = minX >> 4, cxMax = maxX >> 4;
        int czMin = minZ >> 4, czMax = maxZ >> 4;

        Map<TeamColor, Vector> beds = new EnumMap<TeamColor, Vector>(TeamColor.class);
        Map<TeamColor, List<Vector>> ironCandidates = new EnumMap<TeamColor, List<Vector>>(TeamColor.class);
        Map<TeamColor, List<Vector>> goldCandidates = new EnumMap<TeamColor, List<Vector>>(TeamColor.class);
        Map<TeamColor, List<Vector>> shopCandidates = new EnumMap<TeamColor, List<Vector>>(TeamColor.class);
        Map<TeamColor, List<Vector>> upgradeCandidates = new EnumMap<TeamColor, List<Vector>>(TeamColor.class);
        List<Vector> diamondSpots = new ArrayList<Vector>();
        List<Vector> emeraldSpots = new ArrayList<Vector>();
        Vector mapCenter = new Vector(0, 0, 0);
        int centerCount = 0;

        for (int cx = cxMin; cx <= cxMax; cx++) {
            for (int cz = czMin; cz <= czMax; cz++) {
                Chunk chunk = world.getChunkAt(cx, cz);
                if (!chunk.isLoaded() && !chunk.load(false)) continue;
                for (int lx = 0; lx < 16; lx++) {
                    for (int lz = 0; lz < 16; lz++) {
                        int wx = (cx << 4) + lx;
                        int wz = (cz << 4) + lz;
                        // parcourt de bas en haut ; on s'arrete a max-y utile (120)
                        for (int y = 30; y <= 120; y++) {
                            Block b = world.getBlockAt(wx, y, wz);
                            Material type = b.getType();
                            if (type == Material.AIR) continue;

                            if (type == Material.BED_BLOCK || type == Material.BED) {
                                // Un lit occupe 2 blocs : on ne garde que la tete ou le pied ;
                                // on se contente du premier trouve par couleur.
                                TeamColor color = colorFromData(b.getData());
                                if (color != null && !beds.containsKey(color)) {
                                    beds.put(color, new Vector(wx, y, wz));
                                }
                            } else if (type == Material.EMERALD_BLOCK) {
                                emeraldSpots.add(new Vector(wx, y, wz));
                                mapCenter.add(new Vector(wx, 0, wz));
                                centerCount++;
                            } else if (type == Material.DIAMOND_BLOCK) {
                                diamondSpots.add(new Vector(wx, y, wz));
                                mapCenter.add(new Vector(wx, 0, wz));
                                centerCount++;
                            } else if (type == Material.IRON_BLOCK) {
                                // Candidate pour iron generator. Rattache plus tard.
                                addCandidate(ironCandidates, null, new Vector(wx, y, wz));
                            } else if (type == Material.GOLD_BLOCK) {
                                addCandidate(goldCandidates, null, new Vector(wx, y, wz));
                            } else if (type == Material.ENDER_STONE) {
                                addCandidate(shopCandidates, null, new Vector(wx, y, wz));
                            } else if (type == Material.OBSIDIAN) {
                                addCandidate(upgradeCandidates, null, new Vector(wx, y, wz));
                            }
                        }
                    }
                }
            }
        }

        if (centerCount == 0) {
            mapCenter = new Vector(0, 64, 0);
        } else {
            mapCenter = new Vector(mapCenter.getX() / centerCount, 64, mapCenter.getZ() / centerCount);
        }

        // Attribue les generateurs/shops au team le plus proche
        assignNearest(ironCandidates, beds);
        assignNearest(goldCandidates, beds);
        assignNearest(shopCandidates, beds);
        assignNearest(upgradeCandidates, beds);

        // Injecte dans le template (les vecteurs deviennent "offset 0")
        template.getBedLocations().clear();
        template.getSpawnLocations().clear();
        template.getShopLocations().clear();
        template.getUpgradeLocations().clear();
        template.getIronGenLocations().clear();
        template.getGoldGenLocations().clear();
        template.getDiamondGenLocations().clear();
        template.getEmeraldGenLocations().clear();

        for (Map.Entry<TeamColor, Vector> e : beds.entrySet()) {
            TeamColor color = e.getKey();
            Vector bed = e.getValue();
            template.getBedLocations().put(color, bed.clone());

            // Spawn : 3 blocs au-dessus et oriente vers le centre
            Vector spawn = findSpawnNear(world, bed, mapCenter);
            template.getSpawnLocations().put(color, spawn);

            Vector iron = pickFrom(ironCandidates, color);
            Vector gold = pickFrom(goldCandidates, color);
            Vector shop = pickFrom(shopCandidates, color);
            Vector upg  = pickFrom(upgradeCandidates, color);

            // Fallback si aucun marqueur
            if (iron == null) iron = new Vector(bed.getX(), bed.getY() + 1, bed.getZ());
            if (gold == null) gold = new Vector(bed.getX() + 1, bed.getY() + 1, bed.getZ());
            if (shop == null) shop = new Vector(bed.getX() - 2, bed.getY() + 1, bed.getZ());
            if (upg  == null) upg  = new Vector(bed.getX() + 2, bed.getY() + 1, bed.getZ());

            template.getIronGenLocations().put(color, iron);
            template.getGoldGenLocations().put(color, gold);
            template.getShopLocations().put(color, shop);
            template.getUpgradeLocations().put(color, upg);
        }

        template.getDiamondGenLocations().addAll(diamondSpots);
        template.getEmeraldGenLocations().addAll(emeraldSpots);

        // Queue spawn : au-dessus du centre si pas defini
        Vector qs = template.getQueueSpawn();
        if (qs == null || (qs.getX() == 0 && qs.getY() == 0 && qs.getZ() == 0)) {
            template.setQueueSpawn(new Vector(mapCenter.getX(), mapCenter.getY() + 40, mapCenter.getZ()));
        }

        plugin.getLogger().info(String.format(
                "Auto-detect map '%s': %d teams, %d diamant, %d emeraude",
                template.getId(), beds.size(), diamondSpots.size(), emeraldSpots.size()));
    }

    private Vector findSpawnNear(World world, Vector bed, Vector mapCenter) {
        double dx = mapCenter.getX() - bed.getX();
        double dz = mapCenter.getZ() - bed.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.01) { dx = 1; dz = 0; len = 1; }
        dx /= len; dz /= len;
        double sx = bed.getX() + dx * 3 + 0.5;
        double sz = bed.getZ() + dz * 3 + 0.5;
        int startY = (int) bed.getY();
        // trouve le premier bloc solide vers le bas puis pose au-dessus
        for (int y = startY + 2; y >= startY - 5; y--) {
            Block b = world.getBlockAt((int) Math.floor(sx), y, (int) Math.floor(sz));
            if (b.getType() != Material.AIR) {
                return new Vector(sx, y + 1, sz);
            }
        }
        return new Vector(sx, bed.getY() + 1, sz);
    }

    private TeamColor colorFromData(byte data) {
        int wool = data & 0xF;
        // Dans 1.9 les beds sont tous rouges (pas de data par couleur), cependant la
        // convention BedWars est de coller un bloc de LAINE a cote du lit. On priorise
        // donc une recherche par wool a proximite si plusieurs lits rouges sont trouves.
        // En attendant, 0 == blanc, 14 == rouge etc.
        switch (wool) {
            case 0:  return TeamColor.WHITE;
            case 1:  return TeamColor.YELLOW;   // orange -> yellow
            case 3:  return TeamColor.AQUA;
            case 4:  return TeamColor.YELLOW;
            case 5:  return TeamColor.GREEN;
            case 6:  return TeamColor.PINK;
            case 7:  return TeamColor.GRAY;
            case 9:  return TeamColor.AQUA;
            case 11: return TeamColor.BLUE;
            case 14: return TeamColor.RED;
            default: return TeamColor.RED;
        }
    }

    private void addCandidate(Map<TeamColor, List<Vector>> map, TeamColor color, Vector v) {
        TeamColor key = color == null ? TeamColor.WHITE : color;
        List<Vector> list = map.get(key);
        if (list == null) {
            list = new ArrayList<Vector>();
            map.put(key, list);
        }
        list.add(v);
    }

    private void assignNearest(Map<TeamColor, List<Vector>> map, Map<TeamColor, Vector> beds) {
        // On regroupe tous les candidats "orphelins" (cle WHITE par defaut ci-dessus) et on
        // les attribue a la team dont le lit est le plus proche.
        List<Vector> all = new ArrayList<Vector>();
        for (List<Vector> v : map.values()) all.addAll(v);
        map.clear();
        for (Vector v : all) {
            TeamColor nearest = null;
            double bestDist = Double.MAX_VALUE;
            for (Map.Entry<TeamColor, Vector> b : beds.entrySet()) {
                double dx = b.getValue().getX() - v.getX();
                double dz = b.getValue().getZ() - v.getZ();
                double d = dx * dx + dz * dz;
                if (d < bestDist) { bestDist = d; nearest = b.getKey(); }
            }
            if (nearest != null) {
                List<Vector> list = map.get(nearest);
                if (list == null) { list = new ArrayList<Vector>(); map.put(nearest, list); }
                list.add(v);
            }
        }
    }

    private Vector pickFrom(Map<TeamColor, List<Vector>> map, TeamColor color) {
        List<Vector> list = map.get(color);
        if (list == null || list.isEmpty()) return null;
        // Prend le vecteur le plus haut (Y max) pour placer sur plateforme plutot qu'au sol
        Vector best = list.get(0);
        for (Vector v : list) if (v.getY() > best.getY()) best = v;
        return best;
    }
}
