package fr.pastequeworld.labyroyal.arena;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

@SuppressWarnings("deprecation")
public class MazeGenerator {

    private final int cells;
    private final int wallHeight;
    private final int baseY;
    private final double oreChance;
    private final int specialRoomCount;
    private final int bonusChestCount;

    private boolean[][] grid;
    private int gridSize;
    private final Random random = new Random();
    private final List<int[]> deadEnds = new ArrayList<int[]>();
    private final List<int[]> specialRoomCells = new ArrayList<int[]>();

    private static final int CELL_BLOCK_SIZE = 3;

    public MazeGenerator(int cells, int wallHeight, int baseY, double oreChance, int specialRoomCount, int bonusChestCount) {
        this.cells = cells;
        this.wallHeight = wallHeight;
        this.baseY = baseY;
        this.oreChance = oreChance;
        this.specialRoomCount = specialRoomCount;
        this.bonusChestCount = bonusChestCount;
        this.gridSize = 2 * cells + 1;
    }

    public void generate(World world) {
        generateMazeGrid();
        findDeadEnds();
        selectSpecialRooms();
        placeBlocks(world);
        placeSpecialRooms(world);
        placeBonusChests(world);
    }

    private void generateMazeGrid() {
        grid = new boolean[gridSize][gridSize];
        boolean[][] visited = new boolean[cells][cells];

        Deque<int[]> stack = new ArrayDeque<int[]>();
        int startX = random.nextInt(cells);
        int startZ = random.nextInt(cells);

        visited[startX][startZ] = true;
        grid[2 * startX + 1][2 * startZ + 1] = true;
        stack.push(new int[]{startX, startZ});

        while (!stack.isEmpty()) {
            int[] current = stack.peek();
            List<int[]> neighbors = getUnvisitedNeighbors(current[0], current[1], visited);

            if (neighbors.isEmpty()) {
                stack.pop();
            } else {
                int[] next = neighbors.get(random.nextInt(neighbors.size()));
                int wallGX = 2 * current[0] + 1 + (next[0] - current[0]);
                int wallGZ = 2 * current[1] + 1 + (next[1] - current[1]);
                grid[wallGX][wallGZ] = true;
                grid[2 * next[0] + 1][2 * next[1] + 1] = true;
                visited[next[0]][next[1]] = true;
                stack.push(next);
            }
        }
    }

    private List<int[]> getUnvisitedNeighbors(int cx, int cz, boolean[][] visited) {
        List<int[]> neighbors = new ArrayList<int[]>();
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] d : dirs) {
            int nx = cx + d[0];
            int nz = cz + d[1];
            if (nx >= 0 && nx < cells && nz >= 0 && nz < cells && !visited[nx][nz]) {
                neighbors.add(new int[]{nx, nz});
            }
        }
        return neighbors;
    }

    private void findDeadEnds() {
        deadEnds.clear();
        for (int cx = 0; cx < cells; cx++) {
            for (int cz = 0; cz < cells; cz++) {
                int gx = 2 * cx + 1;
                int gz = 2 * cz + 1;
                int openSides = 0;
                if (gx > 0 && grid[gx - 1][gz]) openSides++;
                if (gx < gridSize - 1 && grid[gx + 1][gz]) openSides++;
                if (gz > 0 && grid[gx][gz - 1]) openSides++;
                if (gz < gridSize - 1 && grid[gx][gz + 1]) openSides++;
                if (openSides == 1) {
                    deadEnds.add(new int[]{cx, cz});
                }
            }
        }
    }

    private void selectSpecialRooms() {
        specialRoomCells.clear();
        List<int[]> candidates = new ArrayList<int[]>(deadEnds);
        Collections.shuffle(candidates, random);
        int count = Math.min(specialRoomCount, candidates.size());
        for (int i = 0; i < count; i++) {
            specialRoomCells.add(candidates.get(i));
        }
    }

    private int getOffset() {
        return -(gridSize * CELL_BLOCK_SIZE) / 2;
    }

    private void placeBlocks(World world) {
        int offset = getOffset();

        for (int gx = 0; gx < gridSize; gx++) {
            for (int gz = 0; gz < gridSize; gz++) {
                int blockStartX = gx * CELL_BLOCK_SIZE + offset;
                int blockStartZ = gz * CELL_BLOCK_SIZE + offset;

                if (grid[gx][gz]) {
                    placePassage(world, blockStartX, blockStartZ);
                } else {
                    placeWall(world, blockStartX, blockStartZ, gx, gz);
                }
            }
        }
    }

    private void placePassage(World world, int startX, int startZ) {
        for (int dx = 0; dx < CELL_BLOCK_SIZE; dx++) {
            for (int dz = 0; dz < CELL_BLOCK_SIZE; dz++) {
                int x = startX + dx;
                int z = startZ + dz;

                // Floor
                world.getBlockAt(x, baseY, z).setType(getPassageFloorBlock());

                // Air above
                for (int y = baseY + 1; y < baseY + wallHeight - 1; y++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR);
                }

                // Ceiling
                world.getBlockAt(x, baseY + wallHeight - 1, z).setType(Material.BEDROCK);

                // Bedrock under floor
                world.getBlockAt(x, baseY - 1, z).setType(Material.BEDROCK);
            }
        }
    }

    private void placeWall(World world, int startX, int startZ, int gx, int gz) {
        boolean isOuterWall = (gx == 0 || gx == gridSize - 1 || gz == 0 || gz == gridSize - 1);

        for (int dx = 0; dx < CELL_BLOCK_SIZE; dx++) {
            for (int dz = 0; dz < CELL_BLOCK_SIZE; dz++) {
                int x = startX + dx;
                int z = startZ + dz;

                boolean isCenter = (dx == CELL_BLOCK_SIZE / 2 && dz == CELL_BLOCK_SIZE / 2);
                boolean isCross = (dx == CELL_BLOCK_SIZE / 2 || dz == CELL_BLOCK_SIZE / 2);

                for (int y = baseY - 1; y <= baseY + wallHeight - 1; y++) {
                    if (isOuterWall || isCenter || (y == baseY - 1) || (y == baseY + wallHeight - 1)) {
                        world.getBlockAt(x, y, z).setType(Material.BEDROCK);
                    } else if (isCross) {
                        world.getBlockAt(x, y, z).setType(Material.OBSIDIAN);
                    } else {
                        world.getBlockAt(x, y, z).setType(getWallBlock());
                    }
                }
            }
        }
    }

    private Material getWallBlock() {
        double roll = random.nextDouble();
        if (roll < oreChance) {
            return getRandomOre();
        }
        // Dark wall blocks for 1.9.4
        double baseRoll = random.nextDouble();
        if (baseRoll < 0.30) return Material.SMOOTH_BRICK;     // Stone bricks
        if (baseRoll < 0.50) return Material.NETHER_BRICK;     // Nether bricks (dark)
        if (baseRoll < 0.65) return Material.STONE;             // Stone
        if (baseRoll < 0.78) return Material.COAL_BLOCK;        // Coal block (very dark)
        if (baseRoll < 0.88) return Material.COBBLESTONE;       // Cobblestone
        if (baseRoll < 0.94) return Material.ENDER_STONE;       // End stone
        return Material.MOSSY_COBBLESTONE;                       // Mossy cobblestone
    }

    private Material getRandomOre() {
        double roll = random.nextDouble();
        if (roll < 0.25) return Material.COAL_ORE;
        if (roll < 0.48) return Material.IRON_ORE;
        if (roll < 0.63) return Material.LOG;                   // Oak log (wood)
        if (roll < 0.75) return Material.GOLD_ORE;
        if (roll < 0.83) return Material.LAPIS_ORE;
        if (roll < 0.89) return Material.COBBLESTONE;
        if (roll < 0.94) return Material.REDSTONE_ORE;
        if (roll < 0.98) return Material.DIAMOND_ORE;
        return Material.EMERALD_ORE;
    }

    private Material getPassageFloorBlock() {
        double roll = random.nextDouble();
        if (roll < 0.05) {
            return getRandomOre();
        }
        if (roll < 0.40) return Material.SMOOTH_BRICK;
        if (roll < 0.65) return Material.STONE;
        if (roll < 0.80) return Material.NETHER_BRICK;
        return Material.COBBLESTONE;
    }

    private void placeSpecialRooms(World world) {
        int offset = getOffset();
        for (int[] cell : specialRoomCells) {
            int gx = 2 * cell[0] + 1;
            int gz = 2 * cell[1] + 1;
            int centerX = gx * CELL_BLOCK_SIZE + offset + CELL_BLOCK_SIZE / 2;
            int centerZ = gz * CELL_BLOCK_SIZE + offset + CELL_BLOCK_SIZE / 2;
            int floorY = baseY + 1;

            // Enchanting table
            world.getBlockAt(centerX, floorY, centerZ).setType(Material.ENCHANTMENT_TABLE);

            // Bookshelves
            placeIfAir(world, centerX - 1, floorY, centerZ - 1, Material.BOOKSHELF);
            placeIfAir(world, centerX + 1, floorY, centerZ - 1, Material.BOOKSHELF);
            placeIfAir(world, centerX - 1, floorY, centerZ + 1, Material.BOOKSHELF);
            placeIfAir(world, centerX + 1, floorY, centerZ + 1, Material.BOOKSHELF);
            placeIfAir(world, centerX - 1, floorY + 1, centerZ - 1, Material.BOOKSHELF);
            placeIfAir(world, centerX + 1, floorY + 1, centerZ - 1, Material.BOOKSHELF);
            placeIfAir(world, centerX - 1, floorY + 1, centerZ + 1, Material.BOOKSHELF);
            placeIfAir(world, centerX + 1, floorY + 1, centerZ + 1, Material.BOOKSHELF);

            // Torches for light
            placeIfAir(world, centerX, floorY + 2, centerZ - 1, Material.SEA_LANTERN);
            placeIfAir(world, centerX, floorY + 2, centerZ + 1, Material.SEA_LANTERN);

            // Crafting table
            placeIfAir(world, centerX, floorY, centerZ - 1, Material.WORKBENCH);

            // Anvil
            placeIfAir(world, centerX, floorY, centerZ + 1, Material.ANVIL);
        }
    }

    private void placeIfAir(World world, int x, int y, int z, Material material) {
        Block block = world.getBlockAt(x, y, z);
        if (block.getType() == Material.AIR) {
            block.setType(material);
        }
    }

    private void placeBonusChests(World world) {
        int offset = getOffset();
        List<int[]> candidates = new ArrayList<int[]>(deadEnds);
        Iterator<int[]> it = candidates.iterator();
        while (it.hasNext()) {
            int[] cell = it.next();
            for (int[] sr : specialRoomCells) {
                if (sr[0] == cell[0] && sr[1] == cell[1]) {
                    it.remove();
                    break;
                }
            }
        }
        Collections.shuffle(candidates, random);

        int count = Math.min(bonusChestCount, candidates.size());
        for (int i = 0; i < count; i++) {
            int[] cell = candidates.get(i);
            int gx = 2 * cell[0] + 1;
            int gz = 2 * cell[1] + 1;
            int cx = gx * CELL_BLOCK_SIZE + offset + CELL_BLOCK_SIZE / 2;
            int cz = gz * CELL_BLOCK_SIZE + offset + CELL_BLOCK_SIZE / 2;
            int floorY = baseY + 1;

            Block chestBlock = world.getBlockAt(cx, floorY, cz);
            chestBlock.setType(Material.CHEST);

            if (chestBlock.getState() instanceof Chest) {
                Chest chest = (Chest) chestBlock.getState();
                fillBonusChest(chest.getInventory());
            }
        }
    }

    private void fillBonusChest(Inventory inv) {
        List<ItemStack> possibleLoot = new ArrayList<ItemStack>();
        possibleLoot.add(new ItemStack(Material.IRON_INGOT, random.nextInt(3) + 1));
        possibleLoot.add(new ItemStack(Material.GOLD_INGOT, random.nextInt(2) + 1));
        possibleLoot.add(new ItemStack(Material.DIAMOND, 1));
        possibleLoot.add(new ItemStack(Material.GOLDEN_APPLE, random.nextInt(2) + 1));
        possibleLoot.add(new ItemStack(Material.ARROW, random.nextInt(8) + 4));
        possibleLoot.add(new ItemStack(Material.STRING, random.nextInt(2) + 2));
        possibleLoot.add(new ItemStack(Material.BOOK, random.nextInt(2) + 1));
        possibleLoot.add(new ItemStack(Material.INK_SACK, random.nextInt(4) + 2, (short) 4)); // Lapis
        possibleLoot.add(new ItemStack(Material.EXP_BOTTLE, random.nextInt(5) + 3));
        possibleLoot.add(new ItemStack(Material.WOOD, random.nextInt(8) + 4));
        possibleLoot.add(new ItemStack(Material.COOKED_BEEF, random.nextInt(4) + 2));
        possibleLoot.add(new ItemStack(Material.IRON_HELMET, 1));
        possibleLoot.add(new ItemStack(Material.IRON_BOOTS, 1));
        possibleLoot.add(new ItemStack(Material.OBSIDIAN, random.nextInt(3) + 1));
        possibleLoot.add(new ItemStack(Material.ENDER_PEARL, 1));

        Collections.shuffle(possibleLoot, random);
        int itemCount = random.nextInt(4) + 3;
        for (int i = 0; i < itemCount && i < possibleLoot.size(); i++) {
            int slot = random.nextInt(27);
            inv.setItem(slot, possibleLoot.get(i));
        }
    }

    public List<Location> getSpawnPoints(World world, int count) {
        List<Location> spawns = new ArrayList<Location>();
        int offset = getOffset();

        int sectorsPerSide = (int) Math.ceil(Math.sqrt(count));
        int cellsPerSector = cells / sectorsPerSide;

        List<int[]> sectorCenters = new ArrayList<int[]>();
        for (int sx = 0; sx < sectorsPerSide; sx++) {
            for (int sz = 0; sz < sectorsPerSide; sz++) {
                int centerCX = sx * cellsPerSector + cellsPerSector / 2;
                int centerCZ = sz * cellsPerSector + cellsPerSector / 2;
                centerCX = Math.min(centerCX, cells - 1);
                centerCZ = Math.min(centerCZ, cells - 1);
                sectorCenters.add(new int[]{centerCX, centerCZ});
            }
        }

        Collections.shuffle(sectorCenters, random);
        for (int[] center : sectorCenters) {
            if (spawns.size() >= count) break;

            int[] passageCell = findNearestPassageCell(center[0], center[1]);
            if (passageCell != null) {
                int gx = 2 * passageCell[0] + 1;
                int gz = 2 * passageCell[1] + 1;
                int bx = gx * CELL_BLOCK_SIZE + offset + CELL_BLOCK_SIZE / 2;
                int bz = gz * CELL_BLOCK_SIZE + offset + CELL_BLOCK_SIZE / 2;
                Location loc = new Location(world, bx + 0.5, baseY + 1, bz + 0.5);
                spawns.add(loc);
            }
        }

        while (spawns.size() < count) {
            int cx = random.nextInt(cells);
            int cz = random.nextInt(cells);
            int gx = 2 * cx + 1;
            int gz = 2 * cz + 1;
            if (grid[gx][gz]) {
                int bx = gx * CELL_BLOCK_SIZE + offset + CELL_BLOCK_SIZE / 2;
                int bz = gz * CELL_BLOCK_SIZE + offset + CELL_BLOCK_SIZE / 2;
                Location loc = new Location(world, bx + 0.5, baseY + 1, bz + 0.5);

                boolean tooClose = false;
                for (Location existing : spawns) {
                    if (existing.distance(loc) < 20) {
                        tooClose = true;
                        break;
                    }
                }
                if (!tooClose) {
                    spawns.add(loc);
                }
            }
        }

        return spawns;
    }

    private int[] findNearestPassageCell(int cx, int cz) {
        if (cx >= 0 && cx < cells && cz >= 0 && cz < cells) {
            int gx = 2 * cx + 1;
            int gz = 2 * cz + 1;
            if (grid[gx][gz]) return new int[]{cx, cz};
        }

        for (int radius = 1; radius < cells; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    int nx = cx + dx;
                    int nz = cz + dz;
                    if (nx >= 0 && nx < cells && nz >= 0 && nz < cells) {
                        int gx = 2 * nx + 1;
                        int gz = 2 * nz + 1;
                        if (grid[gx][gz]) return new int[]{nx, nz};
                    }
                }
            }
        }
        return null;
    }

    public int getMazeBlockSize() {
        return gridSize * CELL_BLOCK_SIZE;
    }

    public Location getMazeCenter(World world) {
        return new Location(world, 0.5, baseY + 1, 0.5);
    }

    @SuppressWarnings("deprecation")
    public void placeStarterChest(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        Block block = findChestSpot(location);
        block.setType(Material.CHEST);

        if (block.getState() instanceof Chest) {
            Chest chest = (Chest) block.getState();
            Inventory inv = chest.getInventory();
            inv.setItem(0, new ItemStack(Material.STONE_SWORD, 1));
            inv.setItem(1, new ItemStack(Material.STONE_PICKAXE, 1));
            inv.setItem(2, new ItemStack(Material.STONE_AXE, 1));
            inv.setItem(3, new ItemStack(Material.LEATHER_HELMET, 1));
            inv.setItem(4, new ItemStack(Material.LEATHER_CHESTPLATE, 1));
            inv.setItem(5, new ItemStack(Material.LEATHER_LEGGINGS, 1));
            inv.setItem(6, new ItemStack(Material.LEATHER_BOOTS, 1));
            inv.setItem(9, new ItemStack(Material.COOKED_BEEF, 16));
            inv.setItem(10, new ItemStack(Material.GOLDEN_APPLE, 2));
            inv.setItem(11, new ItemStack(Material.WORKBENCH, 1));
            inv.setItem(12, new ItemStack(Material.TORCH, 8));
            inv.setItem(13, new ItemStack(Material.WOOD, 8));
            inv.setItem(14, new ItemStack(Material.COBBLESTONE, 16));
            inv.setItem(15, new ItemStack(Material.FURNACE, 1));
        }
    }

    @SuppressWarnings("deprecation")
    public void placeStarterChestDuel(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        Block block = findChestSpot(location);
        block.setType(Material.CHEST);

        if (block.getState() instanceof Chest) {
            Chest chest = (Chest) block.getState();
            Inventory inv = chest.getInventory();
            // Armes
            inv.setItem(0, new ItemStack(Material.IRON_SWORD, 1));
            inv.setItem(1, new ItemStack(Material.IRON_PICKAXE, 1));
            inv.setItem(2, new ItemStack(Material.IRON_AXE, 1));
            inv.setItem(3, new ItemStack(Material.BOW, 1));
            // Armure compl\u00e8te en fer
            inv.setItem(4, new ItemStack(Material.IRON_HELMET, 1));
            inv.setItem(5, new ItemStack(Material.IRON_CHESTPLATE, 1));
            inv.setItem(6, new ItemStack(Material.IRON_LEGGINGS, 1));
            inv.setItem(7, new ItemStack(Material.IRON_BOOTS, 1));
            // Nourriture et utilitaires
            inv.setItem(9, new ItemStack(Material.COOKED_BEEF, 32));
            inv.setItem(10, new ItemStack(Material.GOLDEN_APPLE, 4));
            inv.setItem(11, new ItemStack(Material.ARROW, 16));
            inv.setItem(12, new ItemStack(Material.WORKBENCH, 1));
            inv.setItem(13, new ItemStack(Material.WOOD, 16));
            inv.setItem(14, new ItemStack(Material.COBBLESTONE, 32));
            inv.setItem(15, new ItemStack(Material.FURNACE, 1));
            inv.setItem(16, new ItemStack(Material.TORCH, 16));
            inv.setItem(17, new ItemStack(Material.SHIELD, 1));
        }
    }

    private Block findChestSpot(Location location) {
        World world = location.getWorld();
        Block block = world.getBlockAt(location.getBlockX() + 1, location.getBlockY(), location.getBlockZ());
        if (block.getType() != Material.AIR && block.getType() != Material.CHEST) {
            block = world.getBlockAt(location.getBlockX() - 1, location.getBlockY(), location.getBlockZ());
        }
        if (block.getType() != Material.AIR && block.getType() != Material.CHEST) {
            block = world.getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ() + 1);
        }
        return block;
    }
}
