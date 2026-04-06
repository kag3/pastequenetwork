package fr.pasteque.skyblock.guild;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Guild {

    private static final int[] XP_THRESHOLDS = {0, 500, 1500, 3000, 6000, 10000, 16000, 25000, 40000, 60000};
    private static final int[] MAX_MEMBERS   = {5, 8, 10, 12, 15, 18, 20, 25, 30, 40};

    private final String id;
    private String displayName;
    private UUID leader;
    private final Set<UUID> officers = new HashSet<UUID>();
    private final Set<UUID> members = new HashSet<UUID>();
    private final long createdAt;
    private String motd;
    private int level;
    private int xp;

    public Guild(String id, String displayName, UUID leader, long createdAt) {
        this.id = id;
        this.displayName = displayName;
        this.leader = leader;
        this.createdAt = createdAt;
        this.level = 1;
        this.xp = 0;
        this.motd = "";
        this.members.add(leader);
    }

    /* ── XP / Level ── */

    public void addXp(int amount) {
        this.xp += amount;
        while (level < 10 && xp >= XP_THRESHOLDS[level]) {
            level++;
        }
    }

    public int getXpForNextLevel() {
        if (level >= 10) return 0;
        return XP_THRESHOLDS[level];
    }

    /* ── Members ── */

    public boolean addMember(UUID uuid) {
        if (members.size() >= getMaxMembers()) return false;
        return members.add(uuid);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        officers.remove(uuid);
    }

    public void promote(UUID uuid) {
        if (members.contains(uuid) && !uuid.equals(leader)) {
            officers.add(uuid);
        }
    }

    public void demote(UUID uuid) {
        officers.remove(uuid);
    }

    public boolean isLeader(UUID uuid) {
        return leader.equals(uuid);
    }

    public boolean isOfficer(UUID uuid) {
        return officers.contains(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public int getMaxMembers() {
        int idx = Math.max(0, Math.min(level - 1, MAX_MEMBERS.length - 1));
        return MAX_MEMBERS[idx];
    }

    /* ── Getters / Setters ── */

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public UUID getLeader() { return leader; }
    public void setLeader(UUID leader) { this.leader = leader; }
    public Set<UUID> getOfficers() { return officers; }
    public Set<UUID> getMembers() { return members; }
    public long getCreatedAt() { return createdAt; }
    public String getMotd() { return motd; }
    public void setMotd(String motd) { this.motd = motd; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = Math.max(1, Math.min(10, level)); }
    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }
}
