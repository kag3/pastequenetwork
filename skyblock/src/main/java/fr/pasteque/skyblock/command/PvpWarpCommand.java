package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PvpWarpCommand implements CommandExecutor {
    private final PastequeSkyblockPlugin plugin;
    public PvpWarpCommand(PastequeSkyblockPlugin plugin) { this.plugin = plugin; }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Commande joueur uniquement."); return true; }
        Player player = (Player) sender;
        if (plugin.getPvpManager().getHardcoreWarp() == null) { MessageUtil.send(player, plugin.getPrefix(), "&fAucun warp PvP hardcore n'est configuré."); return true; }
        player.teleport(plugin.getPvpManager().getHardcoreWarp());
        MessageUtil.send(player, plugin.getPrefix(), "&aTéléportation au warp PvP hardcore.");
        return true;
    }
}
