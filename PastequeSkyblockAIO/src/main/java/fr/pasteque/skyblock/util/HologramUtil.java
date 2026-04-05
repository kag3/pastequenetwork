package fr.pasteque.skyblock.util;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.lang.reflect.Method;

/**
 * Helper reflection-based pour ArmorStand (non present dans le stub Spigot-API).
 */
public final class HologramUtil {

    private HologramUtil() {}

    public static Entity spawnLine(Location loc, String coloredName, String tagSuffix) {
        try {
            EntityType type = EntityType.valueOf("ARMOR_STAND");
            Entity e = loc.getWorld().spawnEntity(loc, type);
            if (e instanceof LivingEntity) {
                LivingEntity le = (LivingEntity) e;
                le.setCustomName(PastequeSkyblockPlugin.color(coloredName) + tagSuffix);
                le.setCustomNameVisible(true);
            }
            invokeVoid(e, "setVisible", new Class<?>[] { boolean.class }, new Object[] { false });
            invokeVoid(e, "setGravity", new Class<?>[] { boolean.class }, new Object[] { false });
            invokeVoid(e, "setMarker", new Class<?>[] { boolean.class }, new Object[] { true });
            invokeVoid(e, "setSmall", new Class<?>[] { boolean.class }, new Object[] { true });
            invokeVoid(e, "setBasePlate", new Class<?>[] { boolean.class }, new Object[] { false });
            invokeVoid(e, "setArms", new Class<?>[] { boolean.class }, new Object[] { false });
            return e;
        } catch (Throwable t) {
            return null;
        }
    }

    private static void invokeVoid(Object target, String name, Class<?>[] params, Object[] args) {
        try {
            Method m = target.getClass().getMethod(name, params);
            m.invoke(target, args);
        } catch (Throwable ignored) {}
    }

    public static void removeLinesNear(Location base, String tagSuffix) {
        World w = base.getWorld();
        if (w == null) return;
        // Iterate world entities in the chunk area
        int cx = base.getBlockX() >> 4;
        int cz = base.getBlockZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!w.isChunkLoaded(cx + dx, cz + dz)) continue;
                for (Entity e : w.getChunkAt(cx + dx, cz + dz).getEntities()) {
                    if (!(e instanceof LivingEntity)) continue;
                    LivingEntity le = (LivingEntity) e;
                    if (le.getCustomName() == null || !le.getCustomName().contains(tagSuffix)) continue;
                    if (e.getLocation().distanceSquared(base) > 25) continue;
                    e.remove();
                }
            }
        }
    }
}
