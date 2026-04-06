package fr.pasteque.skyblock.arena;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.generator.EmptyChunkGenerator;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class ArenaWorldService {

    private final PastequeSkyblockPlugin plugin;
    private final Map<UUID, GameMode> previousGamemodes = new HashMap<UUID, GameMode>();
    private World arenaWorld;
    private World duelWorld;

    private int nextMatchId = 1;
    private final Random random = new Random();

    public ArenaWorldService(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public void initializeArenaWorld() {
        String worldName = plugin.getConfig().getString("world-name", "skyblockarena");
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            WorldCreator creator = new WorldCreator(worldName);
            creator.type(WorldType.NORMAL);
            creator.generator(new EmptyChunkGenerator());
            world = creator.createWorld();
        }
        if (world == null) {
            throw new IllegalStateException("Impossible de creer le monde d'arene");
        }
        this.arenaWorld = world;
        applyWorldRules(world);

        // Dedicated duel world (isolated from general arena, zero corruption)
        String duelWorldName = plugin.getConfig().getString("duel-world-name", "skyblockduels");
        World dWorld = plugin.getServer().getWorld(duelWorldName);
        if (dWorld == null) {
            WorldCreator dCreator = new WorldCreator(duelWorldName);
            dCreator.type(WorldType.NORMAL);
            dCreator.generator(new EmptyChunkGenerator());
            dWorld = dCreator.createWorld();
        }
        if (dWorld != null) {
            this.duelWorld = dWorld;
            dWorld.setStorm(false);
            dWorld.setThundering(false);
            dWorld.setWeatherDuration(Integer.MAX_VALUE);
            dWorld.setDifficulty(Difficulty.PEACEFUL);
            dWorld.setPVP(true);
            dWorld.setGameRuleValue("doMobSpawning", "false");
            dWorld.setGameRuleValue("doFireTick", "false");
            dWorld.setGameRuleValue("mobGriefing", "false");
            dWorld.setGameRuleValue("doDaylightCycle", "false");
            dWorld.setGameRuleValue("doWeatherCycle", "false");
            dWorld.setGameRuleValue("naturalRegeneration", "true");
            dWorld.setSpawnLocation(0, 80, 0);
        }
    }

    public World getDuelWorld() {
        return duelWorld;
    }

    public boolean isDuelWorld(World world) {
        return duelWorld != null && world != null && duelWorld.getName().equalsIgnoreCase(world.getName());
    }

    private void applyWorldRules(World world) {
        world.setStorm(false);
        world.setThundering(false);
        world.setWeatherDuration(Integer.MAX_VALUE);
        world.setDifficulty(Difficulty.valueOf(plugin.getConfig().getString("world-difficulty", "PEACEFUL")));
        world.setPVP(true);
        world.setGameRuleValue("doMobSpawning", "false");
        world.setGameRuleValue("doFireTick", "false");
        world.setGameRuleValue("naturalRegeneration", "true");
        Location spawn = getConfiguredSpawn(world);
        world.setSpawnLocation(spawn.getBlockX(), spawn.getBlockY(), spawn.getBlockZ());
    }

    public boolean isArenaWorld(World world) {
        return arenaWorld != null && world != null && arenaWorld.getName().equalsIgnoreCase(world.getName());
    }

    public World getArenaWorld() {
        return arenaWorld;
    }

    public Location getConfiguredSpawn(World world) {
        double x = plugin.getConfig().getDouble("world-spawn.x", 0.5D);
        double y = plugin.getConfig().getDouble("world-spawn.y", 80.0D);
        double z = plugin.getConfig().getDouble("world-spawn.z", 0.5D);
        float yaw = (float) plugin.getConfig().getDouble("world-spawn.yaw", 0.0D);
        float pitch = (float) plugin.getConfig().getDouble("world-spawn.pitch", 0.0D);
        return new Location(world, x, y, z, yaw, pitch);
    }

    public void setConfiguredSpawn(Location location) {
        plugin.getConfig().set("world-spawn.x", location.getX());
        plugin.getConfig().set("world-spawn.y", location.getY());
        plugin.getConfig().set("world-spawn.z", location.getZ());
        plugin.getConfig().set("world-spawn.yaw", (double) location.getYaw());
        plugin.getConfig().set("world-spawn.pitch", (double) location.getPitch());
        plugin.saveConfig();
        if (arenaWorld != null) {
            arenaWorld.setSpawnLocation(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }
    }

    public void sendToArena(Player player) {
        if (player == null || arenaWorld == null) {
            return;
        }
        if (!player.hasPermission("pastequearena.admin")) {
            previousGamemodes.put(player.getUniqueId(), player.getGameMode());
            player.setGameMode(GameMode.SURVIVAL);
        }
        player.teleport(getConfiguredSpawn(arenaWorld));
    }

    public void handleWorldChange(Player player, World from, World to) {
        if (isArenaWorld(to) && !player.hasPermission("pastequearena.admin")) {
            previousGamemodes.put(player.getUniqueId(), player.getGameMode());
            player.setGameMode(GameMode.SURVIVAL);
            return;
        }
        if (isArenaWorld(from) && !isArenaWorld(to) && previousGamemodes.containsKey(player.getUniqueId())) {
            GameMode previous = previousGamemodes.remove(player.getUniqueId());
            if (previous != null) {
                player.setGameMode(previous);
            }
        }
    }

    // =========================================================================
    //  Per-match arena instances
    // =========================================================================

    /**
     * Allocates and returns the next match ID, then builds a random map.
     */
    public int allocateMatchId() {
        return nextMatchId++;
    }

    /**
     * Returns the center Location for a given match arena (in the DEDICATED duel world).
     */
    public Location getMatchCenter(int matchId) {
        World w = duelWorld != null ? duelWorld : arenaWorld;
        return new Location(w, matchId * 200, 80, 0);
    }

    /**
     * Returns spawn point 1 (positive X offset) for the given match.
     */
    public Location getSpawn1(int matchId, int offsetX) {
        Location center = getMatchCenter(matchId);
        Location spawn = center.clone().add(offsetX, 1, 0);
        spawn.setYaw(-90.0F); // Face toward negative X (toward spawn2)
        spawn.setPitch(0.0F);
        return spawn;
    }

    /**
     * Returns spawn point 2 (negative X offset) for the given match.
     */
    public Location getSpawn2(int matchId, int offsetX) {
        Location center = getMatchCenter(matchId);
        Location spawn = center.clone().add(-offsetX, 1, 0);
        spawn.setYaw(90.0F); // Face toward positive X (toward spawn1)
        spawn.setPitch(0.0F);
        return spawn;
    }

    /**
     * Builds a random arena map at the given match location.
     * Returns the spawn offset X for that map type.
     */
    @SuppressWarnings("deprecation")
    public int createMatchArena(int matchId) {
        int mapType = random.nextInt(4);
        Location center = getMatchCenter(matchId);
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        switch (mapType) {
            case 0:
                buildClassicArena(cx, cy, cz);
                return 12;
            case 1:
                buildFootballField(cx, cy, cz);
                return 15;
            case 2:
                buildNetherRuins(cx, cy, cz);
                return 12;
            case 3:
                buildAbandonedCity(cx, cy, cz);
                return 15;
            default:
                buildClassicArena(cx, cy, cz);
                return 12;
        }
    }

    /**
     * Clears the arena area for a given match.
     */
    public void destroyMatchArena(int matchId) {
        Location center = getMatchCenter(matchId);
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        // Clear a generous 50x30x50 area
        for (int x = cx - 25; x <= cx + 25; x++) {
            for (int y = cy - 2; y <= cy + 20; y++) {
                for (int z = cz - 25; z <= cz + 25; z++) {
                    setBlock(x, y, z, Material.AIR);
                }
            }
        }
    }

    // =========================================================================
    //  Map builders
    // =========================================================================

    @SuppressWarnings("deprecation")
    private void buildClassicArena(int cx, int cy, int cz) {
        // 35x35 stone brick floor
        int half = 17;
        for (int x = cx - half; x <= cx + half; x++) {
            for (int z = cz - half; z <= cz + half; z++) {
                setBlock(x, cy, z, Material.SMOOTH_BRICK);
            }
        }

        // Iron fence border
        for (int x = cx - half; x <= cx + half; x++) {
            setBlock(x, cy + 1, cz - half, Material.IRON_FENCE);
            setBlock(x, cy + 1, cz + half, Material.IRON_FENCE);
        }
        for (int z = cz - half; z <= cz + half; z++) {
            setBlock(cx - half, cy + 1, z, Material.IRON_FENCE);
            setBlock(cx + half, cy + 1, z, Material.IRON_FENCE);
        }

        // 4 quartz pillars at corners (5 high)
        int[][] corners = {
            {cx - half, cz - half},
            {cx + half, cz - half},
            {cx - half, cz + half},
            {cx + half, cz + half}
        };
        for (int[] corner : corners) {
            for (int y = cy + 1; y <= cy + 5; y++) {
                setBlock(corner[0], y, corner[1], Material.QUARTZ_BLOCK);
            }
        }

        // Center platform (3x3, 1 block high, gold)
        for (int x = cx - 1; x <= cx + 1; x++) {
            for (int z = cz - 1; z <= cz + 1; z++) {
                setBlock(x, cy + 1, z, Material.GOLD_BLOCK);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void buildFootballField(int cx, int cy, int cz) {
        // 40x20 green wool floor (lime wool = data 5)
        int halfX = 20;
        int halfZ = 10;
        for (int x = cx - halfX; x <= cx + halfX; x++) {
            for (int z = cz - halfZ; z <= cz + halfZ; z++) {
                setBlockWithData(x, cy, z, Material.WOOL, (byte) 5);
            }
        }

        // White wool center line
        for (int z = cz - halfZ; z <= cz + halfZ; z++) {
            setBlock(x(cx), cy, z, Material.WOOL);
        }

        // White wool sidelines
        for (int x = cx - halfX; x <= cx + halfX; x++) {
            setBlock(x, cy, cz - halfZ, Material.WOOL);
            setBlock(x, cy, cz + halfZ, Material.WOOL);
        }

        // White wool end lines
        for (int z = cz - halfZ; z <= cz + halfZ; z++) {
            setBlock(cx - halfX, cy, z, Material.WOOL);
            setBlock(cx + halfX, cy, z, Material.WOOL);
        }

        // Center circle (radius 4)
        for (int x = cx - 4; x <= cx + 4; x++) {
            for (int z = cz - 4; z <= cz + 4; z++) {
                double dist = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
                if (dist >= 3.5 && dist <= 4.5) {
                    setBlock(x, cy, z, Material.WOOL);
                }
            }
        }

        // Goals at each end (fence posts + white wool top bar)
        // Goal 1 at -halfX
        for (int z = cz - 2; z <= cz + 2; z++) {
            setBlock(cx - halfX - 1, cy + 1, z, Material.FENCE);
            setBlock(cx - halfX - 1, cy + 2, z, Material.FENCE);
            setBlock(cx - halfX - 1, cy + 3, z, Material.WOOL);
        }
        // Goal 2 at +halfX
        for (int z = cz - 2; z <= cz + 2; z++) {
            setBlock(cx + halfX + 1, cy + 1, z, Material.FENCE);
            setBlock(cx + halfX + 1, cy + 2, z, Material.FENCE);
            setBlock(cx + halfX + 1, cy + 3, z, Material.WOOL);
        }
    }

    @SuppressWarnings("deprecation")
    private void buildNetherRuins(int cx, int cy, int cz) {
        int half = 17;
        // Nether brick floor
        for (int x = cx - half; x <= cx + half; x++) {
            for (int z = cz - half; z <= cz + half; z++) {
                setBlock(x, cy, z, Material.NETHER_BRICK);
            }
        }

        // Soul sand patches
        int[][] soulPatches = {
            {cx - 8, cz - 5}, {cx + 6, cz + 7}, {cx - 3, cz + 10},
            {cx + 10, cz - 8}, {cx + 2, cz - 12}
        };
        for (int[] patch : soulPatches) {
            for (int x = patch[0] - 1; x <= patch[0] + 1; x++) {
                for (int z = patch[1] - 1; z <= patch[1] + 1; z++) {
                    setBlock(x, cy, z, Material.SOUL_SAND);
                }
            }
        }

        // Broken walls (nether brick, 2-4 blocks high with gaps)
        buildNetherWall(cx - 6, cy, cz - 6, cx - 6, cy, cz + 2, 3);
        buildNetherWall(cx + 4, cy, cz - 3, cx + 4, cy, cz + 8, 4);
        buildNetherWall(cx - 2, cy, cz + 8, cx + 5, cy, cz + 8, 2);
        buildNetherWall(cx - 10, cy, cz - 12, cx - 4, cy, cz - 12, 3);

        // Glowstone light sources
        int[][] lights = {
            {cx - 10, cz - 10}, {cx + 10, cz - 10},
            {cx - 10, cz + 10}, {cx + 10, cz + 10},
            {cx, cz}
        };
        for (int[] light : lights) {
            setBlock(light[0], cy + 1, light[1], Material.GLOWSTONE);
        }

        // Lava pools with glass covers
        int[][] lavaPools = {
            {cx - 5, cz + 3}, {cx + 7, cz - 4}
        };
        for (int[] pool : lavaPools) {
            setBlock(pool[0], cy, pool[1], Material.LAVA);
            setBlock(pool[0], cy + 1, pool[1], Material.GLASS);
        }
    }

    private void buildNetherWall(int x1, int baseY, int z1, int x2, int baseYIgnored, int z2, int maxHeight) {
        if (x1 == x2) {
            // Wall along Z
            int minZ = Math.min(z1, z2);
            int maxZ = Math.max(z1, z2);
            for (int z = minZ; z <= maxZ; z++) {
                // Add gaps every 3-4 blocks
                if ((z - minZ) % 4 == 3) continue;
                int h = 2 + random.nextInt(Math.max(1, maxHeight - 1));
                for (int y = baseY + 1; y <= baseY + h; y++) {
                    setBlock(x1, y, z, Material.NETHER_BRICK);
                }
            }
        } else {
            // Wall along X
            int minX = Math.min(x1, x2);
            int maxX = Math.max(x1, x2);
            for (int x = minX; x <= maxX; x++) {
                if ((x - minX) % 4 == 3) continue;
                int h = 2 + random.nextInt(Math.max(1, maxHeight - 1));
                for (int y = baseY + 1; y <= baseY + h; y++) {
                    setBlock(x, y, z1, Material.NETHER_BRICK);
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void buildAbandonedCity(int cx, int cy, int cz) {
        int half = 20;

        // Stone/cobblestone mixed floor
        for (int x = cx - half; x <= cx + half; x++) {
            for (int z = cz - half; z <= cz + half; z++) {
                if (random.nextBoolean()) {
                    setBlock(x, cy, z, Material.STONE);
                } else {
                    setBlock(x, cy, z, Material.COBBLESTONE);
                }
            }
        }

        // Two main crossing paths (smooth stone)
        for (int x = cx - half; x <= cx + half; x++) {
            for (int z = cz - 1; z <= cz + 1; z++) {
                setBlock(x, cy, z, Material.STEP);
            }
        }
        for (int z = cz - half; z <= cz + half; z++) {
            for (int x = cx - 1; x <= cx + 1; x++) {
                setBlock(x, cy, z, Material.STEP);
            }
        }

        // Small buildings in each quadrant (3x3x4 with door openings)
        int[][] buildings = {
            {cx - 10, cz - 10}, {cx + 8, cz - 10},
            {cx - 10, cz + 8}, {cx + 8, cz + 8},
            {cx - 5, cz + 5}, {cx + 5, cz - 5},
            {cx - 14, cz}, {cx + 14, cz}
        };
        for (int[] b : buildings) {
            buildSmallBuilding(b[0], cy, b[1]);
        }

        // Broken walls scattered around
        buildBrokenWall(cx - 7, cy, cz - 14, 6, true);
        buildBrokenWall(cx + 3, cy, cz + 14, 5, true);
        buildBrokenWall(cx + 15, cy, cz - 5, 4, false);
        buildBrokenWall(cx - 15, cy, cz + 4, 5, false);

        // Stairs at a few points
        setBlock(cx - 3, cy + 1, cz - 3, Material.COBBLESTONE);
        setBlock(cx + 3, cy + 1, cz + 3, Material.COBBLESTONE);
    }

    private void buildSmallBuilding(int bx, int baseY, int bz) {
        // 3x3x4 building with door opening on one side
        for (int x = bx; x <= bx + 2; x++) {
            for (int z = bz; z <= bz + 2; z++) {
                for (int y = baseY + 1; y <= baseY + 4; y++) {
                    // Walls only (not interior)
                    if (x == bx || x == bx + 2 || z == bz || z == bz + 2) {
                        setBlock(x, y, z, Material.COBBLESTONE);
                    }
                }
                // Roof
                setBlock(x, baseY + 5, z, Material.COBBLESTONE);
            }
        }
        // Door opening (2 high)
        setBlock(bx + 1, baseY + 1, bz, Material.AIR);
        setBlock(bx + 1, baseY + 2, bz, Material.AIR);
    }

    private void buildBrokenWall(int startX, int baseY, int startZ, int length, boolean alongX) {
        for (int i = 0; i < length; i++) {
            int h = 1 + random.nextInt(3);
            int x = alongX ? startX + i : startX;
            int z = alongX ? startZ : startZ + i;
            for (int y = baseY + 1; y <= baseY + h; y++) {
                setBlock(x, y, z, Material.COBBLESTONE);
            }
        }
    }

    // =========================================================================
    //  Block helpers
    // =========================================================================

    private void setBlock(int x, int y, int z, Material material) {
        World w = duelWorld != null ? duelWorld : arenaWorld;
        if (w == null) return;
        Block block = w.getBlockAt(x, y, z);
        block.setType(material);
    }

    @SuppressWarnings("deprecation")
    private void setBlockWithData(int x, int y, int z, Material material, byte data) {
        World w = duelWorld != null ? duelWorld : arenaWorld;
        if (w == null) return;
        Block block = w.getBlockAt(x, y, z);
        block.setType(material);
        block.setData(data);
    }

    /**
     * Identity helper so center-line loop compiles cleanly.
     */
    private static int x(int v) {
        return v;
    }
}
