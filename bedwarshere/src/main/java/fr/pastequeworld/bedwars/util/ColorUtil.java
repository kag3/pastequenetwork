package fr.pastequeworld.bedwars.util;

import org.bukkit.ChatColor;

/**
 * Utilitaire de couleurs. Gere les codes & et les &#RRGGBB n'etant pas supportes
 * en 1.9.4, on se limite aux codes legacy.
 */
public final class ColorUtil {

    private ColorUtil() {}

    public static String color(String input) {
        if (input == null) return "";
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    public static String strip(String input) {
        if (input == null) return "";
        return ChatColor.stripColor(color(input));
    }
}
