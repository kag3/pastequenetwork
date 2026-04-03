package fr.pastequeworld.party.data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PartyManager {

    // Player UUID -> Party they belong to
    private final Map<UUID, Party> playerPartyMap = new ConcurrentHashMap<UUID, Party>();

    // All active parties
    private final Set<Party> parties = Collections.newSetFromMap(new ConcurrentHashMap<Party, Boolean>());

    public static final int MAX_PARTY_SIZE = 16;

    // ==================== PARTY OPERATIONS ====================

    /**
     * Create a new party with the given player as leader.
     * Returns null if the player is already in a party.
     */
    public Party createParty(UUID leader) {
        if (playerPartyMap.containsKey(leader)) {
            return null;
        }
        Party party = new Party(leader);
        parties.add(party);
        playerPartyMap.put(leader, party);
        return party;
    }

    /**
     * Get the party a player belongs to, or null.
     */
    public Party getParty(UUID player) {
        return playerPartyMap.get(player);
    }

    /**
     * Get the party whose leader matches the given UUID.
     */
    public Party getPartyByLeader(UUID leader) {
        Party party = playerPartyMap.get(leader);
        if (party != null && party.isLeader(leader)) {
            return party;
        }
        return null;
    }

    /**
     * Find a party that has an invite pending for this player, by leader name.
     */
    public Party findPartyWithInvite(UUID player, UUID leaderUuid) {
        Party party = playerPartyMap.get(leaderUuid);
        if (party != null && party.hasInvite(player)) {
            return party;
        }
        return null;
    }

    /**
     * Find any party that has an invite pending for this player.
     */
    public Party findAnyPartyWithInvite(UUID player) {
        for (Party party : parties) {
            if (party.hasInvite(player)) {
                return party;
            }
        }
        return null;
    }

    /**
     * Add a player to a party.
     */
    public void joinParty(UUID player, Party party) {
        party.addMember(player);
        playerPartyMap.put(player, party);
    }

    /**
     * Remove a player from their party.
     * Handles leader succession and party disbanding.
     * Returns the party if it still exists, null if disbanded.
     */
    public Party leaveParty(UUID player) {
        Party party = playerPartyMap.remove(player);
        if (party == null) return null;

        party.removeMember(player);

        if (party.getSize() == 0) {
            // Party is empty, disband
            parties.remove(party);
            return null;
        }

        if (party.isLeader(player)) {
            // Need new leader
            party.promoteNextLeader();
        }

        // If only 1 member left, disband
        if (party.getSize() == 1) {
            UUID last = party.getMembers().iterator().next();
            playerPartyMap.remove(last);
            parties.remove(party);
            return null;
        }

        return party;
    }

    /**
     * Disband a party entirely.
     */
    public void disbandParty(Party party) {
        for (UUID member : party.getMembers()) {
            playerPartyMap.remove(member);
        }
        parties.remove(party);
    }

    /**
     * Transfer leadership to another member.
     */
    public boolean transferLeadership(Party party, UUID newLeader) {
        if (!party.isMember(newLeader)) return false;
        party.setLeader(newLeader);
        return true;
    }

    /**
     * Check if a player is in any party.
     */
    public boolean isInParty(UUID player) {
        return playerPartyMap.containsKey(player);
    }
}
