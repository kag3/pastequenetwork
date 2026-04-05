package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.arena.DuelService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DuelCommand implements CommandExecutor {

    private final PastequeSkyblockPlugin plugin;
    private final DuelService duelService;

    public DuelCommand(PastequeSkyblockPlugin plugin, DuelService duelService) {
        this.plugin = plugin;
        this.duelService = duelService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&fCommande reservee aux joueurs."));
            return true;
        }
        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&fUtilisation : &d/duel <joueur> &fou &d/duel accept"));
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            duelService.accept(player);
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cJoueur introuvable ou hors ligne."));
            return true;
        }

        duelService.invite(player, target);
        return true;
    }
}
