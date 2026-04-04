package fr.pasteque.skyblock.arena;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.generator.EmptyChunkGenerator;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArenaWorldService {

    private final PastequeSkyblockPlugin plugin;
    private final Map<UUID, GameMode> previousGamemodes = new HashMap<UUID, GameMode>();
    private World arenaWorld;

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
}
