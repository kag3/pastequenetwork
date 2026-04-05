package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.island.IslandUpgradeManager;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class IslandUpgradeCommand implements CommandExecutor {

    private final PastequeSkyblockPlugin plugin;
    private final IslandUpgradeManager upgradeManager;

    public IslandUpgradeCommand(PastequeSkyblockPlugin plugin, IslandUpgradeManager upgradeManager) {
        this.plugin = plugin;
        this.upgradeManager = upgradeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Commande joueur uniquement.");
            return true;
        }
        Player player = (Player) sender;
        upgradeManager.openUpgradeGui(player);
        return true;
    }
}
