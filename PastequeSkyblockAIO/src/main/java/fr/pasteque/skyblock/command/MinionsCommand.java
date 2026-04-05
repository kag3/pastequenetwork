package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.minion.MinionShopGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /minions — ouvre la boutique de minions.
 */
public class MinionsCommand implements CommandExecutor {

    private final PastequeSkyblockPlugin plugin;

    public MinionsCommand(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PastequeSkyblockPlugin.color("&cCommande joueur uniquement."));
            return true;
        }
        MinionShopGui.open((Player) sender);
        return true;
    }
}
