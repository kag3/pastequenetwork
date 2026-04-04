package fr.pasteque.skyblock.listener;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.model.Island;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    private final PastequeSkyblockPlugin plugin;

    public ChatListener(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.getWorld().getName().equalsIgnoreCase(plugin.getWorldManager().getIslandWorldName())) {
            return;
        }
        String message = event.getMessage();
        event.setCancelled(true);

        if (message.startsWith("@")) {
            String clean = message.substring(1).trim();
            String formatted = MessageUtil.color("&8[&aGlobal&8] &a" + player.getName() + "&7: &f" + clean);
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getWorld().getName().equalsIgnoreCase(plugin.getWorldManager().getIslandWorldName())) {
                    online.sendMessage(formatted);
                }
            }
            return;
        }

        Island island = plugin.getIslandManager().getIslandByPlayer(player.getUniqueId());
        boolean forcedIslandChat = plugin.isIslandChatEnabled(player.getUniqueId());
        if (forcedIslandChat && island != null) {
            String formatted = MessageUtil.color("&8[&2Île&8] &a" + player.getName() + "&7: &f" + message);
            for (Player online : Bukkit.getOnlinePlayers()) {
                Island targetIsland = plugin.getIslandManager().getIslandByPlayer(online.getUniqueId());
                if (targetIsland != null && targetIsland.getOwner().equals(island.getOwner())) {
                    online.sendMessage(formatted);
                }
            }
            return;
        }

        Island currentIsland = plugin.getIslandManager().getIslandAt(player.getLocation());
        if (currentIsland == null) {
            player.sendMessage(MessageUtil.color("&8[&aGlobal&8] &a" + player.getName() + "&7: &f" + message));
            return;
        }
        String formatted = MessageUtil.color("&8[&2Plot/Île&8] &a" + player.getName() + "&7: &f" + message);
        for (Player online : Bukkit.getOnlinePlayers()) {
            Island other = plugin.getIslandManager().getIslandAt(online.getLocation());
            if (other != null && other.getOwner().equals(currentIsland.getOwner())) {
                online.sendMessage(formatted);
            }
        }
    }
}
