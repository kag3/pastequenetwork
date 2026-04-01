package fr.pastequeworld.labyroyal.arena;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class LobbyBuilder {

    private final int yLevel;
    private final int size;

    public LobbyBuilder(int yLevel, int size) {
        this.yLevel = yLevel;
        this.size = size;
    }

    public void build(World world) {
        int half = size / 2;

        // Floor - polished blackstone with pattern
        for (int x = -half; x <= half; x++) {
            for (int z = -half; z <= half; z++) {
                Material floor;
                if ((Math.abs(x) + Math.abs(z)) % 2 == 0) {
                    floor = Material.POLISHED_BLACKSTONE;
                } else {
                    floor = Material.POLISHED_BLACKSTONE_BRICKS;
                }
                world.getBlockAt(x, yLevel, z).setType(floor);

                // Bedrock under floor
                world.getBlockAt(x, yLevel - 1, z).setType(Material.BEDROCK);

                // Air inside
                for (int y = yLevel + 1; y <= yLevel + 5; y++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR);
                }

                // Glass ceiling
                world.getBlockAt(x, yLevel + 6, z).setType(Material.TINTED_GLASS);
            }
        }

        // Walls
        for (int y = yLevel + 1; y <= yLevel + 5; y++) {
            for (int x = -half; x <= half; x++) {
                placeWallBlock(world, x, y, -half);
                placeWallBlock(world, x, y, half);
            }
            for (int z = -half + 1; z < half; z++) {
                placeWallBlock(world, -half, y, z);
                placeWallBlock(world, half, y, z);
            }
        }

        // Corner pillars - gilded blackstone
        for (int y = yLevel + 1; y <= yLevel + 5; y++) {
            world.getBlockAt(-half, y, -half).setType(Material.GILDED_BLACKSTONE);
            world.getBlockAt(half, y, -half).setType(Material.GILDED_BLACKSTONE);
            world.getBlockAt(-half, y, half).setType(Material.GILDED_BLACKSTONE);
            world.getBlockAt(half, y, half).setType(Material.GILDED_BLACKSTONE);
        }

        // Lanterns for lighting
        for (int x = -half + 2; x <= half - 2; x += 4) {
            for (int z = -half + 2; z <= half - 2; z += 4) {
                world.getBlockAt(x, yLevel + 5, z).setType(Material.SOUL_LANTERN);
            }
        }

        // Central decoration - gold block pedestal
        world.getBlockAt(0, yLevel + 1, 0).setType(Material.GOLD_BLOCK);
        world.getBlockAt(0, yLevel + 2, 0).setType(Material.SOUL_LANTERN);

        // Barrier ceiling above glass to prevent escaping
        for (int x = -half - 1; x <= half + 1; x++) {
            for (int z = -half - 1; z <= half + 1; z++) {
                world.getBlockAt(x, yLevel + 7, z).setType(Material.BARRIER);
            }
        }
    }

    private void placeWallBlock(World world, int x, int y, int z) {
        Material mat;
        if (y == yLevel + 1 || y == yLevel + 5) {
            mat = Material.CHISELED_POLISHED_BLACKSTONE;
        } else if (y == yLevel + 3) {
            mat = Material.POLISHED_BLACKSTONE_BRICK_SLAB;
            world.getBlockAt(x, y, z).setType(Material.POLISHED_BLACKSTONE_BRICKS);
            return;
        } else {
            mat = Material.POLISHED_BLACKSTONE_BRICKS;
        }
        world.getBlockAt(x, y, z).setType(mat);
    }

    public Location getSpawnLocation(World world) {
        return new Location(world, 0.5, yLevel + 1, 0.5);
    }
}
