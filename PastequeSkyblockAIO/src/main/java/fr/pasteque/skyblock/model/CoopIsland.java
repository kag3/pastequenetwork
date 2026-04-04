package fr.pasteque.skyblock.model;

import org.bukkit.Location;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class CoopIsland {
    private final String id;
    private final UUID owner;
    private final int centerX;
    private final int centerZ;
    private final int size;
    private final Set<UUID> members = new LinkedHashSet<UUID>();
    private Location home;
    private String name;
    private boolean miningUnlocked;
    private Location miningHome;

    public CoopIsland(String id, UUID owner, int centerX, int centerZ, int size) {
        this.id = id;
        this.owner = owner;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.size = size;
    }

    public String getId() { return id; }
    public UUID getOwner() { return owner; }
    public int getCenterX() { return centerX; }
    public int getCenterZ() { return centerZ; }
    public int getSize() { return size; }
    public Set<UUID> getMembers() { return members; }
    public Location getHome() { return home; }
    public void setHome(Location home) { this.home = home; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isMiningUnlocked() { return miningUnlocked; }
    public void setMiningUnlocked(boolean miningUnlocked) { this.miningUnlocked = miningUnlocked; }
    public Location getMiningHome() { return miningHome; }
    public void setMiningHome(Location miningHome) { this.miningHome = miningHome; }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public boolean canBuild(UUID uuid) {
        return members.contains(uuid);
    }

    public boolean isInside(Location location) {
        int half = size / 2;
        int x = location.getBlockX();
        int z = location.getBlockZ();
        return x >= centerX - half && x <= centerX + half - 1 && z >= centerZ - half && z <= centerZ + half - 1;
    }
}
