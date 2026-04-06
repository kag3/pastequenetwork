package fr.pasteque.skyblock;

import fr.pasteque.skyblock.announce.AnnouncementManager;
import fr.pasteque.skyblock.announce.AnnouncementListener;
import fr.pasteque.skyblock.quest.QuestManager;
import fr.pasteque.skyblock.quest.QuestListener;
import fr.pasteque.skyblock.quest.QuestCommand;
import fr.pasteque.skyblock.combatpass.CombatPassManager;
import fr.pasteque.skyblock.combatpass.CombatPassListener;
import fr.pasteque.skyblock.command.*;
import fr.pasteque.skyblock.darkauction.DarkAuctionManager;
import fr.pasteque.skyblock.darkauction.DarkAuctionListener;
import fr.pasteque.skyblock.listener.*;
import fr.pasteque.skyblock.manager.*;
import fr.pasteque.skyblock.slayer.SlayerManager;
import fr.pasteque.skyblock.slayer.SlayerListener;
import fr.pasteque.skyblock.staff.StaffModeManager;
import fr.pasteque.skyblock.staff.StaffListener;
import fr.pasteque.skyblock.skill.SkillManager;
import fr.pasteque.skyblock.skill.SkillListener;
import fr.pasteque.skyblock.collection.CollectionManager;
import fr.pasteque.skyblock.collection.CollectionListener;
import fr.pasteque.skyblock.minion.MinionManager;
import fr.pasteque.skyblock.minion.MinionListener;
import fr.pasteque.skyblock.pet.PetManager;
import fr.pasteque.skyblock.pet.PetListener;
import fr.pasteque.skyblock.guard.SanctionService;
import fr.pasteque.skyblock.guard.ReportService;
import fr.pasteque.skyblock.guard.FilterService;
import fr.pasteque.skyblock.listener.guard.GuardChatListener;
import fr.pasteque.skyblock.listener.guard.GuardConnectionListener;
import fr.pasteque.skyblock.listener.guard.GuardPanelListener;
import fr.pasteque.skyblock.playershop.PlayerShopManager;
import fr.pasteque.skyblock.arena.ArenaWorldService;
import fr.pasteque.skyblock.arena.PlayerDataService;
import fr.pasteque.skyblock.arena.ArenaKitService;
import fr.pasteque.skyblock.arena.ArenaLevelService;
import fr.pasteque.skyblock.arena.CombatTagService;
import fr.pasteque.skyblock.arena.SafeZoneService;
import fr.pasteque.skyblock.arena.SelectionService;
import fr.pasteque.skyblock.arena.EloService;
import fr.pasteque.skyblock.arena.DuelService;
import fr.pasteque.skyblock.arena.BountyService;
import fr.pasteque.skyblock.arena.KillStreakService;
import fr.pasteque.skyblock.listener.arena.ArenaCombatListener;
import fr.pasteque.skyblock.listener.arena.ArenaKitListener;
import fr.pasteque.skyblock.listener.arena.ArenaProtectionListener;
import fr.pasteque.skyblock.listener.arena.ArenaSessionListener;
import fr.pasteque.skyblock.listener.arena.ArenaGlobalChatListener;
import fr.pasteque.skyblock.listener.arena.ArenaLifecycleListener;
import fr.pasteque.skyblock.island.IslandUpgradeManager;
import fr.pasteque.skyblock.island.IslandWarpManager;
import fr.pasteque.skyblock.island.IslandPresetGui;
import fr.pasteque.skyblock.island.IslandUpgradeListener;
import fr.pasteque.skyblock.gui.ScoreboardManager;
import fr.pasteque.skyblock.gui.TabListManager;
import fr.pasteque.skyblock.gui.ScoreboardListener;
import fr.pasteque.skyblock.gui.MenuListener;
import fr.pasteque.skyblock.pvp.OldPvPListener;
import fr.pasteque.skyblock.pvp.OffhandBlocker;
import fr.pasteque.skyblock.guild.GuildManager;
import fr.pasteque.skyblock.guild.GuildCommand;
import fr.pasteque.skyblock.guild.GuildListener;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PastequeSkyblockPlugin extends JavaPlugin {

    /* ── Prefixes ── */
    private String prefix;
    private String guardPrefix;

    /* ── PastequeSkyblock managers ── */
    private WorldManager worldManager;
    private IslandManager islandManager;
    private CoopManager coopManager;
    private EconomyManager economyManager;
    private AuctionManager auctionManager;
    private ChallengeManager challengeManager;
    private ShopManager shopManager;
    private PvpManager pvpManager;
    private CombatManager combatManager;
    private SocialManager socialManager;
    private BorderManager borderManager;
    private InvasionManager invasionManager;
    private EndEventManager endEventManager;
    private ConfirmationManager confirmationManager;
    private DataFile dataFile;

    /* ── PastequeGuard services ── */
    private SanctionService sanctionService;
    private ReportService reportService;
    private FilterService filterService;

    /* ── PastequeMyLittleShop ── */
    private PlayerShopManager playerShopManager;

    /* ── PastequeSkyBlockArena services ── */
    private ArenaWorldService arenaWorldService;
    private PlayerDataService playerDataService;
    private ArenaKitService arenaKitService;
    private ArenaLevelService arenaLevelService;
    private CombatTagService combatTagService;
    private SafeZoneService safeZoneService;
    private SelectionService selectionService;

    /* ── CombatPass ── */
    private CombatPassManager combatPassManager;

    /* ── PastequeSkills ── */
    private SkillManager skillManager;

    /* ── Collections, Minions, Pets ── */
    private CollectionManager collectionManager;
    private MinionManager minionManager;
    private PetManager petManager;

    /* ── Island upgrades, warps, presets ── */
    private IslandUpgradeManager islandUpgradeManager;
    private IslandWarpManager islandWarpManager;
    private IslandPresetGui islandPresetGui;
    private IslandUpgradeListener islandUpgradeListener;

    /* ── GUI managers ── */
    private ScoreboardManager scoreboardManager;
    private TabListManager tabListManager;

    /* ── Arena PvP extensions ── */
    private EloService eloService;
    private DuelService duelService;
    private BountyService bountyService;
    private KillStreakService killStreakService;

    /* ── Dark Auction, Slayer, Staff ── */
    private DarkAuctionManager darkAuctionManager;
    private SlayerManager slayerManager;
    private StaffModeManager staffModeManager;

    /* ── Announcements ── */
    private AnnouncementManager announcementManager;

    /* ── Quests ── */
    private QuestManager questManager;

    /* ── Trade ── */
    private fr.pasteque.skyblock.trade.TradeManager tradeManager;

    /* ── Guild ── */
    private GuildManager guildManager;

    /* ── Custom Enchantments ── */
    private fr.pasteque.skyblock.enchant.EnchantManager enchantManager;

    /* ── Custom Farming ── */
    private fr.pasteque.skyblock.farming.FarmingManager farmingManager;

    /* ── Dungeons ── */
    private fr.pasteque.skyblock.dungeon.DungeonManager dungeonManager;

    /* ── Old PvP 1.8 ── */
    private OldPvPListener oldPvPListener;

    /* ── Multi-event system ── */
    private fr.pasteque.skyblock.serverevent.EventManager eventManager;

    /* ── Anti-cheat ── */
    private fr.pasteque.skyblock.anticheat.AntiCheatManager antiCheatManager;

    /* ── NPC, Leaderboards, DailyReward ── */
    private fr.pasteque.skyblock.npc.NpcManager npcManager;
    private fr.pasteque.skyblock.leaderboard.LeaderboardManager leaderboardManager;
    private fr.pasteque.skyblock.daily.DailyRewardManager dailyRewardManager;

    /* ── Island chat toggles (persisted) ── */
    private final Map<UUID, Boolean> islandChatToggles = new HashMap<UUID, Boolean>();

    // =========================================================================
    //  Lifecycle
    // =========================================================================

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.prefix = getConfig().getString("prefix", "&2Pasteque &5Skyblock &8\u00bb ");
        this.guardPrefix = getConfig().getString("guard.prefix", "&2Pasteque &5Guard &8\u00bb ");

        /* ── DataFile (shared persistence) ── */
        this.dataFile = new DataFile(this, "data.yml");
        loadIslandChatToggles();

        /* ── PastequeSkyblock init ── */
        this.worldManager = new WorldManager(this);
        worldManager.getOrCreateIslandWorld();
        worldManager.getOrCreateCoopWorld();
        this.economyManager = new EconomyManager(this);
        this.socialManager = new SocialManager(this);
        this.pvpManager = new PvpManager(this);
        this.islandManager = new IslandManager(this);
        this.coopManager = new CoopManager(this);
        this.auctionManager = new AuctionManager(this);
        this.challengeManager = new ChallengeManager(this);
        this.shopManager = new ShopManager(this);
        this.confirmationManager = new ConfirmationManager(this);
        this.borderManager = new BorderManager(this);
        this.combatManager = new CombatManager(this);
        this.invasionManager = new InvasionManager(this);
        this.endEventManager = new EndEventManager(this);

        /* ── Multi-event registry ── */
        this.eventManager = new fr.pasteque.skyblock.serverevent.EventManager(this);
        eventManager.register(new fr.pasteque.skyblock.serverevent.MeteorShowerEvent(this));
        fr.pasteque.skyblock.serverevent.KingOfTheHillEvent kothEvent =
                new fr.pasteque.skyblock.serverevent.KingOfTheHillEvent(this);
        eventManager.register(kothEvent);
        eventManager.register(new fr.pasteque.skyblock.serverevent.TreasureHuntEvent(this));
        // Build KOTH world/arena once at startup
        kothEvent.initializeWorld();

        /* ── Anti-cheat ── */
        this.antiCheatManager = new fr.pasteque.skyblock.anticheat.AntiCheatManager(this);

        /* ── NPC / Leaderboard / DailyReward init ── */
        this.npcManager = new fr.pasteque.skyblock.npc.NpcManager(this);
        this.npcManager.load();
        this.leaderboardManager = new fr.pasteque.skyblock.leaderboard.LeaderboardManager(this);
        this.leaderboardManager.load();
        this.dailyRewardManager = new fr.pasteque.skyblock.daily.DailyRewardManager(this);
        this.dailyRewardManager.load();

        /* ── PastequeGuard init ── */
        this.sanctionService = new SanctionService(this);
        this.reportService = new ReportService(this);
        this.filterService = new FilterService(this, sanctionService);

        /* ── PastequeMyLittleShop init ── */
        this.playerShopManager = new PlayerShopManager(this);
        this.playerShopManager.load();

        /* ── PastequeSkyBlockArena init ── */
        this.selectionService = new SelectionService();
        this.arenaWorldService = new ArenaWorldService(this);
        this.safeZoneService = new SafeZoneService(this, arenaWorldService);
        this.playerDataService = new PlayerDataService(this, arenaWorldService);
        this.arenaLevelService = new ArenaLevelService(this, playerDataService);
        this.combatTagService = new CombatTagService(this, playerDataService, arenaWorldService);
        this.arenaKitService = new ArenaKitService(this, arenaWorldService);

        arenaWorldService.initializeArenaWorld();
        safeZoneService.load();
        playerDataService.load();

        /* ── CombatPass init ── */
        this.combatPassManager = new CombatPassManager(this);

        /* ── PastequeSkills init ── */
        this.skillManager = new SkillManager(this);
        this.skillManager.load();

        /* ── Collections, Minions, Pets init ── */
        this.collectionManager = new CollectionManager(this);
        this.collectionManager.load();
        this.minionManager = new MinionManager(this);
        this.minionManager.load();
        this.petManager = new PetManager(this);
        this.petManager.load();

        /* ── Island upgrades, warps, presets init ── */
        this.islandUpgradeManager = new IslandUpgradeManager(this);
        this.islandWarpManager = new IslandWarpManager(this);
        this.islandPresetGui = new IslandPresetGui(this);
        this.islandUpgradeListener = new IslandUpgradeListener(this, islandUpgradeManager, islandWarpManager);

        /* ── GUI managers init ── */
        this.scoreboardManager = new ScoreboardManager(this);
        this.tabListManager = new TabListManager(this);

        /* ── Arena PvP extensions init ── */
        this.eloService = new EloService(this);
        this.eloService.load();
        this.bountyService = new BountyService(this);
        this.bountyService.load();
        this.duelService = new DuelService(this, arenaWorldService, eloService);
        this.killStreakService = new KillStreakService(this);

        /* ── Dark Auction, Slayer, Staff init ── */
        this.darkAuctionManager = new DarkAuctionManager(this);
        this.slayerManager = new SlayerManager(this);
        this.staffModeManager = new StaffModeManager(this);

        /* ── Announcements init ── */
        this.announcementManager = new AnnouncementManager(this);

        /* ── Quests init ── */
        this.questManager = new QuestManager(this);
        this.questManager.load();

        /* ── Trade init ── */
        this.tradeManager = new fr.pasteque.skyblock.trade.TradeManager(this);

        /* ── Guild init ── */
        this.guildManager = new GuildManager(this);

        /* ── Custom Enchantments init ── */
        this.enchantManager = new fr.pasteque.skyblock.enchant.EnchantManager(this);
        this.enchantManager.load();

        /* ── Custom Farming init ── */
        this.farmingManager = new fr.pasteque.skyblock.farming.FarmingManager(this);
        this.farmingManager.load();

        /* ── Dungeons init ── */
        this.dungeonManager = new fr.pasteque.skyblock.dungeon.DungeonManager(this);
        this.dungeonManager.initializeWorld();

        /* ── Commands ── */
        registerSkyblockCommands();
        registerGuardCommands();
        registerShopCommands();
        registerArenaCommands();
        registerSkillCommands();
        registerCombatPassCommands();
        registerMenuCommands();
        registerIslandUpgradeCommands();
        registerCollectionCommands();
        registerPetCommands();
        registerArenaExtCommands();
        registerDarkAuctionCommands();
        registerSlayerCommands();
        registerStaffCommands();
        registerQuestCommands();
        registerTradeCommands();
        registerGuildCommands();
        registerEnchantCommands();
        registerFarmingCommands();
        registerDungeonCommands();

        /* ── Listeners ── */
        registerSkyblockListeners();
        registerGuardListeners();
        registerShopListeners();
        registerArenaListeners();
        registerSkillListeners();
        registerCombatPassListeners();
        registerMenuListeners();
        registerIslandUpgradeListeners();
        registerCollectionListeners();
        registerMinionListeners();
        registerPetListeners();
        registerDarkAuctionListeners();
        registerSlayerListeners();
        registerStaffListeners();
        registerQuestListeners();
        registerTradeListeners();
        registerGuildListeners();
        registerEnchantListeners();
        registerFarmingListeners();
        registerDungeonListeners();

        /* ── Announcement listener ── */
        registerEvents(new AnnouncementListener(announcementManager));

        /* ── Old PvP 1.8 style (no cooldown, no offhand, custom KB) ── */
        if (getConfig().getBoolean("old-pvp.enabled", true)) {
            this.oldPvPListener = new OldPvPListener(this);
            registerEvents(oldPvPListener);
            OffhandBlocker.register(this);
            getLogger().info("[PvP] Mode PvP 1.8 active (pas de cooldown, pas d'offhand, KB custom)");
        }

        /* ── Scoreboard update task (every 3 seconds) ── */
        Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override public void run() {
                scoreboardManager.updateAll();
            }
        }, 20L * 3L, 20L * 3L);

        /* ── Scheduled tasks ── */
        coopManager.start();
        borderManager.start();
        combatManager.start();
        invasionManager.start();
        endEventManager.start();

        Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override public void run() { auctionManager.purgeExpired(); }
        }, 20L * 60L, 20L * 60L);

        startArenaActivityTicker();

        /* ── Minion ticker (every second) ── */
        minionManager.startTicking();

        /* ── Dark Auction scheduler ── */
        darkAuctionManager.schedule();

        /* ── Auto announcements (every 5 minutes) ── */
        announcementManager.startAutoAnnouncements();

        /* ── Leaderboard hologram auto-refresh (30s) ── */
        leaderboardManager.startAutoRefresh();

        /* ── Auto KOTH scheduler (Wed/Sat 16h) ── */
        new fr.pasteque.skyblock.serverevent.AutoKothScheduler(this).start();

    }

    @Override
    public void onDisable() {
        /* PastequeSkyblock save */
        islandManager.save();
        coopManager.save();
        economyManager.save();
        auctionManager.save();
        challengeManager.save();
        pvpManager.save();
        socialManager.save();
        combatManager.save();

        /* Persist island chat toggles */
        saveIslandChatToggles();

        /* Island upgrades & warps save */
        if (islandUpgradeManager != null) {
            islandUpgradeManager.save();
        }
        if (islandWarpManager != null) {
            islandWarpManager.save();
        }

        /* CombatPass save */
        if (combatPassManager != null) {
            combatPassManager.save();
        }

        /* PastequeSkills save */
        if (skillManager != null) {
            skillManager.save();
        }

        /* Collections, Minions, Pets save */
        if (collectionManager != null) {
            collectionManager.save();
        }
        if (minionManager != null) {
            minionManager.save();
        }
        if (petManager != null) {
            petManager.save();
        }

        /* ELO & Bounty save */
        if (eloService != null) {
            eloService.save();
        }
        if (bountyService != null) {
            bountyService.save();
        }

        /* Slayer save */
        if (slayerManager != null) {
            slayerManager.save();
        }

        /* Quest save */
        if (questManager != null) {
            questManager.save();
        }

        /* PastequeMyLittleShop save */
        if (playerShopManager != null) {
            playerShopManager.save();
        }

        /* PastequeSkyBlockArena save */
        safeZoneService.save();
        playerDataService.save();

        /* Guild save */
        if (guildManager != null) {
            guildManager.save();
        }

        /* Enchant save */
        if (enchantManager != null) enchantManager.save();

        /* Farming save */
        if (farmingManager != null) farmingManager.save();

        /* NPC / Leaderboard / DailyReward save */
        if (npcManager != null) npcManager.save();
        if (leaderboardManager != null) leaderboardManager.save();
        if (dailyRewardManager != null) dailyRewardManager.save();
    }

    // =========================================================================
    //  Command registration
    // =========================================================================

    private void registerSkyblockCommands() {
        IslandCommand islandCommand = new IslandCommand(this);
        MoneyCommand moneyCommand = new MoneyCommand(this);
        SocialCommand socialCommand = new SocialCommand(this);

        getCommand("is").setExecutor(islandCommand);
        getCommand("iscoop").setExecutor(new CoopCommand(this));
        getCommand("hdv").setExecutor(new HdvCommand(this));
        getCommand("money").setExecutor(moneyCommand);
        getCommand("pay").setExecutor(moneyCommand);
        getCommand("psky").setExecutor(new PSkyAdminCommand(this));
        getCommand("friends").setExecutor(socialCommand);
        getCommand("enemy").setExecutor(socialCommand);
        getCommand("alliance").setExecutor(socialCommand);
        getCommand("pvpwarp").setExecutor(new PvpWarpCommand(this));
        getCommand("endevent").setExecutor(new EndEventCommand(this));
        getCommand("aend").setExecutor(new EndEventCommand(this));
        getCommand("aevent").setExecutor(new ServerEventCommand(this));
        getCommand("koth").setExecutor(new KothCommand(this));
        getCommand("npc").setExecutor(new NpcCommand(this, npcManager));
        getCommand("minions").setExecutor(new MinionsCommand(this));
        getCommand("top").setExecutor(new TopCommand(this, leaderboardManager));
        getCommand("dailychest").setExecutor(new DailyChestCommand(this, dailyRewardManager));
    }

    private void registerGuardCommands() {
        GuardCommand pgCommand = new GuardCommand(this, sanctionService, reportService, filterService);
        PluginCommand command = getCommand("pg");
        if (command != null) {
            command.setExecutor(pgCommand);
            command.setTabCompleter(pgCommand);
        }
    }

    private void registerShopCommands() {
        MyShopCommand myShopCommand = new MyShopCommand(this, playerShopManager);
        bind("myshop", myShopCommand);

        MyShopAdminCommand adminCommand = new MyShopAdminCommand(this, playerShopManager);
        bind("myshopadmin", adminCommand);
    }

    private void registerCombatPassCommands() {
        CombatPassCommand passCommand = new CombatPassCommand(this, combatPassManager);
        bind("combatpass", passCommand);
    }

    private void registerMenuCommands() {
        bind("menu", new MenuCommand(this));
    }

    private void registerIslandUpgradeCommands() {
        bind("isupgrade", new IslandUpgradeCommand(this, islandUpgradeManager));
        bind("iswarp", new IslandWarpCommand(this, islandWarpManager));
    }

    private void registerSkillCommands() {
        bind("skills", new SkillsCommand(skillManager));
    }

    private void registerCollectionCommands() {
        bind("collection", new CollectionCommand(collectionManager));
    }

    private void registerPetCommands() {
        bind("pet", new PetCommand(petManager));
    }

    private void registerArenaExtCommands() {
        bind("duel", new DuelCommand(this, duelService));
        bind("bounty", new BountyCommand(this, bountyService));
        bind("elo", new EloCommand(this, eloService));
    }

    private void registerDarkAuctionCommands() {
        bind("darkauction", new DarkAuctionCommand(this, darkAuctionManager));
    }

    private void registerSlayerCommands() {
        bind("slayer", new SlayerCommand(this, slayerManager));
    }

    private void registerStaffCommands() {
        bind("staff", new StaffCommand(this, staffModeManager));
    }

    private void registerQuestCommands() {
        bind("quest", new QuestCommand(this, questManager));
    }

    private void registerTradeCommands() {
        bind("trade", new fr.pasteque.skyblock.trade.TradeCommand(this, tradeManager));
    }

    private void registerGuildCommands() {
        bind("guild", new GuildCommand(this, guildManager));
    }

    private void registerEnchantCommands() {
        bind("enchant", new fr.pasteque.skyblock.enchant.EnchantCommand(this, enchantManager));
    }

    private void registerFarmingCommands() {
        bind("farming", new fr.pasteque.skyblock.farming.FarmingCommand(this, farmingManager));
    }

    private void registerDungeonCommands() {
        bind("dungeon", new fr.pasteque.skyblock.dungeon.DungeonCommand(this, dungeonManager));
    }

    private void registerArenaCommands() {
        ArenaCommand arenaCommand = new ArenaCommand(this, arenaWorldService);
        ArenaLevelCommand levelCommand = new ArenaLevelCommand(this, playerDataService, arenaLevelService);
        ArenaKitCommand kitCommand = new ArenaKitCommand(this, arenaKitService);
        ArenaAdminCommand adminCommand = new ArenaAdminCommand(this, arenaWorldService, safeZoneService, selectionService, playerDataService);

        bind("arena", arenaCommand);
        bind("arenalevel", levelCommand);
        bind("arenakit", kitCommand);
        bind("arenaadmin", adminCommand);
    }

    private void bind(String name, Object executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            return;
        }
        if (executor instanceof org.bukkit.command.CommandExecutor) {
            command.setExecutor((org.bukkit.command.CommandExecutor) executor);
        }
        if (executor instanceof org.bukkit.command.TabCompleter) {
            command.setTabCompleter((org.bukkit.command.TabCompleter) executor);
        }
    }

    // =========================================================================
    //  Listener registration
    // =========================================================================

    private void registerSkyblockListeners() {
        registerEvents(new ProtectionListener(this));
        registerEvents(new ConnectionListener(this));
        registerEvents(new ChatListener(this));
        registerEvents(new ShopListener(this));
        registerEvents(new GuiListener(this));
        registerEvents(new PvpListener(this));
        registerEvents(new EmbassyListener(this));
        registerEvents(new MiscServerListener(this));
        registerEvents(new EndEventListener(this));
        registerEvents(new fr.pasteque.skyblock.serverevent.ServerEventListener(this, eventManager));
        registerEvents(new fr.pasteque.skyblock.anticheat.AntiCheatListener(this, antiCheatManager));
        registerEvents(new fr.pasteque.skyblock.npc.NpcListener(this, npcManager));
        registerEvents(new fr.pasteque.skyblock.daily.DailyRewardListener(this, dailyRewardManager));
    }

    private void registerGuardListeners() {
        registerEvents(new GuardChatListener(this, sanctionService, filterService));
        registerEvents(new GuardConnectionListener(this, sanctionService));
        registerEvents(new GuardPanelListener(this, reportService));
    }

    private void registerShopListeners() {
        registerEvents(new PlayerShopListener(this, playerShopManager));
    }

    private void registerCombatPassListeners() {
        registerEvents(new CombatPassListener(this, combatPassManager));
    }

    private void registerMenuListeners() {
        registerEvents(new MenuListener());
        registerEvents(new ScoreboardListener(this, scoreboardManager, tabListManager));
    }

    private void registerIslandUpgradeListeners() {
        registerEvents(islandUpgradeListener);
    }

    private void registerSkillListeners() {
        registerEvents(new SkillListener(skillManager));
    }

    private void registerCollectionListeners() {
        registerEvents(new CollectionListener(collectionManager));
    }

    private void registerMinionListeners() {
        registerEvents(new MinionListener(minionManager));
    }

    private void registerPetListeners() {
        registerEvents(new PetListener(petManager));
    }

    private void registerDarkAuctionListeners() {
        registerEvents(new DarkAuctionListener(this, darkAuctionManager));
    }

    private void registerSlayerListeners() {
        registerEvents(new SlayerListener(this, slayerManager));
    }

    private void registerStaffListeners() {
        registerEvents(new StaffListener(this, staffModeManager));
    }

    private void registerQuestListeners() {
        registerEvents(new QuestListener(this, questManager));
    }

    private void registerTradeListeners() {
        registerEvents(new fr.pasteque.skyblock.trade.TradeListener(this, tradeManager));
    }

    private void registerGuildListeners() {
        registerEvents(new GuildListener(this, guildManager));
    }

    private void registerEnchantListeners() {
        registerEvents(new fr.pasteque.skyblock.enchant.EnchantListener(this));
        registerEvents(new fr.pasteque.skyblock.enchant.EnchantTableListener(this, enchantManager));
    }

    private void registerFarmingListeners() {
        registerEvents(new fr.pasteque.skyblock.farming.FarmingListener(this, farmingManager));
    }

    private void registerDungeonListeners() {
        registerEvents(new fr.pasteque.skyblock.dungeon.DungeonListener(this, dungeonManager));
    }

    private void registerArenaListeners() {
        registerEvents(new ArenaProtectionListener(this, arenaWorldService, safeZoneService));
        registerEvents(new ArenaCombatListener(this, arenaWorldService, safeZoneService, combatTagService, arenaLevelService, playerDataService, duelService));
        registerEvents(new ArenaLifecycleListener(this, arenaWorldService, combatTagService, playerDataService));
        registerEvents(new ArenaKitListener(this, arenaKitService));
        registerEvents(new ArenaSessionListener(this, arenaWorldService, arenaLevelService, playerDataService));
        registerEvents(new ArenaGlobalChatListener(this, arenaWorldService, playerDataService));
    }

    private void registerEvents(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, this);
    }

    // =========================================================================
    //  Arena activity ticker
    // =========================================================================

    private void startArenaActivityTicker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                playerDataService.tickSessions();
                arenaLevelService.grantActivityXp();
                combatTagService.tick();
            }
        }.runTaskTimer(this, 20L, 20L);
    }

    // =========================================================================
    //  Island chat persistence (FIX: was volatile before)
    // =========================================================================

    private void loadIslandChatToggles() {
        if (dataFile.getConfig().isConfigurationSection("island-chat-toggles")) {
            for (String key : dataFile.getConfig().getConfigurationSection("island-chat-toggles").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    boolean enabled = dataFile.getConfig().getBoolean("island-chat-toggles." + key);
                    islandChatToggles.put(uuid, enabled);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    private void saveIslandChatToggles() {
        dataFile.getConfig().set("island-chat-toggles", null);
        for (Map.Entry<UUID, Boolean> entry : islandChatToggles.entrySet()) {
            dataFile.getConfig().set("island-chat-toggles." + entry.getKey().toString(), entry.getValue());
        }
        dataFile.save();
    }

    public boolean isIslandChatEnabled(UUID uuid) {
        return islandChatToggles.containsKey(uuid) && islandChatToggles.get(uuid);
    }

    public void toggleIslandChat(UUID uuid) {
        islandChatToggles.put(uuid, !isIslandChatEnabled(uuid));
    }

    // =========================================================================
    //  Utility methods
    // =========================================================================

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    public String getPrefix() {
        return color(prefix);
    }

    public String getGuardPrefix() {
        return color(guardPrefix);
    }

    // =========================================================================
    //  PastequeSkyblock getters
    // =========================================================================

    public WorldManager getWorldManager() { return worldManager; }
    public IslandManager getIslandManager() { return islandManager; }
    public CoopManager getCoopManager() { return coopManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public AuctionManager getAuctionManager() { return auctionManager; }
    public ChallengeManager getChallengeManager() { return challengeManager; }
    public PvpManager getPvpManager() { return pvpManager; }
    public ShopManager getShopManager() { return shopManager; }
    public ConfirmationManager getConfirmationManager() { return confirmationManager; }
    public BorderManager getBorderManager() { return borderManager; }
    public CombatManager getCombatManager() { return combatManager; }
    public SocialManager getSocialManager() { return socialManager; }
    public InvasionManager getInvasionManager() { return invasionManager; }
    public EndEventManager getEndEventManager() { return endEventManager; }
    public DataFile getDataFile() { return dataFile; }
    public fr.pasteque.skyblock.serverevent.EventManager getEventManager() { return eventManager; }
    public fr.pasteque.skyblock.anticheat.AntiCheatManager getAntiCheatManager() { return antiCheatManager; }
    public fr.pasteque.skyblock.npc.NpcManager getNpcManager() { return npcManager; }
    public fr.pasteque.skyblock.leaderboard.LeaderboardManager getLeaderboardManager() { return leaderboardManager; }
    public fr.pasteque.skyblock.daily.DailyRewardManager getDailyRewardManager() { return dailyRewardManager; }

    // =========================================================================
    //  PastequeGuard getters
    // =========================================================================

    public SanctionService getSanctionService() { return sanctionService; }
    public ReportService getReportService() { return reportService; }
    public FilterService getFilterService() { return filterService; }

    // =========================================================================
    //  PastequeMyLittleShop getters
    // =========================================================================

    public PlayerShopManager getPlayerShopManager() { return playerShopManager; }

    // =========================================================================
    //  PastequeSkyBlockArena getters
    // =========================================================================

    public ArenaWorldService getArenaWorldService() { return arenaWorldService; }
    public PlayerDataService getPlayerDataService() { return playerDataService; }
    public ArenaKitService getArenaKitService() { return arenaKitService; }
    public ArenaLevelService getArenaLevelService() { return arenaLevelService; }
    public CombatTagService getCombatTagService() { return combatTagService; }
    public SafeZoneService getSafeZoneService() { return safeZoneService; }
    public SelectionService getSelectionService() { return selectionService; }

    // =========================================================================
    //  PastequeSkills getters
    // =========================================================================

    public SkillManager getSkillManager() { return skillManager; }

    // =========================================================================
    //  CombatPass getters
    // =========================================================================

    public CombatPassManager getCombatPassManager() { return combatPassManager; }

    // =========================================================================
    //  GUI getters
    // =========================================================================

    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public TabListManager getTabListManager() { return tabListManager; }

    // =========================================================================
    //  Island upgrades, warps, presets getters
    // =========================================================================

    public IslandUpgradeManager getIslandUpgradeManager() { return islandUpgradeManager; }
    public IslandWarpManager getIslandWarpManager() { return islandWarpManager; }
    public IslandPresetGui getIslandPresetGui() { return islandPresetGui; }
    public IslandUpgradeListener getIslandUpgradeListener() { return islandUpgradeListener; }

    // =========================================================================
    //  Collections, Minions, Pets getters
    // =========================================================================

    public CollectionManager getCollectionManager() { return collectionManager; }
    public MinionManager getMinionManager() { return minionManager; }
    public PetManager getPetManager() { return petManager; }

    // =========================================================================
    //  Dark Auction, Slayer, Staff getters
    // =========================================================================

    public EloService getEloService() { return eloService; }
    public DuelService getDuelService() { return duelService; }
    public BountyService getBountyService() { return bountyService; }
    public KillStreakService getKillStreakService() { return killStreakService; }
    public DarkAuctionManager getDarkAuctionManager() { return darkAuctionManager; }
    public SlayerManager getSlayerManager() { return slayerManager; }
    public StaffModeManager getStaffModeManager() { return staffModeManager; }

    // =========================================================================
    //  Announcements getter
    // =========================================================================

    public AnnouncementManager getAnnouncementManager() { return announcementManager; }

    // =========================================================================
    //  Quests getter
    // =========================================================================

    public QuestManager getQuestManager() { return questManager; }

    // =========================================================================
    //  Guild getter
    // =========================================================================

    public GuildManager getGuildManager() { return guildManager; }

    // =========================================================================
    //  Enchant, Farming, Dungeon getters
    // =========================================================================

    public fr.pasteque.skyblock.enchant.EnchantManager getEnchantManager() { return enchantManager; }
    public fr.pasteque.skyblock.farming.FarmingManager getFarmingManager() { return farmingManager; }
    public fr.pasteque.skyblock.dungeon.DungeonManager getDungeonManager() { return dungeonManager; }
}
