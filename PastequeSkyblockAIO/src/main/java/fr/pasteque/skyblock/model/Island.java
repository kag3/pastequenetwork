package fr.pasteque.skyblock.model;

import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Island {
    private final UUID owner;
    private final int gridX;
    private final int gridZ;
    private final int centerX;
    private final int centerZ;
    private Location home;
    private boolean publicVisit;
    private boolean pvpEnabled;
    private double bankBalance;
    private String name;
    private int inviteUnlocks;
    private boolean farmingUnlocked;
    private boolean miningUnlocked;
    private Location farmingHome;
    private Location miningHome;
    private final Set<UUID> members = new HashSet<UUID>();
    private final Set<UUID> trusted = new HashSet<UUID>();
    private final Set<UUID> banned = new HashSet<UUID>();

    public Island(UUID owner, int gridX, int gridZ, int centerX, int centerZ) {
        this.owner = owner;
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.centerX = centerX;
        this.centerZ = centerZ;
    }

    public UUID getOwner() { return owner; }
    public int getGridX() { return gridX; }
    public int getGridZ() { return gridZ; }
    public int getCenterX() { return centerX; }
    public int getCenterZ() { return centerZ; }
    public Location getHome() { return home; }
    public void setHome(Location home) { this.home = home; }
    public boolean isPublicVisit() { return publicVisit; }
    public void setPublicVisit(boolean publicVisit) { this.publicVisit = publicVisit; }
    public boolean isPvpEnabled() { return pvpEnabled; }
    public void setPvpEnabled(boolean pvpEnabled) { this.pvpEnabled = pvpEnabled; }
    public double getBankBalance() { return bankBalance; }
    public void setBankBalance(double bankBalance) { this.bankBalance = bankBalance; }
    public Set<UUID> getMembers() { return members; }
    public Set<UUID> getTrusted() { return trusted; }
    public Set<UUID> getBanned() { return banned; }
    public boolean isOwner(UUID uuid) { return owner.equals(uuid); }
    public boolean isMember(UUID uuid) { return owner.equals(uuid) || members.contains(uuid); }
    public boolean canBuild(UUID uuid) { return owner.equals(uuid) || members.contains(uuid) || trusted.contains(uuid); }
    public boolean canVisit(UUID uuid) { return canBuild(uuid) || (publicVisit && !banned.contains(uuid)); }
    public boolean isInside(Location location, int size) {
        int half = size / 2;
        int x = location.getBlockX();
        int z = location.getBlockZ();
        return x >= centerX - half && x <= centerX + half - 1 && z >= centerZ - half && z <= centerZ + half - 1;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getInviteUnlocks() { return inviteUnlocks; }
    public void setInviteUnlocks(int inviteUnlocks) { this.inviteUnlocks = Math.max(0, inviteUnlocks); }
    public void addInviteUnlock() { this.inviteUnlocks++; }
    public boolean consumeInviteUnlock() {
        if (inviteUnlocks <= 0) return false;
        inviteUnlocks--;
        return true;
    }

    public boolean isFarmingUnlocked() { return farmingUnlocked; }
    public void setFarmingUnlocked(boolean farmingUnlocked) { this.farmingUnlocked = farmingUnlocked; }
    public boolean isMiningUnlocked() { return miningUnlocked; }
    public void setMiningUnlocked(boolean miningUnlocked) { this.miningUnlocked = miningUnlocked; }
    public Location getFarmingHome() { return farmingHome; }
    public void setFarmingHome(Location farmingHome) { this.farmingHome = farmingHome; }
    public Location getMiningHome() { return miningHome; }
    public void setMiningHome(Location miningHome) { this.miningHome = miningHome; }

    public String getDisplayName(String ownerName) {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        String base = ownerName == null || ownerName.trim().isEmpty() ? "Player" : ownerName.trim();
        return base + "'s Island";
    }
}
