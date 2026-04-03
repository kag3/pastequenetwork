package fr.pastequeworld.labyroyal.listener;

import fr.pastequeworld.labyroyal.LabyRoyalPlugin;
import fr.pastequeworld.labyroyal.game.LabyGameMode;
import fr.pastequeworld.labyroyal.util.MessageUtil;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Receives PARTY_INFO from BungeeCord PastequeParty plugin.
 * Stores party data so that when the leader picks a mode,
 * all party members auto-join the same mode.
 *
 * Flow:
 * 1. BungeeCord sends PARTY_INFO (leader UUID + member UUIDs)
 * 2. Members arrive on the server and get the cabin GUI
 * 3. Leader picks a mode -> ModeSelectListener calls autoJoinPartyMembers()
 * 4. All members skip the cabin and join the same mode
 */
public class PartyChannelListener implements PluginMessageListener {

    private final LabyRoyalPlugin plugin;

    // Leader UUID -> list of member UUIDs (awaiting mode choice)
    private final Map<UUID, List<UUID>> pendingParties = new ConcurrentHashMap<UUID, List<UUID>>();

    // Member UUID -> Leader UUID (reverse lookup)
    private final Map<UUID, UUID> memberToLeader = new ConcurrentHashMap<UUID, UUID>();

    public PartyChannelListener(LabyRoyalPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player dummy, byte[] data) {
        if (!"PastequeParty".equals(channel)) return;

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        try {
            String subChannel = in.readUTF();

            if ("PARTY_INFO".equals(subChannel)) {
                String leaderUuidStr = in.readUTF();
                UUID leaderUuid = UUID.fromString(leaderUuidStr);

                int memberCount = in.readInt();
                List<UUID> members = new ArrayList<UUID>();
                for (int i = 0; i < memberCount; i++) {
                    UUID memberUuid = UUID.fromString(in.readUTF());
                    members.add(memberUuid);
                    memberToLeader.put(memberUuid, leaderUuid);
                }

                pendingParties.put(leaderUuid, members);
                plugin.getLogger().info("Party reçue: leader=" + leaderUuidStr
                        + ", " + memberCount + " membre(s)");

                // Auto-expire after 60 seconds (in case leader never picks)
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() {
                        cleanupParty(leaderUuid);
                    }
                }, 1200L); // 60 seconds
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Erreur lecture message PastequeParty: " + e.getMessage());
        }
    }

    /**
     * Called by ModeSelectListener when the leader picks a mode.
     * Auto-joins all party members to the same mode.
     */
    public void autoJoinPartyMembers(UUID leaderUuid, LabyGameMode mode) {
        List<UUID> members = pendingParties.remove(leaderUuid);
        if (members == null || members.isEmpty()) return;

        for (final UUID memberUuid : members) {
            memberToLeader.remove(memberUuid);

            // Small delay to let them load fully
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    Player member = Bukkit.getPlayer(memberUuid);
                    if (member == null || !member.isOnline()) return;

                    // Mark as chosen so cabin doesn't reopen
                    plugin.getModeSelectListener().markChosen(memberUuid);

                    // Unfreeze
                    member.setWalkSpeed(0.2f);
                    member.setFlySpeed(0.1f);
                    member.closeInventory();

                    // Auto-join
                    MessageUtil.send(member, "&d\u25B6 &7Votre chef a choisi &e" + mode.getDisplayName() + " &7!");
                    MessageUtil.send(member, "&eRejoindre la partie " + mode.getDisplayName() + "...");

                    boolean joined = plugin.getGameManager().joinGame(member, mode);
                    if (joined) {
                        MessageUtil.send(member, "&aVous avez rejoint la partie !");
                    }
                }
            }, 10L);
        }
    }

    /**
     * Check if a player is a party member waiting for leader's choice.
     */
    public boolean isPartyMember(UUID playerUuid) {
        return memberToLeader.containsKey(playerUuid);
    }

    /**
     * Get the leader UUID for a party member.
     */
    public UUID getLeaderOf(UUID memberUuid) {
        return memberToLeader.get(memberUuid);
    }

    private void cleanupParty(UUID leaderUuid) {
        List<UUID> members = pendingParties.remove(leaderUuid);
        if (members != null) {
            for (UUID uuid : members) {
                memberToLeader.remove(uuid);
            }
        }
    }
}
