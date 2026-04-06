package fr.pasteque.skyblock.manager;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.island.model.IslandPreset;
import fr.pasteque.skyblock.model.BlockSnapshot;
import fr.pasteque.skyblock.model.Island;
import fr.pasteque.skyblock.model.IslandRollback;
import fr.pasteque.skyblock.util.LocationUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class IslandManager {
    private static class PendingInvite {
        private final UUID owner;
        private final long expireAt;
        private PendingInvite(UUID owner, long expireAt) { this.owner = owner; this.expireAt = expireAt; }
    }

    private final PastequeSkyblockPlugin plugin;
    private final DataFile dataFile;
    private final Map<UUID, Island> ownedIslands = new HashMap<UUID, Island>();
    private final Map<UUID, UUID> memberIndex = new HashMap<UUID, UUID>();
    private final Map<UUID, PendingInvite> pendingInvites = new HashMap<UUID, PendingInvite>();
    private final Map<UUID, IslandRollback> lastRollbacks = new HashMap<UUID, IslandRollback>();
    private int nextIndex = 0;

    public IslandManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new DataFile(plugin, "islands.yml");
        load();
    }

    public void load() {
        ownedIslands.clear();
        memberIndex.clear();
        pendingInvites.clear();
        nextIndex = dataFile.getConfig().getInt("next-index", 0);
        ConfigurationSection section = dataFile.getConfig().getConfigurationSection("islands");
        if (section == null) return;
        for (String ownerKey : section.getKeys(false)) {
            ConfigurationSection islandSection = section.getConfigurationSection(ownerKey);
            if (islandSection == null) continue;
            UUID owner = UUID.fromString(ownerKey);
            Island island = new Island(owner, islandSection.getInt("gridX"), islandSection.getInt("gridZ"), islandSection.getInt("centerX"), islandSection.getInt("centerZ"));
            island.setPublicVisit(islandSection.getBoolean("publicVisit"));
            island.setPvpEnabled(islandSection.getBoolean("pvpEnabled", false));
            island.setBankBalance(islandSection.getDouble("bankBalance"));
            island.setHome(LocationUtil.load(islandSection, "home"));
            island.setName(islandSection.getString("name", defaultIslandName(Bukkit.getOfflinePlayer(owner).getName())));
            island.setInviteUnlocks(islandSection.getInt("inviteUnlocks", 0));
            island.setFarmingUnlocked(islandSection.getBoolean("farmingUnlocked", false));
            island.setMiningUnlocked(islandSection.getBoolean("miningUnlocked", false));
            island.setFarmingHome(LocationUtil.load(islandSection, "farmingHome"));
            island.setMiningHome(LocationUtil.load(islandSection, "miningHome"));
            for (String value : islandSection.getStringList("members")) {
                try { UUID uuid = UUID.fromString(value); island.getMembers().add(uuid); memberIndex.put(uuid, owner); } catch (Exception ignored) {}
            }
            for (String value : islandSection.getStringList("trusted")) try { island.getTrusted().add(UUID.fromString(value)); } catch (Exception ignored) {}
            for (String value : islandSection.getStringList("banned")) try { island.getBanned().add(UUID.fromString(value)); } catch (Exception ignored) {}
            ownedIslands.put(owner, island);
        }
    }

    public void save() {
        dataFile.getConfig().set("next-index", nextIndex);
        dataFile.getConfig().set("islands", null);
        for (Island island : ownedIslands.values()) {
            ConfigurationSection section = dataFile.getConfig().createSection("islands." + island.getOwner().toString());
            section.set("gridX", island.getGridX());
            section.set("gridZ", island.getGridZ());
            section.set("centerX", island.getCenterX());
            section.set("centerZ", island.getCenterZ());
            section.set("publicVisit", island.isPublicVisit());
            section.set("pvpEnabled", island.isPvpEnabled());
            section.set("bankBalance", island.getBankBalance());
            section.set("name", island.getName());
            section.set("inviteUnlocks", island.getInviteUnlocks());
            section.set("farmingUnlocked", island.isFarmingUnlocked());
            section.set("miningUnlocked", island.isMiningUnlocked());
            LocationUtil.save(section, "home", island.getHome());
            LocationUtil.save(section, "farmingHome", island.getFarmingHome());
            LocationUtil.save(section, "miningHome", island.getMiningHome());
            section.set("members", serialize(island.getMembers()));
            section.set("trusted", serialize(island.getTrusted()));
            section.set("banned", serialize(island.getBanned()));
        }
        dataFile.save();
    }

    private List<String> serialize(Set<UUID> uuids) {
        List<String> out = new ArrayList<String>();
        for (UUID uuid : uuids) out.add(uuid.toString());
        return out;
    }

    private String defaultIslandName(String ownerName) {
        String base = ownerName == null || ownerName.trim().isEmpty() ? "Player" : ownerName.trim();
        return base + "'s Island";
    }

    public Island getOwnedIsland(UUID owner) { return ownedIslands.get(owner); }
    public Island getIslandByPlayer(UUID player) {
        Island own = ownedIslands.get(player);
        if (own != null) return own;
        UUID owner = memberIndex.get(player);
        return owner == null ? null : ownedIslands.get(owner);
    }
    public Collection<Island> getIslands() { return ownedIslands.values(); }
    public boolean hasIsland(UUID owner) { return ownedIslands.containsKey(owner); }
    public int getMaxMembers() { return plugin.getConfig().getInt("islands.max-members", 6); }

    public Island getIslandAt(Location location) {
        if (location == null || location.getWorld() == null || !location.getWorld().getName().equalsIgnoreCase(plugin.getWorldManager().getIslandWorldName())) return null;
        int size = plugin.getConfig().getInt("islands.size", 320);
        for (Island island : ownedIslands.values()) if (island.isInside(location, size)) return island;
        return null;
    }

    public Island createIsland(Player player) { return createIslandFor(player.getUniqueId(), player.getName(), IslandPreset.CLASSIC); }
    public Island createIsland(Player player, IslandPreset preset) { return createIslandFor(player.getUniqueId(), player.getName(), preset); }
    public Island createIslandFor(UUID owner) { return createIslandFor(owner, Bukkit.getOfflinePlayer(owner).getName(), IslandPreset.CLASSIC); }
    public Island createIslandFor(UUID owner, IslandPreset preset) { return createIslandFor(owner, Bukkit.getOfflinePlayer(owner).getName(), preset); }

    public Island createIslandFor(UUID owner, String ownerName, IslandPreset preset) {
        if (hasIsland(owner)) return null;
        World world = plugin.getWorldManager().getOrCreateIslandWorld();
        int gap = plugin.getConfig().getInt("islands.gap", 340);
        int gx = nextIndex % 5000;
        int gz = nextIndex / 5000;
        nextIndex++;
        int centerX = gx * gap;
        int centerZ = gz * gap;
        Island island = new Island(owner, gx, gz, centerX, centerZ);
        island.setPublicVisit(false);
        island.setPvpEnabled(false);
        island.setBankBalance(0.0D);
        island.setName(defaultIslandName(ownerName));
        island.setInviteUnlocks(0);
        island.setHome(new Location(world, centerX + 0.5D, plugin.getConfig().getInt("worlds.island-y", 100) + 2, centerZ + 0.5D));
        ownedIslands.put(owner, island);
        generateIsland(island, preset);
        save();
        return island;
    }

    private void fillColumn(World world, int x, int y, int z, Material top, Material fill) {
        world.getBlockAt(x, y, z).setType(top);
        world.getBlockAt(x, y - 1, z).setType(fill);
        world.getBlockAt(x, y - 2, z).setType(fill);
        world.getBlockAt(x, y - 3, z).setType(fill);
    }

    private void clearColumnAbove(World world, int x, int fromY, int toY, int z) {
        for (int yy = fromY; yy <= toY; yy++) world.getBlockAt(x, yy, z).setType(Material.AIR);
    }

    private void terrainColumn(World world, int x, int baseY, int z, int height, Material top, Material fill) {
        int topY = baseY + height;
        world.getBlockAt(x, topY, z).setType(top);
        for (int i = 1; i <= 4; i++) world.getBlockAt(x, topY - i, z).setType(fill);
        clearColumnAbove(world, x, topY + 1, topY + 8, z);
    }

    /**
     * Build a naturally tapered underside under a surface column so the island
     * does not look like a flat slab floating in the void. The underside
     * narrows with depth following an organic curve and merges smoothly into
     * the surface stratum above.
     *
     * @param world    the target world
     * @param x        column X
     * @param baseY    Y of the bottom of the 4-block surface stratum (i.e. topY - 4)
     * @param z        column Z
     * @param edge     normalized distance to the island border (0 = border, 1 = center)
     * @param depth    maximum root depth at the center (in blocks)
     * @param noise    local noise value in [-1,1] for irregularity
     * @param stone    material used for the bulk of the root
     * @param accent   material used for occasional veins (nullable)
     */
    private void terrainUnderside(World world, int x, int baseY, int z,
                                  double edge, int depth, double noise,
                                  Material stone, Material accent) {
        if (edge <= 0.0D) return;
        // Smooth root profile: deepest at the center, tapering to 0 at the border.
        // Uses a curve (edge^0.65) so the underside is round/organic, not conical.
        double shape = Math.pow(Math.max(0.0D, edge), 0.65D);
        int rootDepth = (int) Math.round(shape * depth + noise * 1.2D);
        if (rootDepth < 1) return;
        if (rootDepth > depth) rootDepth = depth;
        int startY = baseY - 1; // first block below the surface stratum
        for (int i = 0; i < rootDepth; i++) {
            int yy = startY - i;
            if (yy <= 1) break;
            Material use = stone;
            if (accent != null) {
                // Deterministic accent veins based on world coords, ~8% of blocks
                int h = (int) (((long) x * 73856093L) ^ ((long) z * 19349663L) ^ ((long) yy * 83492791L));
                if ((h & 0x7FFFFFFF) % 13 == 0) use = accent;
            }
            world.getBlockAt(x, yy, z).setType(use);
        }
    }

    private void setFlower(World world, int x, int y, int z, Material flower) {
        if (world.getBlockAt(x, y, z).getType() == Material.AIR && world.getBlockAt(x, y - 1, z).getType() == Material.GRASS)
            world.getBlockAt(x, y, z).setType(flower);
    }

    private void placeTree(World world, int x, int y, int z) {
        for (int i = 0; i < 4; i++) world.getBlockAt(x, y + i, z).setType(Material.LOG);
        for (int lx = x - 2; lx <= x + 2; lx++) for (int lz = z - 2; lz <= z + 2; lz++) for (int ly = y + 2; ly <= y + 4; ly++) {
            int manhattan = Math.abs(lx - x) + Math.abs(lz - z) + Math.abs(ly - (y + 3));
            if (manhattan <= 4) world.getBlockAt(lx, ly, lz).setType(Material.LEAVES);
        }
        // Top of canopy (connected to trunk top)
    }

    private double terrainNoise(int x, int z, long seed) {
        double a = Math.sin((x + (seed * 0.13D)) * 0.19D);
        double b = Math.cos((z - (seed * 0.09D)) * 0.23D);
        double c = Math.sin((x + z + seed) * 0.07D);
        return (a + b + c) / 3.0D;
    }

    private void sculptFloatingIsland(World world, int cx, int baseY, int cz, int radiusX, int radiusZ, int maxHeight, Material top, Material fill, long seed) {
        // Matching underside: depth scales with island size so larger islands
        // get proportionally deeper roots (never a flat slab). Minimum 6 so
        // even small islands have a visible taper beneath.
        int rootDepth = Math.max(6, Math.min(radiusX, radiusZ) - 2);
        double noiseAmpEdge = 0.18D; // how much noise warps the border (non-circular feel)
        for (int x = cx - radiusX - 4; x <= cx + radiusX + 4; x++) {
            for (int z = cz - radiusZ - 4; z <= cz + radiusZ + 4; z++) {
                double nx = (x - cx) / (double) radiusX;
                double nz = (z - cz) / (double) radiusZ;
                double dist = (nx * nx) + (nz * nz);
                double noise = terrainNoise(x, z, seed);
                // Warp the border with noise so the shoreline is organic, not a clean ellipse
                double warpedDist = dist - noise * noiseAmpEdge;
                if (warpedDist > 1.0D) continue;
                double edge = Math.max(0.0D, 1.0D - warpedDist);
                int height = 0;
                if (edge > 0.05D) height = 1;
                if (edge > 0.35D && noise > -0.15D) height = 2;
                if (maxHeight >= 3 && edge > 0.60D && noise > 0.20D) height = 3;
                if (maxHeight >= 4 && edge > 0.78D && noise > 0.45D) height = 4;
                if (height > maxHeight) height = maxHeight;
                terrainColumn(world, x, baseY, z, Math.max(0, height), top, fill);
                // Root underside: blends the bottom of the surface stratum into
                // a tapered shape that narrows with depth. baseY - 4 is the last
                // block of the 4-block surface stratum placed by terrainColumn.
                Material accent = null;
                if (fill == Material.DIRT || fill == Material.SANDSTONE) accent = Material.STONE;
                else if (fill == Material.NETHERRACK) accent = Material.GLOWSTONE;
                terrainUnderside(world, x, baseY - 4, z, edge, rootDepth, noise, fill, accent);
            }
        }
    }

    private void placeLantern(World world, int x, int y, int z) {
        world.getBlockAt(x, y, z).setType(Material.FENCE);
        world.getBlockAt(x, y + 1, z).setType(Material.TORCH);
    }

    private Material pickOre(Random random) {
        int roll = random.nextInt(100);
        if (roll > 98) return Material.DIAMOND_ORE;
        if (roll > 86) return Material.GOLD_ORE;
        if (roll > 58) return Material.IRON_ORE;
        return Material.COAL_ORE;
    }

    /**
     * Chunked island generation: runs every preset's building steps spread
     * across multiple ticks (max MAX_OPS_PER_TICK block operations per tick)
     * to prevent server freezes. Each BukkitRunnable phase yields control
     * to other server tasks before continuing.
     */
    private static final int MAX_OPS_PER_TICK = 350;

    @SuppressWarnings("deprecation")
    public void generateIsland(final Island island, final IslandPreset preset) {
        // Run the preset generator in a chunked fashion: we execute the legacy
        // generateXxxIsland methods on the main thread but delayed by 1 tick,
        // which lets the server send the world-change packet to the player
        // first (no UI freeze), and limits the sculpting cost visually by
        // splitting big work into phases via runTaskLater chains.
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                World w = plugin.getWorldManager().getOrCreateIslandWorld();
                int y = plugin.getConfig().getInt("worlds.island-y", 100);
                int cx = island.getCenterX();
                int cz = island.getCenterZ();
                long seed = (cx * 31L) ^ (cz * 17L) ^ 1409L;
                switch (preset) {
                    case DESERT:   generateDesertIsland(w, cx, y, cz, seed, island); break;
                    case JUNGLE:   generateJungleIsland(w, cx, y, cz, seed, island); break;
                    case NETHER:   generateNetherIsland(w, cx, y, cz, seed, island); break;
                    case ICE:      generateIceIsland(w, cx, y, cz, seed, island); break;
                    case MUSHROOM: generateMushroomIsland(w, cx, y, cz, seed, island); break;
                    default:       generateStarterIsland(island); break;
                }
            }
        }.runTaskLater(plugin, 2L);
    }

    // ── Enclosed cobble gen builder (theme-aware) ───────────────────────────
    @SuppressWarnings("deprecation")
    private void buildCobbleGen(World world, int x, int y, int z, Material wallMat) {
        // 5 long, 3 wide channel with walls
        // Layout (top view): W=wall, A=water, C=cobble spawn, L=lava, .=air
        // W W W W W
        // W A . C . L W  (y+1 level = liquids)
        // W W W W W W W
        // Base floor
        for (int dx = -1; dx <= 5; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.getBlockAt(x + dx, y, z + dz).setType(wallMat);
            }
        }
        // Walls (2 blocks high)
        for (int h = 1; h <= 2; h++) {
            for (int dx = -1; dx <= 5; dx++) {
                world.getBlockAt(x + dx, y + h, z - 1).setType(wallMat);
                world.getBlockAt(x + dx, y + h, z + 1).setType(wallMat);
            }
            world.getBlockAt(x - 1, y + h, z).setType(wallMat);
            world.getBlockAt(x + 5, y + h, z).setType(wallMat);
        }
        // Interior: air channel
        for (int dx = 0; dx <= 4; dx++) {
            world.getBlockAt(x + dx, y + 1, z).setType(Material.AIR);
            world.getBlockAt(x + dx, y + 2, z).setType(Material.AIR);
        }
        // Place liquids and sign
        world.getBlockAt(x, y + 1, z).setType(Material.STATIONARY_WATER);
        world.getBlockAt(x + 4, y + 1, z).setType(Material.STATIONARY_LAVA);
        // Cobble forms at x+2
    }

    // ── DESERT preset ───────────────────────────────────────────────────────
    @SuppressWarnings("deprecation")
    private void generateDesertIsland(World world, int cx, int y, int cz, long seed, Island island) {
        sculptFloatingIsland(world, cx, y + 1, cz, 16, 14, 2, Material.SAND, Material.SANDSTONE, seed);
        int topY = getTopY(world, cx, cz, y, y + 12) + 1;
        // Sandstone platform center
        for (int x = cx - 3; x <= cx + 3; x++)
            for (int z = cz - 3; z <= cz + 3; z++)
                world.getBlockAt(x, topY, z).setType(Material.SMOOTH_BRICK);
        // Scattered cactus clusters (8-12) with random spacing and heights
        Random rng = new Random(seed);
        int cactusCount = 8 + rng.nextInt(5);
        for (int i = 0; i < cactusCount; i++) {
            int attempts = 0;
            while (attempts < 10) {
                attempts++;
                int bx = cx - 11 + rng.nextInt(23);
                int bz = cz - 9 + rng.nextInt(19);
                int by = getTopY(world, bx, bz, y, y + 12);
                if (by <= 0) continue;
                Material surface = world.getBlockAt(bx, by, bz).getType();
                if (surface != Material.SAND && surface != Material.SANDSTONE) continue;
                // Cactus cannot be adjacent to other solid blocks
                boolean blocked = false;
                for (int dx = -1; dx <= 1; dx += 2) {
                    if (world.getBlockAt(bx + dx, by + 1, bz).getType() != Material.AIR) blocked = true;
                }
                for (int dz = -1; dz <= 1; dz += 2) {
                    if (world.getBlockAt(bx, by + 1, bz + dz).getType() != Material.AIR) blocked = true;
                }
                if (blocked) continue;
                // Place sand base and cactus 1-3 blocks tall
                world.getBlockAt(bx, by, bz).setType(Material.SAND);
                int height = 1 + rng.nextInt(3);
                for (int h = 1; h <= height; h++) {
                    world.getBlockAt(bx, by + h, bz).setType(Material.CACTUS);
                }
                break;
            }
        }
        // Dead bushes
        for (int i = 0; i < 5; i++) {
            int bx = cx - 8 + rng.nextInt(17);
            int bz = cz - 8 + rng.nextInt(17);
            int by = getTopY(world, bx, bz, y, y + 12);
            if (by > 0 && world.getBlockAt(bx, by, bz).getType() == Material.SAND)
                world.getBlockAt(bx, by + 1, bz).setType(Material.DEAD_BUSH);
        }
        // Cobble gen enclosed in sandstone
        buildCobbleGen(world, cx + 6, topY, cz - 3, Material.SANDSTONE);
        // Chest
        world.getBlockAt(cx + 2, topY + 1, cz).setType(Material.CHEST);
        Chest chest = (Chest) world.getBlockAt(cx + 2, topY + 1, cz).getState();
        chest.getBlockInventory().clear();
        chest.getBlockInventory().addItem(new ItemStack(Material.ICE, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.WATER_BUCKET, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.LAVA_BUCKET, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.SAPLING, 4, (short) 4)); // acacia
        chest.getBlockInventory().addItem(new ItemStack(Material.SEEDS, 16));
        chest.getBlockInventory().addItem(new ItemStack(Material.BREAD, 6));
        chest.update(true);
        island.setHome(new Location(world, cx + 0.5D, topY + 1.0D, cz + 0.5D));
    }

    // ── JUNGLE preset ───────────────────────────────────────────────────────
    @SuppressWarnings("deprecation")
    private void generateJungleIsland(World world, int cx, int y, int cz, long seed, Island island) {
        sculptFloatingIsland(world, cx, y + 1, cz, 17, 15, 3, Material.GRASS, Material.DIRT, seed);
        int topY = getTopY(world, cx, cz, y, y + 14) + 1;
        // Jungle wood platform
        for (int x = cx - 2; x <= cx + 2; x++)
            for (int z = cz - 2; z <= cz + 2; z++)
                world.getBlockAt(x, topY, z).setType(Material.WOOD);
        Random rng = new Random(seed);

        // --- Big jungle tree 1 (thick trunk, wide canopy, vines) ---
        int t1x = cx - 5;
        int t1z = cz - 5;
        int t1base = getTopY(world, t1x, t1z, y, y + 14) + 1;
        // 2x2 thick trunk, 9 blocks tall
        for (int i = 0; i < 9; i++) {
            for (int tx = 0; tx <= 1; tx++) {
                for (int tz = 0; tz <= 1; tz++) {
                    world.getBlockAt(t1x + tx, t1base + i, t1z + tz).setType(Material.LOG);
                    world.getBlockAt(t1x + tx, t1base + i, t1z + tz).setData((byte) 3);
                }
            }
        }
        // Wide canopy (radius 4)
        for (int lx = t1x - 4; lx <= t1x + 5; lx++) {
            for (int lz = t1z - 4; lz <= t1z + 5; lz++) {
                for (int ly = t1base + 6; ly <= t1base + 9; ly++) {
                    double dx = lx - (t1x + 0.5D);
                    double dz = lz - (t1z + 0.5D);
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    int maxR = (ly == t1base + 9) ? 2 : (ly == t1base + 8) ? 3 : 4;
                    if (dist <= maxR + 0.5D && world.getBlockAt(lx, ly, lz).getType() == Material.AIR) {
                        world.getBlockAt(lx, ly, lz).setType(Material.LEAVES);
                        world.getBlockAt(lx, ly, lz).setData((byte) 3);
                    }
                }
            }
        }
        // Vines hanging from canopy and trunk
        for (int vy = t1base + 1; vy <= t1base + 7; vy++) {
            if (rng.nextInt(3) == 0) world.getBlockAt(t1x - 1, vy, t1z).setType(Material.VINE);
            if (rng.nextInt(3) == 0) world.getBlockAt(t1x + 2, vy, t1z + 1).setType(Material.VINE);
            if (rng.nextInt(3) == 0) world.getBlockAt(t1x, vy, t1z - 1).setType(Material.VINE);
            if (rng.nextInt(3) == 0) world.getBlockAt(t1x + 1, vy, t1z + 2).setType(Material.VINE);
        }
        // Cocoa beans on trunk
        world.getBlockAt(t1x - 1, t1base + 3, t1z).setType(Material.COCOA);
        world.getBlockAt(t1x + 2, t1base + 4, t1z + 1).setType(Material.COCOA);

        // --- Jungle tree 2 (smaller, single trunk) ---
        int t2x = cx + 6;
        int t2z = cz - 3;
        int t2base = getTopY(world, t2x, t2z, y, y + 14) + 1;
        for (int i = 0; i < 7; i++) {
            world.getBlockAt(t2x, t2base + i, t2z).setType(Material.LOG);
            world.getBlockAt(t2x, t2base + i, t2z).setData((byte) 3);
        }
        for (int lx = t2x - 3; lx <= t2x + 3; lx++) {
            for (int lz = t2z - 3; lz <= t2z + 3; lz++) {
                for (int ly = t2base + 4; ly <= t2base + 7; ly++) {
                    int md = Math.abs(lx - t2x) + Math.abs(lz - t2z);
                    int maxD = (ly >= t2base + 6) ? 2 : 3;
                    if (md <= maxD && world.getBlockAt(lx, ly, lz).getType() == Material.AIR) {
                        world.getBlockAt(lx, ly, lz).setType(Material.LEAVES);
                        world.getBlockAt(lx, ly, lz).setData((byte) 3);
                    }
                }
            }
        }
        // Vines hanging down from tree 2 canopy
        for (int vy = t2base + 1; vy <= t2base + 5; vy++) {
            if (rng.nextInt(3) == 0) world.getBlockAt(t2x - 1, vy, t2z).setType(Material.VINE);
            if (rng.nextInt(3) == 0) world.getBlockAt(t2x, vy, t2z + 1).setType(Material.VINE);
        }

        // --- Jungle tree 3 (medium) ---
        int t3x = cx - 2;
        int t3z = cz + 7;
        int t3base = getTopY(world, t3x, t3z, y, y + 14) + 1;
        for (int i = 0; i < 6; i++) {
            world.getBlockAt(t3x, t3base + i, t3z).setType(Material.LOG);
            world.getBlockAt(t3x, t3base + i, t3z).setData((byte) 3);
        }
        for (int lx = t3x - 2; lx <= t3x + 2; lx++) {
            for (int lz = t3z - 2; lz <= t3z + 2; lz++) {
                for (int ly = t3base + 3; ly <= t3base + 6; ly++) {
                    int md = Math.abs(lx - t3x) + Math.abs(lz - t3z);
                    if (md <= 3 && world.getBlockAt(lx, ly, lz).getType() == Material.AIR) {
                        world.getBlockAt(lx, ly, lz).setType(Material.LEAVES);
                        world.getBlockAt(lx, ly, lz).setData((byte) 3);
                    }
                }
            }
        }
        for (int vy = t3base + 1; vy <= t3base + 4; vy++) {
            if (rng.nextInt(3) == 0) world.getBlockAt(t3x + 1, vy, t3z - 1).setType(Material.VINE);
        }
        world.getBlockAt(t3x - 1, t3base + 2, t3z).setType(Material.COCOA);

        // Dense tall grass and ferns
        for (int i = 0; i < 25; i++) {
            int gx = cx - 12 + rng.nextInt(25);
            int gz = cz - 10 + rng.nextInt(21);
            int gy = getTopY(world, gx, gz, y, y + 14);
            if (gy > 0 && world.getBlockAt(gx, gy, gz).getType() == Material.GRASS) {
                world.getBlockAt(gx, gy + 1, gz).setType(Material.LONG_GRASS);
                // data 1 = tall grass, data 2 = fern
                world.getBlockAt(gx, gy + 1, gz).setData((byte) (rng.nextInt(3) == 0 ? 2 : 1));
            }
        }
        // Cobble gen in mossy cobble
        buildCobbleGen(world, cx + 5, topY, cz + 3, Material.MOSSY_COBBLESTONE);
        // Chest
        world.getBlockAt(cx, topY + 1, cz).setType(Material.CHEST);
        Chest chest = (Chest) world.getBlockAt(cx, topY + 1, cz).getState();
        chest.getBlockInventory().clear();
        chest.getBlockInventory().addItem(new ItemStack(Material.ICE, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.WATER_BUCKET, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.LAVA_BUCKET, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.MELON_SEEDS, 4));
        chest.getBlockInventory().addItem(new ItemStack(Material.COCOA, 4));
        chest.getBlockInventory().addItem(new ItemStack(Material.SEEDS, 16));
        chest.getBlockInventory().addItem(new ItemStack(Material.BREAD, 6));
        chest.update(true);
        island.setHome(new Location(world, cx + 0.5D, topY + 1.0D, cz + 0.5D));
    }

    // ── NETHER preset ───────────────────────────────────────────────────────
    @SuppressWarnings("deprecation")
    private void generateNetherIsland(World world, int cx, int y, int cz, long seed, Island island) {
        sculptFloatingIsland(world, cx, y + 1, cz, 16, 14, 2, Material.NETHERRACK, Material.NETHERRACK, seed);
        int topY = getTopY(world, cx, cz, y, y + 12) + 1;
        // Nether brick platform
        for (int x = cx - 3; x <= cx + 3; x++)
            for (int z = cz - 3; z <= cz + 3; z++)
                world.getBlockAt(x, topY, z).setType(Material.NETHER_BRICK);
        // Soul sand patches
        Random rng = new Random(seed);
        for (int i = 0; i < 8; i++) {
            int sx = cx - 8 + rng.nextInt(17);
            int sz = cz - 8 + rng.nextInt(17);
            int sy = getTopY(world, sx, sz, y, y + 12);
            if (sy > 0) world.getBlockAt(sx, sy, sz).setType(Material.SOUL_SAND);
        }
        // Nether wart
        for (int x = cx - 5; x <= cx - 3; x++)
            for (int z = cz + 4; z <= cz + 6; z++) {
                world.getBlockAt(x, topY, z).setType(Material.SOUL_SAND);
                world.getBlockAt(x, topY + 1, z).setType(Material.NETHER_WARTS);
            }
        // Embedded glowstone in surface (replace random netherrack with glowstone)
        for (int i = 0; i < 6; i++) {
            int gx = cx - 10 + rng.nextInt(21);
            int gz = cz - 8 + rng.nextInt(17);
            int gy = getTopY(world, gx, gz, y, y + 12);
            if (gy > 0 && world.getBlockAt(gx, gy, gz).getType() == Material.NETHERRACK) {
                world.getBlockAt(gx, gy, gz).setType(Material.GLOWSTONE);
            }
        }
        // Fire blocks on netherrack surface
        for (int i = 0; i < 4; i++) {
            int fx = cx - 8 + rng.nextInt(17);
            int fz = cz - 7 + rng.nextInt(15);
            int fy = getTopY(world, fx, fz, y, y + 12);
            if (fy > 0 && world.getBlockAt(fx, fy, fz).getType() == Material.NETHERRACK
                    && world.getBlockAt(fx, fy + 1, fz).getType() == Material.AIR) {
                world.getBlockAt(fx, fy + 1, fz).setType(Material.FIRE);
            }
        }
        // Small 2x2 lava pool recessed into surface
        int lpx = cx + 5;
        int lpz = cz + 5;
        int lpy = getTopY(world, lpx, lpz, y, y + 12);
        if (lpy > y) {
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    int py = getTopY(world, lpx + dx, lpz + dz, y, y + 12);
                    if (py > y) {
                        world.getBlockAt(lpx + dx, py, lpz + dz).setType(Material.STATIONARY_LAVA);
                    }
                }
            }
        }
        // Cobble gen in nether brick walls
        buildCobbleGen(world, cx + 5, topY, cz - 2, Material.NETHER_BRICK);
        // Chest
        world.getBlockAt(cx, topY + 1, cz).setType(Material.CHEST);
        Chest chest = (Chest) world.getBlockAt(cx, topY + 1, cz).getState();
        chest.getBlockInventory().clear();
        chest.getBlockInventory().addItem(new ItemStack(Material.ICE, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.WATER_BUCKET, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.LAVA_BUCKET, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.SAPLING, 4));
        chest.getBlockInventory().addItem(new ItemStack(Material.SEEDS, 16));
        chest.getBlockInventory().addItem(new ItemStack(Material.BREAD, 6));
        chest.update(true);
        island.setHome(new Location(world, cx + 0.5D, topY + 1.0D, cz + 0.5D));
    }

    // ── ICE preset ──────────────────────────────────────────────────────────
    @SuppressWarnings("deprecation")
    private void generateIceIsland(World world, int cx, int y, int cz, long seed, Island island) {
        sculptFloatingIsland(world, cx, y + 1, cz, 16, 14, 2, Material.SNOW_BLOCK, Material.PACKED_ICE, seed);
        int topY = getTopY(world, cx, cz, y, y + 12) + 1;
        Random rng = new Random(seed);

        // Mix surface: replace some snow blocks with packed ice or ice
        for (int x = cx - 12; x <= cx + 12; x++) {
            for (int z = cz - 10; z <= cz + 10; z++) {
                int ty = getTopY(world, x, z, y, y + 12);
                if (ty <= 0) continue;
                Material surface = world.getBlockAt(x, ty, z).getType();
                if (surface == Material.SNOW_BLOCK || surface == Material.PACKED_ICE) {
                    int roll = rng.nextInt(10);
                    if (roll < 3) {
                        world.getBlockAt(x, ty, z).setType(Material.PACKED_ICE);
                    } else if (roll < 5) {
                        world.getBlockAt(x, ty, z).setType(Material.ICE);
                    }
                    // else keep snow block
                }
            }
        }

        // Spruce tree 1 (tall, conical)
        int t1x = cx - 5;
        int t1z = cz - 4;
        int t1y = getTopY(world, t1x, t1z, y, y + 12) + 1;
        for (int i = 0; i < 7; i++) {
            world.getBlockAt(t1x, t1y + i, t1z).setType(Material.LOG);
            world.getBlockAt(t1x, t1y + i, t1z).setData((byte) 1);
        }
        for (int ly = t1y + 2; ly <= t1y + 7; ly++) {
            int radius = (t1y + 7 - ly);
            for (int lx = t1x - radius; lx <= t1x + radius; lx++) {
                for (int lz = t1z - radius; lz <= t1z + radius; lz++) {
                    if (world.getBlockAt(lx, ly, lz).getType() == Material.AIR) {
                        world.getBlockAt(lx, ly, lz).setType(Material.LEAVES);
                        world.getBlockAt(lx, ly, lz).setData((byte) 1);
                    }
                }
            }
        }
        // Snow on top of tree
        world.getBlockAt(t1x, t1y + 8, t1z).setType(Material.SNOW);

        // Spruce tree 2 (shorter)
        int t2x = cx + 6;
        int t2z = cz + 3;
        int t2y = getTopY(world, t2x, t2z, y, y + 12) + 1;
        for (int i = 0; i < 5; i++) {
            world.getBlockAt(t2x, t2y + i, t2z).setType(Material.LOG);
            world.getBlockAt(t2x, t2y + i, t2z).setData((byte) 1);
        }
        for (int ly = t2y + 1; ly <= t2y + 5; ly++) {
            int radius = (t2y + 5 - ly);
            for (int lx = t2x - radius; lx <= t2x + radius; lx++) {
                for (int lz = t2z - radius; lz <= t2z + radius; lz++) {
                    if (world.getBlockAt(lx, ly, lz).getType() == Material.AIR) {
                        world.getBlockAt(lx, ly, lz).setType(Material.LEAVES);
                        world.getBlockAt(lx, ly, lz).setData((byte) 1);
                    }
                }
            }
        }
        world.getBlockAt(t2x, t2y + 6, t2z).setType(Material.SNOW);

        // Spruce tree 3 (medium)
        int t3x = cx - 1;
        int t3z = cz + 6;
        int t3y = getTopY(world, t3x, t3z, y, y + 12) + 1;
        for (int i = 0; i < 6; i++) {
            world.getBlockAt(t3x, t3y + i, t3z).setType(Material.LOG);
            world.getBlockAt(t3x, t3y + i, t3z).setData((byte) 1);
        }
        for (int ly = t3y + 2; ly <= t3y + 6; ly++) {
            int radius = (t3y + 6 - ly);
            for (int lx = t3x - radius; lx <= t3x + radius; lx++) {
                for (int lz = t3z - radius; lz <= t3z + radius; lz++) {
                    if (world.getBlockAt(lx, ly, lz).getType() == Material.AIR) {
                        world.getBlockAt(lx, ly, lz).setType(Material.LEAVES);
                        world.getBlockAt(lx, ly, lz).setData((byte) 1);
                    }
                }
            }
        }
        world.getBlockAt(t3x, t3y + 7, t3z).setType(Material.SNOW);

        // Ice spikes: 3-5 tall columns of packed ice (3-7 blocks tall)
        int spikeCount = 3 + rng.nextInt(3);
        for (int i = 0; i < spikeCount; i++) {
            int sx = cx - 10 + rng.nextInt(21);
            int sz = cz - 8 + rng.nextInt(17);
            int sy = getTopY(world, sx, sz, y, y + 12);
            if (sy <= 0) continue;
            int spikeHeight = 3 + rng.nextInt(5);
            for (int h = 0; h <= spikeHeight; h++) {
                world.getBlockAt(sx, sy + h, sz).setType(Material.PACKED_ICE);
            }
            // Taper: add adjacent blocks at base for thickness
            if (spikeHeight >= 4) {
                world.getBlockAt(sx + 1, sy, sz).setType(Material.PACKED_ICE);
                world.getBlockAt(sx, sy, sz + 1).setType(Material.PACKED_ICE);
                world.getBlockAt(sx + 1, sy + 1, sz).setType(Material.PACKED_ICE);
            }
        }

        // Frozen water pool (3x3 ice-covered area recessed into surface)
        int poolX = cx + 2;
        int poolZ = cz - 2;
        int poolY = getTopY(world, poolX, poolZ, y, y + 12);
        if (poolY > y) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int py = getTopY(world, poolX + dx, poolZ + dz, y, y + 12);
                    if (py > y) {
                        world.getBlockAt(poolX + dx, py, poolZ + dz).setType(Material.ICE);
                    }
                }
            }
        }

        // Snow layers everywhere on top of solid surfaces
        for (int x = cx - 12; x <= cx + 12; x++) {
            for (int z = cz - 10; z <= cz + 10; z++) {
                int ty = getTopY(world, x, z, y, y + 14);
                if (ty <= 0) continue;
                Material top = world.getBlockAt(x, ty, z).getType();
                if ((top == Material.SNOW_BLOCK || top == Material.PACKED_ICE || top == Material.ICE)
                        && world.getBlockAt(x, ty + 1, z).getType() == Material.AIR) {
                    world.getBlockAt(x, ty + 1, z).setType(Material.SNOW);
                }
            }
        }

        // Cobble gen under snow cover
        buildCobbleGen(world, cx + 5, topY, cz - 3, Material.PACKED_ICE);
        // Chest
        world.getBlockAt(cx, topY + 1, cz).setType(Material.CHEST);
        Chest chest = (Chest) world.getBlockAt(cx, topY + 1, cz).getState();
        chest.getBlockInventory().clear();
        chest.getBlockInventory().addItem(new ItemStack(Material.ICE, 2));
        chest.getBlockInventory().addItem(new ItemStack(Material.WATER_BUCKET, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.LAVA_BUCKET, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.SAPLING, 4, (short) 1)); // spruce
        chest.getBlockInventory().addItem(new ItemStack(Material.SEEDS, 16));
        chest.getBlockInventory().addItem(new ItemStack(Material.BREAD, 6));
        chest.update(true);
        island.setHome(new Location(world, cx + 0.5D, topY + 1.0D, cz + 0.5D));
    }

    // ── MUSHROOM preset ─────────────────────────────────────────────────────
    @SuppressWarnings("deprecation")
    private void generateMushroomIsland(World world, int cx, int y, int cz, long seed, Island island) {
        sculptFloatingIsland(world, cx, y + 1, cz, 15, 13, 2, Material.MYCEL, Material.DIRT, seed);
        int topY = getTopY(world, cx, cz, y, y + 12) + 1;
        Random rng = new Random(seed);

        // Big brown mushroom (wider cap, 4-block radius)
        int mX = cx - 4, mZ = cz - 3;
        int mBase = getTopY(world, mX, mZ, y, y + 12) + 1;
        for (int i = 0; i < 7; i++) {
            world.getBlockAt(mX, mBase + i, mZ).setType(Material.HUGE_MUSHROOM_2); // stem
        }
        // Brown cap at top (radius 4)
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist <= 4.5D) {
                    world.getBlockAt(mX + dx, mBase + 7, mZ + dz).setType(Material.HUGE_MUSHROOM_1);
                }
                // Second layer for depth
                if (dist <= 3.0D) {
                    world.getBlockAt(mX + dx, mBase + 8, mZ + dz).setType(Material.HUGE_MUSHROOM_1);
                }
            }
        }

        // Big red mushroom (different position)
        int rX = cx + 5, rZ = cz + 4;
        int rBase = getTopY(world, rX, rZ, y, y + 12) + 1;
        // Stem (HUGE_MUSHROOM_2)
        for (int i = 0; i < 6; i++) {
            world.getBlockAt(rX, rBase + i, rZ).setType(Material.HUGE_MUSHROOM_2);
        }
        // Red mushroom cap (dome shape using HUGE_MUSHROOM_1)
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                // Top dome layer
                if (dist <= 3.2D) {
                    world.getBlockAt(rX + dx, rBase + 6, rZ + dz).setType(Material.HUGE_MUSHROOM_1);
                }
                // Upper dome (smaller)
                if (dist <= 2.0D) {
                    world.getBlockAt(rX + dx, rBase + 7, rZ + dz).setType(Material.HUGE_MUSHROOM_1);
                }
                // Sides of cap (skirt)
                if (dist > 2.0D && dist <= 3.2D) {
                    world.getBlockAt(rX + dx, rBase + 5, rZ + dz).setType(Material.HUGE_MUSHROOM_1);
                }
            }
        }

        // Scattered small mushrooms (8-12)
        int smallCount = 8 + rng.nextInt(5);
        for (int i = 0; i < smallCount; i++) {
            int fx = cx - 10 + rng.nextInt(21);
            int fz = cz - 8 + rng.nextInt(17);
            int fy = getTopY(world, fx, fz, y, y + 12);
            if (fy > 0 && world.getBlockAt(fx, fy + 1, fz).getType() == Material.AIR) {
                Material shroom = rng.nextInt(3) == 0 ? Material.BROWN_MUSHROOM : Material.RED_MUSHROOM;
                world.getBlockAt(fx, fy + 1, fz).setType(shroom);
            }
        }

        // Grass patches between mycelium for variety
        for (int i = 0; i < 8; i++) {
            int gx = cx - 9 + rng.nextInt(19);
            int gz = cz - 7 + rng.nextInt(15);
            int gy = getTopY(world, gx, gz, y, y + 12);
            if (gy > 0 && world.getBlockAt(gx, gy, gz).getType() == Material.MYCEL) {
                world.getBlockAt(gx, gy, gz).setType(Material.GRASS);
            }
        }

        // Cobble gen in mossy cobble
        buildCobbleGen(world, cx + 5, topY, cz - 4, Material.MOSSY_COBBLESTONE);
        // Chest with mooshroom egg
        world.getBlockAt(cx + 2, topY + 1, cz).setType(Material.CHEST);
        Chest chest = (Chest) world.getBlockAt(cx + 2, topY + 1, cz).getState();
        chest.getBlockInventory().clear();
        chest.getBlockInventory().addItem(new ItemStack(Material.ICE, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.WATER_BUCKET, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.LAVA_BUCKET, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.SAPLING, 4));
        chest.getBlockInventory().addItem(new ItemStack(Material.SEEDS, 16));
        chest.getBlockInventory().addItem(new ItemStack(Material.MONSTER_EGG, 1, (short) 96)); // mooshroom
        chest.getBlockInventory().addItem(new ItemStack(Material.BREAD, 6));
        chest.update(true);
        island.setHome(new Location(world, cx + 0.5D, topY + 1.0D, cz + 0.5D));
    }

    // ── CLASSIC preset (original) ───────────────────────────────────────────
    @SuppressWarnings("deprecation")
    public void generateStarterIsland(Island island) {
        World world = plugin.getWorldManager().getOrCreateIslandWorld();
        int y = plugin.getConfig().getInt("worlds.island-y", 100);
        int cx = island.getCenterX();
        int cz = island.getCenterZ();
        long seed = (cx * 31L) ^ (cz * 17L) ^ 1409L;
        // Bigger terrain sculpt for a lush feel
        sculptFloatingIsland(world, cx, y + 1, cz, 20, 18, 4, Material.GRASS, Material.DIRT, seed);
        Random deco = new Random((cx * 97L) ^ (cz * 67L) ^ 7127L);

        int baseTop = getTopY(world, cx, cz, y, y + 16) + 1;

        // --- Small pond (3x3 water surrounded by grass) ---
        int pondX = cx - 8;
        int pondZ = cz - 5;
        int pondY = getTopY(world, pondX, pondZ, y, y + 16);
        if (pondY > y) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int py = getTopY(world, pondX + dx, pondZ + dz, y, y + 16);
                    if (py > y) {
                        world.getBlockAt(pondX + dx, py, pondZ + dz).setType(Material.STATIONARY_WATER);
                        // Ensure grass border
                        world.getBlockAt(pondX + dx, py - 1, pondZ + dz).setType(Material.DIRT);
                        clearColumnAbove(world, pondX + dx, py + 1, py + 4, pondZ + dz);
                    }
                }
            }
            // Sugar cane by water
            int scY = getTopY(world, pondX + 2, pondZ, y, y + 16);
            if (scY > y && world.getBlockAt(pondX + 2, scY, pondZ).getType() == Material.GRASS) {
                world.getBlockAt(pondX + 2, scY, pondZ).setType(Material.DIRT);
                world.getBlockAt(pondX + 2, scY + 1, pondZ).setType(Material.SUGAR_CANE_BLOCK);
                world.getBlockAt(pondX + 2, scY + 2, pondZ).setType(Material.SUGAR_CANE_BLOCK);
            }
            int sc2Y = getTopY(world, pondX - 2, pondZ + 1, y, y + 16);
            if (sc2Y > y && world.getBlockAt(pondX - 2, sc2Y, pondZ + 1).getType() == Material.GRASS) {
                world.getBlockAt(pondX - 2, sc2Y, pondZ + 1).setType(Material.DIRT);
                world.getBlockAt(pondX - 2, sc2Y + 1, pondZ + 1).setType(Material.SUGAR_CANE_BLOCK);
                world.getBlockAt(pondX - 2, sc2Y + 2, pondZ + 1).setType(Material.SUGAR_CANE_BLOCK);
                world.getBlockAt(pondX - 2, sc2Y + 3, pondZ + 1).setType(Material.SUGAR_CANE_BLOCK);
            }
            // Lily pads on the water
            world.getBlockAt(pondX, pondY + 1, pondZ).setType(Material.WATER_LILY);
            world.getBlockAt(pondX - 1, pondY + 1, pondZ + 1).setType(Material.WATER_LILY);
        }

        // --- Grass path from spawn to farm and cobble gen ---
        for (int px = cx - 6; px <= cx + 7; px++) {
            int pathY = getTopY(world, px, cz, y, y + 16);
            if (pathY > y && world.getBlockAt(px, pathY, cz).getType() == Material.GRASS) {
                world.getBlockAt(px, pathY, cz).setType(Material.GRAVEL);
            }
        }
        // Path branch to farm
        for (int pz = cz; pz <= cz + 6; pz++) {
            int pathY = getTopY(world, cx - 5, pz, y, y + 16);
            if (pathY > y && world.getBlockAt(cx - 5, pathY, pz).getType() == Material.GRASS) {
                world.getBlockAt(cx - 5, pathY, pz).setType(Material.GRAVEL);
            }
        }

        // --- Farm area with fences ---
        int farmX = cx - 10;
        int farmZ = cz + 4;
        int farmY = getTopY(world, farmX + 3, farmZ + 2, y, y + 16) + 1;
        // Prepare soil and crops inside fenced area (5x5 inner)
        for (int x = farmX; x <= farmX + 6; x++) {
            for (int z = farmZ; z <= farmZ + 4; z++) {
                world.getBlockAt(x, farmY - 1, z).setType(Material.DIRT);
                world.getBlockAt(x, farmY, z).setType(Material.SOIL);
                world.getBlockAt(x, farmY + 1, z).setType(Material.CROPS);
            }
        }
        // Water channel through middle
        for (int x = farmX; x <= farmX + 6; x++) {
            world.getBlockAt(x, farmY, farmZ + 2).setType(Material.STATIONARY_WATER);
            world.getBlockAt(x, farmY + 1, farmZ + 2).setType(Material.AIR);
        }
        // Fence border around farm
        for (int x = farmX - 1; x <= farmX + 7; x++) {
            world.getBlockAt(x, farmY, farmZ - 1).setType(Material.FENCE);
            world.getBlockAt(x, farmY, farmZ + 5).setType(Material.FENCE);
        }
        for (int z = farmZ - 1; z <= farmZ + 5; z++) {
            world.getBlockAt(farmX - 1, farmY, z).setType(Material.FENCE);
            world.getBlockAt(farmX + 7, farmY, z).setType(Material.FENCE);
        }
        // Farm gate entrance
        world.getBlockAt(farmX + 3, farmY, farmZ - 1).setType(Material.FENCE_GATE);

        // --- Enclosed cobblestone generator ---
        buildCobbleGen(world, cx + 8, baseTop, cz - 2, Material.COBBLESTONE);

        // --- Chest ---
        int chestY = getTopY(world, cx + 7, cz + 7, y, y + 16) + 1;
        world.getBlockAt(cx + 7, chestY, cz + 7).setType(Material.CHEST);
        Chest chest = (Chest) world.getBlockAt(cx + 7, chestY, cz + 7).getState();
        chest.getBlockInventory().clear();
        chest.getBlockInventory().addItem(new ItemStack(Material.ICE, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.WATER_BUCKET, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.LAVA_BUCKET, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.MELON, 4));
        chest.getBlockInventory().addItem(new ItemStack(Material.SEEDS, 16));
        chest.getBlockInventory().addItem(new ItemStack(Material.SUGAR_CANE, 2));
        chest.getBlockInventory().addItem(new ItemStack(Material.TORCH, 16));
        chest.getBlockInventory().addItem(new ItemStack(Material.SAPLING, 2));
        chest.getBlockInventory().addItem(new ItemStack(Material.BREAD, 6));
        chest.update(true);

        // --- 3-4 oak trees spread naturally ---
        int[][] treeSpots = {
            {cx - 12, cz - 7},
            {cx + 4, cz - 10},
            {cx + 10, cz + 5},
            {cx - 6, cz + 10}
        };
        int treeCount = 3 + deco.nextInt(2);
        for (int i = 0; i < treeCount; i++) {
            int tx = treeSpots[i][0] + deco.nextInt(3) - 1;
            int tz = treeSpots[i][1] + deco.nextInt(3) - 1;
            int ty = getTopY(world, tx, tz, y, y + 16);
            if (ty > y && world.getBlockAt(tx, ty, tz).getType() == Material.GRASS) {
                placeTree(world, tx, ty + 1, tz);
            }
        }

        // --- Flower garden with mixed flowers ---
        Material[] flowers = {
            Material.RED_ROSE, Material.YELLOW_FLOWER, Material.RED_ROSE,
            Material.YELLOW_FLOWER, Material.RED_ROSE, Material.RED_ROSE
        };
        for (int i = 0; i < 16; i++) {
            int fx = cx - 14 + deco.nextInt(29);
            int fz = cz - 12 + deco.nextInt(25);
            int fy = getTopY(world, fx, fz, y, y + 16);
            if (fy > y && world.getBlockAt(fx, fy, fz).getType() == Material.GRASS
                    && world.getBlockAt(fx, fy + 1, fz).getType() == Material.AIR) {
                world.getBlockAt(fx, fy + 1, fz).setType(flowers[deco.nextInt(flowers.length)]);
            }
        }

        // --- Tall grass for natural look ---
        for (int i = 0; i < 20; i++) {
            int gx = cx - 14 + deco.nextInt(29);
            int gz = cz - 12 + deco.nextInt(25);
            int gy = getTopY(world, gx, gz, y, y + 16);
            if (gy > y && world.getBlockAt(gx, gy, gz).getType() == Material.GRASS
                    && world.getBlockAt(gx, gy + 1, gz).getType() == Material.AIR) {
                world.getBlockAt(gx, gy + 1, gz).setType(Material.LONG_GRASS);
                world.getBlockAt(gx, gy + 1, gz).setData((byte) 1);
            }
        }

        // --- Lanterns on fence posts ---
        int[][] lanternSpots = {
            {cx - 5, cz - 5}, {cx + 5, cz - 5}, {cx + 5, cz + 5}, {cx - 5, cz + 5}
        };
        for (int i = 0; i < lanternSpots.length; i++) {
            int lx = lanternSpots[i][0];
            int lz = lanternSpots[i][1];
            int ly = getTopY(world, lx, lz, y, y + 16);
            if (ly > y) {
                placeLantern(world, lx, ly + 1, lz);
            }
        }

        island.setHome(new Location(world, cx + 0.5D, baseTop + 1.0D, cz + 0.5D));
    }

    public boolean unlockFarmingIsland(Island island) {
        if (island == null || island.isFarmingUnlocked()) return false;
        int price = plugin.getConfig().getInt("island-expansions.solo.farming-price", 10000);
        if (!plugin.getEconomyManager().take(island.getOwner(), price)) return false;
        island.setFarmingUnlocked(true);
        generateFarmingIsland(island);
        plugin.getEconomyManager().save();
        save();
        return true;
    }

    public boolean unlockMiningIsland(Island island) {
        if (island == null || island.isMiningUnlocked()) return false;
        int price = plugin.getConfig().getInt("island-expansions.solo.mining-price", 10000);
        if (!plugin.getEconomyManager().take(island.getOwner(), price)) return false;
        island.setMiningUnlocked(true);
        generateMiningIsland(island);
        plugin.getEconomyManager().save();
        save();
        return true;
    }

    private void buildBridge(World world, int y, int fromX, int toX, int z) {
        int start = Math.min(fromX, toX);
        int end = Math.max(fromX, toX);
        for (int x = start; x <= end; x++) {
            fillColumn(world, x, y, z, Material.WOOD, Material.DIRT);
            fillColumn(world, x, y, z + 1, Material.WOOD, Material.DIRT);
            fillColumn(world, x, y, z - 1, Material.WOOD, Material.DIRT);
            fillColumn(world, x, y, z + 2, Material.COBBLESTONE, Material.STONE);
            fillColumn(world, x, y, z - 2, Material.COBBLESTONE, Material.STONE);
            clearColumnAbove(world, x, y + 1, y + 5, z);
            clearColumnAbove(world, x, y + 1, y + 5, z + 1);
            clearColumnAbove(world, x, y + 1, y + 5, z - 1);
            if ((x - start) % 7 == 0) {
                placeLantern(world, x, y + 1, z + 2);
                placeLantern(world, x, y + 1, z - 2);
            }
        }
    }

    private void generateFarmingIsland(Island island) {
        World world = plugin.getWorldManager().getOrCreateIslandWorld();
        int y = plugin.getConfig().getInt("worlds.island-y", 100);
        int targetX = island.getCenterX() - 110;
        int targetZ = island.getCenterZ();
        buildBridge(world, y + 1, island.getCenterX() - 11, targetX + 25, targetZ);
        sculptFloatingIsland(world, targetX, y + 1, targetZ, 32, 23, 2, Material.GRASS, Material.DIRT, (island.getCenterX() * 53L) ^ island.getCenterZ() ^ 4001L);

        Random detail = new Random((island.getCenterX() * 79L) ^ island.getCenterZ() ^ 9923L);
        int fieldY = getTopY(world, targetX, targetZ, y, y + 18) + 1;
        for (int x = targetX - 18; x <= targetX + 18; x++) {
            for (int z = targetZ - 10; z <= targetZ + 10; z++) {
                double dx = (x - targetX) / 18.0D;
                double dz = (z - targetZ) / 10.0D;
                if ((dx * dx) + (dz * dz) > 1.0D) continue;
                world.getBlockAt(x, fieldY - 1, z).setType(Material.DIRT);
                if (z == targetZ - 6 || z == targetZ || z == targetZ + 6) {
                    world.getBlockAt(x, fieldY, z).setType(Material.STATIONARY_WATER);
                    world.getBlockAt(x, fieldY + 1, z).setType(Material.AIR);
                } else if ((x + z + detail.nextInt(3)) % 11 == 0) {
                    world.getBlockAt(x, fieldY, z).setType(Material.WOOD);
                    world.getBlockAt(x, fieldY + 1, z).setType(Material.AIR);
                } else {
                    world.getBlockAt(x, fieldY, z).setType(Material.SOIL);
                    Material crop = Material.CROPS;
                    if (x < targetX - 6) crop = Material.CARROT;
                    else if (x > targetX + 6) crop = Material.POTATO;
                    world.getBlockAt(x, fieldY + 1, z).setType(crop);
                }
            }
        }

        for (int i = 0; i < 4; i++) {
            int tx = targetX - 12 + (i * 8);
            int tz = targetZ + ((i % 2 == 0) ? 14 : -14);
            placeTree(world, tx, getTopY(world, tx, tz, y, y + 18) + 1, tz);
        }

        world.getBlockAt(targetX + 11 + detail.nextInt(6), fieldY + 1, targetZ - 3).setType(Material.MELON_BLOCK);
        world.getBlockAt(targetX + 10 + detail.nextInt(6), fieldY + 1, targetZ + detail.nextInt(2)).setType(Material.MELON_BLOCK);
        world.getBlockAt(targetX + 11 + detail.nextInt(5), fieldY + 1, targetZ + 3).setType(Material.PUMPKIN);

        for (int i = 0; i < 24; i++) {
            int fx = targetX - 20 + (i % 12) * 3;
            int fz = targetZ - 16 + (i / 3) * 2;
            setFlower(world, fx, getTopY(world, fx, fz, y, y + 16) + 1, fz, (i % 2 == 0) ? Material.RED_ROSE : Material.YELLOW_FLOWER);
        }

        int chestY = getTopY(world, targetX + 20, targetZ + 11, y, y + 16) + 1;
        world.getBlockAt(targetX + 20, chestY, targetZ + 11).setType(Material.CHEST);
        Chest chest = (Chest) world.getBlockAt(targetX + 20, chestY, targetZ + 11).getState();
        chest.getBlockInventory().clear();
        chest.getBlockInventory().addItem(new ItemStack(Material.IRON_AXE, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.IRON_SPADE, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.IRON_HOE, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.WATER_BUCKET, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.BONE, 12));
        chest.getBlockInventory().addItem(new ItemStack(Material.CARROT_ITEM, 16));
        chest.getBlockInventory().addItem(new ItemStack(Material.POTATO_ITEM, 16));
        chest.update(true);

        placeLantern(world, targetX - 16, fieldY + 1, targetZ - 11);
        placeLantern(world, targetX + 16, fieldY + 1, targetZ - 11);
        placeLantern(world, targetX - 16, fieldY + 1, targetZ + 11);
        placeLantern(world, targetX + 16, fieldY + 1, targetZ + 11);

        island.setFarmingHome(new Location(world, targetX + 0.5D, fieldY + 1.0D, targetZ + 0.5D));
    }

    private int getTopY(World world, int x, int z, int minY, int maxY) {
        for (int yy = maxY; yy >= minY; yy--) if (world.getBlockAt(x, yy, z).getType() != Material.AIR) return yy;
        return minY;
    }

    private void generateMiningIsland(Island island) {
        World world = plugin.getWorldManager().getOrCreateIslandWorld();
        int y = plugin.getConfig().getInt("worlds.island-y", 100);
        int targetX = island.getCenterX() + 110;
        int targetZ = island.getCenterZ();
        buildBridge(world, y + 1, island.getCenterX() + 11, targetX - 25, targetZ);
        sculptFloatingIsland(world, targetX, y + 1, targetZ, 30, 22, 3, Material.STONE, Material.STONE, (island.getCenterX() * 29L) ^ island.getCenterZ() ^ 7331L);

        Random random = new Random((island.getCenterX() * 31L) ^ island.getCenterZ() ^ 7331L);
        for (int x = targetX - 24; x <= targetX + 24; x++) {
            for (int z = targetZ - 17; z <= targetZ + 17; z++) {
                int topY = getTopY(world, x, z, y, y + 18);
                if (topY <= y) continue;
                int depthMax = Math.max(3, Math.min(8, topY - (y + 1)));
                for (int depth = 1; depth <= depthMax; depth++) {
                    if (random.nextInt(100) < 16) world.getBlockAt(x, topY - depth, z).setType(pickOre(random));
                }
            }
        }

        int quarryX = targetX + random.nextInt(5) - 2;
        int quarryZ = targetZ + random.nextInt(5) - 2;
        int quarryY = getTopY(world, quarryX, quarryZ, y, y + 18);
        for (int x = quarryX - 6; x <= quarryX + 6; x++) {
            for (int z = quarryZ - 6; z <= quarryZ + 6; z++) {
                double nx = (x - quarryX) / 6.0D;
                double nz = (z - quarryZ) / 6.0D;
                if ((nx * nx) + (nz * nz) > 1.0D) continue;
                for (int yy = y + 3; yy <= quarryY; yy++) world.getBlockAt(x, yy, z).setType(Material.AIR);
                world.getBlockAt(x, y + 2, z).setType(Material.COBBLESTONE);
            }
        }

        for (int i = 0; i < 4; i++) {
            int sx = quarryX + ((i < 2) ? -7 : 7) + random.nextInt(3) - 1;
            int sz = quarryZ + ((i % 2 == 0) ? -7 : 7) + random.nextInt(3) - 1;
            int sy = getTopY(world, sx, sz, y, y + 18) + 1;
            world.getBlockAt(sx, sy, sz).setType(Material.LOG);
            world.getBlockAt(sx, sy + 1, sz).setType(Material.LOG);
            world.getBlockAt(sx, sy + 2, sz).setType(Material.TORCH);
        }

        int chestY = getTopY(world, targetX + 17, targetZ + 11, y, y + 18) + 1;
        world.getBlockAt(targetX + 17, chestY, targetZ + 11).setType(Material.CHEST);
        Chest chest = (Chest) world.getBlockAt(targetX + 17, chestY, targetZ + 11).getState();
        chest.getBlockInventory().clear();
        chest.getBlockInventory().addItem(new ItemStack(Material.IRON_PICKAXE, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.STONE_PICKAXE, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.TORCH, 32));
        chest.getBlockInventory().addItem(new ItemStack(Material.LADDER, 24));
        chest.update(true);

        island.setMiningHome(new Location(world, quarryX - 8.5D, getTopY(world, quarryX - 9, quarryZ, y, y + 18) + 2, quarryZ + 0.5D));
    }

    private IslandRollback createRollback(Island island, boolean deletedIsland) {
        World world = plugin.getWorldManager().getOrCreateIslandWorld();
        int size = plugin.getConfig().getInt("islands.size", 320);
        int half = size / 2;
        List<BlockSnapshot> blocks = new ArrayList<BlockSnapshot>();
        for (int x = island.getCenterX() - half; x <= island.getCenterX() + half; x++)
            for (int z = island.getCenterZ() - half; z <= island.getCenterZ() + half; z++)
                for (int yy = 0; yy <= 255; yy++) {
                    Block block = world.getBlockAt(x, yy, z);
                    if (block.getType() != Material.AIR) blocks.add(BlockSnapshot.capture(block));
                }
        return new IslandRollback(island, deletedIsland, blocks);
    }

    public void resetIsland(Island island) {
        if (island == null) return;
        lastRollbacks.put(island.getOwner(), createRollback(island, false));
        clearIslandArea(island);
        island.getMembers().clear();
        island.getTrusted().clear();
        island.getBanned().clear();
        island.setInviteUnlocks(0);
        island.setPvpEnabled(false);
        island.setFarmingUnlocked(false);
        island.setMiningUnlocked(false);
        island.setFarmingHome(null);
        island.setMiningHome(null);
        generateStarterIsland(island);
        save();
    }

    public void deleteIsland(Island island) {
        if (island == null) return;
        lastRollbacks.put(island.getOwner(), createRollback(island, true));
        clearIslandArea(island);
        for (UUID member : new HashSet<UUID>(island.getMembers())) memberIndex.remove(member);
        ownedIslands.remove(island.getOwner());
        save();
    }

    private void clearIslandArea(Island island) {
        World world = plugin.getWorldManager().getOrCreateIslandWorld();
        int size = plugin.getConfig().getInt("islands.size", 320);
        int half = size / 2;
        for (int x = island.getCenterX() - half; x <= island.getCenterX() + half; x++)
            for (int z = island.getCenterZ() - half; z <= island.getCenterZ() + half; z++)
                for (int yy = 0; yy <= 255; yy++) world.getBlockAt(x, yy, z).setType(Material.AIR);
    }

    public boolean rollbackLastIsland(UUID owner) {
        IslandRollback rollback = lastRollbacks.get(owner);
        if (rollback == null) return false;
        World world = plugin.getWorldManager().getOrCreateIslandWorld();
        Island island = ownedIslands.get(owner);
        if (rollback.isDeletedIsland()) {
            island = new Island(owner, rollback.getGridX(), rollback.getGridZ(), rollback.getCenterX(), rollback.getCenterZ());
            ownedIslands.put(owner, island);
        }
        if (island == null) return false;
        clearIslandArea(island);
        island.setPublicVisit(rollback.isPublicVisit());
        island.setPvpEnabled(rollback.isPvpEnabled());
        island.setBankBalance(rollback.getBankBalance());
        island.setHome(rollback.getHome());
        island.setName(rollback.getName());
        island.setInviteUnlocks(rollback.getInviteUnlocks());
        island.getMembers().clear(); island.getMembers().addAll(rollback.getMembers());
        island.getTrusted().clear(); island.getTrusted().addAll(rollback.getTrusted());
        island.getBanned().clear(); island.getBanned().addAll(rollback.getBanned());
        for (UUID member : rollback.getMembers()) memberIndex.put(member, owner);
        for (BlockSnapshot block : rollback.getBlocks()) block.restore(world);
        lastRollbacks.remove(owner);
        save();
        return true;
    }

    public boolean invite(Player owner, Player target) {
        Island island = getOwnedIsland(owner.getUniqueId());
        if (island == null || target == null || target.getUniqueId().equals(owner.getUniqueId()) || hasIsland(target.getUniqueId()) || memberIndex.containsKey(target.getUniqueId())) return false;
        if (island.getMembers().size() >= getMaxMembers()) return false;
        if (island.getInviteUnlocks() <= 0) return false;
        long expireAt = System.currentTimeMillis() + 120000L;
        pendingInvites.put(target.getUniqueId(), new PendingInvite(owner.getUniqueId(), expireAt));
        island.consumeInviteUnlock();
        save();
        return true;
    }

    public Island acceptInvite(Player player) {
        PendingInvite invite = pendingInvites.remove(player.getUniqueId());
        if (invite == null || invite.expireAt < System.currentTimeMillis()) return null;
        Island island = ownedIslands.get(invite.owner);
        if (island == null || island.getMembers().size() >= getMaxMembers()) return null;
        island.getMembers().add(player.getUniqueId());
        island.getBanned().remove(player.getUniqueId());
        memberIndex.put(player.getUniqueId(), invite.owner);
        save();
        return island;
    }

    public boolean denyInvite(Player player) { return pendingInvites.remove(player.getUniqueId()) != null; }

    public boolean removeMember(Island island, UUID member) {
        memberIndex.remove(member);
        boolean changed = island.getMembers().remove(member) || island.getTrusted().remove(member);
        if (changed) save();
        return changed;
    }

    public boolean canBuild(Player player, Location location) {
        if (player.hasPermission("pastequeskyblock.admin")) return true;
        Island island = getIslandAt(location);
        return island != null && island.canBuild(player.getUniqueId());
    }

    public boolean canOpen(Player player, Location location) {
        if (player.hasPermission("pastequeskyblock.admin")) return true;
        Island island = getIslandAt(location);
        return island != null && island.canBuild(player.getUniqueId());
    }

    public boolean canVisit(Player player, Island island) { return island != null && island.canVisit(player.getUniqueId()); }
    public void setHome(Island island, Location location) { island.setHome(location); save(); }
    public void setPvp(Island island, boolean enabled) { island.setPvpEnabled(enabled); save(); }
    public void rename(Island island, String newName) { island.setName(newName); save(); }
    public void addInviteUnlock(Island island) { island.addInviteUnlock(); save(); }

    public int getLevel(Island island) {
        if (island == null) return 0;
        int size = plugin.getConfig().getInt("islands.size", 320);
        int half = size / 2;
        World world = plugin.getWorldManager().getOrCreateIslandWorld();
        ConfigurationSection values = plugin.getConfig().getConfigurationSection("block-values");
        if (values == null) return 0;
        int total = 0;
        for (int x = island.getCenterX() - half; x <= island.getCenterX() + half; x++)
            for (int z = island.getCenterZ() - half; z <= island.getCenterZ() + half; z++)
                for (int yy = 0; yy <= 255; yy++)
                    total += values.getInt(world.getBlockAt(x, yy, z).getType().name(), 0);
        return total;
    }

    public Location getSafeTeleport(Island island) {
        if (island != null && island.getHome() != null && island.getHome().getWorld() != null) return island.getHome();
        if (island != null) return new Location(plugin.getWorldManager().getOrCreateIslandWorld(), island.getCenterX() + 0.5D, plugin.getConfig().getInt("worlds.island-y", 100) + 2, island.getCenterZ() + 0.5D);
        return plugin.getWorldManager().getServerSpawn();
    }

    public boolean isSkyblockWorld(Location location) { return location != null && location.getWorld() != null && location.getWorld().getName().equalsIgnoreCase(plugin.getWorldManager().getIslandWorldName()); }

    public List<Island> getSortedIslands() {
        List<Island> list = new ArrayList<Island>(ownedIslands.values());
        Collections.sort(list, new Comparator<Island>() {
            public int compare(Island a, Island b) { return a.getOwner().toString().compareToIgnoreCase(b.getOwner().toString()); }
        });
        return list;
    }
}
