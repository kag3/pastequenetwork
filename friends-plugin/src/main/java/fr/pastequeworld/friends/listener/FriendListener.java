package fr.pastequeworld.friends.listener;

import fr.pastequeworld.friends.PastequeFriends;
import fr.pastequeworld.friends.data.FriendData;
import fr.pastequeworld.friends.data.FriendManager;
import fr.pastequeworld.friends.util.Msg;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class FriendListener implements Listener {

    private final PastequeFriends plugin;

    public FriendListener(PastequeFriends plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        FriendManager fm = plugin.getFriendManager();

        // Update name cache
        fm.updatePlayerName(player.getUniqueId(), player.getName());

        // Notify friends that this player is now online (small delay to let them connect fully)
        plugin.getProxy().getScheduler().schedule(plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isConnected()) return;

                String playerUuid = player.getUniqueId().toString();
                FriendData data = fm.getData(playerUuid);
                if (data == null) return;

                int onlineFriends = 0;
                for (String friendUuid : data.getFriends()) {
                    ProxiedPlayer friend = getPlayer(friendUuid);
                    if (friend != null && friend.isConnected()) {
                        Msg.send(friend, "&a\u25B2 &e" + player.getName() + " &7s'est connecté.");
                        onlineFriends++;
                    }
                }

                // Tell the player how many friends are online
                if (onlineFriends > 0) {
                    Msg.send(player, "&a" + onlineFriends + " &7de vos amis "
                            + (onlineFriends == 1 ? "est" : "sont") + " en ligne ! &e/friend list");
                }

                // Notify about pending requests
                if (data.getPendingRequests() != null && !data.getPendingRequests().isEmpty()) {
                    int pending = data.getPendingRequests().size();
                    Msg.send(player, "&eVous avez &6" + pending + " &edemande"
                            + (pending > 1 ? "s" : "") + " d'ami en attente ! &6/friend requests");
                }
            }
        }, 2, TimeUnit.SECONDS);
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        FriendManager fm = plugin.getFriendManager();
        String playerUuid = player.getUniqueId().toString();

        FriendData data = fm.getData(playerUuid);
        if (data == null) return;

        // Notify online friends
        for (String friendUuid : data.getFriends()) {
            ProxiedPlayer friend = getPlayer(friendUuid);
            if (friend != null && friend.isConnected()) {
                Msg.send(friend, "&c\u25BC &e" + player.getName() + " &7s'est déconnecté.");
            }
        }
    }

    @EventHandler
    public void onServerSwitch(ServerSwitchEvent event) {
        ProxiedPlayer player = event.getPlayer();
        FriendManager fm = plugin.getFriendManager();
        String playerUuid = player.getUniqueId().toString();

        FriendData data = fm.getData(playerUuid);
        if (data == null) return;

        String serverName = player.getServer() != null
                ? player.getServer().getInfo().getName() : "???";
        String displayName = getDisplayServerName(serverName);

        // Notify online friends about server change
        for (String friendUuid : data.getFriends()) {
            ProxiedPlayer friend = getPlayer(friendUuid);
            if (friend != null && friend.isConnected()) {
                Msg.send(friend, "&e" + player.getName() + " &7a rejoint &a" + displayName + "&7.");
            }
        }
    }

    private ProxiedPlayer getPlayer(String uuid) {
        try {
            return plugin.getProxy().getPlayer(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String getDisplayServerName(String serverName) {
        String lower = serverName.toLowerCase();
        if (lower.contains("hub") || lower.contains("lobby")) return "Hub";
        if (lower.contains("labyroyale") || lower.contains("laby")) return "LabyRoyale";
        if (lower.contains("skyblock")) return "SkyBlock";
        if (lower.contains("build")) return "Build";
        if (lower.contains("raft")) return "Raft";
        return serverName.substring(0, 1).toUpperCase() + serverName.substring(1);
    }
}
