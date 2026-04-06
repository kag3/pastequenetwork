package fr.pasteque.skyblock.dungeon;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.generator.EmptyChunkGenerator;
import fr.pasteque.skyblock.gui.GuiHelper;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class DungeonManager {

    private static final int INSTANCE_SPACING = 200;
    private static final String WORLD_NAME = "skyblockdungeons";

    private final PastequeSkyblockPlugin plugin;
    private World dungeonWorld;

    private final Map<String, DungeonInstance> instances = new HashMap<String, DungeonInstance>();
    private final Map<UUID, String> playerInstance = new HashMap<UUID, String>();
    private final Map<UUID, DungeonParty> parties = new HashMap<UUID, DungeonParty>();
    private final Map<UUID, UUID> partyInvites = new HashMap<UUID, UUID>(); // target → leader
    private final List<Integer> freeSlots = new ArrayList<Integer>();
    private int nextSlot = 0;
    private int taskId = -1;
    private final Random random = new Random();

    public DungeonManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    // =========================================================================
    //  World init
    // =========================================================================

    public void initializeWorld() {
        World w = Bukkit.getWorld(WORLD_NAME);
        if (w == null) {
            WorldCreator creator = new WorldCreator(WORLD_NAME);
            creator.type(WorldType.NORMAL);
            creator.generator(new EmptyChunkGenerator());
            w = creator.createWorld();
        }
        if (w == null) {
            plugin.getLogger().warning("[Dungeon] Impossible de creer le monde " + WORLD_NAME);
            return;
        }
        this.dungeonWorld = w;
        w.setStorm(false);
        w.setThundering(false);
        w.setWeatherDuration(Integer.MAX_VALUE);
        w.setDifficulty(Difficulty.HARD);
        w.setPVP(false);
        w.setGameRuleValue("doMobSpawning", "false");
        w.setGameRuleValue("doFireTick", "false");
        w.setGameRuleValue("mobGriefing", "false");
        w.setGameRuleValue("doDaylightCycle", "false");
        w.setGameRuleValue("naturalRegeneration", "true");

        // Start dungeon ticker
        startTicker();
        plugin.getLogger().info("[Dungeon] Monde " + WORLD_NAME + " initialise");
    }

    // =========================================================================
    //  Party management
    // =========================================================================

    public DungeonParty getParty(UUID player) {
        return parties.get(player);
    }

    public DungeonParty getOrCreateParty(UUID leader) {
        DungeonParty party = parties.get(leader);
        if (party == null) {
            party = new DungeonParty(leader);
            parties.put(leader, party);
        }
        return party;
    }

    public DungeonParty findPartyOf(UUID player) {
        for (DungeonParty party : parties.values()) {
            if (party.isMember(player)) {
                return party;
            }
        }
        return null;
    }

    public void inviteToParty(Player sender, Player target) {
        DungeonParty party = getOrCreateParty(sender.getUniqueId());
        if (!party.getLeader().equals(sender.getUniqueId())) {
            sender.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cSeul le leader peut inviter."));
            return;
        }
        if (!party.invite(target.getUniqueId())) {
            sender.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cImpossible d'inviter (deja dans le groupe ou groupe plein)."));
            return;
        }
        partyInvites.put(target.getUniqueId(), sender.getUniqueId());
        target.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&a" + sender.getName() + " &7t'invite a rejoindre son groupe donjon !"));
        target.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&e/dungeon party accept &7pour accepter"));
        sender.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&7Invitation envoyee a &a" + target.getName()));
    }

    public void acceptPartyInvite(Player target) {
        UUID leaderId = partyInvites.remove(target.getUniqueId());
        if (leaderId == null) {
            target.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cAucune invitation en attente."));
            return;
        }
        DungeonParty party = parties.get(leaderId);
        if (party == null || !party.accept(target.getUniqueId())) {
            target.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cInvitation expiree ou groupe plein."));
            return;
        }
        // Map target to this party's leader
        parties.put(target.getUniqueId(), party);
        party.broadcast(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&a" + target.getName() + " &7a rejoint le groupe !"));
    }

    public void leaveParty(Player player) {
        DungeonParty party = findPartyOf(player.getUniqueId());
        if (party == null) {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cVous n'etes dans aucun groupe."));
            return;
        }
        parties.remove(player.getUniqueId());
        if (!party.leave(player.getUniqueId())) {
            // Party disbanded
            for (UUID m : new HashSet<UUID>(party.getMembers())) {
                parties.remove(m);
            }
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&7Groupe dissous."));
        } else {
            party.broadcast(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&c" + player.getName() + " &7a quitte le groupe."));
        }
    }

    // =========================================================================
    //  Instance creation
    // =========================================================================

    public String getPlayerInstanceId(UUID player) {
        return playerInstance.get(player);
    }

    public DungeonInstance getPlayerInstance(UUID player) {
        String id = playerInstance.get(player);
        return id != null ? instances.get(id) : null;
    }

    public DungeonInstance createInstance(DungeonType type, List<Player> partyMembers) {
        if (dungeonWorld == null) {
            initializeWorld();
        }
        if (dungeonWorld == null) return null;

        int slot;
        if (!freeSlots.isEmpty()) {
            slot = freeSlots.remove(freeSlots.size() - 1);
        } else {
            slot = nextSlot++;
        }

        int originX = slot * INSTANCE_SPACING;
        Location origin = new Location(dungeonWorld, originX, 80, 0);

        String instanceId = UUID.randomUUID().toString().substring(0, 8);
        Set<UUID> playerIds = new HashSet<UUID>();
        for (Player p : partyMembers) {
            playerIds.add(p.getUniqueId());
        }

        DungeonInstance instance = new DungeonInstance(instanceId, type, playerIds,
                partyMembers.get(0).getUniqueId(), origin, slot);

        instances.put(instanceId, instance);
        for (UUID pid : playerIds) {
            playerInstance.put(pid, instanceId);
        }

        // Build the dungeon
        buildDungeon(type, origin);

        // Teleport players
        Location spawn = instance.getSpawnPoint();
        for (Player p : partyMembers) {
            p.teleport(spawn);
            p.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&5&lDONJON &8- &d" + type.getDisplayName()));
            p.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&7Tuez tous les mobs pour progresser. &c3 vies par joueur."));
            GuiHelper.playOpen(p);
        }

        // Start first wave
        instance.setState(DungeonInstance.DungeonState.ACTIVE);
        instance.setStartedAt(System.currentTimeMillis());
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                startWave(instance);
            }
        }, 60L); // 3 second delay

        return instance;
    }

    // =========================================================================
    //  Arena building
    // =========================================================================

    private void buildDungeon(DungeonType type, Location origin) {
        World w = origin.getWorld();
        int ox = origin.getBlockX();
        int oy = origin.getBlockY();
        int oz = origin.getBlockZ();

        Material floor, wall;
        switch (type) {
            case NETHER_FORTRESS:
                floor = Material.NETHER_BRICK;
                wall = Material.NETHER_BRICK;
                break;
            case ENDER_SANCTUM:
                floor = Material.ENDER_STONE;
                wall = Material.ENDER_STONE;
                break;
            default: // CRYPT
                floor = Material.MOSSY_COBBLESTONE;
                wall = Material.SMOOTH_BRICK;
                break;
        }

        // 31 wide (x: 0-30), 31 long (z: 0-30), 8 high
        for (int x = 0; x <= 30; x++) {
            for (int z = 0; z <= 30; z++) {
                // Floor
                w.getBlockAt(ox + x, oy, oz + z).setType(floor);
                // Ceiling
                w.getBlockAt(ox + x, oy + 7, oz + z).setType(wall);

                // Walls
                if (x == 0 || x == 30 || z == 0 || z == 30) {
                    for (int y = 1; y <= 6; y++) {
                        w.getBlockAt(ox + x, oy + y, oz + z).setType(wall);
                    }
                }
            }
        }

        // Room dividers at z=6, z=12, z=18, z=24 with doorways at x=14-16
        int[] dividers = {6, 12, 18, 24};
        for (int dz : dividers) {
            for (int x = 1; x <= 29; x++) {
                for (int y = 1; y <= 6; y++) {
                    if (x >= 14 && x <= 16 && y <= 3) continue; // doorway
                    w.getBlockAt(ox + x, oy + y, oz + dz).setType(wall);
                }
            }
        }

        // Place torches for lighting
        for (int z = 3; z <= 27; z += 6) {
            w.getBlockAt(ox + 1, oy + 3, oz + z).setType(Material.TORCH);
            w.getBlockAt(ox + 29, oy + 3, oz + z).setType(Material.TORCH);
        }

        // Type-specific decorations
        if (type == DungeonType.NETHER_FORTRESS) {
            // Lava pools
            w.getBlockAt(ox + 5, oy, oz + 9).setType(Material.LAVA);
            w.getBlockAt(ox + 25, oy, oz + 9).setType(Material.LAVA);
            w.getBlockAt(ox + 5, oy, oz + 21).setType(Material.LAVA);
            w.getBlockAt(ox + 25, oy, oz + 21).setType(Material.LAVA);
        } else if (type == DungeonType.ENDER_SANCTUM) {
            // End rod pillars
            for (int y = 1; y <= 4; y++) {
                w.getBlockAt(ox + 5, oy + y, oz + 9).setType(Material.OBSIDIAN);
                w.getBlockAt(ox + 25, oy + y, oz + 9).setType(Material.OBSIDIAN);
                w.getBlockAt(ox + 5, oy + y, oz + 21).setType(Material.OBSIDIAN);
                w.getBlockAt(ox + 25, oy + y, oz + 21).setType(Material.OBSIDIAN);
            }
        }
    }

    // =========================================================================
    //  Wave management
    // =========================================================================

    public void startWave(DungeonInstance instance) {
        if (instance.getState() == DungeonInstance.DungeonState.COMPLETED ||
                instance.getState() == DungeonInstance.DungeonState.FAILED) return;

        int wave = instance.getCurrentWave() + 1;
        instance.setCurrentWave(wave);

        int playerCount = instance.getPlayers().size();
        EntityType[] mobTypes = instance.getType().getMobTypes();
        Location spawnLoc = instance.getWaveSpawnLocation(wave);

        if (wave <= 3) {
            // Regular waves
            int mobCount = (wave == 1 ? 5 : wave == 2 ? 8 : 10) + playerCount;
            instance.setMobsRemaining(mobCount);
            instance.setState(DungeonInstance.DungeonState.ACTIVE);

            broadcastToInstance(instance, "&5&lVague " + wave + "/4 &8- &d" + mobCount + " mobs !");

            for (int i = 0; i < mobCount; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 6;
                double offsetZ = (random.nextDouble() - 0.5) * 4;
                Location mobLoc = spawnLoc.clone().add(offsetX, 0, offsetZ);
                EntityType t = mobTypes[random.nextInt(mobTypes.length)];
                spawnLoc.getWorld().spawnEntity(mobLoc, t);
            }
        } else {
            // Boss wave
            instance.setState(DungeonInstance.DungeonState.BOSS);
            instance.setMobsRemaining(4); // boss + 3 minions

            broadcastToInstance(instance, "&c&l\u2620 BOSS &8- &4" + instance.getType().getBossName() + " &c&l\u2620");

            // Spawn boss
            Entity boss = spawnLoc.getWorld().spawnEntity(spawnLoc, instance.getType().getBossType());
            if (boss instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) boss;
                living.setCustomName(PastequeSkyblockPlugin.color(instance.getType().getBossName()));
                living.setCustomNameVisible(true);
                living.setMaxHealth(living.getMaxHealth() * 5);
                living.setHealth(living.getMaxHealth());
            }

            // Spawn 3 minions
            for (int i = 0; i < 3; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 4;
                double offsetZ = (random.nextDouble() - 0.5) * 4;
                Location mobLoc = spawnLoc.clone().add(offsetX, 0, offsetZ);
                EntityType t = mobTypes[random.nextInt(mobTypes.length)];
                spawnLoc.getWorld().spawnEntity(mobLoc, t);
            }
        }
    }

    // =========================================================================
    //  Mob kill tracking
    // =========================================================================

    public void onMobKill(DungeonInstance instance, Player killer) {
        int remaining = instance.getMobsRemaining() - 1;
        instance.setMobsRemaining(remaining);
        instance.incrementTotalKills();

        if (remaining <= 0) {
            if (instance.getCurrentWave() >= 4) {
                completeDungeon(instance);
            } else {
                broadcastToInstance(instance, "&a&lVague terminee ! &7Prochaine dans 5 secondes...");
                // Next wave after 5 seconds
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() {
                        startWave(instance);
                    }
                }, 100L);
            }
        }
    }

    // =========================================================================
    //  Completion / Failure
    // =========================================================================

    public void completeDungeon(DungeonInstance instance) {
        instance.setState(DungeonInstance.DungeonState.COMPLETED);

        DungeonType type = instance.getType();
        double reward = type.getMinReward() + random.nextDouble() * (type.getMaxReward() - type.getMinReward());
        int rewardInt = (int) reward;

        broadcastToInstance(instance, "&a&l\u2605 DONJON TERMINE ! \u2605");
        broadcastToInstance(instance, "&7Recompense: &a+" + rewardInt + " Pasteques &7+ loot !");

        Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation();

        for (UUID pid : instance.getPlayers()) {
            Player p = Bukkit.getPlayer(pid);
            if (p != null && p.isOnline()) {
                plugin.getEconomyManager().add(pid, rewardInt);
                giveLoot(p, type);
                GuiHelper.playSuccess(p);

                // TP back after 3 seconds
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() {
                        if (p.isOnline()) p.teleport(spawn);
                    }
                }, 60L);
            }
            playerInstance.remove(pid);
        }

        // Cleanup after 5 seconds
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                cleanupInstance(instance);
            }
        }, 100L);
    }

    public void failDungeon(DungeonInstance instance) {
        instance.setState(DungeonInstance.DungeonState.FAILED);

        broadcastToInstance(instance, "&c&l\u2716 TEMPS ECOULE ! Donjon echoue.");

        Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation();
        for (UUID pid : instance.getPlayers()) {
            Player p = Bukkit.getPlayer(pid);
            if (p != null && p.isOnline()) {
                GuiHelper.playDeny(p);
                p.teleport(spawn);
            }
            playerInstance.remove(pid);
        }

        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                cleanupInstance(instance);
            }
        }, 40L);
    }

    private void giveLoot(Player player, DungeonType type) {
        // Random loot based on difficulty
        ItemStack[] lootTable;
        switch (type) {
            case NETHER_FORTRESS:
                lootTable = new ItemStack[]{
                        new ItemStack(Material.DIAMOND, 2 + random.nextInt(3)),
                        new ItemStack(Material.GOLD_INGOT, 8 + random.nextInt(8)),
                        new ItemStack(Material.BLAZE_ROD, 4 + random.nextInt(4))
                };
                break;
            case ENDER_SANCTUM:
                lootTable = new ItemStack[]{
                        new ItemStack(Material.DIAMOND, 5 + random.nextInt(5)),
                        new ItemStack(Material.EMERALD, 8 + random.nextInt(8)),
                        new ItemStack(Material.GOLDEN_APPLE, 2 + random.nextInt(2)),
                        new ItemStack(Material.ENDER_PEARL, 4 + random.nextInt(4))
                };
                break;
            default: // CRYPT
                lootTable = new ItemStack[]{
                        new ItemStack(Material.IRON_INGOT, 8 + random.nextInt(8)),
                        new ItemStack(Material.GOLD_INGOT, 4 + random.nextInt(4)),
                        new ItemStack(Material.DIAMOND, 1 + random.nextInt(2))
                };
                break;
        }

        for (ItemStack item : lootTable) {
            if (random.nextDouble() < 0.7) { // 70% chance per item
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                for (ItemStack drop : overflow.values()) {
                    player.getWorld().dropItem(player.getLocation(), drop);
                }
            }
        }
    }

    // =========================================================================
    //  Cleanup
    // =========================================================================

    private void cleanupInstance(DungeonInstance instance) {
        if (dungeonWorld == null) return;
        Location origin = instance.getOrigin();
        int ox = origin.getBlockX();
        int oy = origin.getBlockY();
        int oz = origin.getBlockZ();

        // Clear blocks
        for (int x = 0; x <= 30; x++) {
            for (int z = 0; z <= 30; z++) {
                for (int y = 0; y <= 8; y++) {
                    dungeonWorld.getBlockAt(ox + x, oy + y, oz + z).setType(Material.AIR);
                }
            }
        }

        // Remove entities in area
        for (Entity entity : dungeonWorld.getEntities()) {
            if (entity instanceof Player) continue;
            Location loc = entity.getLocation();
            if (loc.getBlockX() >= ox && loc.getBlockX() <= ox + 30 &&
                    loc.getBlockZ() >= oz && loc.getBlockZ() <= oz + 30) {
                entity.remove();
            }
        }

        // Free the slot
        freeSlots.add(instance.getSlotIndex());
        instances.remove(instance.getInstanceId());

        plugin.getLogger().info("[Dungeon] Instance " + instance.getInstanceId() + " nettoyee (slot " + instance.getSlotIndex() + " libere)");
    }

    // =========================================================================
    //  Ticker
    // =========================================================================

    private void startTicker() {
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                for (DungeonInstance inst : new ArrayList<DungeonInstance>(instances.values())) {
                    if (inst.getState() == DungeonInstance.DungeonState.ACTIVE ||
                            inst.getState() == DungeonInstance.DungeonState.BOSS) {
                        if (inst.isExpired()) {
                            failDungeon(inst);
                        }
                    }
                }
            }
        }, 20L, 20L).getTaskId();
    }

    // =========================================================================
    //  GUI
    // =========================================================================

    public void openDungeonGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54,
                PastequeSkyblockPlugin.color("&2&lPasteque &5&lDonjons"));

        // Row 1: 3 dungeon types
        for (DungeonType type : DungeonType.values()) {
            int slot = type.ordinal() == 0 ? 11 : type.ordinal() == 1 ? 13 : 15;
            inv.setItem(slot, GuiHelper.fluidItem(type.getIcon(),
                    "&d&l" + type.getDisplayName(),
                    "Difficulte: " + PastequeSkyblockPlugin.color(type.getDifficultyLabel()),
                    new String[]{
                            "Joueurs: " + type.getMinPlayers() + "-" + type.getMaxPlayers(),
                            "Duree: " + type.getTimerMinutes() + " minutes",
                            "Recompense: " + (int) type.getMinReward() + "-" + (int) type.getMaxReward() + " Pasteques"
                    },
                    "Clic pour lancer ce donjon"));
        }

        // Row 3: party info
        DungeonParty party = findPartyOf(player.getUniqueId());
        if (party != null) {
            StringBuilder memberNames = new StringBuilder();
            for (UUID mid : party.getMembers()) {
                Player p = Bukkit.getPlayer(mid);
                if (p != null) {
                    if (memberNames.length() > 0) memberNames.append(", ");
                    memberNames.append(p.getName());
                    if (mid.equals(party.getLeader())) memberNames.append(" &6[Chef]");
                }
            }
            inv.setItem(31, GuiHelper.createItem(Material.SKULL_ITEM, 3,
                    "&a&lVotre Groupe",
                    "&7Membres: &f" + party.size() + "/5",
                    "&7" + memberNames.toString()));
        } else {
            inv.setItem(31, GuiHelper.createItem(Material.SKULL_ITEM, 3,
                    "&7&lPas de groupe",
                    "&7Creez un groupe avec",
                    "&e/dungeon party invite <joueur>"));
        }

        inv.setItem(49, GuiHelper.closeButton());
        GuiHelper.decorate(inv, GuiHelper.Theme.PVP);
        player.openInventory(inv);
        GuiHelper.playOpen(player);
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    private void broadcastToInstance(DungeonInstance instance, String message) {
        String formatted = PastequeSkyblockPlugin.color(plugin.getPrefix() + message);
        for (UUID pid : instance.getPlayers()) {
            Player p = Bukkit.getPlayer(pid);
            if (p != null && p.isOnline()) {
                p.sendMessage(formatted);
            }
        }
    }

    public World getDungeonWorld() {
        return dungeonWorld;
    }

    /** Scoreboard integration helper */
    public String getTypeName(DungeonInstance inst) {
        return inst.getType().getDisplayName();
    }
}
