package fr.pasteque.skyblock.guard.model;

public class ReportEntry {
    private final String id;
    private final String author;
    private final String target;
    private final String reason;
    private final long createdAt;
    private final boolean handled;

    public ReportEntry(String id, String author, String target, String reason, long createdAt, boolean handled) {
        this.id = id;
        this.author = author;
        this.target = target;
        this.reason = reason;
        this.createdAt = createdAt;
        this.handled = handled;
    }

    public String getId() { return id; }
    public String getAuthor() { return author; }
    public String getTarget() { return target; }
    public String getReason() { return reason; }
    public long getCreatedAt() { return createdAt; }
    public boolean isHandled() { return handled; }
}
