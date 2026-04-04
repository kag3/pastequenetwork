package fr.pasteque.skyblock.model;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public class PvpZone {
    private final String name;
    private final String worldName;
    private int minX;
    private int minY;
    private int minZ;
    private int maxX;
    private int maxY;
    private int maxZ;

    public PvpZone(String name, String worldName, Location a, Location b) {
        this.name = name;
        this.worldName = worldName;
        this.minX = Math.min(a.getBlockX(), b.getBlockX());
        this.minY = Math.min(a.getBlockY(), b.getBlockY());
        this.minZ = Math.min(a.getBlockZ(), b.getBlockZ());
        this.maxX = Math.max(a.getBlockX(), b.getBlockX());
        this.maxY = Math.max(a.getBlockY(), b.getBlockY());
        this.maxZ = Math.max(a.getBlockZ(), b.getBlockZ());
    }

    public String getName() {
        return name;
    }

    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (!location.getWorld().getName().equalsIgnoreCase(worldName)) {
            return false;
        }
        return location.getBlockX() >= minX && location.getBlockX() <= maxX
                && location.getBlockY() >= minY && location.getBlockY() <= maxY
                && location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ;
    }

    public void save(ConfigurationSection section) {
        section.set("world", worldName);
        section.set("minX", minX);
        section.set("minY", minY);
        section.set("minZ", minZ);
        section.set("maxX", maxX);
        section.set("maxY", maxY);
        section.set("maxZ", maxZ);
    }

    public static PvpZone load(String name, ConfigurationSection section) {
        World world = org.bukkit.Bukkit.getWorld(section.getString("world"));
        if (world == null) {
            return null;
        }
        Location a = new Location(world, section.getInt("minX"), section.getInt("minY"), section.getInt("minZ"));
        Location b = new Location(world, section.getInt("maxX"), section.getInt("maxY"), section.getInt("maxZ"));
        return new PvpZone(name, world.getName(), a, b);
    }
}
