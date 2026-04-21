package fr.pastequeworld.bedwars.command;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.game.Arena;
import fr.pastequeworld.bedwars.game.GameMode;
import fr.pastequeworld.bedwars.util.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BedWarsCommand implements CommandExecutor {

    private final BedWarsPlugin plugin;

    public BedWarsCommand(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        if ("help".equals(sub)) {
            help(sender);
            return true;
        }

        if ("reload".equals(sub)) {
            if (!sender.hasPermission("pastequebedwars.admin")) {
                sender.sendMessage(plugin.getMessageManager().get("errors.no-permission"));
                return true;
            }
            plugin.reloadConfig();
            plugin.getConfigManager().load();
            plugin.getMessageManager().load();
            plugin.getShopConfig().load();
            plugin.getMapRegistry().load();
            sender.sendMessage(ColorUtil.color("&aConfiguration rechargee."));
            return true;
        }

        if ("leave".equals(sub) || "quit".equals(sub)) {
            if (!(sender instanceof Player)) return true;
            plugin.getQueueManager().leave((Player) sender);
            return true;
        }

        if ("join".equals(sub)) {
            if (!(sender instanceof Player)) return true;
            if (args.length < 2) {
                sender.sendMessage(ColorUtil.color("&cUtilisation: /bw join <solo|duo|teams>"));
                return true;
            }
            GameMode mode = GameMode.fromConfigKey(args[1]);
            if (mode == null) {
                sender.sendMessage(ColorUtil.color("&cMode invalide."));
                return true;
            }
            plugin.getQueueManager().join((Player) sender, mode);
            return true;
        }

        if ("start".equals(sub)) {
            if (!(sender instanceof Player)) return true;
            if (!sender.hasPermission("pastequebedwars.admin")) {
                sender.sendMessage(plugin.getMessageManager().get("errors.no-permission"));
                return true;
            }
            Arena arena = plugin.getArenaManager().getArenaOfPlayer((Player) sender);
            if (arena == null) {
                sender.sendMessage(plugin.getMessageManager().get("errors.not-in-game"));
                return true;
            }
            arena.start();
            sender.sendMessage(ColorUtil.color("&aPartie demarree manuellement."));
            return true;
        }

        if ("stop".equals(sub)) {
            if (!sender.hasPermission("pastequebedwars.admin")) {
                sender.sendMessage(plugin.getMessageManager().get("errors.no-permission"));
                return true;
            }
            if (!(sender instanceof Player)) return true;
            Arena arena = plugin.getArenaManager().getArenaOfPlayer((Player) sender);
            if (arena == null) {
                sender.sendMessage(plugin.getMessageManager().get("errors.not-in-game"));
                return true;
            }
            arena.end(null);
            return true;
        }

        if ("arenas".equals(sub)) {
            if (!sender.hasPermission("pastequebedwars.admin")) return true;
            sender.sendMessage(ColorUtil.color("&e&lARENES ACTIVES:"));
            for (Arena a : plugin.getArenaManager().getArenas()) {
                sender.sendMessage(ColorUtil.color("&7- &a" + a.getId()
                        + " &7[" + a.getState().name() + "] &f"
                        + a.getPlayers().size() + " joueurs"));
            }
            return true;
        }

        help(sender);
        return true;
    }

    private void help(CommandSender sender) {
        sender.sendMessage(ColorUtil.color("&7&m---------------------------------"));
        sender.sendMessage(ColorUtil.color("&b&lBedWars &7- &fAide"));
        sender.sendMessage(ColorUtil.color("&e/bw join <mode> &7- Rejoindre une partie"));
        sender.sendMessage(ColorUtil.color("&e/bw leave &7- Quitter la partie"));
        sender.sendMessage(ColorUtil.color("&e/bw help &7- Cette aide"));
        if (sender.hasPermission("pastequebedwars.admin")) {
            sender.sendMessage(ColorUtil.color("&c/bw start &7- Forcer le demarrage"));
            sender.sendMessage(ColorUtil.color("&c/bw stop &7- Forcer l'arret"));
            sender.sendMessage(ColorUtil.color("&c/bw reload &7- Recharger la config"));
            sender.sendMessage(ColorUtil.color("&c/bw arenas &7- Lister les arenes"));
            sender.sendMessage(ColorUtil.color("&c/bwsetup &7- Setup d'une map"));
        }
        sender.sendMessage(ColorUtil.color("&7&m---------------------------------"));
    }
}
