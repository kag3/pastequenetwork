package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.playershop.PlayerShopManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class MyShopAdminCommand implements CommandExecutor, TabCompleter {

    private final PastequeSkyblockPlugin plugin;
    private final PlayerShopManager shopManager;

    public MyShopAdminCommand(PastequeSkyblockPlugin plugin, PlayerShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pastequemylittleshop.admin")) {
            sender.sendMessage(shopManager.prefix() + PastequeSkyblockPlugin.color("&fTu n'as pas acc\u00e8s \u00e0 cette commande."));
            return true;
        }
        if (args.length >= 3 && "delete".equalsIgnoreCase(args[0])) {
            String owner = args[1];
            StringBuilder builder = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                if (i > 2) builder.append(' ');
                builder.append(args[i]);
            }
            if (shopManager.adminDelete(owner, builder.toString())) {
                sender.sendMessage(shopManager.prefix() + PastequeSkyblockPlugin.color(plugin.getConfig().getString("messages.admin-shop-deleted").replace("%shop%", builder.toString()).replace("%owner%", owner)));
            } else {
                sender.sendMessage(shopManager.message("messages.unknown-shop"));
            }
            return true;
        }
        sender.sendMessage(PastequeSkyblockPlugin.color(plugin.getConfig().getString("messages.help-admin-delete")));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 1) out.add("delete");
        return out;
    }
}
