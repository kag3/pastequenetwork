package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EndEventCommand implements CommandExecutor {
    private final PastequeSkyblockPlugin plugin;

    public EndEventCommand(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PastequeSkyblockPlugin.color("&2&lPasteque &8\u00bb &cCommande joueur uniquement."));
            return true;
        }
        Player player = (Player) sender;
        if (label.equalsIgnoreCase("endevent")) {
            if (args.length >= 1 && args[0].equalsIgnoreCase("status")) {
                if (!plugin.getEndEventManager().isActive()) {
                    MessageUtil.send(player, plugin.getPrefix(), "&fAucun End Event actif.");
                    return true;
                }
                long sec = plugin.getEndEventManager().getRemainingSeconds();
                long min = sec / 60L;
                long rem = sec % 60L;
                MessageUtil.send(player, plugin.getPrefix(), "&dPasteque Dragon actif. Temps restant: &e" + min + "m " + rem + "s");
                return true;
            }
            plugin.getEndEventManager().teleport(player);
            return true;
        }
        if (!player.hasPermission("pastequeskyblock.admin")) {
            MessageUtil.send(player, plugin.getPrefix(), "&fCommande admin.");
            return true;
        }
        if (args.length == 0) {
            MessageUtil.send(player, plugin.getPrefix(), "&7/aend launch, stop, setspawn");
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("launch")) {
            plugin.getEndEventManager().launch(true);
            MessageUtil.send(player, plugin.getPrefix(), "&aEnd Event lancé.");
            return true;
        }
        if (sub.equals("stop")) {
            plugin.getEndEventManager().stop(false);
            MessageUtil.send(player, plugin.getPrefix(), "&aEnd Event arrêté et nettoyé.");
            return true;
        }
        if (sub.equals("setspawn")) {
            plugin.getEndEventManager().setEventSpawn(player.getLocation());
            MessageUtil.send(player, plugin.getPrefix(), "&aSpawn de l'End Event défini.");
            return true;
        }
        MessageUtil.send(player, plugin.getPrefix(), "&7/aend launch, stop, setspawn");
        return true;
    }
}
