package fr.pastequeworld.party.listener;

import fr.pastequeworld.party.PastequeParty;
import fr.pastequeworld.party.data.Party;
import fr.pastequeworld.party.data.PartyManager;
import fr.pastequeworld.party.util.Msg;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.*;
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

        // If the leader is going to LabyRoyale, do NOT teleport party yet.
        // We wait for the Spigot plugin to send us the mode choice.
        if (serverName.contains("labyroyale") || serverName.contains("laby")) {
            // Notify party that leader is choosing a mode
            for (UUID memberUuid : party.getMembers()) {
                if (memberUuid.equals(player.getUniqueId())) continue;
                ProxiedPlayer member = plugin.getProxy().getPlayer(memberUuid);
                if (member != null) {
                    Msg.send(member, "&e" + player.getName() + " &7choisit un mode de jeu sur &dLabyRoyale&7...");
                    Msg.send(member, "&7Vous serez téléportés automatiquement !");
                }
            }
            return;
        }

        // For all other servers: teleport party members immediately
        teleportPartyToServer(party, player, targetServer);
    }

    /**
     * Teleport all party members (except leader) to the given server.
     */
    private void teleportPartyToServer(Party party, ProxiedPlayer leader, ServerInfo server) {
        String displayName = getDisplayServerName(server.getName());

        for (UUID memberUuid : party.getMembers()) {
            if (memberUuid.equals(leader.getUniqueId())) continue;

            ProxiedPlayer member = plugin.getProxy().getPlayer(memberUuid);
            if (member == null || !member.isConnected()) continue;

            // Don't teleport if already on that server
            if (member.getServer() != null && member.getServer().getInfo().equals(server)) continue;

            Msg.send(member, "&d\u25B6 &7Téléportation vers &a" + displayName + " &7(groupe de &e" + leader.getName() + "&7)");
            member.connect(server);
        }
    }

    // ==================== LABYROYALE INTEGRATION ====================
    // When the leader picks a mode in LabyRoyale, the Spigot plugin sends
    // a plugin message on channel "PastequeParty" with:
    //   - UTF: leader UUID
    //   - UTF: game mode (SOLO/DUO/DUEL)
    // We then send all party members to the LabyRoyale server, and forward
    // a message for each member so they auto-join the same mode.

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equals(PastequeParty.CHANNEL)) return;

        // Read the message
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()));
        try {
            String subChannel = in.readUTF();

            if ("MODE_CHOSEN".equals(subChannel)) {
                String leaderUuidStr = in.readUTF();
                String gameMode = in.readUTF();
                UUID leaderUuid = UUID.fromString(leaderUuidStr);

                handleLeaderModeChosen(leaderUuid, gameMode);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Erreur lecture message plugin: " + e.getMessage());
        }
    }

    private void handleLeaderModeChosen(UUID leaderUuid, String gameMode) {
        PartyManager pm = plugin.getPartyManager();
        Party party = pm.getPartyByLeader(leaderUuid);

        if (party == null) return;

        ProxiedPlayer leader = plugin.getProxy().getPlayer(leaderUuid);
        if (leader == null || leader.getServer() == null) return;

        ServerInfo labyServer = leader.getServer().getInfo();
        String leaderName = leader.getName();

        for (UUID memberUuid : party.getMembers()) {
            if (memberUuid.equals(leaderUuid)) continue;

            ProxiedPlayer member = plugin.getProxy().getPlayer(memberUuid);
            if (member == null || !member.isConnected()) continue;

            Msg.send(member, "&d\u25B6 &e" + leaderName + " &7a choisi &d" + gameMode
                    + " &7! Téléportation vers &aLabyRoyale&7...");

            // Send member to LabyRoyale server
            member.connect(labyServer);

            // After a short delay, send a plugin message to the Spigot server
            // telling it to auto-join this player to the chosen mode
            final UUID memUuid = memberUuid;
            final String mode = gameMode;
            final ServerInfo server = labyServer;

            plugin.getProxy().getScheduler().schedule(plugin, new Runnable() {
                @Override
                public void run() {
                    try {
                        ByteArrayOutputStream b = new ByteArrayOutputStream();
                        DataOutputStream out = new DataOutputStream(b);
                        out.writeUTF("PARTY_JOIN");
                        out.writeUTF(memUuid.toString());
                        out.writeUTF(mode);
                        server.sendData(PastequeParty.CHANNEL, b.toByteArray());
                    } catch (IOException e) {
                        plugin.getLogger().warning("Erreur envoi PARTY_JOIN: " + e.getMessage());
                    }
                }
            }, 1, TimeUnit.SECONDS);
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
