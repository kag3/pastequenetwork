package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.arena.EloService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EloCommand implements CommandExecutor {

    private final PastequeSkyblockPlugin plugin;
    private final EloService eloService;

    public EloCommand(PastequeSkyblockPlugin plugin, EloService eloService) {
        this.plugin = plugin;
        this.eloService = eloService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&fCommande reservee aux joueurs."));
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            showOwnElo(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("top")) {
            showTopElo(player);
            return true;
        }

        // /elo <player>
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cJoueur introuvable ou hors ligne."));
            return true;
        }

        int elo = eloService.getElo(target.getUniqueId());
        String rank = eloService.getRank(elo);
        String color = eloService.getRankColor(elo);
        player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&fELO de &d" + target.getName() + " &f: &d" + elo + " &f(" + color + rank + "&f)"));
        return true;
    }

    private void showOwnElo(Player player) {
        int elo = eloService.getElo(player.getUniqueId());
        String rank = eloService.getRank(elo);
        String color = eloService.getRankColor(elo);
        player.sendMessage(PastequeSkyblockPlugin.color("&5&l&m--------&r &5&lVotre Classement ELO &5&l&m--------"));
        player.sendMessage(PastequeSkyblockPlugin.color("  &fELO : &d" + elo));
        player.sendMessage(PastequeSkyblockPlugin.color("  &fRang : " + color + rank));
        player.sendMessage(PastequeSkyblockPlugin.color("&5&l&m-------------------------------------"));
    }

    private void showTopElo(Player player) {
        List<Map.Entry<UUID, Integer>> top = eloService.getTopPlayers(10);
        player.sendMessage(PastequeSkyblockPlugin.color("&5&l&m--------&r &5&lClassement ELO Top 10 &5&l&m--------"));
        if (top.isEmpty()) {
            player.sendMessage(PastequeSkyblockPlugin.color("  &7Aucun joueur classe."));
        } else {
            int rank = 1;
            for (Map.Entry<UUID, Integer> entry : top) {
                Player target = Bukkit.getPlayer(entry.getKey());
                String name = target != null ? target.getName() : entry.getKey().toString().substring(0, 8);
                int elo = entry.getValue();
                String rankName = eloService.getRank(elo);
                String color = eloService.getRankColor(elo);
                player.sendMessage(PastequeSkyblockPlugin.color("  &d#" + rank + " &f" + name + " &7- &d" + elo + " &f(" + color + rankName + "&f)"));
                rank++;
            }
        }
        player.sendMessage(PastequeSkyblockPlugin.color("&5&l&m-----------------------------------------"));
    }
}
