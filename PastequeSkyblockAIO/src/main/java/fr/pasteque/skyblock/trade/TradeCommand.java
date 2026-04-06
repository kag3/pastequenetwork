package fr.pasteque.skyblock.trade;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TradeCommand implements CommandExecutor {

    private final PastequeSkyblockPlugin plugin;
    private final TradeManager tradeManager;

    public TradeCommand(PastequeSkyblockPlugin plugin, TradeManager tradeManager) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&fCommande reservee aux joueurs."));
            return true;
        }

        Player player = (Player) sender;
        String prefix = plugin.getPrefix();

        if (args.length < 1) {
            player.sendMessage(PastequeSkyblockPlugin.color(prefix + "&fUtilisation :"));
            player.sendMessage(PastequeSkyblockPlugin.color(prefix + "&d/trade <joueur> &f- Envoyer une demande d'echange"));
            player.sendMessage(PastequeSkyblockPlugin.color(prefix + "&d/trade accept &f- Accepter une demande"));
            player.sendMessage(PastequeSkyblockPlugin.color(prefix + "&d/trade deny &f- Refuser une demande"));
            player.sendMessage(PastequeSkyblockPlugin.color(prefix + "&d/trade cancel &f- Annuler l'echange en cours"));
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("accept")) {
            tradeManager.acceptRequest(player);
            return true;
        }

        if (sub.equals("deny")) {
            tradeManager.denyRequest(player);
            return true;
        }

        if (sub.equals("cancel")) {
            if (!tradeManager.hasActiveTrade(player.getUniqueId())) {
                player.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cVous n'avez pas d'echange en cours."));
                return true;
            }
            tradeManager.cancelTrade(player.getUniqueId());
            return true;
        }

        // /trade <player> — send request
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cJoueur introuvable ou hors ligne."));
            return true;
        }

        tradeManager.sendRequest(player, target);
        return true;
    }
}
