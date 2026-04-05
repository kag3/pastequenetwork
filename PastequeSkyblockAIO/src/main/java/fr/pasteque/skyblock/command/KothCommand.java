package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.serverevent.KingOfTheHillEvent;
import fr.pasteque.skyblock.serverevent.ServerEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /koth — teleporte le joueur dans l'arene KOTH si l'event est actif.
 */
public class KothCommand implements CommandExecutor {

    private final PastequeSkyblockPlugin plugin;

    public KothCommand(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PastequeSkyblockPlugin.color("&cCommande joueur uniquement."));
            return true;
        }
        Player player = (Player) sender;
        ServerEvent ev = plugin.getEventManager().get("koth");
        if (!(ev instanceof KingOfTheHillEvent) || !ev.isActive()) {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                    + "&cAucun KOTH n'est en cours actuellement."));
            return true;
        }
        ((KingOfTheHillEvent) ev).teleportToArena(player);
        return true;
    }
}
