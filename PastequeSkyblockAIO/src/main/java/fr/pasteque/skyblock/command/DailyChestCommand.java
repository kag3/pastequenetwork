package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.daily.DailyRewardManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * /dailychest give [joueur]  — donne l'ender chest quotidien (admin)
 */
public class DailyChestCommand implements CommandExecutor {

    private final PastequeSkyblockPlugin plugin;
    private final DailyRewardManager manager;

    public DailyChestCommand(PastequeSkyblockPlugin plugin, DailyRewardManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pastequeskyblock.admin") && !sender.isOp()) {
            sender.sendMessage(PastequeSkyblockPlugin.color("&cPermission requise."));
            return true;
        }
        if (args.length == 0 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(PastequeSkyblockPlugin.color("&eUsage: /dailychest give [joueur]"));
            return true;
        }
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(PastequeSkyblockPlugin.color("&cJoueur introuvable."));
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(PastequeSkyblockPlugin.color("&cPrecisez un joueur depuis la console."));
            return true;
        }
        ItemStack item = manager.createDailyChestItem();
        target.getInventory().addItem(item);
        target.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                + "&aVous avez recu un &6Coffre de Recompense Quotidienne &a. Posez-le ou vous voulez !"));
        if (target != sender) {
            sender.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                    + "&aCoffre donne a &e" + target.getName() + "&a."));
        }
        return true;
    }
}
