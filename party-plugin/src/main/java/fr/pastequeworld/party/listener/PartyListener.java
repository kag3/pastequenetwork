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

    // Member UUID -> leader name (for members sent to LabyRoyale waiting for leader's choice)
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

        // LabyRoyale: special handling - send members after delay, they run /lr partywait
        if (serverName.contains("labyroyale") || serverName.contains("laby")) {
            handleLabyRoyaleJoin(party, player, targetServer);
            return;
        }

        // For all other servers: teleport party members immediately
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

        // Force the member to execute /lr partywait <leaderName> after a short delay
        // so the Spigot server has time to register the player
        plugin.getProxy().getScheduler().schedule(plugin, new Runnable() {
            @Override
            public void run() {
                if (member.isConnected()) {
                    member.chat("/lr partywait " + leaderName);
                }
            }
        }, 500, TimeUnit.MILLISECONDS);
    }

    // ==================== STANDARD SERVER FOLLOW ====================

    private void teleportPartyToServer(Party party, ProxiedPlayer leader, ServerInfo server) {
        String displayName = getDisplayServerName(server.getName());

        for (UUID memberUuid : party.getMembers()) {
            if (memberUuid.equals(leader.getUniqueId())) continue;

            ProxiedPlayer member = plugin.getProxy().getPlayer(memberUuid);
            if (member == null || !member.isConnected()) continue;

            if (member.getServer() != null && member.getServer().getInfo().equals(server)) continue;

            Msg.send(member, "&d\u25B6 &7Téléportation vers &a" + displayName + " &7(groupe de &e" + leader.getName() + "&7)");
            member.connect(server);
        }
    }

    // ==================== LABYROYALE INTEGRATION ====================
    //
    // Flow (zero plugin messaging):
    // 1. Leader joins LabyRoyale -> notify members, wait 2s
    // 2. Send members to LabyRoyale server
    // 3. On ServerConnectedEvent: force member.chat("/lr partywait <leaderName>")
    // 4. Spigot side: /lr partywait marks member as waiting, freezes them (NO cabin)
    // 5. When leader picks a mode, ModeSelectListener auto-joins all waiters
    //

    private void handleLabyRoyaleJoin(final Party party, final ProxiedPlayer leader, final ServerInfo server) {
        // Collect members to send
        final List<UUID> memberUuids = new ArrayList<UUID>();
        for (UUID memberUuid : party.getMembers()) {
            if (memberUuid.equals(leader.getUniqueId())) continue;
            ProxiedPlayer member = plugin.getProxy().getPlayer(memberUuid);
            if (member != null && member.isConnected()) {
                memberUuids.add(memberUuid);
                Msg.send(member, "&e" + leader.getName() + " &7choisit un mode de jeu sur &dLabyRoyale&7...");
                Msg.send(member, "&7Vous serez téléportés automatiquement !");
            }
        }

        if (memberUuids.isEmpty()) return;

        // Register pending members and send them after 2 seconds
        for (UUID uuid : memberUuids) {
            pendingLabyMembers.put(uuid, leader.getName());
        }

        plugin.getProxy().getScheduler().schedule(plugin, new Runnable() {
            @Override
            public void run() {
                for (UUID uuid : memberUuids) {
                    ProxiedPlayer member = plugin.getProxy().getPlayer(uuid);
                    if (member == null || !member.isConnected()) {
                        pendingLabyMembers.remove(uuid);
                        continue;
                    }
                    // Don't send if already on this server
                    if (member.getServer() != null && member.getServer().getInfo().equals(server)) {
                        // Already there - force the command directly
                        member.chat("/lr partywait " + leader.getName());
                        pendingLabyMembers.remove(uuid);
                        continue;
                    }
                    Msg.send(member, "&d\u25B6 &7Téléportation vers &dLabyRoyale&7...");
                    member.connect(server);
                }
            }
        }, 2, TimeUnit.SECONDS);

        // Auto-expire pending entries after 30 seconds
        plugin.getProxy().getScheduler().schedule(plugin, new Runnable() {
            @Override
            public void run() {
                for (UUID uuid : memberUuids) {
                    pendingLabyMembers.remove(uuid);
                }
            }
        }, 30, TimeUnit.SECONDS);
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
                    Msg.send(member, "&c- &e" + playerName + " &7s'est déconnecté et a quitté le groupe. &8(" + remaining.getSize() + ")");

                    if (wasLeader && remaining.isLeader(memberUuid)) {
                        Msg.send(member, "&6\u2605 &eVous êtes maintenant le chef du groupe !");
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
