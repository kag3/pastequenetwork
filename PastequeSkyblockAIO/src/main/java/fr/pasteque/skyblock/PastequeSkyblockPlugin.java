package fr.pasteque.skyblock;

import fr.pasteque.skyblock.command.*;
import fr.pasteque.skyblock.listener.*;
import fr.pasteque.skyblock.manager.*;
import fr.pasteque.skyblock.guard.service.SanctionService;
import fr.pasteque.skyblock.guard.service.ReportService;
import fr.pasteque.skyblock.guard.service.FilterService;
import fr.pasteque.skyblock.guard.command.PgCommand;
import fr.pasteque.skyblock.guard.listener.GuardChatListener;
import fr.pasteque.skyblock.guard.listener.GuardConnectionListener;
import fr.pasteque.skyblock.guard.listener.PanelListener;
import fr.pasteque.skyblock.playershop.PlayerShopManager;
import fr.pasteque.skyblock.arena.service.ArenaWorldService;
import fr.pasteque.skyblock.arena.service.PlayerDataService;
import fr.pasteque.skyblock.arena.service.ArenaKitService;
import fr.pasteque.skyblock.arena.service.ArenaLevelService;
import fr.pasteque.skyblock.arena.service.CombatTagService;
import fr.pasteque.skyblock.arena.service.SafeZoneService;
import fr.pasteque.skyblock.arena.service.SelectionService;
import fr.pasteque.skyblock.arena.service.EconomyBridge;
import fr.pasteque.skyblock.arena.command.ArenaCommand;
import fr.pasteque.skyblock.arena.command.ArenaKitCommand;
import fr.pasteque.skyblock.arena.command.ArenaLevelCommand;
import fr.pasteque.skyblock.arena.command.ArenaAdminCommand;
import fr.pasteque.skyblock.arena.listener.ArenaCombatListener;
import fr.pasteque.skyblock.arena.listener.ArenaKitListener;
import fr.pasteque.skyblock.arena.listener.ArenaProtectionListener;
import fr.pasteque.skyblock.arena.listener.ArenaSessionListener;
import fr.pasteque.skyblock.arena.listener.GlobalChatListener;
import fr.pasteque.skyblock.arena.listener.PlayerLifecycleListener;
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
    private EconomyBridge arenaEconomyBridge;

    /* ── Island chat toggles (persisted) ── */
    private final Map<UUID, Boolean> islandChatToggles = new HashMap<UUID, Boolean>();

    // =========================================================================
    //  Lifecycle
    // =========================================================================

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.prefix = getConfig().getString("prefix", "&2&lPasteque &a&lSkyblock &7» ");
        this.guardPrefix = getConfig().getString("guard-prefix", "&2&lPasteque&b&lGuard &f» ");

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
        this.arenaEconomyBridge = new EconomyBridge(this);
        this.safeZoneService = new SafeZoneService(this, arenaWorldService);
        this.playerDataService = new PlayerDataService(this, arenaWorldService);
        this.arenaLevelService = new ArenaLevelService(this, playerDataService);
        this.combatTagService = new CombatTagService(this, arenaEconomyBridge, playerDataService, arenaWorldService);
        this.arenaKitService = new ArenaKitService(this, arenaEconomyBridge, arenaWorldService);

        arenaWorldService.initializeArenaWorld();
        safeZoneService.load();
        playerDataService.load();

        /* ── Commands ── */
        registerSkyblockCommands();
        registerGuardCommands();
        registerShopCommands();
        registerArenaCommands();

        /* ── Listeners ── */
        registerSkyblockListeners();
        registerGuardListeners();
        registerShopListeners();
        registerArenaListeners();

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

        /* PastequeMyLittleShop save */
        if (playerShopManager != null) {
            playerShopManager.save();
        }

        /* PastequeSkyBlockArena save */
        safeZoneService.save();
        playerDataService.save();
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
    }

    private void registerGuardCommands() {
        PgCommand pgCommand = new PgCommand(this, sanctionService, reportService, filterService);
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

    private void registerArenaCommands() {
        ArenaCommand arenaCommand = new ArenaCommand(this, arenaWorldService);
        ArenaLevelCommand levelCommand = new ArenaLevelCommand(this, playerDataService, arenaLevelService);
        ArenaKitCommand kitCommand = new ArenaKitCommand(this, arenaKitService);
        ArenaAdminCommand adminCommand = new ArenaAdminCommand(this, arenaWorldService, safeZoneService, selectionService, arenaEconomyBridge, playerDataService);

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
    }

    private void registerGuardListeners() {
        registerEvents(new GuardChatListener(this, sanctionService, filterService));
        registerEvents(new GuardConnectionListener(this, sanctionService));
        registerEvents(new PanelListener(this, reportService));
    }

    private void registerShopListeners() {
        registerEvents(new PlayerShopListener(this, playerShopManager));
    }

    private void registerArenaListeners() {
        registerEvents(new ArenaProtectionListener(this, arenaWorldService, safeZoneService));
        registerEvents(new ArenaCombatListener(this, arenaWorldService, safeZoneService, combatTagService, arenaLevelService, arenaEconomyBridge, playerDataService));
        registerEvents(new PlayerLifecycleListener(this, arenaWorldService, combatTagService, playerDataService));
        registerEvents(new ArenaKitListener(this, arenaKitService));
        registerEvents(new ArenaSessionListener(this, arenaWorldService, arenaLevelService, playerDataService));
        registerEvents(new GlobalChatListener(this, arenaWorldService, playerDataService));
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
    public EconomyBridge getArenaEconomyBridge() { return arenaEconomyBridge; }
}
