package fr.pasteque.skyblock.guild;

import java.util.UUID;

public class GuildInvite {

    private final UUID sender;
    private final UUID target;
    private final String guildId;
    private final long sentAt;

    public GuildInvite(UUID sender, UUID target, String guildId, long sentAt) {
        this.sender = sender;
        this.target = target;
        this.guildId = guildId;
        this.sentAt = sentAt;
    }

    public UUID getSender() { return sender; }
    public UUID getTarget() { return target; }
    public String getGuildId() { return guildId; }
    public long getSentAt() { return sentAt; }

    public boolean isExpired() {
        return System.currentTimeMillis() - sentAt > 60000L;
    }
}
