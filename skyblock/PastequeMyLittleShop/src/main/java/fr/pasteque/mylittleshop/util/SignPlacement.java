package fr.pasteque.mylittleshop.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;

public class SignPlacement {
    private final Location location;
    private final Material material;
    private final BlockFace wallFacing;

    public SignPlacement(Location location, Material material, BlockFace wallFacing) {
        this.location = location;
        this.material = material;
        this.wallFacing = wallFacing;
    }

    public Location getLocation() { return location; }
    public Material getMaterial() { return material; }
    public BlockFace getWallFacing() { return wallFacing; }
}
