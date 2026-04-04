package fr.pasteque.skyblock;

import fr.pasteque.skyblock.command.*;
import fr.pasteque.skyblock.listener.*;
import fr.pasteque.skyblock.manager.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class PastequeSkyblockPlugin extends JavaPlugin {
    private String prefix;
    private WorldManager worldManager;
    private IslandManager islandManager;
    private CoopManager coopManager;
    private EconomyManager economyManager;
    private AuctionManager auctionManager;
    private ChallengeManager challengeManager;
    private PvpManager pvpManager;
    private ShopManager shopManager;
    private ConfirmationManager confirmationManager;
    private BorderManager borderManager;
    private CombatManager combatManager;
    private SocialManager socialManager;
    private InvasionManager invasionManager;
    private EndEventManager endEventManager;
    private final java.util.Map<java.util.UUID, Boolean> islandChatToggles = new java.util.HashMap<java.util.UUID, Boolean>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.prefix = getConfig().getString("prefix", "&2&lPasteque &a&lSkyblock &7» ");
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

        Bukkit.getPluginManager().registerEvents(new ProtectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ConnectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ShopListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PvpListener(this), this);
        Bukkit.getPluginManager().registerEvents(new EmbassyListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MiscServerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new EndEventListener(this), this);

        coopManager.start();
        borderManager.start();
        combatManager.start();
        invasionManager.start();
        endEventManager.start();
        Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override public void run() { auctionManager.purgeExpired(); }
        }, 20L * 60L, 20L * 60L);
    }

    @Override
    public void onDisable() {
        islandManager.save();
        coopManager.save();
        economyManager.save();
        auctionManager.save();
        challengeManager.save();
        pvpManager.save();
        socialManager.save();
        combatManager.save();
    }

    public String getPrefix() { return prefix; }
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
    public boolean isIslandChatEnabled(java.util.UUID uuid) { return islandChatToggles.containsKey(uuid) && islandChatToggles.get(uuid); }
    public void toggleIslandChat(java.util.UUID uuid) { islandChatToggles.put(uuid, !isIslandChatEnabled(uuid)); }
}
