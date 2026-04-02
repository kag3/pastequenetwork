package fr.pastequeworld.friends.util;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class Msg {

    public static final String PREFIX = "\u00a76\u00a7l\u2726 \u00a7eAmis \u00a76\u00a7l\u2726 \u00a77\u00bb \u00a7f";

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(TextComponent.fromLegacyText(PREFIX + color(message)));
    }

    public static void sendRaw(CommandSender sender, String message) {
        sender.sendMessage(TextComponent.fromLegacyText(color(message)));
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Send a clickable accept/deny message for friend requests.
     */
    public static void sendFriendRequest(CommandSender sender, String fromName) {
        TextComponent msg = new TextComponent(TextComponent.fromLegacyText(
                PREFIX + color("&e" + fromName + " &7vous a envoyé une demande d'ami ! ")));

        TextComponent accept = new TextComponent(TextComponent.fromLegacyText(
                color("&a&l[ACCEPTER]")));
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/friend accept " + fromName));
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(color("&aCliquez pour accepter")).create()));

        TextComponent space = new TextComponent(" ");

        TextComponent deny = new TextComponent(TextComponent.fromLegacyText(
                color("&c&l[REFUSER]")));
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/friend deny " + fromName));
        deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(color("&cCliquez pour refuser")).create()));

        sender.sendMessage(msg, accept, space, deny);
    }

    /**
     * Send a clickable join button for friend list.
     */
    public static TextComponent createJoinButton(String playerName, String serverName) {
        TextComponent btn = new TextComponent(TextComponent.fromLegacyText(
                color(" &8[&a\u25B6&8]")));
        btn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/friend join " + playerName));
        btn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(color("&7Rejoindre &e" + playerName + " &7sur &a" + serverName)).create()));
        return btn;
    }
}
