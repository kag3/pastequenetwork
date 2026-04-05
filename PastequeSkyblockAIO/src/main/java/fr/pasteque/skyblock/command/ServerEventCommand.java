package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.serverevent.EventManager;
import fr.pasteque.skyblock.serverevent.ServerEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ServerEventCommand implements CommandExecutor {
    private final PastequeSkyblockPlugin plugin;

    public ServerEventCommand(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pastequeskyblock.admin")) {
            sender.sendMessage(PastequeSkyblockPlugin.color("&2&lPasteque &8\u00bb &cCommande admin uniquement."));
            return true;
        }
        EventManager em = plugin.getEventManager();
        if (args.length == 0) {
            usage(sender, em);
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("list")) {
            sender.sendMessage(PastequeSkyblockPlugin.color("&2&lPasteque &5Events &8\u00bb &fEvents disponibles:"));
            for (ServerEvent se : em.getAll()) {
                String state = se.isActive() ? "&a[ACTIF " + se.getRemainingSeconds() + "s]" : "&7[inactif]";
                sender.sendMessage(PastequeSkyblockPlugin.color(" &8- &d" + se.getId() + " &8(&f" + se.getDisplayName() + "&8) " + state));
            }
            sender.sendMessage(PastequeSkyblockPlugin.color("&7Events actifs simultanement: &e" + em.getActiveCount()));
            return true;
        }
        if (args.length < 2) {
            usage(sender, em);
            return true;
        }
        String id = args[1].toLowerCase();
        if (sub.equals("start")) {
            if (em.startEvent(id)) {
                sender.sendMessage(PastequeSkyblockPlugin.color("&2&lPasteque &8\u00bb &aEvent &d" + id + " &alance."));
            } else {
                sender.sendMessage(PastequeSkyblockPlugin.color("&2&lPasteque &8\u00bb &cEvent introuvable ou deja actif."));
            }
            return true;
        }
        if (sub.equals("stop")) {
            if (em.stopEvent(id)) {
                sender.sendMessage(PastequeSkyblockPlugin.color("&2&lPasteque &8\u00bb &aEvent &d" + id + " &aarrete."));
            } else {
                sender.sendMessage(PastequeSkyblockPlugin.color("&2&lPasteque &8\u00bb &cEvent introuvable ou pas actif."));
            }
            return true;
        }
        usage(sender, em);
        return true;
    }

    private void usage(CommandSender sender, EventManager em) {
        sender.sendMessage(PastequeSkyblockPlugin.color("&2&lPasteque &5Events &8\u00bb"));
        sender.sendMessage(PastequeSkyblockPlugin.color(" &8- &f/aevent list"));
        sender.sendMessage(PastequeSkyblockPlugin.color(" &8- &f/aevent start <id>"));
        sender.sendMessage(PastequeSkyblockPlugin.color(" &8- &f/aevent stop <id>"));
        StringBuilder ids = new StringBuilder();
        for (ServerEvent se : em.getAll()) {
            if (ids.length() > 0) ids.append(", ");
            ids.append(se.getId());
        }
        sender.sendMessage(PastequeSkyblockPlugin.color("&7Events: &f" + ids.toString()));
    }
}
