package fr.pasteque.skyblockarena.command;

import fr.pasteque.skyblockarena.PastequeSkyBlockArenaPlugin;
import fr.pasteque.skyblockarena.service.ArenaKitService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ArenaKitCommand implements CommandExecutor {

    private final PastequeSkyBlockArenaPlugin plugin;
    private final ArenaKitService arenaKitService;

    public ArenaKitCommand(PastequeSkyBlockArenaPlugin plugin, ArenaKitService arenaKitService) {
        this.plugin = plugin;
        this.arenaKitService = arenaKitService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.color(plugin.prefix() + "&fCommande réservée aux joueurs."));
            return true;
        }
        arenaKitService.open((Player) sender);
        return true;
    }
}
