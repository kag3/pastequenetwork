package fr.pastequeworld.party.data;

import java.util.*;

public class Party {

    private UUID leader;
    private final Set<UUID> members = new LinkedHashSet<UUID>();
    private final Map<UUID, Long> pendingInvites = new HashMap<UUID, Long>();

    private static final long INVITE_TIMEOUT = 60000; // 60 seconds

    public Party(UUID leader) {
        this.leader = leader;
        this.members.add(leader);
    }

    public UUID getLeader() {
        return leader;
    }

    public void setLeader(UUID leader) {
        this.leader = leader;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public int getSize() {
        return members.size();
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public boolean isLeader(UUID uuid) {
        return leader.equals(uuid);
    }

    public void addMember(UUID uuid) {
        members.add(uuid);
        pendingInvites.remove(uuid);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    // ==================== INVITES ====================

    public void addInvite(UUID uuid) {
        pendingInvites.put(uuid, System.currentTimeMillis());
    }

    public boolean hasInvite(UUID uuid) {
        Long time = pendingInvites.get(uuid);
        if (time == null) return false;
        if (System.currentTimeMillis() - time > INVITE_TIMEOUT) {
            pendingInvites.remove(uuid);
            return false;
        }
        return true;
    }

    public void removeInvite(UUID uuid) {
        pendingInvites.remove(uuid);
    }

    /**
     * Pick a new leader from remaining members.
     * Returns the new leader UUID, or null if party is empty.
     */
    public UUID promoteNextLeader() {
        if (members.isEmpty()) return null;
        UUID newLeader = members.iterator().next();
        this.leader = newLeader;
        return newLeader;
    }
}
