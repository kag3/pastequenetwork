package fr.pasteque.skyblock.dungeon;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DungeonParty {

    public static final int MAX_SIZE = 5;

    private UUID leader;
    private final Set<UUID> members;
    private final Set<UUID> invited;

    public DungeonParty(UUID leader) {
        this.leader = leader;
        this.members = new HashSet<UUID>();
        this.members.add(leader);
        this.invited = new HashSet<UUID>();
    }

    // =========================================================================
    //  Getters
    // =========================================================================

    public UUID getLeader() {
        return leader;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public Set<UUID> getInvited() {
        return invited;
    }

    public int size() {
        return members.size();
    }

    // =========================================================================
    //  Actions
    // =========================================================================

    /**
     * Invite a player to the party. Returns false if already invited/member or party full.
     */
    public boolean invite(UUID uuid) {
        if (members.contains(uuid) || invited.contains(uuid)) {
            return false;
        }
        if (members.size() >= MAX_SIZE) {
            return false;
        }
        invited.add(uuid);
        return true;
    }

    /**
     * Accept an invitation. Returns false if not invited or party full.
     */
    public boolean accept(UUID uuid) {
        if (!invited.contains(uuid)) {
            return false;
        }
        if (members.size() >= MAX_SIZE) {
            invited.remove(uuid);
            return false;
        }
        invited.remove(uuid);
        members.add(uuid);
        return true;
    }

    /**
     * Remove a player from the party. If the leader leaves, promote next member or disband.
     * Returns true if the party still exists, false if it was disbanded.
     */
    public boolean leave(UUID uuid) {
        members.remove(uuid);
        if (members.isEmpty()) {
            return false;
        }
        if (uuid.equals(leader)) {
            leader = members.iterator().next();
        }
        return true;
    }

    /**
     * Kick a player from the party (leader action). Returns false if target is leader or not in party.
     */
    public boolean kick(UUID uuid) {
        if (uuid.equals(leader)) {
            return false;
        }
        return members.remove(uuid);
    }

    /**
     * Disband the party entirely.
     */
    public void disband() {
        members.clear();
        invited.clear();
    }

    /**
     * Check if the party is ready to start a dungeon.
     * All members must be online and not in another dungeon.
     */
    public boolean isReady(DungeonManager manager) {
        for (UUID uuid : members) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                return false;
            }
            if (manager.getPlayerInstanceId(uuid) != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if a UUID is a member of this party.
     */
    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    /**
     * Check if a UUID has been invited to this party.
     */
    public boolean isInvited(UUID uuid) {
        return invited.contains(uuid);
    }

    /**
     * Send a message to all online members.
     */
    public void broadcast(String message) {
        for (UUID uuid : members) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }
}
