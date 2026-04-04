package fr.pasteque.skyblock.guard;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.guard.model.SanctionType;
import fr.pasteque.skyblock.util.TimeUtil;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class FilterService {

    private static final long CLEANUP_INTERVAL_MS = 5L * 60L * 1000L;

    private final PastequeSkyblockPlugin plugin;
    private final SanctionService sanctionService;
    private final Map<UUID, Long> lastMessageAt = new HashMap<UUID, Long>();
    private final Map<UUID, String> lastNormalizedMessage = new HashMap<UUID, String>();
    private final Map<UUID, Integer> repeatCount = new HashMap<UUID, Integer>();
    private final Map<UUID, Integer> violations = new HashMap<UUID, Integer>();
    private final Map<UUID, Long> violationWindow = new HashMap<UUID, Long>();
    private final Map<UUID, Long> lastSeen = new HashMap<UUID, Long>();
    private long lastCleanup = System.currentTimeMillis();

    public FilterService(PastequeSkyblockPlugin plugin, SanctionService sanctionService) {
        this.plugin = plugin;
        this.sanctionService = sanctionService;
    }

    public boolean shouldBlock(Player player, String message) {
        if (player.hasPermission("pastequeguard.bypass.chatfilter")) {
            return false;
        }

        long now = System.currentTimeMillis();
        long minDelay = plugin.getConfig().getLong("auto-moderation.min-delay-ms", 1200L);
        long resetWindow = plugin.getConfig().getLong("auto-moderation.violation-reset-seconds", 25L) * 1000L;
        int repeatThreshold = plugin.getConfig().getInt("auto-moderation.repeat-threshold", 3);
        double capsThreshold = plugin.getConfig().getDouble("auto-moderation.caps-threshold", 0.72D);
        int maxSameCharRun = plugin.getConfig().getInt("auto-moderation.max-same-char-run", 12);

        UUID uuid = player.getUniqueId();
        lastSeen.put(uuid, now);
        Long lastAt = lastMessageAt.get(uuid);
        String normalized = normalizeMessage(message);

        boolean violation = false;
        if (lastAt != null && now - lastAt < minDelay) {
            violation = true;
        }

        String lastMessage = lastNormalizedMessage.get(uuid);
        if (lastMessage != null && lastMessage.equals(normalized)) {
            int count = repeatCount.containsKey(uuid) ? repeatCount.get(uuid) + 1 : 2;
            repeatCount.put(uuid, count);
            if (count >= repeatThreshold) {
                violation = true;
            }
        } else {
            repeatCount.put(uuid, 1);
        }

        if (capsRatio(message) >= capsThreshold) {
            violation = true;
        }
        if (largestRun(message) >= maxSameCharRun) {
            violation = true;
        }

        lastMessageAt.put(uuid, now);
        lastNormalizedMessage.put(uuid, normalized);

        if (!violation) {
            cleanupViolations(uuid, now, resetWindow);
            cleanupOldEntries(now);
            return false;
        }

        cleanupViolations(uuid, now, resetWindow);
        int total = violations.containsKey(uuid) ? violations.get(uuid) + 1 : 1;
        violations.put(uuid, total);
        violationWindow.put(uuid, now);

        player.sendMessage(plugin.getGuardPrefix() + plugin.color(plugin.getConfig().getString("messages.anti-spam-warn", "&fMerci d'arrêter de spam/flood sous peine de sanction.")));
        if (total >= 4) {
            long duration = TimeUtil.parseDuration(plugin.getConfig().getString("auto-moderation.automute-duration", "10m"));
            sanctionService.applySanction(player.getName(), player.getUniqueId(), "PastequeGuard", SanctionType.AUTOMUTE, "Spam/Flood automatique", duration);
            player.sendMessage(plugin.getGuardPrefix() + plugin.color(plugin.getConfig().getString("messages.auto-muted", "&fVous avez été temporairement réduit au silence pour spam/flood répété.")));
            violations.remove(uuid);
            violationWindow.remove(uuid);
        }

        cleanupOldEntries(now);
        return true;
    }

    private void cleanupViolations(UUID uuid, long now, long window) {
        Long last = violationWindow.get(uuid);
        if (last != null && now - last > window) {
            violations.remove(uuid);
            violationWindow.remove(uuid);
        }
    }

    /**
     * Removes entries older than 5 minutes from all HashMaps to prevent memory leaks.
     * Only runs the full scan at most once every 5 minutes.
     */
    private void cleanupOldEntries(long now) {
        if (now - lastCleanup < CLEANUP_INTERVAL_MS) {
            return;
        }
        lastCleanup = now;

        Iterator<Map.Entry<UUID, Long>> it = lastSeen.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            if (now - entry.getValue() > CLEANUP_INTERVAL_MS) {
                UUID uuid = entry.getKey();
                it.remove();
                lastMessageAt.remove(uuid);
                lastNormalizedMessage.remove(uuid);
                repeatCount.remove(uuid);
                violations.remove(uuid);
                violationWindow.remove(uuid);
            }
        }
    }

    private String normalizeMessage(String message) {
        return message.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private double capsRatio(String input) {
        int letters = 0;
        int upper = 0;
        for (char c : input.toCharArray()) {
            if (Character.isLetter(c)) {
                letters++;
                if (Character.isUpperCase(c)) {
                    upper++;
                }
            }
        }
        if (letters < 6) {
            return 0D;
        }
        return (double) upper / (double) letters;
    }

    private int largestRun(String input) {
        int best = 0;
        int current = 0;
        char last = '\0';
        for (char c : input.toCharArray()) {
            if (c == last) {
                current++;
            } else {
                current = 1;
                last = c;
            }
            if (current > best) {
                best = current;
            }
        }
        return best;
    }
}
