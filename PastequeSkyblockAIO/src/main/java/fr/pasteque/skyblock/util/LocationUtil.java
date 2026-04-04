package fr.pasteque.skyblock.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public final class LocationUtil {
    private LocationUtil() {
    }

    public static void save(ConfigurationSection section, String path, Location location) {
        if (location == null) {
            section.set(path, null);
            return;
        }
        section.set(path + ".world", location.getWorld().getName());
        section.set(path + ".x", location.getX());
        section.set(path + ".y", location.getY());
        section.set(path + ".z", location.getZ());
        section.set(path + ".yaw", location.getYaw());
        section.set(path + ".pitch", location.getPitch());
    }

    public static Location load(ConfigurationSection section, String path) {
        if (!section.isConfigurationSection(path)) {
            return null;
        }
        String worldName = section.getString(path + ".world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(
                world,
                section.getDouble(path + ".x"),
                section.getDouble(path + ".y"),
                section.getDouble(path + ".z"),
                (float) section.getDouble(path + ".yaw"),
                (float) section.getDouble(path + ".pitch")
        );
    }
}
