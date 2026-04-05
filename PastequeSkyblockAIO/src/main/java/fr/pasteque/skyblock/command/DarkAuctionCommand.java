package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.darkauction.DarkAuctionManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DarkAuctionCommand implements CommandExecutor {

    private final PastequeSkyblockPlugin plugin;
    private final DarkAuctionManager manager;

    public DarkAuctionCommand(PastequeSkyblockPlugin plugin, DarkAuctionManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PastequeSkyblockPlugin.color("&2&lPasteque &8\u00bb &cCommande joueur uniquement."));
            return true;
        }

        Player player = (Player) sender;

        // Admin force start
        if (args.length >= 1 && args[0].equalsIgnoreCase("start")) {
            if (!player.hasPermission("pastequeskyblock.admin")) {
                player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                        + "&cVous n'avez pas la permission."));
                return true;
            }
            if (manager.isActive()) {
                player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                        + "&cUne enchere est deja en cours !"));
                return true;
            }
            manager.startAuction();
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                    + "&aEnchere sombre demarree manuellement."));
            return true;
        }

        // Admin force stop
        if (args.length >= 1 && args[0].equalsIgnoreCase("stop")) {
            if (!player.hasPermission("pastequeskyblock.admin")) {
                player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                        + "&cVous n'avez pas la permission."));
                return true;
            }
            if (!manager.isActive()) {
                player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                        + "&cAucune enchere en cours."));
                return true;
            }
            manager.endAuction();
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                    + "&cEnchere terminee manuellement."));
            return true;
        }

        // Open GUI
        if (manager.isActive()) {
            manager.openAuctionGui(player);
        } else {
            long intervalMin = plugin.getConfig().getLong("dark-auction.interval-minutes", 120);
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                    + "&cAucune enchere en cours. Prochaine enchere dans environ &e" + intervalMin + " minutes&c."));
        }

        return true;
    }
}
