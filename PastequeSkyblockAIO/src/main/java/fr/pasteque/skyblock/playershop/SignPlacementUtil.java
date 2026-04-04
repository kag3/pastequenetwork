package fr.pasteque.skyblock.playershop;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public final class SignPlacementUtil {
    private SignPlacementUtil() {}

    public static SignPlacement findPlacement(Player player, Block target) {
        if (target == null || target.getType() == Material.AIR) return null;

        Block above = target.getRelative(BlockFace.UP);
        if (above.getType() == Material.AIR) {
            return new SignPlacement(above.getLocation(), Material.SIGN_POST, BlockFace.SELF);
        }

        BlockFace face = facingFromYaw(player.getLocation());
        Block side = target.getRelative(face);
        if (side.getType() == Material.AIR) {
            return new SignPlacement(side.getLocation(), Material.WALL_SIGN, face.getOppositeFace());
        }

        for (BlockFace candidate : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Block relative = target.getRelative(candidate);
            if (relative.getType() == Material.AIR) {
                return new SignPlacement(relative.getLocation(), Material.WALL_SIGN, candidate.getOppositeFace());
            }
        }
        return null;
    }

    private static BlockFace facingFromYaw(Location location) {
        float yaw = location.getYaw();
        yaw = (yaw % 360 + 360) % 360;
        if (yaw >= 45 && yaw < 135) return BlockFace.WEST;
        if (yaw >= 135 && yaw < 225) return BlockFace.NORTH;
        if (yaw >= 225 && yaw < 315) return BlockFace.EAST;
        return BlockFace.SOUTH;
    }

    public static class SignPlacement {
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
}
