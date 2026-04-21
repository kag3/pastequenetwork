package fr.pastequeworld.bedwars.util;

/**
 * Helpers pour formater des durees (scoreboard / events).
 */
public final class TimeUtil {

    private TimeUtil() {}

    public static String format(int totalSeconds) {
        if (totalSeconds < 0) totalSeconds = 0;
        int m = totalSeconds / 60;
        int s = totalSeconds % 60;
        return (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
    }

    public static String formatShort(int totalSeconds) {
        if (totalSeconds < 60) return totalSeconds + "s";
        int m = totalSeconds / 60;
        int s = totalSeconds % 60;
        if (s == 0) return m + "m";
        return m + "m" + s + "s";
    }
}
