package fr.pasteque.skyblockarena;

import fr.pasteque.skyblockarena.command.ArenaAdminCommand;
import fr.pasteque.skyblockarena.command.ArenaCommand;
import fr.pasteque.skyblockarena.command.ArenaKitCommand;
import fr.pasteque.skyblockarena.command.ArenaLevelCommand;
import fr.pasteque.skyblockarena.listener.ArenaCombatListener;
import fr.pasteque.skyblockarena.listener.ArenaKitListener;
import fr.pasteque.skyblockarena.listener.ArenaProtectionListener;
import fr.pasteque.skyblockarena.listener.ArenaSessionListener;
import fr.pasteque.skyblockarena.listener.GlobalChatListener;
import fr.pasteque.skyblockarena.listener.PlayerLifecycleListener;
import fr.pasteque.skyblockarena.service.ArenaKitService;
import fr.pasteque.skyblockarena.service.ArenaLevelService;
import fr.pasteque.skyblockarena.service.ArenaWorldService;
import fr.pasteque.skyblockarena.service.CombatTagService;
import fr.pasteque.skyblockarena.service.EconomyBridge;
import fr.pasteque.skyblockarena.service.PlayerDataService;
import fr.pasteque.skyblockarena.service.SafeZoneService;
import fr.pasteque.skyblockarena.service.SelectionService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class PastequeSkyBlockArenaPlugin extends JavaPlugin {

    private ArenaWorldService arenaWorldService;
    private SafeZoneService safeZoneService;
    private SelectionService selectionService;
    private EconomyBridge economyBridge;
    private PlayerDataService playerDataService;
    private ArenaLevelService arenaLevelService;
    private CombatTagService combatTagService;
    private ArenaKitService arenaKitService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.selectionService = new SelectionService();
        this.arenaWorldService = new ArenaWorldService(this);
        this.safeZoneService = new SafeZoneService(this, arenaWorldService);
        this.economyBridge = new EconomyBridge(this);
        this.playerDataService = new PlayerDataService(this, arenaWorldService);
        this.arenaLevelService = new ArenaLevelService(this, playerDataService);
        this.combatTagService = new CombatTagService(this, economyBridge, playerDataService, arenaWorldService);
        this.arenaKitService = new ArenaKitService(this, economyBridge, arenaWorldService);

        arenaWorldService.initializeArenaWorld();
        safeZoneService.load();
        playerDataService.load();

        registerCommands();
        registerListeners(
                new ArenaProtectionListener(this, arenaWorldService, safeZoneService),
                new ArenaCombatListener(this, arenaWorldService, safeZoneService, combatTagService, arenaLevelService, economyBridge, playerDataService),
                new PlayerLifecycleListener(this, arenaWorldService, combatTagService, playerDataService),
                new ArenaKitListener(this, arenaKitService),
                new ArenaSessionListener(this, arenaWorldService, arenaLevelService, playerDataService),
                new GlobalChatListener(this, arenaWorldService, playerDataService)
        );

        startActivityTicker();
        getLogger().info(ChatColor.stripColor(color(prefix() + getConfig().getString("messages.world-ready", "Le monde d'arène est prêt."))));
    }

    @Override
    public void onDisable() {
        safeZoneService.save();
        playerDataService.save();
    }

    private void registerCommands() {
        ArenaCommand arenaCommand = new ArenaCommand(this, arenaWorldService);
        ArenaLevelCommand levelCommand = new ArenaLevelCommand(this, playerDataService, arenaLevelService);
        ArenaKitCommand kitCommand = new ArenaKitCommand(this, arenaKitService);
        ArenaAdminCommand adminCommand = new ArenaAdminCommand(this, arenaWorldService, safeZoneService, selectionService, economyBridge, playerDataService);

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

    private void registerListeners(Listener... listeners) {
        for (Listener listener : listeners) {
            Bukkit.getPluginManager().registerEvents(listener, this);
        }
    }

    private void startActivityTicker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                playerDataService.tickSessions();
                arenaLevelService.grantActivityXp();
                combatTagService.tick();
            }
        }.runTaskTimer(this, 20L, 20L);
    }

    public String prefix() {
        return getConfig().getString("prefix", "&2&lPasteque&5&lArena &7» ");
    }

    public String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    public ArenaWorldService getArenaWorldService() {
        return arenaWorldService;
    }

    public SafeZoneService getSafeZoneService() {
        return safeZoneService;
    }

    public SelectionService getSelectionService() {
        return selectionService;
    }

    public EconomyBridge getEconomyBridge() {
        return economyBridge;
    }

    public PlayerDataService getPlayerDataService() {
        return playerDataService;
    }

    public ArenaLevelService getArenaLevelService() {
        return arenaLevelService;
    }

    public CombatTagService getCombatTagService() {
        return combatTagService;
    }

    public ArenaKitService getArenaKitService() {
        return arenaKitService;
    }
}
