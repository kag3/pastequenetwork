package fr.pasteque.skyblockarena.model;

import org.bukkit.Location;
import org.bukkit.World;

public class SafeZone {

    private final String name;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    public SafeZone(String name, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.name = name;
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
    }

    public String getName() { return name; }
    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }

    public boolean contains(Location location) {
        return location != null
                && location.getBlockX() >= minX && location.getBlockX() <= maxX
                && location.getBlockY() >= minY && location.getBlockY() <= maxY
                && location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ;
    }

    public Location getCenter(World world) {
        return new Location(world,
                (minX + maxX) / 2.0D + 0.5D,
                maxY + 1.0D,
                (minZ + maxZ) / 2.0D + 0.5D);
    }
}
