package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.island.IslandWarpManager;
import fr.pasteque.skyblock.island.model.IslandWarp;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class IslandWarpCommand implements CommandExecutor {

    private final PastequeSkyblockPlugin plugin;
    private final IslandWarpManager warpManager;

    public IslandWarpCommand(PastequeSkyblockPlugin plugin, IslandWarpManager warpManager) {
        this.plugin = plugin;
        this.warpManager = warpManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Commande joueur uniquement.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        // /iswarp set <name>
        if (sub.equals("set")) {
            if (args.length < 2) {
                MessageUtil.send(player, plugin.getPrefix(), "&cUtilisation: /iswarp set <nom>");
                return true;
            }
            warpManager.createWarp(player, args[1]);
            return true;
        }

        // /iswarp delete <name>
        if (sub.equals("delete") || sub.equals("del") || sub.equals("remove")) {
            if (args.length < 2) {
                MessageUtil.send(player, plugin.getPrefix(), "&cUtilisation: /iswarp delete <nom>");
                return true;
            }
            warpManager.deleteWarp(player, args[1]);
            return true;
        }

        // /iswarp list
        if (sub.equals("list")) {
            List<IslandWarp> warps = warpManager.getWarps(player.getUniqueId());
            if (warps.isEmpty()) {
                MessageUtil.send(player, plugin.getPrefix(), "&7Tu n'as aucun warp.");
                return true;
            }
            MessageUtil.send(player, plugin.getPrefix(), "&aTes warps:");
            for (IslandWarp warp : warps) {
                String visibility = warp.isPublic() ? "&a[Public]" : "&c[Prive]";
                MessageUtil.send(player, "", " &7- &e" + warp.getName() + " " + visibility
                        + " &7(" + (int) warp.getX() + ", " + (int) warp.getY() + ", " + (int) warp.getZ() + ")");
            }
            return true;
        }

        // /iswarp public
        if (sub.equals("public")) {
            warpManager.openPublicWarpsGui(player);
            return true;
        }

        // /iswarp <player> [name] - teleport to player's warp
        @SuppressWarnings("deprecation")
        Player target = Bukkit.getPlayer(sub);
        if (target == null) {
            // Try offline player
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(sub);
            if (offlineTarget == null || !offlineTarget.hasPlayedBefore()) {
                MessageUtil.send(player, plugin.getPrefix(), "&cJoueur &e" + sub + " &cnon trouve.");
                return true;
            }
            UUID targetUuid = offlineTarget.getUniqueId();
            handleWarpTeleport(player, targetUuid, args);
            return true;
        }

        handleWarpTeleport(player, target.getUniqueId(), args);
        return true;
    }

    private void handleWarpTeleport(Player player, UUID targetOwner, String[] args) {
        if (args.length >= 2) {
            // Specific warp name
            warpManager.warpTo(player, targetOwner, args[1]);
        } else {
            // Open warp GUI for that island
            warpManager.openWarpGui(player, targetOwner);
        }
    }

    private void sendUsage(Player player) {
        MessageUtil.send(player, plugin.getPrefix(), "&eCommandes de warps d'ile:");
        MessageUtil.send(player, "", " &7/iswarp set <nom> &f- Creer un warp");
        MessageUtil.send(player, "", " &7/iswarp delete <nom> &f- Supprimer un warp");
        MessageUtil.send(player, "", " &7/iswarp list &f- Voir tes warps");
        MessageUtil.send(player, "", " &7/iswarp <joueur> [nom] &f- Se teleporter au warp d'un joueur");
        MessageUtil.send(player, "", " &7/iswarp public &f- Voir les warps publics");
    }
}
