package fr.pastequeworld.labyroyal.listener;

import fr.pastequeworld.labyroyal.LabyRoyalPlugin;
import fr.pastequeworld.labyroyal.game.Game;
import fr.pastequeworld.labyroyal.game.GameState;
import fr.pastequeworld.labyroyal.game.PlayerData;
import fr.pastequeworld.labyroyal.util.MessageUtil;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AntiCheatListener implements Listener {

    private final LabyRoyalPlugin plugin;
    private final Map<UUID, Integer> flyViolations = new HashMap<>();
    private final Map<UUID, Long> lastGroundTime = new HashMap<>();

    public AntiCheatListener(LabyRoyalPlugin plugin) {
        this.plugin = plugin;

        // Periodic check for suspicious behavior
        new BukkitRunnable() {
            @Override
            public void run() {
                checkPlayers();
            }
        }.runTaskTimer(plugin, 100L, 40L); // Every 2 seconds
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());
        if (game == null) return;

        if (game.getState() == GameState.PREPARATION || game.getState() == GameState.PVP) {
            event.setCancelled(true);
            player.setFlying(false);
            player.setAllowFlight(false);

            int violations = flyViolations.getOrDefault(player.getUniqueId(), 0) + 1;
            flyViolations.put(player.getUniqueId(), violations);

            if (violations >= 3) {
                kickPlayer(player, "&cVous avez ete expulse pour comportement suspect (fly).");
                game.eliminatePlayer(player, null, true);
            } else {
                MessageUtil.send(player, "&c\u26a0 Comportement suspect detecte ! (" + violations + "/3)");
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR) return;

        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());
        if (game == null) return;
        if (game.getState() != GameState.PREPARATION && game.getState() != GameState.PVP) return;

        PlayerData data = game.getPlayers().get(player.getUniqueId());
        if (data == null || !data.isAlive()) return;

        // Track ground time
        if (player.isOnGround()) {
            lastGroundTime.put(player.getUniqueId(), System.currentTimeMillis());
        }

        // Check if above maze ceiling (y > 73 = suspicious)
        if (player.getLocation().getY() > 73) {
            // Teleport back down
            player.teleport(player.getLocation().clone().subtract(0, player.getLocation().getY() - 65, 0));
            MessageUtil.send(player, "&c\u26a0 Vous etes en dehors de la zone de jeu !");
        }
    }

    private void checkPlayers() {
        if (!plugin.getConfig().getBoolean("anti-cheat.fly-detection", true)) return;

        for (Game game : plugin.getGameManager().getGames()) {
            if (game.getState() != GameState.PREPARATION && game.getState() != GameState.PVP) continue;

            for (Player player : game.getAlivePlayers()) {
                if (player.getGameMode() == GameMode.SPECTATOR) continue;

                // Check if player hasn't touched ground for too long (10 seconds)
                Long lastGround = lastGroundTime.get(player.getUniqueId());
                if (lastGround != null && System.currentTimeMillis() - lastGround > 10000) {
                    if (!player.isOnGround() && player.getLocation().getY() > 62) {
                        int violations = flyViolations.getOrDefault(player.getUniqueId(), 0) + 1;
                        flyViolations.put(player.getUniqueId(), violations);

                        if (violations >= 5) {
                            kickPlayer(player, "&cExpulse pour comportement suspect (fly prolonge).");
                            game.eliminatePlayer(player, null, true);
                        }
                    }
                }
            }
        }
    }

    private void kickPlayer(Player player, String reason) {
        if (plugin.getConfig().getBoolean("anti-cheat.auto-kick", true)) {
            player.kickPlayer(MessageUtil.color(reason));
        }
    }

    public void clearData(UUID uuid) {
        flyViolations.remove(uuid);
        lastGroundTime.remove(uuid);
    }
}
