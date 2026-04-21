package fr.pastequeworld.bedwars.listener;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.game.Arena;
import fr.pastequeworld.bedwars.player.BedWarsPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Gere join/quit + format de chat global.
 */
public class PlayerConnectionListener implements Listener {

    private final BedWarsPlugin plugin;

    public PlayerConnectionListener(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        Player player = event.getPlayer();
        plugin.getPlayerDataManager().register(player);
        plugin.getLobbyManager().setupLobbyPlayer(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        Player player = event.getPlayer();
        Arena arena = plugin.getArenaManager().getArenaOfPlayer(player);
        if (arena != null) arena.removePlayer(player);
        plugin.getPlayerDataManager().unregister(player.getUniqueId());
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        BedWarsPlayer bw = plugin.getPlayerDataManager().get(player);
        event.getRecipients().clear();

        if (bw == null || bw.getArena() == null) {
            // Chat du lobby : global aux joueurs du lobby
            String msg = plugin.getMessageManager().getRaw("chat.format-lobby")
                    .replace("%rank%", "")
                    .replace("%player%", player.getName())
                    .replace("%message%", event.getMessage());
            msg = fr.pastequeworld.bedwars.util.ColorUtil.color(msg);
            for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                BedWarsPlayer bp = plugin.getPlayerDataManager().get(p);
                if (bp == null || bp.getArena() == null) {
                    p.sendMessage(msg);
                }
            }
            event.setCancelled(true);
            return;
        }

        // Chat de partie : isole a l'arene
        String teamColor = bw.getTeam() != null
                ? bw.getTeam().getColor().getChatColor().toString() : "&7";
        String teamPrefix = bw.getTeam() != null
                ? bw.getTeam().getColor().getPrefix().toUpperCase() : "-";
        String format;
        if (bw.isSpectator()) {
            format = plugin.getMessageManager().getRaw("chat.format-spectator");
        } else {
            format = plugin.getMessageManager().getRaw("chat.format-game");
        }
        String msg = format
                .replace("%rank%", "")
                .replace("%player%", player.getName())
                .replace("%team_color%", teamColor)
                .replace("%team_prefix%", teamPrefix)
                .replace("%message%", event.getMessage());
        msg = fr.pastequeworld.bedwars.util.ColorUtil.color(msg);
        bw.getArena().broadcast(msg);
        event.setCancelled(true);
    }
}
