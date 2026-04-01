package fr.pastequeworld.labyroyal.listener;

import fr.pastequeworld.labyroyal.LabyRoyalPlugin;
import fr.pastequeworld.labyroyal.game.Game;
import fr.pastequeworld.labyroyal.game.PlayerData;
import fr.pastequeworld.labyroyal.util.MessageUtil;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Iterator;

public class ChatListener implements Listener {

    private final LabyRoyalPlugin plugin;

    public ChatListener(LabyRoyalPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(sender.getUniqueId());

        if (game == null) {
            // Player is not in a game - remove all in-game players from recipients
            // so they don't see global chat
            Iterator<Player> it = event.getRecipients().iterator();
            while (it.hasNext()) {
                Player recipient = it.next();
                if (plugin.getGameManager().getPlayerGame(recipient.getUniqueId()) != null) {
                    it.remove();
                }
            }
            return;
        }

        // Player is in a game - cancel default broadcast and send only to game players
        event.setCancelled(true);

        PlayerData data = game.getPlayers().get(sender.getUniqueId());
        String playerName = (data != null) ? data.getName() : sender.getName();

        String prefix;
        if (data != null && !data.isAlive()) {
            prefix = "&7[MORT] ";
        } else {
            prefix = "";
        }

        String formatted = MessageUtil.color(prefix + "&7" + playerName + " &8\u00bb &f" + event.getMessage());

        // Send to all players in the same game
        for (Player recipient : game.getOnlinePlayers()) {
            recipient.sendMessage(formatted);
        }
    }
}
