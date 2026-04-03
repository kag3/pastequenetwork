package fr.pastequeworld.party.util;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class Msg {

    public static final String PREFIX = "\u00a76\u00a7l\u2726 \u00a7dGroupe \u00a76\u00a7l\u2726 \u00a77\u00bb \u00a7f";

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(TextComponent.fromLegacyText(PREFIX + color(message)));
    }

    public static void sendRaw(CommandSender sender, String message) {
        sender.sendMessage(TextComponent.fromLegacyText(color(message)));
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static void sendInvite(CommandSender sender, String fromName) {
        TextComponent msg = new TextComponent(TextComponent.fromLegacyText(
                PREFIX + color("&e" + fromName + " &7vous invite à rejoindre son groupe ! ")));

        TextComponent accept = new TextComponent(TextComponent.fromLegacyText(
                color("&a&l[ACCEPTER]")));
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party accept " + fromName));
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(color("&aCliquez pour rejoindre le groupe")).create()));

        TextComponent space = new TextComponent(" ");

        TextComponent deny = new TextComponent(TextComponent.fromLegacyText(
                color("&c&l[REFUSER]")));
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party deny " + fromName));
        deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(color("&cCliquez pour refuser")).create()));

        sender.sendMessage(msg, accept, space, deny);
    }
}
