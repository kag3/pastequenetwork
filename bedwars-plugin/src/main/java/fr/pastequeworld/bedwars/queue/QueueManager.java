package fr.pastequeworld.bedwars.queue;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.game.Arena;
import fr.pastequeworld.bedwars.game.GameMode;
import org.bukkit.entity.Player;

/**
 * Wrapper leger : "mettre en queue" = rejoindre une arena WAITING de ce mode
 * (ou en creer une si besoin). L'arene elle-meme contient la file d'attente.
 */
public class QueueManager {

    private final BedWarsPlugin plugin;

    public QueueManager(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void join(Player player, GameMode mode) {
        Arena arena = plugin.getArenaManager().findOrCreate(mode);
        if (arena == null) {
            player.sendMessage(plugin.getMessageManager().get("errors.no-map-available"));
            return;
        }
        if (!arena.canJoin()) {
            player.sendMessage(plugin.getMessageManager().get("errors.queue-full"));
            return;
        }
        arena.addPlayer(player);
    }

    public void leave(Player player) {
        Arena arena = plugin.getArenaManager().getArenaOfPlayer(player);
        if (arena == null) {
            player.sendMessage(plugin.getMessageManager().get("errors.not-in-game"));
            return;
        }
        arena.removePlayer(player);
        plugin.getLobbyManager().setupLobbyPlayer(player);
        player.sendMessage(plugin.getMessageManager().get("queue.left"));
    }
}
