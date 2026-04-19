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
        try {
            player.sendTitle(ColorUtil.color(title == null ? "" : title),
                    ColorUtil.color(subtitle == null ? "" : subtitle), fadeIn, stay, fadeOut);
        } catch (NoSuchMethodError ignored) {
            // fallback sans durees (Spigot pre-1.9)
            player.sendTitle(ColorUtil.color(title == null ? "" : title),
                    ColorUtil.color(subtitle == null ? "" : subtitle));
        }
    }

    public static void clear(Player player) {
        if (player == null) return;
        player.resetTitle();
    }
}
