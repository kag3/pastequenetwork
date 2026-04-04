package fr.pasteque.skyblock.util;

import java.util.concurrent.TimeUnit;

public final class TimeUtil {

    private TimeUtil() {
    }

    public static long parseDuration(String input) {
        if (input == null || input.trim().isEmpty()) {
            return -1L;
        }
        long total = 0L;
        StringBuilder current = new StringBuilder();
        for (char c : input.toLowerCase().toCharArray()) {
            if (Character.isDigit(c)) {
                current.append(c);
                continue;
            }
            if (current.length() == 0) {
                return -1L;
            }
            long value = Long.parseLong(current.toString());
            current.setLength(0);
            switch (c) {
                case 's': total += TimeUnit.SECONDS.toMillis(value); break;
                case 'm': total += TimeUnit.MINUTES.toMillis(value); break;
                case 'h': total += TimeUnit.HOURS.toMillis(value); break;
                case 'd': total += TimeUnit.DAYS.toMillis(value); break;
                default: return -1L;
            }
        }
        if (current.length() > 0) {
            return -1L;
        }
        return total;
    }

    public static String formatRemaining(long expiresAt) {
        if (expiresAt <= 0L) {
            return "Permanent";
        }
        long remaining = expiresAt - System.currentTimeMillis();
        if (remaining <= 0L) {
            return "Expiré";
        }
        long days = TimeUnit.MILLISECONDS.toDays(remaining);
        remaining -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(remaining);
        remaining -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(remaining);
        remaining -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(remaining);

        StringBuilder out = new StringBuilder();
        if (days > 0) out.append(days).append("j ");
        if (hours > 0) out.append(hours).append("h ");
        if (minutes > 0) out.append(minutes).append("m ");
        if (seconds > 0 || out.length() == 0) out.append(seconds).append("s");
        return out.toString().trim();
    }
}
