package fr.pasteque.skyblock.guard.model;

public class SanctionEntry {
    private final String id;
    private final SanctionType type;
    private final String targetName;
    private final String staffName;
    private final String reason;
    private final long createdAt;
    private final long expiresAt;
    private final boolean active;

    public SanctionEntry(String id, SanctionType type, String targetName, String staffName, String reason, long createdAt, long expiresAt, boolean active) {
        this.id = id;
        this.type = type;
        this.targetName = targetName;
        this.staffName = staffName;
        this.reason = reason;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.active = active;
    }

    public String getId() { return id; }
    public SanctionType getType() { return type; }
    public String getTargetName() { return targetName; }
    public String getStaffName() { return staffName; }
    public String getReason() { return reason; }
    public long getCreatedAt() { return createdAt; }
    public long getExpiresAt() { return expiresAt; }
    public boolean isActive() { return active; }
    public boolean isPermanent() { return expiresAt <= 0L; }
    public boolean isExpired() { return !isPermanent() && System.currentTimeMillis() >= expiresAt; }
}
