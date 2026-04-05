package fr.pasteque.skyblock.minion.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlacedMinion {

    private final UUID owner;
    private final MinionType type;
    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private int level;
    private final HashMap<String, Integer> storage;
    private long lastCollectTime;

    public PlacedMinion(UUID owner, MinionType type, Location location, int level) {
        this.owner = owner;
        this.type = type;
        this.world = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.level = level;
        this.storage = new HashMap<String, Integer>();
        this.lastCollectTime = System.currentTimeMillis();
    }

    public PlacedMinion(UUID owner, MinionType type, String world, double x, double y, double z,
                        int level, HashMap<String, Integer> storage, long lastCollectTime) {
        this.owner = owner;
        this.type = type;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.level = level;
        this.storage = storage;
        this.lastCollectTime = lastCollectTime;
    }

    public UUID getOwner() {
        return owner;
    }

    public MinionType getType() {
        return type;
    }

    public Location getLocation() {
        if (Bukkit.getWorld(world) == null) {
            return null;
        }
        return new Location(Bukkit.getWorld(world), x, y, z);
    }

    public String getWorld() {
        return world;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.min(5, Math.max(1, level));
    }

    public HashMap<String, Integer> getStorage() {
        return storage;
    }

    public int getStorageCount() {
        int total = 0;
        for (Integer count : storage.values()) {
            total += count;
        }
        return total;
    }

    public void addProduce() {
        if (getStorageCount() >= getMaxStorage()) {
            return;
        }
        String mat = type.getProduceMaterial();
        Integer current = storage.get(mat);
        storage.put(mat, (current == null ? 0 : current) + 1);
    }

    public void collectAll(Player player) {
        for (Map.Entry<String, Integer> entry : storage.entrySet()) {
            Material mat = Material.matchMaterial(entry.getKey());
            if (mat == null) {
                continue;
            }
            int amount = entry.getValue();
            while (amount > 0) {
                int stack = Math.min(amount, 64);
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(new ItemStack(mat, stack));
                if (!overflow.isEmpty()) {
                    for (ItemStack drop : overflow.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                }
                amount -= stack;
            }
        }
        storage.clear();
        lastCollectTime = System.currentTimeMillis();
    }

    public int getMaxStorage() {
        return level * 64;
    }

    public int getInterval() {
        return type.getIntervalSeconds() / level;
    }

    public long getLastCollectTime() {
        return lastCollectTime;
    }

    public void setLastCollectTime(long lastCollectTime) {
        this.lastCollectTime = lastCollectTime;
    }
}
