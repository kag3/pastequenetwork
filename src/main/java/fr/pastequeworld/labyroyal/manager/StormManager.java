package fr.pastequeworld.labyroyal.manager;

import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;

import java.util.List;

public class StormManager {

    private final World world;
    private final double initialSize;
    private final double finalSize;
    private boolean active;

    public StormManager(World world, double mazeBlockSize, double centerDiameter) {
        this.world = world;
        this.initialSize = mazeBlockSize + 10;
        this.finalSize = Math.max(centerDiameter, 10);
        this.active = false;
    }

    public void startShrinking(int durationSeconds) {
        active = true;
        WorldBorder border = world.getWorldBorder();
        border.setCenter(0, 0);

        // Shrink to the center arena size over the duration
        border.setSize(finalSize, durationSeconds);
    }

    public void applyDamage(List<Player> alivePlayers) {
        if (!active) return;

        WorldBorder border = world.getWorldBorder();
        double halfSize = border.getSize() / 2.0;
        double centerX = border.getCenter().getX();
        double centerZ = border.getCenter().getZ();

        for (Player player : alivePlayers) {
            double px = player.getLocation().getX();
            double pz = player.getLocation().getZ();

            double distX = Math.abs(px - centerX);
            double distZ = Math.abs(pz - centerZ);

            if (distX > halfSize || distZ > halfSize) {
                // Player is outside border - apply damage
                double overflow = Math.max(distX - halfSize, distZ - halfSize);
                double damage = Math.min(1.0 + overflow * 0.2, 6.0);
                player.damage(damage);
            }
        }
    }

    public boolean isActive() {
        return active;
    }

    public double getCurrentSize() {
        return world.getWorldBorder().getSize();
    }
}
