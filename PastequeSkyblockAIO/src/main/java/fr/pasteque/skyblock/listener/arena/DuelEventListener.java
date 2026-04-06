package fr.pasteque.skyblock.listener.arena;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.arena.ArenaWorldService;
import fr.pasteque.skyblock.arena.DuelService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Handles duel-specific events:
 * - Void fall = loser, opponent wins (with full animation)
 * - Disconnect = loser, opponent wins (with full animation)
 */
public class DuelEventListener implements Listener {

    private final PastequeSkyblockPlugin plugin;
    private final ArenaWorldService arenaWorldService;
    private final DuelService duelService;

    public DuelEventListener(PastequeSkyblockPlugin plugin, ArenaWorldService arenaWorldService, DuelService duelService) {
        this.plugin = plugin;
        this.arenaWorldService = arenaWorldService;
        this.duelService = duelService;
    }

    /**
     * Void fall detection: if a dueling player falls below Y=0, they lose.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        if (event.getTo().getBlockY() >= 0) return;

        Player player = event.getPlayer();
        if (!duelService.isDueling(player.getUniqueId())) return;

        // Player fell into the void during a duel -> they lose
        UUID opponentId = duelService.getOpponent(player.getUniqueId());
        if (opponentId == null) {
            // Team duel fallback
            duelService.cancelDuel(player.getUniqueId());
            return;
        }

        final Player opponent = Bukkit.getPlayer(opponentId);
        if (opponent != null && opponent.isOnline()) {
            // Prevent further fall damage / death
            player.setFallDistance(0);
            player.setHealth(player.getMaxHealth());
            // End duel with opponent as winner
            duelService.endDuel(opponent, player);
        } else {
            duelService.cancelDuel(player.getUniqueId());
        }
    }

    /**
     * Disconnect during duel: disconnecting player loses, opponent wins.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent event) {
        Player quitter = event.getPlayer();
        if (!duelService.isDueling(quitter.getUniqueId())) return;

        UUID opponentId = duelService.getOpponent(quitter.getUniqueId());
        if (opponentId != null) {
            Player opponent = Bukkit.getPlayer(opponentId);
            if (opponent != null && opponent.isOnline()) {
                // Opponent wins by forfeit
                duelService.endDuel(opponent, quitter);
                return;
            }
        }

        // Fallback: cancel duel for team duels or if opponent is also offline
        duelService.cancelDuel(quitter.getUniqueId());
    }

    /**
     * Prevent void damage from killing dueling players (we handle it via move check).
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID) return;

        Player player = (Player) event.getEntity();
        if (duelService.isDueling(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
