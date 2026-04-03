package fr.pastequeworld.party.listener;

import fr.pastequeworld.party.PastequeParty;
import fr.pastequeworld.party.data.Party;
import fr.pastequeworld.party.data.PartyManager;
import fr.pastequeworld.party.util.Msg;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PartyListener implements Listener {

    private final PastequeParty plugin;

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

        // LabyRoyale: special handling - send party info to Spigot, then send members
        if (serverName.contains("labyroyale") || serverName.contains("laby")) {
            handleLabyRoyaleJoin(party, player, targetServer);
            return;
        }

        // For all other servers: teleport party members immediately
        teleportPartyToServer(party, player, targetServer);
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
    // Flow:
    // 1. Leader joins LabyRoyale -> we notify members
    // 2. After 1 second, we send PARTY_INFO to the Spigot server
    //    (BungeeCord -> Spigot direction, which works reliably)
    //    containing: leader UUID + all member UUIDs
    // 3. After 2 seconds, we send all members to LabyRoyale
    // 4. On Spigot side: members arrive, ModeSelectListener freezes them in cabin
    //    BUT the PartyChannelListener has stored the party info
    // 5. When the leader picks a mode, ModeSelectListener checks for party members
    //    and auto-joins them to the same mode (skipping their cabin GUI)
    //

    private void handleLabyRoyaleJoin(final Party party, final ProxiedPlayer leader, final ServerInfo server) {
        // Notify party members - they stay where they are
        for (UUID memberUuid : party.getMembers()) {
            if (memberUuid.equals(leader.getUniqueId())) continue;
            ProxiedPlayer member = plugin.getProxy().getPlayer(memberUuid);
            if (member != null) {
                Msg.send(member, "&e" + leader.getName() + " &7choisit un mode de jeu sur &dLabyRoyale&7...");
                Msg.send(member, "&7Vous serez téléportés automatiquement !");
            }
        }

        // Send PARTY_INFO to the LabyRoyale Spigot server (BungeeCord->Spigot)
        // The Spigot server stores this and when the leader picks a mode,
        // it uses BungeeCord "ConnectOther" to TP members directly to the game
        plugin.getProxy().getScheduler().schedule(plugin, new Runnable() {
            @Override
            public void run() {
                sendPartyInfoToServer(party, leader.getUniqueId(), server);
            }
        }, 1, TimeUnit.SECONDS);

        // Members DO NOT get sent here - they stay on their current server
        // They will be teleported by the Spigot plugin via "ConnectOther"
        // only AFTER the leader has chosen a game mode
    }

    /**
     * Send party info to the Spigot server via plugin messaging.
     * Direction: BungeeCord -> Spigot (reliable, used by LabyRoyalBungee already).
     *
     * Format:
     *   UTF: "PARTY_INFO"
     *   UTF: leader UUID
     *   INT: number of members (excluding leader)
     *   UTF[]: member UUIDs
     */
    private void sendPartyInfoToServer(Party party, UUID leaderUuid, ServerInfo server) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            out.writeUTF("PARTY_INFO");
            out.writeUTF(leaderUuid.toString());

            // Collect member UUIDs and names (needed for ConnectOther on Spigot side)
            List<String[]> members = new ArrayList<String[]>(); // [uuid, name]
            for (UUID uuid : party.getMembers()) {
                if (uuid.equals(leaderUuid)) continue;
                ProxiedPlayer member = plugin.getProxy().getPlayer(uuid);
                if (member != null) {
                    members.add(new String[]{uuid.toString(), member.getName()});
                }
            }

            out.writeInt(members.size());
            for (String[] entry : members) {
                out.writeUTF(entry[0]); // UUID
                out.writeUTF(entry[1]); // Name
            }

            server.sendData(PastequeParty.CHANNEL, b.toByteArray());
            plugin.getLogger().info("PARTY_INFO envoyé: leader=" + leaderUuid + ", membres=" + members.size());
        } catch (IOException e) {
            plugin.getLogger().warning("Erreur envoi PARTY_INFO: " + e.getMessage());
        }
    }

    // ==================== DISCONNECT ====================

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
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
