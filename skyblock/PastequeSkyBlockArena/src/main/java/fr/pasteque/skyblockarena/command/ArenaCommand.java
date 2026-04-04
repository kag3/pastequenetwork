package fr.pasteque.skyblockarena.command;

import fr.pasteque.skyblockarena.PastequeSkyBlockArenaPlugin;
import fr.pasteque.skyblockarena.service.ArenaWorldService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ArenaCommand implements CommandExecutor {

    private final PastequeSkyBlockArenaPlugin plugin;
    private final ArenaWorldService arenaWorldService;

    public ArenaCommand(PastequeSkyBlockArenaPlugin plugin, ArenaWorldService arenaWorldService) {
        this.plugin = plugin;
        this.arenaWorldService = arenaWorldService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.color(plugin.prefix() + "&fCommande réservée aux joueurs."));
            return true;
        }
        Player player = (Player) sender;
        arenaWorldService.sendToArena(player);
        player.sendMessage(plugin.color(plugin.prefix() + plugin.getConfig().getString("messages.arena-teleport", "&fVous rejoignez l'arène &dskyblockarena&f.")));
        return true;
    }
}
