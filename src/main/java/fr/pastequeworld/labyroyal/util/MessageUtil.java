package fr.pastequeworld.labyroyal.util;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Collection;

public class MessageUtil {

    private static String prefix = "&6&l\u2726 &eLabyRoyal &6&l\u2726 &7\u00bb &f";

    public static void setPrefix(String prefix) {
        MessageUtil.prefix = prefix;
    }

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static void send(Player player, String message) {
        player.sendMessage(color(prefix + message));
    }

    public static void sendRaw(Player player, String message) {
        player.sendMessage(color(message));
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(color(title), color(subtitle), fadeIn, stay, fadeOut);
    }

    public static void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(color(message)));
    }

    public static void broadcast(Collection<? extends Player> players, String message) {
        String formatted = color(prefix + message);
        for (Player player : players) {
            player.sendMessage(formatted);
        }
    }

    public static void broadcastRaw(Collection<? extends Player> players, String message) {
        String formatted = color(message);
        for (Player player : players) {
            player.sendMessage(formatted);
        }
    }

    public static void broadcastTitle(Collection<? extends Player> players, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        String coloredTitle = color(title);
        String coloredSub = color(subtitle);
        for (Player player : players) {
            player.sendTitle(coloredTitle, coloredSub, fadeIn, stay, fadeOut);
        }
    }

    public static void broadcastActionBar(Collection<? extends Player> players, String message) {
        for (Player player : players) {
            sendActionBar(player, message);
        }
    }

    public static String formatTime(int seconds) {
        int min = seconds / 60;
        int sec = seconds % 60;
        if (min > 0) {
            return min + "m " + String.format("%02d", sec) + "s";
        }
        return sec + "s";
    }

    // Decorative line for chat
    public static String line() {
        return "&8&m                                                            ";
    }
}
