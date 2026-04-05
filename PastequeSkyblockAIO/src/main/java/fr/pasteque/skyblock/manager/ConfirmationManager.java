package fr.pasteque.skyblock.manager;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConfirmationManager {
    public enum ActionType { RESET, DELETE }

    public static class PendingAction {
        private final ActionType type;
        private final long expireAt;

        public PendingAction(ActionType type, long expireAt) {
            this.type = type;
            this.expireAt = expireAt;
        }

        public ActionType getType() { return type; }
        public long getExpireAt() { return expireAt; }
    }

    private final PastequeSkyblockPlugin plugin;
    private final Map<UUID, PendingAction> pending = new HashMap<UUID, PendingAction>();

    public ConfirmationManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public void request(UUID uuid, ActionType type) {
        long seconds = plugin.getConfig().getLong("confirm.expire-seconds", 20L);
        pending.put(uuid, new PendingAction(type, System.currentTimeMillis() + (seconds * 1000L)));
    }

    public PendingAction get(UUID uuid) {
        PendingAction action = pending.get(uuid);
        if (action == null) return null;
        if (System.currentTimeMillis() > action.getExpireAt()) {
            pending.remove(uuid);
            return null;
        }
        return action;
    }

    public boolean consume(UUID uuid, ActionType expected) {
        PendingAction action = get(uuid);
        if (action == null || action.getType() != expected) return false;
        pending.remove(uuid);
        return true;
    }

    public void clear(UUID uuid) { pending.remove(uuid); }
}
