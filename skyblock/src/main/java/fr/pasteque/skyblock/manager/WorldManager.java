package fr.pasteque.skyblock.manager;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.generator.EmptySkyblockGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

public class WorldManager {
    private final PastequeSkyblockPlugin plugin;
    private final String spawnWorldName;
    private final String islandWorldName;
    private final String coopWorldName;
    private final String endWorldName;
    private final boolean useWorldSpawn;
    private final int fallbackSpawnY;
    private final int islandY;
    private final int coopY;
    private final EmptySkyblockGenerator generator = new EmptySkyblockGenerator();

    public WorldManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.spawnWorldName = plugin.getConfig().getString("spawn-world-name", "world");
        this.islandWorldName = plugin.getConfig().getString("island-world-name", "pasteque_skyblock_islands");
        this.coopWorldName = plugin.getConfig().getString("coop-world-name", "pasteque_skyblock_coop");
        this.endWorldName = plugin.getConfig().getString("end-event.world-name", spawnWorldName + "_the_end");
        this.useWorldSpawn = plugin.getConfig().getBoolean("spawn-world-use-world-spawn", true);
        this.fallbackSpawnY = plugin.getConfig().getInt("spawn-world-fallback-y", 80);
        this.islandY = plugin.getConfig().getInt("island-world-y", 100);
        this.coopY = plugin.getConfig().getInt("coop-world-y", 100);
    }

    public World getOrCreateIslandWorld() {
        return getOrCreateVoidWorld(islandWorldName, islandY);
    }

    public World getOrCreateCoopWorld() {
        return getOrCreateVoidWorld(coopWorldName, coopY);
    }

    private World getOrCreateVoidWorld(String name, int y) {
        World world = Bukkit.getWorld(name);
        if (world != null) return world;
        WorldCreator creator = new WorldCreator(name);
        creator.type(WorldType.FLAT);
        creator.environment(World.Environment.NORMAL);
        creator.generateStructures(false);
        creator.generator(generator);
        world = creator.createWorld();
        if (world != null) {
            world.setSpawnLocation(0, y, 0);
            world.setGameRuleValue("doMobSpawning", "false");
            world.setGameRuleValue("doFireTick", "false");
            world.setGameRuleValue("mobGriefing", "false");
            world.setGameRuleValue("keepInventory", "true");
            world.setGameRuleValue("naturalRegeneration", "true");
            world.setStorm(false);
            world.setThundering(false);
        }
        return world;
    }

    public World getSpawnWorld() {
        World world = Bukkit.getWorld(spawnWorldName);
        if (world != null) return world;
        if (!Bukkit.getWorlds().isEmpty()) return Bukkit.getWorlds().get(0);
        return getOrCreateIslandWorld();
    }

    public World getEndEventWorld() {
        World world = Bukkit.getWorld(endWorldName);
        if (world != null) return world;
        for (World test : Bukkit.getWorlds()) {
            if (test.getEnvironment() == World.Environment.THE_END) return test;
        }
        WorldCreator creator = new WorldCreator(endWorldName);
        creator.environment(World.Environment.THE_END);
        creator.generateStructures(true);
        return creator.createWorld();
    }

    public Location getServerSpawn() {
        World world = getSpawnWorld();
        if (world == null) world = getOrCreateIslandWorld();
        if (useWorldSpawn && world != null) {
            Location spawn = world.getSpawnLocation();
            if (spawn != null) return spawn.clone().add(0.5D, 0.0D, 0.5D);
        }
        return new Location(world, 0.5D, fallbackSpawnY, 0.5D);
    }

    public String getIslandWorldName() { return islandWorldName; }
    public String getCoopWorldName() { return coopWorldName; }
    public String getSpawnWorldName() { return spawnWorldName; }
    public String getEndWorldName() { return endWorldName; }
}
