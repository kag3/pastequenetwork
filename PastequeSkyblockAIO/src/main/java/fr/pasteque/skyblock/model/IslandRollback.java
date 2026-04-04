package fr.pasteque.skyblock.model;

import org.bukkit.Location;

import java.util.*;

public class IslandRollback {
    private final UUID owner;
    private final boolean deletedIsland;
    private final int gridX;
    private final int gridZ;
    private final int centerX;
    private final int centerZ;
    private final boolean publicVisit;
    private final boolean pvpEnabled;
    private final double bankBalance;
    private final String name;
    private final int inviteUnlocks;
    private final Location home;
    private final Set<UUID> members;
    private final Set<UUID> trusted;
    private final Set<UUID> banned;
    private final List<BlockSnapshot> blocks;

    public IslandRollback(Island island, boolean deletedIsland, List<BlockSnapshot> blocks) {
        this.owner = island.getOwner();
        this.deletedIsland = deletedIsland;
        this.gridX = island.getGridX();
        this.gridZ = island.getGridZ();
        this.centerX = island.getCenterX();
        this.centerZ = island.getCenterZ();
        this.publicVisit = island.isPublicVisit();
        this.pvpEnabled = island.isPvpEnabled();
        this.bankBalance = island.getBankBalance();
        this.name = island.getName();
        this.inviteUnlocks = island.getInviteUnlocks();
        this.home = island.getHome() == null ? null : island.getHome().clone();
        this.members = new HashSet<UUID>(island.getMembers());
        this.trusted = new HashSet<UUID>(island.getTrusted());
        this.banned = new HashSet<UUID>(island.getBanned());
        this.blocks = blocks;
    }

    public UUID getOwner() { return owner; }
    public boolean isDeletedIsland() { return deletedIsland; }
    public int getGridX() { return gridX; }
    public int getGridZ() { return gridZ; }
    public int getCenterX() { return centerX; }
    public int getCenterZ() { return centerZ; }
    public boolean isPublicVisit() { return publicVisit; }
    public boolean isPvpEnabled() { return pvpEnabled; }
    public double getBankBalance() { return bankBalance; }
    public String getName() { return name; }
    public int getInviteUnlocks() { return inviteUnlocks; }
    public Location getHome() { return home == null ? null : home.clone(); }
    public Set<UUID> getMembers() { return new HashSet<UUID>(members); }
    public Set<UUID> getTrusted() { return new HashSet<UUID>(trusted); }
    public Set<UUID> getBanned() { return new HashSet<UUID>(banned); }
    public List<BlockSnapshot> getBlocks() { return blocks; }
}
