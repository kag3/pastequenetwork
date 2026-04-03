package fr.pastequeworld.labyroyal.listener;

import fr.pastequeworld.labyroyal.LabyRoyalPlugin;
import fr.pastequeworld.labyroyal.game.LabyGameMode;
import fr.pastequeworld.labyroyal.util.MessageUtil;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Receives PARTY_INFO from BungeeCord PastequeParty plugin.
 *
 * Flow:
 * 1. BungeeCord sends PARTY_INFO (leader UUID + member UUIDs + member names)
 * 2. Members stay on their current server (Hub etc.) - NOT teleported yet
 * 3. Leader picks a mode in the cabin
 * 4. ModeSelectListener calls autoJoinPartyMembers()
 * 5. This class uses BungeeCord "ConnectOther" to TP members to this server
 * 6. When members arrive (PlayerJoinEvent), they are auto-joined to the mode
 *    and NEVER see the cabin
 */
public class PartyChannelListener implements PluginMessageListener {

    private final LabyRoyalPlugin plugin;

    // Leader UUID -> list of member names (for ConnectOther)
    private final Map<UUID, List<String>> pendingParties = new ConcurrentHashMap<UUID, List<String>>();

    // Member UUID -> mode to auto-join when they arrive on this server
    private final Map<UUID, LabyGameMode> pendingAutoJoin = new ConcurrentHashMap<UUID, LabyGameMode>();

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
                List<String> memberNames = new ArrayList<String>();
                for (int i = 0; i < memberCount; i++) {
                    String memberUuid = in.readUTF();
                    String memberName = in.readUTF();
                    memberNames.add(memberName);
                }

                pendingParties.put(leaderUuid, memberNames);
                plugin.getLogger().info("Party reçue: leader=" + leaderUuidStr
                        + ", " + memberCount + " membre(s): " + memberNames);

                // Auto-expire after 120 seconds
                final UUID expireLeader = leaderUuid;
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() {
                        pendingParties.remove(expireLeader);
                    }
                }, 2400L);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Erreur lecture message PastequeParty: " + e.getMessage());
        }
    }

    /**
     * Called by ModeSelectListener when the leader picks a mode.
     * Uses BungeeCord "ConnectOther" to teleport members to this server,
     * then auto-joins them when they arrive.
     */
    public void autoJoinPartyMembers(UUID leaderUuid, LabyGameMode mode) {
        List<String> memberNames = pendingParties.remove(leaderUuid);
        if (memberNames == null || memberNames.isEmpty()) return;

        Player leader = Bukkit.getPlayer(leaderUuid);
        if (leader == null) return;

        // Get the name of THIS server (for ConnectOther)
        // We need any online player to send the plugin message
        String thisServer = plugin.getConfig().getString("general.server-name", "labyroyale");

        for (final String memberName : memberNames) {
            // Store pending auto-join for when the member arrives
            // We use name-based lookup since we might not have their UUID yet
            pendingAutoJoin.put(nameToTempUUID(memberName), mode);

            // Send ConnectOther via BungeeCord channel
            try {
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(b);
                out.writeUTF("ConnectOther");
                out.writeUTF(memberName);
                out.writeUTF(thisServer);
                leader.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());

                plugin.getLogger().info("ConnectOther: " + memberName + " -> " + thisServer);
            } catch (IOException e) {
                plugin.getLogger().warning("Erreur ConnectOther pour " + memberName + ": " + e.getMessage());
            }
        }

        // Auto-expire pending auto-joins after 30 seconds
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                for (String name : memberNames) {
                    pendingAutoJoin.remove(nameToTempUUID(name));
                }
            }
        }, 600L);
    }

    /**
     * Called by ModeSelectListener.onPlayerJoin to check if this player
     * should skip the cabin and auto-join a game mode.
     * Returns the mode to auto-join, or null if not a party member.
     */
    public LabyGameMode getAndClearAutoJoin(Player player) {
        // Check by name (case-insensitive)
        UUID key = nameToTempUUID(player.getName());
        return pendingAutoJoin.remove(key);
    }

    /**
     * Check if a player has a pending auto-join.
     */
    public boolean hasPendingAutoJoin(Player player) {
        return pendingAutoJoin.containsKey(nameToTempUUID(player.getName()));
    }

    /**
     * Create a deterministic UUID from a player name for lookup.
     * This is just for our internal map, not a real UUID.
     */
    private UUID nameToTempUUID(String name) {
        return UUID.nameUUIDFromBytes(("party:" + name.toLowerCase()).getBytes());
    }
}
