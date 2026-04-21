package fr.pastequeworld.bedwars.util;

import org.bukkit.entity.Player;

/**
 * Helper pour envoyer des titles en 1.9.4 (API Bukkit native).
 */
public final class TitleUtil {

    private TitleUtil() {}

    public static void send(Player player, String title, String subtitle) {
        send(player, title, subtitle, 10, 40, 10);
    }

    public static void send(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null) return;
        String coloredTitle = ColorUtil.color(title == null ? "" : title);
        String coloredSubtitle = ColorUtil.color(subtitle == null ? "" : subtitle);
        try {
            java.lang.reflect.Method method = player.getClass()
                    .getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class);
            method.invoke(player, coloredTitle, coloredSubtitle, fadeIn, stay, fadeOut);
            return;
        } catch (NoSuchMethodException ignored) {
            // API 1.9.4 : signature sans timings
        } catch (Exception ignored) {
            // fallback si reflection impossible
        }
        player.sendTitle(coloredTitle, coloredSubtitle);
    }

    public static void clear(Player player) {
        if (player == null) return;
        player.resetTitle();
    }
}
