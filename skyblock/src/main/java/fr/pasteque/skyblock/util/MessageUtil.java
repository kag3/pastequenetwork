package fr.pasteque.skyblock.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class MessageUtil {
    private MessageUtil() {
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    public static String prefix(String rawPrefix, String message) {
        return color(rawPrefix + message);
    }

    public static void send(CommandSender sender, String rawPrefix, String message) {
        sender.sendMessage(prefix(rawPrefix, message));
    }

    public static String bool(boolean value) {
        return value ? color("&aactivé") : color("&cdésactivé");
    }
}
