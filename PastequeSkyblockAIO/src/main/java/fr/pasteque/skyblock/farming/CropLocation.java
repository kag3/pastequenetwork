package fr.pasteque.skyblock.farming;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public class CropLocation {

    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final CustomCrop type;
    private int growthStage;
    private final long plantedAt;

    public CropLocation(String world, int x, int y, int z, CustomCrop type, int growthStage, long plantedAt) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
        this.growthStage = growthStage;
        this.plantedAt = plantedAt;
    }

    public CropLocation(Location loc, CustomCrop type) {
        this(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                type, 0, System.currentTimeMillis());
    }

    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public CustomCrop getType() {
        return type;
    }

    public int getGrowthStage() {
        return growthStage;
    }

    public void setGrowthStage(int growthStage) {
        this.growthStage = growthStage;
    }

    public long getPlantedAt() {
        return plantedAt;
    }

    public boolean isFullyGrown() {
        return growthStage >= type.getMaxGrowthStage();
    }

    /**
     * Cle unique pour le stockage dans une map.
     */
    public String getKey() {
        return toKey(world, x, y, z);
    }

    public static String toKey(String world, int x, int y, int z) {
        return world + ":" + x + ":" + y + ":" + z;
    }

    public static String toKey(Location loc) {
        return toKey(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public Location toBukkitLocation() {
        World w = Bukkit.getWorld(world);
        if (w == null) {
            return null;
        }
        return new Location(w, x, y, z);
    }

    // ── Serialisation YAML ──────────────────────────────────────────────

    public void saveTo(ConfigurationSection section) {
        section.set("world", world);
        section.set("x", x);
        section.set("y", y);
        section.set("z", z);
        section.set("type", type.name());
        section.set("growthStage", growthStage);
        section.set("plantedAt", plantedAt);
    }

    public static CropLocation loadFrom(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String world = section.getString("world");
        int x = section.getInt("x");
        int y = section.getInt("y");
        int z = section.getInt("z");
        String typeName = section.getString("type");
        int growthStage = section.getInt("growthStage", 0);
        long plantedAt = section.getLong("plantedAt", System.currentTimeMillis());

        CustomCrop type;
        try {
            type = CustomCrop.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return null;
        }

        return new CropLocation(world, x, y, z, type, growthStage, plantedAt);
    }
}
