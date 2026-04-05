package fr.pasteque.skyblock.manager;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.model.CoopIsland;
import fr.pasteque.skyblock.util.LocationUtil;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class CoopManager {
    private static class PendingCreation {
        private final UUID owner;
        private final LinkedHashSet<UUID> invitees;
        private final LinkedHashSet<UUID> accepted = new LinkedHashSet<UUID>();
        private final long expireAt;

        private PendingCreation(UUID owner, Collection<UUID> invitees, long expireAt) {
            this.owner = owner;
            this.invitees = new LinkedHashSet<UUID>(invitees);
            this.accepted.add(owner);
            this.expireAt = expireAt;
        }

        private int total() { return invitees.size() + 1; }
        private int acceptedCount() { return accepted.size(); }
        private int remaining() { return Math.max(0, total() - acceptedCount()); }
        private boolean isComplete() { return acceptedCount() >= total(); }
        private boolean isParticipant(UUID uuid) { return owner.equals(uuid) || invitees.contains(uuid); }
    }

    private final PastequeSkyblockPlugin plugin;
    private final DataFile dataFile;
    private final Map<String, CoopIsland> islands = new LinkedHashMap<String, CoopIsland>();
    private final Map<UUID, Set<String>> memberships = new HashMap<UUID, Set<String>>();
    private final Map<UUID, PendingCreation> pendingByOwner = new HashMap<UUID, PendingCreation>();
    private final Map<UUID, UUID> inviteTargetToOwner = new HashMap<UUID, UUID>();
    private int nextIndex = 0;

    public CoopManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new DataFile(plugin, "coop.yml");
        load();
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override public void run() {
                Iterator<Map.Entry<UUID, PendingCreation>> it = pendingByOwner.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<UUID, PendingCreation> entry = it.next();
                    PendingCreation pending = entry.getValue();
                    if (pending.expireAt < System.currentTimeMillis()) {
                        notifyCancel(pending, "&fLa creation de l'ile coop a expire.");
                        for (UUID uuid : pending.invitees) inviteTargetToOwner.remove(uuid);
                        it.remove();
                        continue;
                    }
                    Player owner = Bukkit.getPlayer(pending.owner);
                    if (owner != null && owner.isOnline()) {
                        sendProgress(owner, pending);
                    }
                }
            }
        }, 20L, 20L);
    }

    public void load() {
        islands.clear();
        memberships.clear();
        nextIndex = dataFile.getConfig().getInt("next-index", 0);
        ConfigurationSection section = dataFile.getConfig().getConfigurationSection("islands");
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            ConfigurationSection cfg = section.getConfigurationSection(id);
            if (cfg == null) continue;
            UUID owner = UUID.fromString(cfg.getString("owner"));
            CoopIsland island = new CoopIsland(id, owner, cfg.getInt("centerX"), cfg.getInt("centerZ"), cfg.getInt("size"));
            island.setHome(LocationUtil.load(cfg, "home"));
            island.setName(cfg.getString("name", defaultName(owner, id)));
            island.setMiningUnlocked(cfg.getBoolean("miningUnlocked", false));
            island.setMiningHome(LocationUtil.load(cfg, "miningHome"));
            List<String> members = cfg.getStringList("members");
            for (String value : members) {
                try {
                    UUID uuid = UUID.fromString(value);
                    island.getMembers().add(uuid);
                    memberships.computeIfAbsent(uuid, k -> new LinkedHashSet<String>()).add(id);
                } catch (Exception ignored) {}
            }
            if (!island.getMembers().contains(owner)) {
                island.getMembers().add(owner);
                memberships.computeIfAbsent(owner, k -> new LinkedHashSet<String>()).add(id);
            }
            islands.put(id, island);
        }
    }

    public void save() {
        dataFile.getConfig().set("next-index", nextIndex);
        dataFile.getConfig().set("islands", null);
        for (CoopIsland island : islands.values()) {
            ConfigurationSection cfg = dataFile.getConfig().createSection("islands." + island.getId());
            cfg.set("owner", island.getOwner().toString());
            cfg.set("centerX", island.getCenterX());
            cfg.set("centerZ", island.getCenterZ());
            cfg.set("size", island.getSize());
            cfg.set("name", island.getName());
            cfg.set("miningUnlocked", island.isMiningUnlocked());
            LocationUtil.save(cfg, "home", island.getHome());
            LocationUtil.save(cfg, "miningHome", island.getMiningHome());
            List<String> memberIds = new ArrayList<String>();
            for (UUID uuid : island.getMembers()) memberIds.add(uuid.toString());
            cfg.set("members", memberIds);
        }
        dataFile.save();
    }

    private String defaultName(UUID owner, String id) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(owner);
        String base = op.getName() == null ? "Coop" : op.getName();
        return base + " Coop";
    }

    public boolean beginCreation(Player owner, List<Player> invitedPlayers) {
        if (pendingByOwner.containsKey(owner.getUniqueId())) {
            MessageUtil.send(owner, plugin.getPrefix(), "&fUne creation de coop est deja en attente.");
            return false;
        }
        LinkedHashSet<UUID> invitees = new LinkedHashSet<UUID>();
        for (Player player : invitedPlayers) {
            if (player == null || !player.isOnline()) continue;
            if (player.getUniqueId().equals(owner.getUniqueId())) continue;
            if (inviteTargetToOwner.containsKey(player.getUniqueId())) {
                MessageUtil.send(owner, plugin.getPrefix(), "&f" + player.getName() + " a deja une invitation coop en attente.");
                return false;
            }
            invitees.add(player.getUniqueId());
        }
        if (invitees.isEmpty()) {
            MessageUtil.send(owner, plugin.getPrefix(), "&fTu dois inviter au moins 1 joueur connecte.");
            return false;
        }
        if (invitees.size() > 5) {
            MessageUtil.send(owner, plugin.getPrefix(), "&fMaximum 5 joueurs invites (6 avec toi).");
            return false;
        }
        PendingCreation pending = new PendingCreation(owner.getUniqueId(), invitees, System.currentTimeMillis() + 120000L);
        pendingByOwner.put(owner.getUniqueId(), pending);
        for (UUID uuid : invitees) {
            inviteTargetToOwner.put(uuid, owner.getUniqueId());
            Player target = Bukkit.getPlayer(uuid);
            if (target != null) {
                MessageUtil.send(target, plugin.getPrefix(), "&d" + owner.getName() + " &fveut lancer une Coop Island avec toi. &a/iscoop accept &fou &7/iscoop deny");
            }
        }
        sendProgress(owner, pending);
        return true;
    }

    public boolean acceptInvite(Player player) {
        UUID ownerId = inviteTargetToOwner.get(player.getUniqueId());
        if (ownerId == null) return false;
        PendingCreation pending = pendingByOwner.get(ownerId);
        if (pending == null || !pending.invitees.contains(player.getUniqueId())) {
            inviteTargetToOwner.remove(player.getUniqueId());
            return false;
        }
        pending.accepted.add(player.getUniqueId());
        inviteTargetToOwner.remove(player.getUniqueId());
        Player owner = Bukkit.getPlayer(ownerId);
        if (owner != null) {
            MessageUtil.send(owner, plugin.getPrefix(), "&a" + player.getName() + " &fa accepte l'invitation coop. Reste &e" + pending.remaining() + " &fvalidation(s).");
            sendProgress(owner, pending);
        }
        if (pending.isComplete()) {
            finalizeCreation(pending);
        }
        return true;
    }

    public boolean denyInvite(Player player) {
        UUID ownerId = inviteTargetToOwner.get(player.getUniqueId());
        if (ownerId == null) return false;
        PendingCreation pending = pendingByOwner.get(ownerId);
        if (pending == null) {
            inviteTargetToOwner.remove(player.getUniqueId());
            return false;
        }
        notifyCancel(pending, "&fLa creation de la Coop Island a ete annulee : &d" + player.getName() + " &fa refuse.");
        clearPending(ownerId);
        return true;
    }

    public int getRemainingApprovals(UUID owner) {
        PendingCreation pending = pendingByOwner.get(owner);
        return pending == null ? -1 : pending.remaining();
    }

    private void finalizeCreation(PendingCreation pending) {
        List<UUID> others = new ArrayList<UUID>(pending.invitees);
        CoopIsland island = createIsland(pending.owner, others);
        for (UUID uuid : island.getMembers()) {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null && member.isOnline()) {
                member.teleport(getSafeTeleport(island));
                MessageUtil.send(member, plugin.getPrefix(), "&aLa Coop Island est prete ! Bienvenue sur &f" + island.getName());
            }
        }
        clearPending(pending.owner);
    }

    private void clearPending(UUID owner) {
        PendingCreation removed = pendingByOwner.remove(owner);
        if (removed != null) {
            for (UUID uuid : removed.invitees) inviteTargetToOwner.remove(uuid);
        }
    }

    private void notifyCancel(PendingCreation pending, String message) {
        for (UUID uuid : pending.accepted) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) MessageUtil.send(player, plugin.getPrefix(), message);
        }
        for (UUID uuid : pending.invitees) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) MessageUtil.send(player, plugin.getPrefix(), message);
        }
    }

    private void sendProgress(Player owner, PendingCreation pending) {
        int total = pending.total();
        int accepted = pending.acceptedCount();
        int filled = (int) Math.round((accepted / (double) total) * 10.0D);
        if (filled < 0) filled = 0;
        if (filled > 10) filled = 10;
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) bar.append(i < filled ? "\u00a7a\u25a0" : "\u00a78\u25a0");
        String msg = "\u00a7dCoop Island \u00a77\u00bb " + bar + " \u00a7f" + accepted + "/" + total + " \u00a77- \u00a7e" + pending.remaining() + " attente(s)";
        try {
            Class<?> chatMessageTypeClass = Class.forName("net.md_5.bungee.api.ChatMessageType");
            Object actionBar = Enum.valueOf((Class<Enum>) chatMessageTypeClass, "ACTION_BAR");
            Class<?> textComponentClass = Class.forName("net.md_5.bungee.api.chat.TextComponent");
            Class<?> baseComponentClass = Class.forName("net.md_5.bungee.api.chat.BaseComponent");
            Object component = textComponentClass.getConstructor(String.class).newInstance(msg);
            Object spigot = owner.getClass().getMethod("spigot").invoke(owner);
            spigot.getClass().getMethod("sendMessage", chatMessageTypeClass, baseComponentClass).invoke(spigot, actionBar, component);
        } catch (Throwable ignored) {
            owner.sendMessage(msg);
        }
    }

    public CoopIsland createIsland(UUID owner, List<UUID> additionalMembers) {
        int total = Math.max(2, Math.min(6, 1 + additionalMembers.size()));
        World world = plugin.getWorldManager().getOrCreateCoopWorld();
        int gap = plugin.getConfig().getInt("islands.coop-gap", 520);
        int index = nextIndex++;
        int gx = index % 4000;
        int gz = index / 4000;
        int centerX = gx * gap + gap;
        int centerZ = gz * gap + gap;
        int size = plugin.getConfig().getInt("islands.coop-base-size", 220) + ((total - 2) * plugin.getConfig().getInt("islands.coop-size-per-player", 30));
        String id = "coop_" + UUID.randomUUID().toString().substring(0, 8);
        CoopIsland island = new CoopIsland(id, owner, centerX, centerZ, size);
        island.setName(defaultName(owner, id));
        island.getMembers().add(owner);
        memberships.computeIfAbsent(owner, k -> new LinkedHashSet<String>()).add(id);
        for (UUID uuid : additionalMembers) {
            island.getMembers().add(uuid);
            memberships.computeIfAbsent(uuid, k -> new LinkedHashSet<String>()).add(id);
        }
        island.setHome(new Location(world, centerX + 0.5D, plugin.getConfig().getInt("worlds.coop-y", 100) + 4, centerZ + 0.5D));
        islands.put(id, island);
        generateStarterIsland(island, total);
        save();
        return island;
    }

    private void fillColumn(World world, int x, int y, int z, Material top, Material fill) {
        world.getBlockAt(x, y, z).setType(top);
        for (int i = 1; i <= 4; i++) world.getBlockAt(x, y - i, z).setType(fill);
    }

    private void clearAbove(World world, int x, int fromY, int z, int height) {
        for (int yy = fromY; yy <= fromY + height; yy++) world.getBlockAt(x, yy, z).setType(Material.AIR);
    }

    private void terrainColumn(World world, int x, int baseY, int z, int height, Material top, Material fill) {
        int topY = baseY + height;
        world.getBlockAt(x, topY, z).setType(top);
        for (int i = 1; i <= 4; i++) world.getBlockAt(x, topY - i, z).setType(fill);
        clearAbove(world, x, topY + 1, z, 8);
    }

    private int getTop(World world, int x, int z, int minY, int maxY) {
        for (int y = maxY; y >= minY; y--) {
            if (world.getBlockAt(x, y, z).getType() != Material.AIR) return y;
        }
        return minY;
    }

    private void placeNaturalTree(World world, int x, int y, int z) {
        for (int i = 0; i < 4; i++) world.getBlockAt(x, y + i, z).setType(Material.LOG);
        for (int lx = x - 2; lx <= x + 2; lx++) for (int lz = z - 2; lz <= z + 2; lz++) for (int ly = y + 2; ly <= y + 4; ly++) {
            int manhattan = Math.abs(lx - x) + Math.abs(lz - z) + Math.abs(ly - (y + 3));
            if (manhattan <= 4) world.getBlockAt(lx, ly, lz).setType(Material.LEAVES);
        }
        world.getBlockAt(x, y + 5, z).setType(Material.LEAVES);
    }

    private double terrainNoise(int x, int z, long seed) {
        double a = Math.sin((x + seed * 0.11D) * 0.17D);
        double b = Math.cos((z - seed * 0.13D) * 0.21D);
        double c = Math.sin((x + z + seed) * 0.06D);
        return (a + b + c) / 3.0D;
    }

    private void sculptIsland(World world, int cx, int baseY, int cz, int radiusX, int radiusZ, int maxLift, Material top, Material fill, long seed) {
        int rootDepth = Math.max(6, Math.min(radiusX, radiusZ) - 2);
        double noiseAmpEdge = 0.18D;
        for (int x = cx - radiusX - 4; x <= cx + radiusX + 4; x++) {
            for (int z = cz - radiusZ - 4; z <= cz + radiusZ + 4; z++) {
                double nx = (x - cx) / (double) radiusX;
                double nz = (z - cz) / (double) radiusZ;
                double dist = (nx * nx) + (nz * nz);
                double noise = terrainNoise(x, z, seed);
                double warpedDist = dist - noise * noiseAmpEdge;
                if (warpedDist > 1.0D) continue;
                double edge = Math.max(0.0D, 1.0D - warpedDist);
                int h = 0;
                if (edge > 0.05D) h = 1;
                if (edge > 0.35D && noise > -0.15D) h = 2;
                if (maxLift >= 3 && edge > 0.60D && noise > 0.20D) h = 3;
                if (maxLift >= 4 && edge > 0.78D && noise > 0.45D) h = 4;
                if (h > maxLift) h = maxLift;
                terrainColumn(world, x, baseY, z, Math.max(0, h), top, fill);
                // Organic tapered underside so coop islands also have natural roots
                terrainUnderside(world, x, baseY - 4, z, edge, rootDepth, noise, fill);
            }
        }
    }

    /**
     * Tapered organic underside under a surface column (coop islands).
     * Depth follows an edge^0.65 curve so the bottom is rounded, not conical.
     */
    private void terrainUnderside(World world, int x, int baseY, int z,
                                  double edge, int depth, double noise, Material fill) {
        if (edge <= 0.0D) return;
        double shape = Math.pow(Math.max(0.0D, edge), 0.65D);
        int rootDepth = (int) Math.round(shape * depth + noise * 1.2D);
        if (rootDepth < 1) return;
        if (rootDepth > depth) rootDepth = depth;
        Material accent = (fill == Material.DIRT) ? Material.STONE : null;
        for (int i = 0; i < rootDepth; i++) {
            int yy = (baseY - 1) - i;
            if (yy <= 1) break;
            Material use = fill;
            if (accent != null) {
                int hv = (int) (((long) x * 73856093L) ^ ((long) z * 19349663L) ^ ((long) yy * 83492791L));
                if ((hv & 0x7FFFFFFF) % 13 == 0) use = accent;
            }
            world.getBlockAt(x, yy, z).setType(use);
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

    public void generateStarterIsland(CoopIsland island, int players) {
        World world = plugin.getWorldManager().getOrCreateCoopWorld();
        int y = plugin.getConfig().getInt("worlds.coop-y", 100);
        int cx = island.getCenterX();
        int cz = island.getCenterZ();
        int radius = 24 + (players * 4);
        sculptIsland(world, cx, y + 1, cz, radius, radius - 3, 3, Material.GRASS, Material.DIRT, (cx * 13L) ^ (cz * 31L) ^ players);

        int plazaY = getTop(world, cx, cz, y - 4, y + 20) + 1;
        for (int x = cx - 6; x <= cx + 6; x++) {
            for (int z = cz - 6; z <= cz + 6; z++) {
                world.getBlockAt(x, plazaY, z).setType(Material.WOOD);
                world.getBlockAt(x, plazaY - 1, z).setType(Material.DIRT);
                clearAbove(world, x, plazaY + 1, z, 8);
            }
        }

        Random deco = new Random((cx * 101L) ^ (cz * 59L) ^ (players * 13L));
        for (int i = 0; i < 10; i++) {
            int tx = cx - radius + 10 + deco.nextInt((radius * 2) - 20);
            int tz = cz - radius + 10 + deco.nextInt((radius * 2) - 20);
            int top = getTop(world, tx, tz, y - 4, y + 20) + 1;
            for (int ox = -1; ox <= 1; ox++) {
                world.getBlockAt(tx + ox, top, tz).setType(Material.WOOD);
                world.getBlockAt(tx + ox, top - 1, tz).setType(Material.DIRT);
            }
        }

        int treeCount = Math.max(3, Math.min(8, players + 2));
        for (int i = 0; i < treeCount; i++) {
            double angle = (Math.PI * 2D / treeCount) * i + 0.35D + (deco.nextDouble() * 0.7D);
            int dist = (radius - 11) + deco.nextInt(5);
            int tx = cx + (int) Math.round(Math.cos(angle) * dist);
            int tz = cz + (int) Math.round(Math.sin(angle) * dist);
            int ty = getTop(world, tx, tz, y - 4, y + 20) + 1;
            placeNaturalTree(world, tx, ty, tz);
        }

        for (int i = 0; i < Math.max(2, players); i++) {
            double angle = (Math.PI * 2D / Math.max(2, players)) * i + 1.1D + (deco.nextDouble() * 0.6D);
            int dist = (radius - 14) + deco.nextInt(4);
            int tx = cx + (int) Math.round(Math.cos(angle) * dist);
            int tz = cz + (int) Math.round(Math.sin(angle) * dist);
            int topY = getTop(world, tx, tz, y - 4, y + 20) + 1;
            world.getBlockAt(tx, topY, tz).setType(Material.CHEST);
            Chest chest = (Chest) world.getBlockAt(tx, topY, tz).getState();
            chest.getBlockInventory().clear();
            chest.getBlockInventory().addItem(new ItemStack(Material.ICE, 1));
            chest.getBlockInventory().addItem(new ItemStack(Material.WATER_BUCKET, 1));
            chest.getBlockInventory().addItem(new ItemStack(Material.LAVA_BUCKET, 1));
            chest.getBlockInventory().addItem(new ItemStack(Material.MELON, 4));
            chest.getBlockInventory().addItem(new ItemStack(Material.SEEDS, 16));
            chest.getBlockInventory().addItem(new ItemStack(Material.SUGAR_CANE, 2));
            chest.getBlockInventory().addItem(new ItemStack(Material.TORCH, 24));
            chest.update(true);
        }

        for (int i = 0; i < 4; i++) {
            int lx = cx - 10 + deco.nextInt(21);
            int lz = cz - 10 + deco.nextInt(21);
            placeLantern(world, lx, plazaY + 1, lz);
        }

        island.setHome(new Location(world, cx + 0.5D, plazaY + 1.0D, cz + 0.5D));
    }

    public boolean unlockMining(CoopIsland island) {
        if (island == null || island.isMiningUnlocked()) return false;
        int price = plugin.getConfig().getInt("island-expansions.coop.mining-price", 50000);
        if (!plugin.getEconomyManager().take(island.getOwner(), price)) return false;
        island.setMiningUnlocked(true);
        World world = plugin.getWorldManager().getOrCreateCoopWorld();
        int y = plugin.getConfig().getInt("worlds.coop-y", 100);
        int offset = (island.getSize() / 2) + 88;
        int targetX = island.getCenterX() + offset;
        int targetZ = island.getCenterZ();
        int startX = island.getCenterX() + (island.getSize() / 2) - 4;
        for (int x = startX; x <= targetX - 24; x++) {
            fillColumn(world, x, y + 2, targetZ, Material.WOOD, Material.DIRT);
            fillColumn(world, x, y + 2, targetZ + 1, Material.WOOD, Material.DIRT);
            fillColumn(world, x, y + 2, targetZ - 1, Material.WOOD, Material.DIRT);
            fillColumn(world, x, y + 2, targetZ + 2, Material.COBBLESTONE, Material.STONE);
            fillColumn(world, x, y + 2, targetZ - 2, Material.COBBLESTONE, Material.STONE);
            if ((x - startX) % 7 == 0) {
                placeLantern(world, x, y + 3, targetZ + 2);
                placeLantern(world, x, y + 3, targetZ - 2);
            }
        }

        sculptIsland(world, targetX, y + 2, targetZ, 24, 19, 3, Material.STONE, Material.STONE, (island.getCenterX() * 31L) ^ island.getCenterZ() ^ 991L);

        Random random = new Random((island.getCenterX() * 31L) ^ island.getCenterZ() ^ 991L);
        for (int x = targetX - 22; x <= targetX + 22; x++) {
            for (int z = targetZ - 16; z <= targetZ + 16; z++) {
                int top = getTop(world, x, z, y, y + 20);
                if (top <= y + 2) continue;
                int maxDepth = Math.max(3, Math.min(8, top - (y + 2)));
                for (int depth = 1; depth <= maxDepth; depth++) {
                    if (random.nextInt(100) < 15) {
                        world.getBlockAt(x, top - depth, z).setType(pickOre(random));
                    }
                }
            }
        }

        int quarryX = targetX + random.nextInt(5) - 2;
        int quarryZ = targetZ + random.nextInt(5) - 2;
        int quarryTop = getTop(world, quarryX, quarryZ, y, y + 20);
        for (int x = quarryX - 7; x <= quarryX + 7; x++) {
            for (int z = quarryZ - 7; z <= quarryZ + 7; z++) {
                double nx = (x - quarryX) / 7.0D;
                double nz = (z - quarryZ) / 7.0D;
                if ((nx * nx) + (nz * nz) > 1.0D) continue;
                for (int yy = y + 4; yy <= quarryTop; yy++) {
                    world.getBlockAt(x, yy, z).setType(Material.AIR);
                }
                world.getBlockAt(x, y + 3, z).setType(Material.COBBLESTONE);
            }
        }

        int chestY = getTop(world, targetX + 14, targetZ + 11, y - 4, y + 20) + 1;
        world.getBlockAt(targetX + 14, chestY, targetZ + 11).setType(Material.CHEST);
        Chest chest = (Chest) world.getBlockAt(targetX + 14, chestY, targetZ + 11).getState();
        chest.getBlockInventory().clear();
        chest.getBlockInventory().addItem(new ItemStack(Material.IRON_PICKAXE, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.STONE_PICKAXE, 1));
        chest.getBlockInventory().addItem(new ItemStack(Material.TORCH, 32));
        chest.getBlockInventory().addItem(new ItemStack(Material.LADDER, 24));
        chest.update(true);
        island.setMiningHome(new Location(world, quarryX - 8.5D, getTop(world, quarryX - 10, quarryZ, y - 4, y + 20) + 2, quarryZ + 0.5D));
        plugin.getEconomyManager().save();
        save();
        return true;
    }

    public CoopIsland getIslandAt(Location location) {
        if (location == null || location.getWorld() == null || !location.getWorld().getName().equalsIgnoreCase(plugin.getWorldManager().getCoopWorldName())) return null;
        for (CoopIsland island : islands.values()) if (island.isInside(location)) return island;
        return null;
    }

    public boolean isCoopWorld(Location location) {
        return location != null && location.getWorld() != null && location.getWorld().getName().equalsIgnoreCase(plugin.getWorldManager().getCoopWorldName());
    }

    public Collection<CoopIsland> getPlayerIslands(UUID uuid) {
        Set<String> ids = memberships.get(uuid);
        List<CoopIsland> list = new ArrayList<CoopIsland>();
        if (ids != null) for (String id : ids) if (islands.containsKey(id)) list.add(islands.get(id));
        return list;
    }

    public CoopIsland getFirstIsland(UUID uuid) {
        Collection<CoopIsland> list = getPlayerIslands(uuid);
        return list.isEmpty() ? null : list.iterator().next();
    }

    public CoopIsland getIsland(String id) { return islands.get(id); }

    public boolean canBuild(UUID uuid, Location location) {
        CoopIsland island = getIslandAt(location);
        return island != null && island.canBuild(uuid);
    }

    public Location getSafeTeleport(CoopIsland island) {
        if (island == null) return plugin.getWorldManager().getServerSpawn();
        if (island.getHome() != null) return island.getHome();
        return new Location(plugin.getWorldManager().getOrCreateCoopWorld(), island.getCenterX() + 0.5D, plugin.getConfig().getInt("worlds.coop-y", 100) + 2, island.getCenterZ() + 0.5D);
    }

    public void setHome(CoopIsland island, Location location) { island.setHome(location); save(); }
    public void rename(CoopIsland island, String name) { island.setName(name); save(); }
}
