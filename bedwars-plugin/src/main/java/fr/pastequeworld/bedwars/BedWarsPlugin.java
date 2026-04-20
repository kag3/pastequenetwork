package fr.pastequeworld.bedwars;

import fr.pastequeworld.bedwars.game.ArenaManager;
import fr.pastequeworld.bedwars.command.BedWarsCommand;
import fr.pastequeworld.bedwars.command.SetupCommand;
import fr.pastequeworld.bedwars.command.ShoutCommand;
import fr.pastequeworld.bedwars.command.TeamChatCommand;
import fr.pastequeworld.bedwars.config.ConfigManager;
import fr.pastequeworld.bedwars.config.MessageManager;
import fr.pastequeworld.bedwars.config.ShopConfig;
import fr.pastequeworld.bedwars.listener.GameListener;
import fr.pastequeworld.bedwars.listener.LobbyListener;
import fr.pastequeworld.bedwars.listener.PlayerConnectionListener;
import fr.pastequeworld.bedwars.listener.ShopListener;
import fr.pastequeworld.bedwars.lobby.LobbyManager;
import fr.pastequeworld.bedwars.lobby.NPCManager;
import fr.pastequeworld.bedwars.map.MapRegistry;
import fr.pastequeworld.bedwars.map.WorldWiper;
import fr.pastequeworld.bedwars.player.PlayerDataManager;
import fr.pastequeworld.bedwars.queue.QueueManager;
import fr.pastequeworld.bedwars.ui.ScoreboardManager;
import fr.pastequeworld.bedwars.ui.TabManager;
import fr.pastequeworld.bedwars.util.ResourceExtractor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Plugin principal PastequeBedWars.
 *
 * Architecture:
 *   - ConfigManager / MessageManager / ShopConfig -> chargement YAML
 *   - PlayerDataManager -> stats en memoire (persistance future via DB)
 *   - MapRegistry -> pool de templates de maps
 *   - ArenaManager -> pool d'arenes actives (lazy-loaded)
 *   - QueueManager -> gere les files d'attente par mode
 *   - LobbyManager + NPCManager -> lobby de selection
 *   - ScoreboardManager / TabManager -> UI temps reel
 */
public class BedWarsPlugin extends JavaPlugin {

    private static BedWarsPlugin instance;

    private ConfigManager configManager;
    private MessageManager messageManager;
    private ShopConfig shopConfig;

    private PlayerDataManager playerDataManager;
    private MapRegistry mapRegistry;
    private ArenaManager arenaManager;
    private QueueManager queueManager;
    private LobbyManager lobbyManager;
    private NPCManager npcManager;

    private ScoreboardManager scoreboardManager;
    private TabManager tabManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultResources();

        // Extraction ALL-IN-ONE: depose le JAR, et les 5 maps + le lobby sont
        // ecrits sur disque au premier boot. Plus besoin de FTP.
        new ResourceExtractor(this).extractOnFirstRun();

        // Wipe-and-rebuild : au premier boot, supprime le monde 'world' genere
        // par Spigot et le remplace par un void pur + paste de la schematic de
        // lobby. Doit tourner AVANT ConfigManager.load() qui charge le monde.
        new WorldWiper(this).wipeOnFirstRun();

        this.configManager = new ConfigManager(this);
        this.configManager.load();

        this.messageManager = new MessageManager(this);
        this.messageManager.load();

        this.shopConfig = new ShopConfig(this);
        this.shopConfig.load();

        this.playerDataManager = new PlayerDataManager(this);
        this.mapRegistry = new MapRegistry(this);
        this.mapRegistry.load();

        this.arenaManager = new ArenaManager(this);
        this.queueManager = new QueueManager(this);

        this.lobbyManager = new LobbyManager(this);
        this.lobbyManager.setup();

        this.npcManager = new NPCManager(this);
        this.npcManager.spawnAll();

        this.scoreboardManager = new ScoreboardManager(this);
        this.tabManager = new TabManager(this);

        registerListeners();
        registerCommands();

        getLogger().info("========================================");
        getLogger().info("  PastequeBedWars v" + getDescription().getVersion());
        getLogger().info("  Hypixel-style BedWars | 1.9.4");
        getLogger().info("  Maps chargees: " + mapRegistry.getAllMapIds().size());
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        if (arenaManager != null) arenaManager.shutdownAll();
        if (npcManager != null) npcManager.despawnAll();
        if (scoreboardManager != null) scoreboardManager.stop();
    }

    private void saveDefaultResources() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("shop.yml", false);
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new LobbyListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GameListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ShopListener(this), this);
    }

    private void registerCommands() {
        getCommand("bedwars").setExecutor(new BedWarsCommand(this));
        getCommand("bwsetup").setExecutor(new SetupCommand(this));
        getCommand("shout").setExecutor(new ShoutCommand(this));
        getCommand("teamchat").setExecutor(new TeamChatCommand(this));
    }

    public static BedWarsPlugin get() {
        return instance;
    }

    public ConfigManager getConfigManager() { return configManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public ShopConfig getShopConfig() { return shopConfig; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public MapRegistry getMapRegistry() { return mapRegistry; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public QueueManager getQueueManager() { return queueManager; }
    public LobbyManager getLobbyManager() { return lobbyManager; }
    public NPCManager getNpcManager() { return npcManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public TabManager getTabManager() { return tabManager; }
}
