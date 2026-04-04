package fr.pasteque.skyblockarena.listener;

import fr.pasteque.skyblockarena.PastequeSkyBlockArenaPlugin;
import fr.pasteque.skyblockarena.model.ArenaPlayerData;
import fr.pasteque.skyblockarena.service.ArenaLevelService;
import fr.pasteque.skyblockarena.service.PlayerDataService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class ArenaSessionListener implements Listener {

    private final PastequeSkyBlockArenaPlugin plugin;
    private final fr.pasteque.skyblockarena.service.ArenaWorldService arenaWorldService;
    private final ArenaLevelService arenaLevelService;
    private final PlayerDataService playerDataService;

    public ArenaSessionListener(PastequeSkyBlockArenaPlugin plugin, fr.pasteque.skyblockarena.service.ArenaWorldService arenaWorldService, ArenaLevelService arenaLevelService, PlayerDataService playerDataService) {
        this.plugin = plugin;
        this.arenaWorldService = arenaWorldService;
        this.arenaLevelService = arenaLevelService;
        this.playerDataService = playerDataService;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        resetStreak(event.getPlayer());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (arenaWorldService.isArenaWorld(event.getFrom().getWorld()) && !arenaWorldService.isArenaWorld(event.getTo().getWorld())) {
            resetStreak(event.getPlayer());
        }
    }

    private void resetStreak(Player player) {
        ArenaPlayerData data = playerDataService.get(player);
        data.setActivitySeconds(0);
        data.setStreakIntervals(0);
    }
}
