package fr.pastequeworld.party.listener;

import fr.pastequeworld.party.PastequeParty;
import fr.pastequeworld.party.data.Party;
import fr.pastequeworld.party.data.PartyManager;
import fr.pastequeworld.party.util.Msg;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PartyListener implements Listener {

    private final PastequeParty plugin;

    // Member UUID -> leader name (pending transfer to LabyRoyale)
    private final Map<UUID, String> pendingLabyMembers = new ConcurrentHashMap<UUID, String>();

    public PartyListener(PastequeParty plugin) {
        this.plugin = plugin;
    }

    // ==================== FOLLOW LEADER ON SERVER SWITCH ====================

    @EventHandler
    public void onServerSwitch(ServerSwitchEvent event) {
        ProxiedPlayer player = event.getPlayer();
        PartyManager pm = plugin.getPartyManager();
        Party party = pm.getParty(player.getUniqueId());

        if (party == null || !party.isLeader(player.getUniqueId())) return;

        ServerInfo targetServer = player.getServer().getInfo();
        String serverName = targetServer.getName().toLowerCase();

        // LabyRoyale: special handling
        if (serverName.contains("labyroyale") || serverName.contains("laby")) {
            handleLabyRoyaleJoin(party, player, targetServer);
            return;
        }

        // Other servers: staggered teleport
        teleportPartyToServer(party, player, targetServer);
    }

    // ==================== MEMBER ARRIVES ON LABYROYALE ====================

    @EventHandler
    public void onServerConnected(ServerConnectedEvent event) {
        final ProxiedPlayer member = event.getPlayer();
        final String leaderName = pendingLabyMembers.remove(member.getUniqueId());
        if (leaderName == null) return;

        String serverName = event.getServer().getInfo().getName().toLowerCase();
        if (!serverName.contains("labyroyale") && !serverName.contains("laby")) return;

        // Wait for Spigot to fully register the player, then force /lr partywait
        plugin.getProxy().getScheduler().schedule(plugin, new Runnable() {
            @Override
            public void run() {
                if (member.isConnected()) {
                    member.chat("/lr partywait " + leaderName);
                }
            }
        }, 1, TimeUnit.SECONDS);
    }

    // ==================== STANDARD SERVER FOLLOW ====================

    private void teleportPartyToServer(final Party party, final ProxiedPlayer leader, final ServerInfo server) {
        final String displayName = getDisplayServerName(server.getName());
        final List<UUID> toSend = new ArrayList<UUID>();

        for (UUID memberUuid : party.getMembers()) {
            if (memberUuid.equals(leader.getUniqueId())) continue;
            ProxiedPlayer member = plugin.getProxy().getPlayer(memberUuid);
            if (member == null || !member.isConnected()) continue;
            if (member.getServer() != null && member.getServer().getInfo().equals(server)) continue;
            toSend.add(memberUuid);
        }

        // Send members one by one, 500ms apart
        // IMPORTANT: connection_throttle must be -1 in BungeeCord config.yml
        // AND connection-throttle must be -1 in Spigot spigot.yml
        for (int i = 0; i < toSend.size(); i++) {
            final UUID uuid = toSend.get(i);
            long delay = 500 + (i * 500);
            plugin.getProxy().getScheduler().schedule(plugin, new Runnable() {
                @Override
                public void run() {
                    ProxiedPlayer member = plugin.getProxy().getPlayer(uuid);
                    if (member != null && member.isConnected()) {
                        Msg.send(member, "&d\u25B6 &7T\u00e9l\u00e9portation vers &a" + displayName + " &7(groupe de &e" + leader.getName() + "&7)");
                        member.connect(server);
                    }
                }
            }, delay, TimeUnit.MILLISECONDS);
        }
    }

    // ==================== LABYROYALE INTEGRATION ====================
    //
    // Flow:
    // 1. Leader joins LabyRoyale -> notify members (stay on Hub)
    // 2. Send members one by one with 5s gap (avoids connection throttled)
    // 3. On ServerConnectedEvent: force member.chat("/lr partywait <leader>")
    // 4. Spigot: /lr partywait marks member as waiting, freezes, skips cabin
    // 5. Leader picks mode -> autoJoinPartyWaiters() puts them in the queue
    //

    private void handleLabyRoyaleJoin(final Party party, final ProxiedPlayer leader, final ServerInfo server) {
        final List<UUID> memberUuids = new ArrayList<UUID>();
        for (UUID memberUuid : party.getMembers()) {
            if (memberUuid.equals(leader.getUniqueId())) continue;
            ProxiedPlayer member = plugin.getProxy().getPlayer(memberUuid);
            if (member != null && member.isConnected()) {
                memberUuids.add(memberUuid);
                Msg.send(member, "&e" + leader.getName() + " &7choisit un mode de jeu sur &dLabyRoyale&7...");
                Msg.send(member, "&7Vous serez t\u00e9l\u00e9port\u00e9s automatiquement !");
            }
        }

        if (memberUuids.isEmpty()) return;

        // Mark all as pending
        for (UUID uuid : memberUuids) {
            pendingLabyMembers.put(uuid, leader.getName());
        }

        // Send members one by one, 500ms apart, starting after 2 seconds
        for (int i = 0; i < memberUuids.size(); i++) {
            final UUID uuid = memberUuids.get(i);
            final String leaderName = leader.getName();
            long delay = 2000 + (i * 500);

            plugin.getProxy().getScheduler().schedule(plugin, new Runnable() {
                @Override
                public void run() {
                    ProxiedPlayer member = plugin.getProxy().getPlayer(uuid);
                    if (member == null || !member.isConnected()) {
                        pendingLabyMembers.remove(uuid);
                        return;
                    }
                    if (member.getServer() != null && member.getServer().getInfo().equals(server)) {
                        // Already on LabyRoyale
                        member.chat("/lr partywait " + leaderName);
                        pendingLabyMembers.remove(uuid);
                        return;
                    }
                    Msg.send(member, "&d\u25B6 &7T\u00e9l\u00e9portation vers &dLabyRoyale&7...");
                    member.connect(server);
                }
            }, delay, TimeUnit.MILLISECONDS);
        }

        // Cleanup pending after 60 seconds
        plugin.getProxy().getScheduler().schedule(plugin, new Runnable() {
            @Override
            public void run() {
                for (UUID uuid : memberUuids) {
                    pendingLabyMembers.remove(uuid);
                }
            }
        }, 60, TimeUnit.SECONDS);
    }

    // ==================== DISCONNECT ====================

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        pendingLabyMembers.remove(player.getUniqueId());

        PartyManager pm = plugin.getPartyManager();
        Party party = pm.getParty(player.getUniqueId());

        if (party == null) return;

        boolean wasLeader = party.isLeader(player.getUniqueId());
        String playerName = player.getName();

        Party remaining = pm.leaveParty(player.getUniqueId());

        if (remaining != null) {
            for (UUID memberUuid : remaining.getMembers()) {
                ProxiedPlayer member = plugin.getProxy().getPlayer(memberUuid);
                if (member != null) {
                    Msg.send(member, "&c- &e" + playerName + " &7s'est d\u00e9connect\u00e9 et a quitt\u00e9 le groupe. &8(" + remaining.getSize() + ")");

                    if (wasLeader && remaining.isLeader(memberUuid)) {
                        Msg.send(member, "&6\u2605 &eVous \u00eates maintenant le chef du groupe !");
                    }
                }
            }
        }
    }

    // ==================== UTILS ====================

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
