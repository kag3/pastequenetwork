package fr.pasteque.mylittleshop.model;

import java.util.UUID;

public class IslandArea {
    private final UUID owner;
    private final String worldName;
    private final int centerX;
    private final int centerZ;

    public IslandArea(UUID owner, String worldName, int centerX, int centerZ) {
        this.owner = owner;
        this.worldName = worldName;
        this.centerX = centerX;
        this.centerZ = centerZ;
    }

    public UUID getOwner() { return owner; }
    public String getWorldName() { return worldName; }
    public int getCenterX() { return centerX; }
    public int getCenterZ() { return centerZ; }
}
