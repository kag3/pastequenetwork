package fr.pasteque.skyblock.gui;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class ScoreboardListener implements Listener {

    private final PastequeSkyblockPlugin plugin;
    private final ScoreboardManager scoreboardManager;
    private final TabListManager tabListManager;

    public ScoreboardListener(PastequeSkyblockPlugin plugin, ScoreboardManager scoreboardManager, TabListManager tabListManager) {
        this.plugin = plugin;
        this.scoreboardManager = scoreboardManager;
        this.tabListManager = tabListManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        // Delay by 1 tick to ensure player is fully loaded
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    scoreboardManager.createScoreboard(player);
                    tabListManager.setTabList(player);
                }
            }
        }.runTaskLater(plugin, 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        scoreboardManager.removeScoreboard(event.getPlayer());
    }
}
