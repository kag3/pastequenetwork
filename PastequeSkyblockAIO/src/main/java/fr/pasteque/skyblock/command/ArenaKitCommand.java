package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.arena.ArenaKitService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ArenaKitCommand implements CommandExecutor {

    private final PastequeSkyblockPlugin plugin;
    private final ArenaKitService arenaKitService;

    public ArenaKitCommand(PastequeSkyblockPlugin plugin, ArenaKitService arenaKitService) {
        this.plugin = plugin;
        this.arenaKitService = arenaKitService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.color(plugin.getPrefix() + "&fCommande reservee aux joueurs."));
            return true;
        }
        arenaKitService.open((Player) sender);
        return true;
    }
}
