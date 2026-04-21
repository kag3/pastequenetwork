package fr.pastequeworld.bedwars.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Lecture / ecriture de positions dans la config YAML.
 * Format :
 *   world: maWorld (optionnel, peut etre injecte)
 *   x: 0.5
 *   y: 100
 *   z: 0.5
 *   yaw: 0
 *   pitch: 0
 */
public final class LocationSerializer {

    private LocationSerializer() {}

    public static Location read(ConfigurationSection section, World defaultWorld) {
        if (section == null) return null;
        World world = defaultWorld;
        String name = section.getString("world");
        if (name != null) {
            World w = Bukkit.getWorld(name);
            if (w != null) world = w;
        }
        if (world == null) return null;
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw", 0);
        float pitch = (float) section.getDouble("pitch", 0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    public static void write(ConfigurationSection section, Location loc) {
        if (section == null || loc == null) return;
        if (loc.getWorld() != null) section.set("world", loc.getWorld().getName());
        section.set("x", loc.getX());
        section.set("y", loc.getY());
        section.set("z", loc.getZ());
        section.set("yaw", loc.getYaw());
        section.set("pitch", loc.getPitch());
    }

    public static Location offset(Location base, double dx, double dy, double dz) {
        return new Location(base.getWorld(), base.getX() + dx, base.getY() + dy, base.getZ() + dz, base.getYaw(), base.getPitch());
    }
}
