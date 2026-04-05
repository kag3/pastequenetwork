package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MoneyCommand implements CommandExecutor {
    private final PastequeSkyblockPlugin plugin;

    public MoneyCommand(PastequeSkyblockPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) && !command.getName().equalsIgnoreCase("money")) {
            sender.sendMessage("Commande joueur uniquement.");
            return true;
        }
        if (command.getName().equalsIgnoreCase("money") || command.getName().equalsIgnoreCase("balance") || command.getName().equalsIgnoreCase("bal")) {
            Player player = (Player) sender;
            double balance = plugin.getEconomyManager().getBalance(player.getUniqueId());
            MessageUtil.send(player, plugin.getPrefix(), "&7Solde : &e" + balance + " " + plugin.getEconomyManager().getCurrencyName());
            return true;
        }
        Player player = (Player) sender;
        if (args.length < 2) {
            MessageUtil.send(player, plugin.getPrefix(), "&7Usage: /pay <joueur> <montant>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            MessageUtil.send(player, plugin.getPrefix(), "&fJoueur introuvable.");
            return true;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (Exception e) {
            MessageUtil.send(player, plugin.getPrefix(), "&fMontant invalide.");
            return true;
        }
        if (amount <= 0) {
            MessageUtil.send(player, plugin.getPrefix(), "&fMontant invalide.");
            return true;
        }
        if (!plugin.getEconomyManager().take(player.getUniqueId(), amount)) {
            MessageUtil.send(player, plugin.getPrefix(), "&fTu n'as pas assez de " + plugin.getEconomyManager().getCurrencyName() + ".");
            return true;
        }
        plugin.getEconomyManager().add(target.getUniqueId(), amount);
        MessageUtil.send(player, plugin.getPrefix(), "&aTu as envoyé &e" + amount + " &aà &f" + target.getName());
        MessageUtil.send(target, plugin.getPrefix(), "&aTu as reçu &e" + amount + " &ade &f" + player.getName());
        return true;
    }
}
