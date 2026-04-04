package fr.pasteque.skyblockarena.listener;

import fr.pasteque.skyblockarena.PastequeSkyBlockArenaPlugin;
import fr.pasteque.skyblockarena.service.ArenaWorldService;
import fr.pasteque.skyblockarena.service.CombatTagService;
import fr.pasteque.skyblockarena.service.PlayerDataService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerLifecycleListener implements Listener {

    private final PastequeSkyBlockArenaPlugin plugin;
    private final ArenaWorldService arenaWorldService;
    private final CombatTagService combatTagService;
    private final PlayerDataService playerDataService;

    public PlayerLifecycleListener(PastequeSkyBlockArenaPlugin plugin, ArenaWorldService arenaWorldService, CombatTagService combatTagService, PlayerDataService playerDataService) {
        this.plugin = plugin;
        this.arenaWorldService = arenaWorldService;
        this.combatTagService = combatTagService;
        this.playerDataService = playerDataService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerDataService.get(player);
        combatTagService.notifyIfPending(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        combatTagService.handleCombatLogout(event.getPlayer());
        playerDataService.save();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        arenaWorldService.handleWorldChange(event.getPlayer(), event.getFrom(), event.getPlayer().getWorld());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!arenaWorldService.isArenaWorld(event.getPlayer().getWorld())) {
            return;
        }
        event.setRespawnLocation(arenaWorldService.getConfiguredSpawn(arenaWorldService.getArenaWorld()));
    }
}
