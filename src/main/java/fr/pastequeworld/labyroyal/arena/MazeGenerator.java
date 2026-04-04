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
    private boolean[][] centerArea;
    private int gridSize;
    private int centerRadius;
    private final Random random = new Random();
    private final List<int[]> deadEnds = new ArrayList<int[]>();
    private final List<int[]> specialRoomCells = new ArrayList<int[]>();

    private static final int CELL_BLOCK_SIZE = 5;

    public MazeGenerator(int cells, int wallHeight, int baseY, double oreChance, int specialRoomCount, int bonusChestCount) {
        this.cells = cells;
        this.wallHeight = wallHeight;
        this.baseY = baseY;
        this.oreChance = oreChance;
        this.specialRoomCount = specialRoomCount;
        this.bonusChestCount = bonusChestCount;
        this.gridSize = 2 * cells + 1;
        this.centerRadius = Math.max(2, cells / 7);
    }

    public void generate(World world) {
        generateMazeGrid();
        openCenterArea();
        findDeadEnds();
        selectSpecialRooms();
        placeBlocks(world);
        placeCenterArena(world);
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

    private void openCenterArea() {
        centerArea = new boolean[gridSize][gridSize];
        int centerCell = cells / 2;

        for (int cx = centerCell - centerRadius; cx <= centerCell + centerRadius; cx++) {
            for (int cz = centerCell - centerRadius; cz <= centerCell + centerRadius; cz++) {
                if (cx < 0 || cx >= cells || cz < 0 || cz >= cells) continue;

                int dx = Math.abs(cx - centerCell);
                int dz = Math.abs(cz - centerCell);
                if (dx + dz > (int)(centerRadius * 1.4)) continue;

                int gx = 2 * cx + 1;
                int gz = 2 * cz + 1;
                grid[gx][gz] = true;
                centerArea[gx][gz] = true;

                // Open walls to adjacent center cells
                if (cx > 0) {
                    int ngx = 2 * (cx - 1) + 1;
                    int ndx = Math.abs((cx - 1) - centerCell);
                    if (ndx + dz <= (int)(centerRadius * 1.4) && (cx - 1) >= centerCell - centerRadius) {
                        grid[gx - 1][gz] = true;
                        centerArea[gx - 1][gz] = true;
                    }
                }
                if (cz > 0) {
                    int ndz = Math.abs((cz - 1) - centerCell);
                    if (dx + ndz <= (int)(centerRadius * 1.4) && (cz - 1) >= centerCell - centerRadius) {
                        grid[gx][gz - 1] = true;
                        centerArea[gx][gz - 1] = true;
                    }
                }
            }
        }
    }

    private boolean isCenterArea(int gx, int gz) {
        return gx >= 0 && gx < gridSize && gz >= 0 && gz < gridSize && centerArea[gx][gz];
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

        // Build ALL cells normally (no skipping) - the center arena will overwrite on top
        for (int gx = 0; gx < gridSize; gx++) {
            for (int gz = 0; gz < gridSize; gz++) {
                int blockStartX = gx * CELL_BLOCK_SIZE + offset;
                int blockStartZ = gz * CELL_BLOCK_SIZE + offset;

                if (grid[gx][gz]) {
                    placePassage(world, blockStartX, blockStartZ, gx, gz);
                } else {
                    placeWall(world, blockStartX, blockStartZ, gx, gz);
                }
            }
        }
    }

    private void placePassage(World world, int startX, int startZ, int gx, int gz) {
        boolean hasLight = ((gx + gz) % 4 == 0);

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

                // Ceiling with lighting
                boolean isLightSpot = hasLight && dx == CELL_BLOCK_SIZE / 2 && dz == CELL_BLOCK_SIZE / 2;
                if (isLightSpot) {
                    world.getBlockAt(x, baseY + wallHeight - 1, z).setType(Material.SEA_LANTERN);
                    world.getBlockAt(x, baseY + wallHeight - 2, z).setType(Material.SEA_LANTERN);
                } else {
                    world.getBlockAt(x, baseY + wallHeight - 1, z).setType(Material.BEDROCK);
                }

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

    // ==================== CENTER ARENA ====================

    private void placeCenterArena(World world) {
        int blockRadius = centerRadius * CELL_BLOCK_SIZE + CELL_BLOCK_SIZE / 2;
        int ceilY = baseY + wallHeight - 1;

        // Build the arena floor, air, and ceiling
        for (int bx = -blockRadius - 2; bx <= blockRadius + 2; bx++) {
            for (int bz = -blockRadius - 2; bz <= blockRadius + 2; bz++) {
                double dist = Math.sqrt(bx * bx + bz * bz);
                if (dist > blockRadius + 2) continue;

                // Sub-floor
                world.getBlockAt(bx, baseY - 1, bz).setType(Material.BEDROCK);

                // Floor pattern
                Material floor;
                if (dist <= 2) {
                    floor = Material.GOLD_BLOCK;
                } else if (dist <= blockRadius * 0.35) {
                    floor = Material.QUARTZ_BLOCK;
                } else if (dist <= blockRadius * 0.7) {
                    if ((Math.abs(bx) + Math.abs(bz)) % 2 == 0) {
                        floor = Material.QUARTZ_BLOCK;
                    } else {
                        floor = Material.SMOOTH_BRICK;
                    }
                } else {
                    floor = Material.SMOOTH_BRICK;
                }

                // Nether brick cross inlay (N-S and E-W axes)
                if ((Math.abs(bx) <= 1 || Math.abs(bz) <= 1) && dist > 3 && dist <= blockRadius * 0.9) {
                    if (Math.abs(bx) <= 1 && bz != 0) floor = Material.NETHER_BRICK;
                    if (Math.abs(bz) <= 1 && bx != 0) floor = Material.NETHER_BRICK;
                }

                // SEA_LANTERN ring in floor at edge
                if (dist >= blockRadius - 1 && dist <= blockRadius && ((bx + bz) % 3 == 0)) {
                    floor = Material.SEA_LANTERN;
                }

                world.getBlockAt(bx, baseY, bz).setType(floor);

                // Air
                for (int y = baseY + 1; y < ceilY; y++) {
                    world.getBlockAt(bx, y, bz).setType(Material.AIR);
                }

                // Ceiling with dense lights
                if ((bx % 3 == 0 && bz % 3 == 0) || dist <= 4) {
                    world.getBlockAt(bx, ceilY, bz).setType(Material.SEA_LANTERN);
                } else {
                    world.getBlockAt(bx, ceilY, bz).setType(Material.BEDROCK);
                }
            }
        }

        // Center platform (5x5 raised 1 block)
        for (int px = -2; px <= 2; px++) {
            for (int pz = -2; pz <= 2; pz++) {
                double d = Math.sqrt(px * px + pz * pz);
                if (d > 2.5) continue;
                world.getBlockAt(px, baseY + 1, pz).setType(Material.QUARTZ_BLOCK);
                if (px == 0 && pz == 0) {
                    world.getBlockAt(px, baseY + 1, pz).setType(Material.SEA_LANTERN);
                    world.getBlockAt(px, baseY + 2, pz).setType(Material.SEA_LANTERN);
                }
            }
        }
        // Gold accents on platform cardinal points
        world.getBlockAt(0, baseY + 1, -2).setType(Material.GOLD_BLOCK);
        world.getBlockAt(0, baseY + 1, 2).setType(Material.GOLD_BLOCK);
        world.getBlockAt(-2, baseY + 1, 0).setType(Material.GOLD_BLOCK);
        world.getBlockAt(2, baseY + 1, 0).setType(Material.GOLD_BLOCK);

        // 8 pillars in octagonal pattern
        int pillarDist = (int)(blockRadius * 0.7);
        int[][] pillarOffsets = {
            {pillarDist, 0}, {-pillarDist, 0}, {0, pillarDist}, {0, -pillarDist},
            {(int)(pillarDist * 0.71), (int)(pillarDist * 0.71)},
            {(int)(pillarDist * 0.71), -(int)(pillarDist * 0.71)},
            {-(int)(pillarDist * 0.71), (int)(pillarDist * 0.71)},
            {-(int)(pillarDist * 0.71), -(int)(pillarDist * 0.71)}
        };

        for (int[] po : pillarOffsets) {
            int px = po[0];
            int pz = po[1];
            // Pillar base to ceiling
            for (int y = baseY; y <= ceilY; y++) {
                world.getBlockAt(px, y, pz).setType(Material.SMOOTH_BRICK);
            }
            // Lantern at mid-height and top
            world.getBlockAt(px, baseY + 2, pz).setType(Material.SEA_LANTERN);
            world.getBlockAt(px, ceilY - 1, pz).setType(Material.SEA_LANTERN);
            world.getBlockAt(px, ceilY, pz).setType(Material.GOLD_BLOCK);
        }
    }

    public int getCenterBlockDiameter() {
        return centerRadius * CELL_BLOCK_SIZE * 2 + CELL_BLOCK_SIZE;
    }

    // ==================== SPECIAL ROOMS ====================

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

        // Minimum distance from map center (0,0) to avoid center arena
        double minCenterDist = (centerRadius * CELL_BLOCK_SIZE) + CELL_BLOCK_SIZE * 3;

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

            int[] passageCell = findNearestPassageCellAwayFromCenter(center[0], center[1]);
            if (passageCell != null) {
                int gx = 2 * passageCell[0] + 1;
                int gz = 2 * passageCell[1] + 1;
                int bx = gx * CELL_BLOCK_SIZE + offset + CELL_BLOCK_SIZE / 2;
                int bz = gz * CELL_BLOCK_SIZE + offset + CELL_BLOCK_SIZE / 2;
                Location loc = new Location(world, bx + 0.5, baseY + 1, bz + 0.5);

                // Reject if too close to center
                if (loc.distance(new Location(world, 0.5, baseY + 1, 0.5)) < minCenterDist) continue;

                spawns.add(loc);
            }
        }

        // Fallback: random cells, always away from center
        int attempts = 0;
        while (spawns.size() < count && attempts < 5000) {
            attempts++;
            int cx = random.nextInt(cells);
            int cz = random.nextInt(cells);
            int gx = 2 * cx + 1;
            int gz = 2 * cz + 1;

            // Skip center area cells
            if (isCenterArea(gx, gz)) continue;

            if (grid[gx][gz]) {
                int bx = gx * CELL_BLOCK_SIZE + offset + CELL_BLOCK_SIZE / 2;
                int bz = gz * CELL_BLOCK_SIZE + offset + CELL_BLOCK_SIZE / 2;
                Location loc = new Location(world, bx + 0.5, baseY + 1, bz + 0.5);

                // Reject if too close to center
                if (loc.distance(new Location(world, 0.5, baseY + 1, 0.5)) < minCenterDist) continue;

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

    /**
     * Find nearest passage cell that is NOT in the center area.
     */
    private int[] findNearestPassageCellAwayFromCenter(int cx, int cz) {
        // Check the cell itself first
        if (cx >= 0 && cx < cells && cz >= 0 && cz < cells) {
            int gx = 2 * cx + 1;
            int gz = 2 * cz + 1;
            if (grid[gx][gz] && !isCenterArea(gx, gz)) return new int[]{cx, cz};
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
                        if (grid[gx][gz] && !isCenterArea(gx, gz)) return new int[]{nx, nz};
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
