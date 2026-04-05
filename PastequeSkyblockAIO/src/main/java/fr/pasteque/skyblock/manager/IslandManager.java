package fr.pasteque.skyblock.manager;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.island.model.IslandPreset;
import fr.pasteque.skyblock.model.BlockSnapshot;
import fr.pasteque.skyblock.model.Island;
import fr.pasteque.skyblock.model.IslandRollback;
import fr.pasteque.skyblock.util.LocationUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class IslandManager {
    private static class PendingInvite {
        private final UUID owner;
        private final long expireAt;
        private PendingInvite(UUID owner, long expireAt) { this.owner = owner; this.expireAt = expireAt; }
    }

    private final PastequeSkyblockPlugin plugin;
    private final DataFile dataFile;
    private final Map<UUID, Island> ownedIslands = new HashMap<UUID, Island>();
    private final Map<UUID, UUID> memberIndex = new HashMap<UUID, UUID>();
    private final Map<UUID, PendingInvite> pendingInvites = new HashMap<UUID, PendingInvite>();
    private final Map<UUID, IslandRollback> lastRollbacks = new HashMap<UUID, IslandRollback>();
    private int nextIndex = 0;

    public IslandManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new DataFile(plugin, "islands.yml");
        load();
    }

    public void load() {
        ownedIslands.clear();
        memberIndex.clear();
        pendingInvites.clear();
        nextIndex = dataFile.getConfig().getInt("next-index", 0);
        ConfigurationSection section = dataFile.getConfig().getConfigurationSection("islands");
        if (section == null) return;
        for (String ownerKey : section.getKeys(false)) {
            ConfigurationSection islandSection = section.getConfigurationSection(ownerKey);
            if (islandSection == null) continue;
            UUID owner = UUID.fromString(ownerKey);
            Island island = new Island(owner, islandSection.getInt("gridX"), islandSection.getInt("gridZ"), islandSection.getInt("centerX"), islandSection.getInt("centerZ"));
            island.setPublicVisit(islandSection.getBoolean("publicVisit"));
            island.setPvpEnabled(islandSection.getBoolean("pvpEnabled", false));
            island.setBankBalance(islandSection.getDouble("bankBalance"));
            island.setHome(LocationUtil.load(islandSection, "home"));
            island.setName(islandSection.getString("name", defaultIslandName(Bukkit.getOfflinePlayer(owner).getName())));
            island.setInviteUnlocks(islandSection.getInt("inviteUnlocks", 0));
            island.setFarmingUnlocked(islandSection.getBoolean("farmingUnlocked", false));
            island.setMiningUnlocked(islandSection.getBoolean("miningUnlocked", false));
            island.setFarmingHome(LocationUtil.load(islandSection, "farmingHome"));
            island.setMiningHome(LocationUtil.load(islandSection, "miningHome"));
            for (String value : islandSection.getStringList("members")) {
                try { UUID uuid = UUID.fromString(value); island.getMembers().add(uuid); memberIndex.put(uuid, owner); } catch (Exception ignored) {}
            }
            for (String value : islandSection.getStringList("trusted")) try { island.getTrusted().add(UUID.fromString(value)); } catch (Exception ignored) {}
            for (String value : islandSection.getStringList("banned")) try { island.getBanned().add(UUID.fromString(value)); } catch (Exception ignored) {}
            ownedIslands.put(owner, island);
        }
    }

    public void save() {
        dataFile.getConfig().set("next-index", nextIndex);
        dataFile.getConfig().set("islands", null);
        for (Island island : ownedIslands.values()) {
            ConfigurationSection section = dataFile.getConfig().createSection("islands." + island.getOwner().toString());
            section.set("gridX", island.getGridX());
            section.set("gridZ", island.getGridZ());
            section.set("centerX", island.getCenterX());
            section.set("centerZ", island.getCenterZ());
            section.set("publicVisit", island.isPublicVisit());
            section.set("pvpEnabled", island.isPvpEnabled());
            section.set("bankBalance", island.getBankBalance());
            section.set("name", island.getName());
            section.set("inviteUnlocks", island.getInviteUnlocks());
            section.set("farmingUnlocked", island.isFarmingUnlocked());
            section.set("miningUnlocked", island.isMiningUnlocked());
            LocationUtil.save(section, "home", island.getHome());
            LocationUtil.save(section, "farmingHome", island.getFarmingHome());
            LocationUtil.save(section, "miningHome", island.getMiningHome());
            section.set("members", serialize(island.getMembers()));
            section.set("trusted", serialize(island.getTrusted()));
            section.set("banned", serialize(island.getBanned()));
        }
        dataFile.save();
    }

    private List<String> serialize(Set<UUID> uuids) {
        List<String> out = new ArrayList<String>();
        for (UUID uuid : uuids) out.add(uuid.toString());
        return out;
    }

    private String defaultIslandName(String ownerName) {
        String base = ownerName == null || ownerName.trim().isEmpty() ? "Player" : ownerName.trim();
        return base + "'s Island";
    }

    public Island getOwnedIsland(UUID owner) { return ownedIslands.get(owner); }
    public Island getIslandByPlayer(UUID player) {
        Island own = ownedIslands.get(player);
        if (own != null) return own;
        UUID owner = memberIndex.get(player);
        return owner == null ? null : ownedIslands.get(owner);
    }
    public Collection<Island> getIslands() { return ownedIslands.values(); }
    public boolean hasIsland(UUID owner) { return ownedIslands.containsKey(owner); }
    public int getMaxMembers() { return plugin.getConfig().getInt("islands.max-members", 6); }

    public Island getIslandAt(Location location) {
        if (location == null || location.getWorld() == null || !location.getWorld().getName().equalsIgnoreCase(plugin.getWorldManager().getIslandWorldName())) return null;
        int size = plugin.getConfig().getInt("islands.size", 320);
        for (Island island : ownedIslands.values()) if (island.isInside(location, size)) return island;
        return null;
    }

    public Island createIsland(Player player) { return createIslandFor(player.getUniqueId(), player.getName()); }
    public Island createIslandFor(UUID owner) { return createIslandFor(owner, Bukkit.getOfflinePlayer(owner).getName()); }

    public Island createIslandFor(UUID owner, String ownerName) {
        if (hasIsland(owner)) return null;
        World world = plugin.getWorldManager().getOrCreateIslandWorld();
        int gap = plugin.getConfig().getInt("islands.gap", 340);
        int gx = nextIndex % 5000;
        int gz = nextIndex / 5000;
        nextIndex++;
        int centerX = gx * gap;
        int centerZ = gz * gap;
        Island island = new Island(owner, gx, gz, centerX, centerZ);
        island.setPublicVisit(false);
        island.setPvpEnabled(false);
        island.setBankBalance(0.0D);
        island.setName(defaultIslandName(ownerName));
        island.setInviteUnlocks(0);
        island.setHome(new Location(world, centerX + 0.5D, plugin.getConfig().getInt("worlds.island-y", 100) + 2, centerZ + 0.5D));
        ownedIslands.put(owner, island);
        generateStarterIsland(island);
        save();
        return island;
    }

    private void fillColumn(World world, int x, int y, int z, Material top, Material fill) {
        world.getBlockAt(x, y, z).setType(top);
        world.getBlockAt(x, y - 1, z).setType(fill);
        world.getBlockAt(x, y - 2, z).setType(fill);
        world.getBlockAt(x, y - 3, z).setType(fill);
    }

    private void clearColumnAbove(World world, int x, int fromY, int toY, int z) {
        for (int yy = fromY; yy <= toY; yy++) world.getBlockAt(x, yy, z).setType(Material.AIR);
    }

    private void terrainColumn(World world, int x, int baseY, int z, int height, Material top, Material fill) {
        int topY = baseY + height;
        world.getBlockAt(x, topY, z).setType(top);
        for (int i = 1; i <= 4; i++) world.getBlockAt(x, topY - i, z).setType(fill);
        clearColumnAbove(world, x, topY + 1, topY + 8, z);
    }

    private void setFlower(World world, int x, int y, int z, Material flower) {
        if (world.getBlockAt(x, y, z).getType() == Material.AIR && world.getBlockAt(x, y - 1, z).getType() == Material.GRASS)
            world.getBlockAt(x, y, z).setType(flower);
    }

    private void placeTree(World world, int x, int y, int z) {
        for (int i = 0; i < 4; i++) world.getBlockAt(x, y + i, z).setType(Material.LOG);
        for (int lx = x - 2; lx <= x + 2; lx++) for (int lz = z - 2; lz <= z + 2; lz++) for (int ly = y + 2; ly <= y + 4; ly++) {
            int manhattan = Math.abs(lx - x) + Math.abs(lz - z) + Math.abs(ly - (y + 3));
            if (manhattan <= 4) world.getBlockAt(lx, ly, lz).setType(Material.LEAVES);
        }
        world.getBlockAt(x, y + 5, z).setType(Material.LEAVES);
    }

    private double terrainNoise(int x, int z, long seed) {
        double a = Math.sin((x + (seed * 0.13D)) * 0.19D);
        double b = Math.cos((z - (seed * 0.09D)) * 0.23D);
        double c = Math.sin((x + z + seed) * 0.07D);
        return (a + b + c) / 3.0D;
    }

    private void sculptFloatingIsland(World world, int cx, int baseY, int cz, int radiusX, int radiusZ, int maxHeight, Material top, Material fill, long seed) {
        for (int x = cx - radiusX - 2; x <= cx + radiusX + 2; x++) {
            for (int z = cz - radiusZ - 2; z <= cz + radiusZ + 2; z++) {
                double nx = (x - cx) / (double) radiusX;
                double nz = (z - cz) / (double) radiusZ;
                double dist = (nx * nx) + (nz * nz);
                if (dist > 1.0D) continue;
                double edge = Math.max(0.0D, 1.0D - dist);
                double noise = terrainNoise(x, z, seed);
                int height = 0;
                if (edge > 0.10D) height = 1;
                if (edge > 0.42D && noise > -0.05D) height = 2;
                if (maxHeight >= 3 && edge > 0.70D && noise > 0.35D) height = 3;
                if (maxHeight >= 4 && edge > 0.82D && noise > 0.55D) height = 4;
                if (height > maxHeight) height = maxHeight;
                terrainColumn(world, x, baseY, z, Math.max(0, height), top, fill);
            }
        }
    }

    private void placeLantern(World world, int x, int y, int z) {
        world.getBlockAt(x, y, z).setType(Material.FENCE);
        world.getBlockAt(x, y + 1, z).setType(Material.TORCH);
    }

    private Material pickOre(Random random) {
        int roll = random.nextInt(100);
        if (roll > 98) return Material.DIAMOND_ORE;
        if (roll > 86) return Material.GOLD_ORE;
        if (roll > 58) return Material.IRON_ORE;
        return Material.COAL_ORE;
    }

    public void generateStarterIsland(Island island) {
        World world = plugin.getWorldManager().getOrCreateIslandWorld();
        int y = plugin.getConfig().getInt("worlds.island-y", 100);
        int cx = island.getCenterX();
        int cz = island.getCenterZ();
        sculptFloatingIsland(world, cx, y + 1, cz, 18, 16, 3, Material.GRASS, Material.DIRT, (cx * 31L) ^ (cz * 17L) ^ 1409L);

        int plazaY = getTopY(world, cx, cz, y, y + 15) + 1;
        for (int x = cx - 4; x <= cx + 4; x++) {
            for (int z = cz - 4; z <= cz + 4; z++) {
                world.getBlockAt(x, plazaY, z).setType(Material.WOOD);
                world.getBlockAt(x, plazaY - 1, z).setType(Material.DIRT);
                clearColumnAbove(world, x, plazaY + 1, plazaY + 6, z);
            }
        }

        int farmY = plazaY + 1;
        for (int x = cx - 10; x <= cx - 4; x++) {
            for (int z = cz + 3; z <= cz + 9; z++) {
                world.getBlockAt(x, farmY, z).setType(Material.SOIL);
                world.getBlockAt(x, farmY - 1, z).setType(Material.DIRT);
                world.getBlockAt(x, farmY + 1, z).setType(Material.CROPS);
            }
        }
        for (int z = cz + 3; z <= cz + 9; z++) {
            world.getBlockAt(cx - 7, farmY, z).setType(Material.STATIONARY_WATER);
            world.getBlockAt(cx - 7, farmY + 1, z).setType(Material.AIR);
        }

        int genY = plazaY + 1;
        for (int x = cx + 7; x <= cx + 11; x++) {
            for (int z = cz - 3; z <= cz; z++) {
                world.getBlockAt(x, genY, z).setType(Material.COBBLESTONE);
                world.getBlockAt(x, genY - 1, z).setType(Material.STONE);
            }
        }
        world.getBlockAt(cx + 8, genY + 1, cz - 2).setType(Material.STATIONARY_WATER);
        world.getBlockAt(cx + 10, genY + 1, cz - 2).setType(Material.STATIONARY_LAVA);
        world.getBlockAt(cx + 9, genY + 1, cz - 2).setType(Material.COBBLESTONE);

        int chestY = getTopY(world, cx + 6, cz + 7, y, y + 14) + 1;
        world.getBlockAt(cx + 6, chestY, cz + 7).setType(Material.CHEST);
        Chest chest = (Chest) world.getBlockAt(cx + 6, chestY, cz + 7).getState();
        chest.getBlockInventory().clear();
        chest.getBlockInventory().addItem(new ItemStack(Material.ICE, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.WATER_BUCKET, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.LAVA_BUCKET, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.MELON, 4));
        chest.getBlockInventory().addItem(new ItemStack(Material.SEEDS, 16));
        chest.getBlockInventory().addItem(new ItemStack(Material.SUGAR_CANE, 2));
        chest.getBlockInventory().addItem(new ItemStack(Material.TORCH, 16));
        chest.getBlockInventory().addItem(new ItemStack(Material.SAPLING, 2));
        chest.getBlockInventory().addItem(new ItemStack(Material.BREAD, 6));
        chest.update(true);

        Random deco = new Random((cx * 97L) ^ (cz * 67L) ^ 7127L);
        int treeOneX = cx - 9 + deco.nextInt(4);
        int treeOneZ = cz - 7 + deco.nextInt(3);
        int treeTwoX = cx + 1 + deco.nextInt(4);
        int treeTwoZ = cz - 10 + deco.nextInt(4);
        placeTree(world, treeOneX, getTopY(world, treeOneX, treeOneZ, y, y + 14) + 1, treeOneZ);
        placeTree(world, treeTwoX, getTopY(world, treeTwoX, treeTwoZ, y, y + 14) + 1, treeTwoZ);

        for (int i = 0; i < 10; i++) {
            int fx = cx - 12 + deco.nextInt(25);
            int fz = cz - 10 + deco.nextInt(21);
            setFlower(world, fx, getTopY(world, fx, fz, y, y + 14) + 1, fz, (i % 2 == 0) ? Material.RED_ROSE : Material.YELLOW_FLOWER);
        }

        for (int i = 0; i < 3; i++) {
            int lx = cx - 5 + deco.nextInt(11);
            int lz = cz - 5 + deco.nextInt(11);
            placeLantern(world, lx, plazaY + 1, lz);
        }

        island.setHome(new Location(world, cx + 0.5D, plazaY + 1.0D, cz + 0.5D));
    }

    public boolean unlockFarmingIsland(Island island) {
        if (island == null || island.isFarmingUnlocked()) return false;
        int price = plugin.getConfig().getInt("island-expansions.solo.farming-price", 10000);
        if (!plugin.getEconomyManager().take(island.getOwner(), price)) return false;
        island.setFarmingUnlocked(true);
        generateFarmingIsland(island);
        plugin.getEconomyManager().save();
        save();
        return true;
    }

    public boolean unlockMiningIsland(Island island) {
        if (island == null || island.isMiningUnlocked()) return false;
        int price = plugin.getConfig().getInt("island-expansions.solo.mining-price", 10000);
        if (!plugin.getEconomyManager().take(island.getOwner(), price)) return false;
        island.setMiningUnlocked(true);
        generateMiningIsland(island);
        plugin.getEconomyManager().save();
        save();
        return true;
    }

    private void buildBridge(World world, int y, int fromX, int toX, int z) {
        int start = Math.min(fromX, toX);
        int end = Math.max(fromX, toX);
        for (int x = start; x <= end; x++) {
            fillColumn(world, x, y, z, Material.WOOD, Material.DIRT);
            fillColumn(world, x, y, z + 1, Material.WOOD, Material.DIRT);
            fillColumn(world, x, y, z - 1, Material.WOOD, Material.DIRT);
            fillColumn(world, x, y, z + 2, Material.COBBLESTONE, Material.STONE);
            fillColumn(world, x, y, z - 2, Material.COBBLESTONE, Material.STONE);
            clearColumnAbove(world, x, y + 1, y + 5, z);
            clearColumnAbove(world, x, y + 1, y + 5, z + 1);
            clearColumnAbove(world, x, y + 1, y + 5, z - 1);
            if ((x - start) % 7 == 0) {
                placeLantern(world, x, y + 1, z + 2);
                placeLantern(world, x, y + 1, z - 2);
            }
        }
    }

    private void generateFarmingIsland(Island island) {
        World world = plugin.getWorldManager().getOrCreateIslandWorld();
        int y = plugin.getConfig().getInt("worlds.island-y", 100);
        int targetX = island.getCenterX() - 110;
        int targetZ = island.getCenterZ();
        buildBridge(world, y + 1, island.getCenterX() - 11, targetX + 25, targetZ);
        sculptFloatingIsland(world, targetX, y + 1, targetZ, 32, 23, 2, Material.GRASS, Material.DIRT, (island.getCenterX() * 53L) ^ island.getCenterZ() ^ 4001L);

        Random detail = new Random((island.getCenterX() * 79L) ^ island.getCenterZ() ^ 9923L);
        int fieldY = getTopY(world, targetX, targetZ, y, y + 18) + 1;
        for (int x = targetX - 18; x <= targetX + 18; x++) {
            for (int z = targetZ - 10; z <= targetZ + 10; z++) {
                double dx = (x - targetX) / 18.0D;
                double dz = (z - targetZ) / 10.0D;
                if ((dx * dx) + (dz * dz) > 1.0D) continue;
                world.getBlockAt(x, fieldY - 1, z).setType(Material.DIRT);
                if (z == targetZ - 6 || z == targetZ || z == targetZ + 6) {
                    world.getBlockAt(x, fieldY, z).setType(Material.STATIONARY_WATER);
                    world.getBlockAt(x, fieldY + 1, z).setType(Material.AIR);
                } else if ((x + z + detail.nextInt(3)) % 11 == 0) {
                    world.getBlockAt(x, fieldY, z).setType(Material.WOOD);
                    world.getBlockAt(x, fieldY + 1, z).setType(Material.AIR);
                } else {
                    world.getBlockAt(x, fieldY, z).setType(Material.SOIL);
                    Material crop = Material.CROPS;
                    if (x < targetX - 6) crop = Material.CARROT;
                    else if (x > targetX + 6) crop = Material.POTATO;
                    world.getBlockAt(x, fieldY + 1, z).setType(crop);
                }
            }
        }

        for (int i = 0; i < 4; i++) {
            int tx = targetX - 12 + (i * 8);
            int tz = targetZ + ((i % 2 == 0) ? 14 : -14);
            placeTree(world, tx, getTopY(world, tx, tz, y, y + 18) + 1, tz);
        }

        world.getBlockAt(targetX + 11 + detail.nextInt(6), fieldY + 1, targetZ - 3).setType(Material.MELON_BLOCK);
        world.getBlockAt(targetX + 10 + detail.nextInt(6), fieldY + 1, targetZ + detail.nextInt(2)).setType(Material.MELON_BLOCK);
        world.getBlockAt(targetX + 11 + detail.nextInt(5), fieldY + 1, targetZ + 3).setType(Material.PUMPKIN);

        for (int i = 0; i < 24; i++) {
            int fx = targetX - 20 + (i % 12) * 3;
            int fz = targetZ - 16 + (i / 3) * 2;
            setFlower(world, fx, getTopY(world, fx, fz, y, y + 16) + 1, fz, (i % 2 == 0) ? Material.RED_ROSE : Material.YELLOW_FLOWER);
        }

        int chestY = getTopY(world, targetX + 20, targetZ + 11, y, y + 16) + 1;
        world.getBlockAt(targetX + 20, chestY, targetZ + 11).setType(Material.CHEST);
        Chest chest = (Chest) world.getBlockAt(targetX + 20, chestY, targetZ + 11).getState();
        chest.getBlockInventory().clear();
        chest.getBlockInventory().addItem(new ItemStack(Material.IRON_AXE, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.IRON_SPADE, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.IRON_HOE, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.WATER_BUCKET, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.BONE, 12));
        chest.getBlockInventory().addItem(new ItemStack(Material.CARROT_ITEM, 16));
        chest.getBlockInventory().addItem(new ItemStack(Material.POTATO_ITEM, 16));
        chest.update(true);

        placeLantern(world, targetX - 16, fieldY + 1, targetZ - 11);
        placeLantern(world, targetX + 16, fieldY + 1, targetZ - 11);
        placeLantern(world, targetX - 16, fieldY + 1, targetZ + 11);
        placeLantern(world, targetX + 16, fieldY + 1, targetZ + 11);

        island.setFarmingHome(new Location(world, targetX + 0.5D, fieldY + 1.0D, targetZ + 0.5D));
    }

    private int getTopY(World world, int x, int z, int minY, int maxY) {
        for (int yy = maxY; yy >= minY; yy--) if (world.getBlockAt(x, yy, z).getType() != Material.AIR) return yy;
        return minY;
    }

    private void generateMiningIsland(Island island) {
        World world = plugin.getWorldManager().getOrCreateIslandWorld();
        int y = plugin.getConfig().getInt("worlds.island-y", 100);
        int targetX = island.getCenterX() + 110;
        int targetZ = island.getCenterZ();
        buildBridge(world, y + 1, island.getCenterX() + 11, targetX - 25, targetZ);
        sculptFloatingIsland(world, targetX, y + 1, targetZ, 30, 22, 3, Material.STONE, Material.STONE, (island.getCenterX() * 29L) ^ island.getCenterZ() ^ 7331L);

        Random random = new Random((island.getCenterX() * 31L) ^ island.getCenterZ() ^ 7331L);
        for (int x = targetX - 24; x <= targetX + 24; x++) {
            for (int z = targetZ - 17; z <= targetZ + 17; z++) {
                int topY = getTopY(world, x, z, y, y + 18);
                if (topY <= y) continue;
                int depthMax = Math.max(3, Math.min(8, topY - (y + 1)));
                for (int depth = 1; depth <= depthMax; depth++) {
                    if (random.nextInt(100) < 16) world.getBlockAt(x, topY - depth, z).setType(pickOre(random));
                }
            }
        }

        int quarryX = targetX + random.nextInt(5) - 2;
        int quarryZ = targetZ + random.nextInt(5) - 2;
        int quarryY = getTopY(world, quarryX, quarryZ, y, y + 18);
        for (int x = quarryX - 6; x <= quarryX + 6; x++) {
            for (int z = quarryZ - 6; z <= quarryZ + 6; z++) {
                double nx = (x - quarryX) / 6.0D;
                double nz = (z - quarryZ) / 6.0D;
                if ((nx * nx) + (nz * nz) > 1.0D) continue;
                for (int yy = y + 3; yy <= quarryY; yy++) world.getBlockAt(x, yy, z).setType(Material.AIR);
                world.getBlockAt(x, y + 2, z).setType(Material.COBBLESTONE);
            }
        }

        for (int i = 0; i < 4; i++) {
            int sx = quarryX + ((i < 2) ? -7 : 7) + random.nextInt(3) - 1;
            int sz = quarryZ + ((i % 2 == 0) ? -7 : 7) + random.nextInt(3) - 1;
            int sy = getTopY(world, sx, sz, y, y + 18) + 1;
            world.getBlockAt(sx, sy, sz).setType(Material.LOG);
            world.getBlockAt(sx, sy + 1, sz).setType(Material.LOG);
            world.getBlockAt(sx, sy + 2, sz).setType(Material.TORCH);
        }

        int chestY = getTopY(world, targetX + 17, targetZ + 11, y, y + 18) + 1;
        world.getBlockAt(targetX + 17, chestY, targetZ + 11).setType(Material.CHEST);
        Chest chest = (Chest) world.getBlockAt(targetX + 17, chestY, targetZ + 11).getState();
        chest.getBlockInventory().clear();
        chest.getBlockInventory().addItem(new ItemStack(Material.IRON_PICKAXE, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.STONE_PICKAXE, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.TORCH, 32));
        chest.getBlockInventory().addItem(new ItemStack(Material.LADDER, 24));
        chest.update(true);

        island.setMiningHome(new Location(world, quarryX - 8.5D, getTopY(world, quarryX - 9, quarryZ, y, y + 18) + 2, quarryZ + 0.5D));
    }

    private IslandRollback createRollback(Island island, boolean deletedIsland) {
        World world = plugin.getWorldManager().getOrCreateIslandWorld();
        int size = plugin.getConfig().getInt("islands.size", 320);
        int half = size / 2;
        List<BlockSnapshot> blocks = new ArrayList<BlockSnapshot>();
        for (int x = island.getCenterX() - half; x <= island.getCenterX() + half; x++)
            for (int z = island.getCenterZ() - half; z <= island.getCenterZ() + half; z++)
                for (int yy = 0; yy <= 255; yy++) {
                    Block block = world.getBlockAt(x, yy, z);
                    if (block.getType() != Material.AIR) blocks.add(BlockSnapshot.capture(block));
                }
        return new IslandRollback(island, deletedIsland, blocks);
    }

    public void resetIsland(Island island) {
        if (island == null) return;
        lastRollbacks.put(island.getOwner(), createRollback(island, false));
        clearIslandArea(island);
        island.getMembers().clear();
        island.getTrusted().clear();
        island.getBanned().clear();
        island.setInviteUnlocks(0);
        island.setPvpEnabled(false);
        island.setFarmingUnlocked(false);
        island.setMiningUnlocked(false);
        island.setFarmingHome(null);
        island.setMiningHome(null);
        generateStarterIsland(island);
        save();
    }

    public void deleteIsland(Island island) {
        if (island == null) return;
        lastRollbacks.put(island.getOwner(), createRollback(island, true));
        clearIslandArea(island);
        for (UUID member : new HashSet<UUID>(island.getMembers())) memberIndex.remove(member);
        ownedIslands.remove(island.getOwner());
        save();
    }

    private void clearIslandArea(Island island) {
        World world = plugin.getWorldManager().getOrCreateIslandWorld();
        int size = plugin.getConfig().getInt("islands.size", 320);
        int half = size / 2;
        for (int x = island.getCenterX() - half; x <= island.getCenterX() + half; x++)
            for (int z = island.getCenterZ() - half; z <= island.getCenterZ() + half; z++)
                for (int yy = 0; yy <= 255; yy++) world.getBlockAt(x, yy, z).setType(Material.AIR);
    }

    public boolean rollbackLastIsland(UUID owner) {
        IslandRollback rollback = lastRollbacks.get(owner);
        if (rollback == null) return false;
        World world = plugin.getWorldManager().getOrCreateIslandWorld();
        Island island = ownedIslands.get(owner);
        if (rollback.isDeletedIsland()) {
            island = new Island(owner, rollback.getGridX(), rollback.getGridZ(), rollback.getCenterX(), rollback.getCenterZ());
            ownedIslands.put(owner, island);
        }
        if (island == null) return false;
        clearIslandArea(island);
        island.setPublicVisit(rollback.isPublicVisit());
        island.setPvpEnabled(rollback.isPvpEnabled());
        island.setBankBalance(rollback.getBankBalance());
        island.setHome(rollback.getHome());
        island.setName(rollback.getName());
        island.setInviteUnlocks(rollback.getInviteUnlocks());
        island.getMembers().clear(); island.getMembers().addAll(rollback.getMembers());
        island.getTrusted().clear(); island.getTrusted().addAll(rollback.getTrusted());
        island.getBanned().clear(); island.getBanned().addAll(rollback.getBanned());
        for (UUID member : rollback.getMembers()) memberIndex.put(member, owner);
        for (BlockSnapshot block : rollback.getBlocks()) block.restore(world);
        lastRollbacks.remove(owner);
        save();
        return true;
    }

    public boolean invite(Player owner, Player target) {
        Island island = getOwnedIsland(owner.getUniqueId());
        if (island == null || target == null || target.getUniqueId().equals(owner.getUniqueId()) || hasIsland(target.getUniqueId()) || memberIndex.containsKey(target.getUniqueId())) return false;
        if (island.getMembers().size() >= getMaxMembers()) return false;
        if (island.getInviteUnlocks() <= 0) return false;
        long expireAt = System.currentTimeMillis() + 120000L;
        pendingInvites.put(target.getUniqueId(), new PendingInvite(owner.getUniqueId(), expireAt));
        island.consumeInviteUnlock();
        save();
        return true;
    }

    public Island acceptInvite(Player player) {
        PendingInvite invite = pendingInvites.remove(player.getUniqueId());
        if (invite == null || invite.expireAt < System.currentTimeMillis()) return null;
        Island island = ownedIslands.get(invite.owner);
        if (island == null || island.getMembers().size() >= getMaxMembers()) return null;
        island.getMembers().add(player.getUniqueId());
        island.getBanned().remove(player.getUniqueId());
        memberIndex.put(player.getUniqueId(), invite.owner);
        save();
        return island;
    }

    public boolean denyInvite(Player player) { return pendingInvites.remove(player.getUniqueId()) != null; }

    public boolean removeMember(Island island, UUID member) {
        memberIndex.remove(member);
        boolean changed = island.getMembers().remove(member) || island.getTrusted().remove(member);
        if (changed) save();
        return changed;
    }

    public boolean canBuild(Player player, Location location) {
        if (player.hasPermission("pastequeskyblock.admin")) return true;
        Island island = getIslandAt(location);
        return island != null && island.canBuild(player.getUniqueId());
    }

    public boolean canOpen(Player player, Location location) {
        if (player.hasPermission("pastequeskyblock.admin")) return true;
        Island island = getIslandAt(location);
        return island != null && island.canBuild(player.getUniqueId());
    }

    public boolean canVisit(Player player, Island island) { return island != null && island.canVisit(player.getUniqueId()); }
    public void setHome(Island island, Location location) { island.setHome(location); save(); }
    public void setPvp(Island island, boolean enabled) { island.setPvpEnabled(enabled); save(); }
    public void rename(Island island, String newName) { island.setName(newName); save(); }
    public void addInviteUnlock(Island island) { island.addInviteUnlock(); save(); }

    public int getLevel(Island island) {
        if (island == null) return 0;
        int size = plugin.getConfig().getInt("islands.size", 320);
        int half = size / 2;
        World world = plugin.getWorldManager().getOrCreateIslandWorld();
        ConfigurationSection values = plugin.getConfig().getConfigurationSection("block-values");
        if (values == null) return 0;
        int total = 0;
        for (int x = island.getCenterX() - half; x <= island.getCenterX() + half; x++)
            for (int z = island.getCenterZ() - half; z <= island.getCenterZ() + half; z++)
                for (int yy = 0; yy <= 255; yy++)
                    total += values.getInt(world.getBlockAt(x, yy, z).getType().name(), 0);
        return total;
    }

    public Location getSafeTeleport(Island island) {
        if (island != null && island.getHome() != null && island.getHome().getWorld() != null) return island.getHome();
        if (island != null) return new Location(plugin.getWorldManager().getOrCreateIslandWorld(), island.getCenterX() + 0.5D, plugin.getConfig().getInt("worlds.island-y", 100) + 2, island.getCenterZ() + 0.5D);
        return plugin.getWorldManager().getServerSpawn();
    }

    public boolean isSkyblockWorld(Location location) { return location != null && location.getWorld() != null && location.getWorld().getName().equalsIgnoreCase(plugin.getWorldManager().getIslandWorldName()); }

    public List<Island> getSortedIslands() {
        List<Island> list = new ArrayList<Island>(ownedIslands.values());
        Collections.sort(list, new Comparator<Island>() {
            public int compare(Island a, Island b) { return a.getOwner().toString().compareToIgnoreCase(b.getOwner().toString()); }
        });
        return list;
    }
}
