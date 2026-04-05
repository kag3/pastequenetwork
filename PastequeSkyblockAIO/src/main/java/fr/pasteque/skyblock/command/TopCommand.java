package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.leaderboard.LeaderboardGui;
import fr.pasteque.skyblock.leaderboard.LeaderboardManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /top                        — ouvre la GUI du classement global
 * /top money|elo|ile          — envoie le top 10 dans le chat
 * /top set money|elo|ile      — (admin) pose un hologramme a la position
 * /top remove money|elo|ile   — (admin) enleve un hologramme
 */
public class TopCommand implements CommandExecutor {

    private final PastequeSkyblockPlugin plugin;
    private final LeaderboardManager manager;

    public TopCommand(PastequeSkyblockPlugin plugin, LeaderboardManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(PastequeSkyblockPlugin.color("&cUsage: /top <money|elo|ile>"));
                return true;
            }
            LeaderboardGui.open((Player) sender, manager);
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("set") || sub.equals("remove")) {
            if (!sender.hasPermission("pastequeskyblock.admin") && !sender.isOp()) {
                sender.sendMessage(PastequeSkyblockPlugin.color("&cPermission requise."));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(PastequeSkyblockPlugin.color("&cUsage: /top " + sub + " <money|elo|ile>"));
                return true;
            }
            String type = args[1].toLowerCase();
            if (sub.equals("set")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(PastequeSkyblockPlugin.color("&cCommande joueur uniquement."));
                    return true;
                }
                Location loc = ((Player) sender).getLocation().add(0, 2, 0);
                manager.placeHologram(type, loc);
                sender.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                        + "&aHologramme &e" + type + " &apose a votre position."));
            } else {
                manager.removeHologram(type);
                sender.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                        + "&aHologramme &e" + type + " &asupprime."));
            }
            return true;
        }

        // Chat top display
        List<LeaderboardManager.Entry> entries;
        String title;
        String unit;
        if (sub.equals("money")) {
            title = "&6&l\u2726 TOP MONEY \u2726";
            entries = manager.getTopMoney(10);
            unit = " Pasteque";
        } else if (sub.equals("elo")) {
            title = "&c&l\u265b TOP ELO \u265b";
            entries = manager.getTopElo(10);
            unit = " ELO";
        } else if (sub.equals("ile") || sub.equals("island")) {
            title = "&a&l\u2766 TOP ILE \u2766";
            entries = manager.getTopIslands(10);
            unit = " niv";
        } else {
            sender.sendMessage(PastequeSkyblockPlugin.color("&cUsage: /top [money|elo|ile]"));
            return true;
        }
        sender.sendMessage(PastequeSkyblockPlugin.color("&8&m----------------------------"));
        sender.sendMessage(PastequeSkyblockPlugin.color("         " + title));
        if (entries.isEmpty()) {
            sender.sendMessage(PastequeSkyblockPlugin.color("  &7Aucun joueur classe."));
        } else {
            for (int i = 0; i < entries.size(); i++) {
                LeaderboardManager.Entry e = entries.get(i);
                sender.sendMessage(PastequeSkyblockPlugin.color(
                        "  &e" + (i + 1) + ". &f" + e.name + " &8- &e" + e.value + unit));
            }
        }
        sender.sendMessage(PastequeSkyblockPlugin.color("&8&m----------------------------"));
        return true;
    }
}
