package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.staff.StaffModeManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StaffCommand implements CommandExecutor {

    private final PastequeSkyblockPlugin plugin;
    private final StaffModeManager manager;

    public StaffCommand(PastequeSkyblockPlugin plugin, StaffModeManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PastequeSkyblockPlugin.color("&2&lPasteque &8\u00bb &cCommande joueur uniquement."));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("pasteque.staff")) {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                    + "&cVous n'avez pas la permission."));
            return true;
        }

        // /staff freeze <player>
        if (args.length >= 2 && args[0].equalsIgnoreCase("freeze")) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null || !target.isOnline()) {
                player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                        + "&cJoueur introuvable ou hors ligne."));
                return true;
            }
            manager.freezePlayer(player, target);
            return true;
        }

        // /staff - toggle staff mode
        manager.toggleStaffMode(player);
        return true;
    }
}
