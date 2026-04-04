package fr.pasteque.skyblock.manager;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.model.Island;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BorderManager {
    private final PastequeSkyblockPlugin plugin;
    private final Map<UUID, Long> cooldown = new HashMap<UUID, Long>();

    public BorderManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        final int period = Math.max(10, plugin.getConfig().getInt("visual-border.refresh-ticks", 20));
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (!player.getWorld().getName().equalsIgnoreCase(plugin.getWorldManager().getIslandWorldName())) {
                        continue;
                    }
                    renderFor(player);
                }
            }
        }.runTaskTimer(plugin, 40L, period);
    }

    public void renderFor(Player player) {
        long now = System.currentTimeMillis();
        Long last = cooldown.get(player.getUniqueId());
        if (last != null && now - last < 700L) {
            return;
        }

        Island island = plugin.getIslandManager().getIslandAt(player.getLocation());
        if (island == null) {
            return;
        }
        int size = plugin.getConfig().getInt("island.size", 320);
        int half = size / 2;
        int warnDistance = plugin.getConfig().getInt("visual-border.show-distance", 14);
        int px = player.getLocation().getBlockX();
        int pz = player.getLocation().getBlockZ();
        int minX = island.getCenterX() - half;
        int maxX = island.getCenterX() + half - 1;
        int minZ = island.getCenterZ() - half;
        int maxZ = island.getCenterZ() + half - 1;

        boolean nearEdge = px - minX <= warnDistance || maxX - px <= warnDistance || pz - minZ <= warnDistance || maxZ - pz <= warnDistance;
        if (!nearEdge) {
            return;
        }
        cooldown.put(player.getUniqueId(), now);
        World world = player.getWorld();
        double y = Math.max(player.getLocation().getY() + 0.2D, plugin.getConfig().getInt("island-world-y", 100) + 1);

        for (int x = minX; x <= maxX; x += 4) {
            world.playEffect(new Location(world, x + 0.5D, y, minZ + 0.5D), Effect.HAPPY_VILLAGER, 0);
            world.playEffect(new Location(world, x + 0.5D, y, maxZ + 0.5D), Effect.HAPPY_VILLAGER, 0);
        }
        for (int z = minZ; z <= maxZ; z += 4) {
            world.playEffect(new Location(world, minX + 0.5D, y, z + 0.5D), Effect.HAPPY_VILLAGER, 0);
            world.playEffect(new Location(world, maxX + 0.5D, y, z + 0.5D), Effect.HAPPY_VILLAGER, 0);
        }
    }
}
