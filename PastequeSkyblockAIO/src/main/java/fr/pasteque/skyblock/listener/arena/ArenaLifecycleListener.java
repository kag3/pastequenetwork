package fr.pasteque.skyblock.listener.arena;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.arena.ArenaWorldService;
import fr.pasteque.skyblock.arena.CombatTagService;
import fr.pasteque.skyblock.arena.PlayerDataService;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class ArenaLifecycleListener implements Listener {

    private final PastequeSkyblockPlugin plugin;
    private final ArenaWorldService arenaWorldService;
    private final CombatTagService combatTagService;
    private final PlayerDataService playerDataService;

    public ArenaLifecycleListener(PastequeSkyblockPlugin plugin, ArenaWorldService arenaWorldService, CombatTagService combatTagService, PlayerDataService playerDataService) {
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
        World arenaWorld = arenaWorldService.getArenaWorld();
        if (arenaWorld == null) {
            return;
        }
        event.setRespawnLocation(arenaWorldService.getConfiguredSpawn(arenaWorld));
    }
}
