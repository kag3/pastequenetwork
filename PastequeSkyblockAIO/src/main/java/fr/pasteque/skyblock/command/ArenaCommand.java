package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.arena.ArenaWorldService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ArenaCommand implements CommandExecutor {

    private final PastequeSkyblockPlugin plugin;
    private final ArenaWorldService arenaWorldService;

    public ArenaCommand(PastequeSkyblockPlugin plugin, ArenaWorldService arenaWorldService) {
        this.plugin = plugin;
        this.arenaWorldService = arenaWorldService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.color(plugin.getPrefix() + "&fCommande reservee aux joueurs."));
            return true;
        }
        Player player = (Player) sender;
        arenaWorldService.sendToArena(player);
        player.sendMessage(plugin.color(plugin.getPrefix() + plugin.getConfig().getString("messages.arena-teleport", "&fVous rejoignez l'arene &dskyblockarena&f.")));
        return true;
    }
}
