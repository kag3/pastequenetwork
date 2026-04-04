package fr.pasteque.mylittleshop.command;

import fr.pasteque.mylittleshop.PastequeMyLittleShopPlugin;
import fr.pasteque.mylittleshop.service.ShopManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class MyShopAdminCommand implements CommandExecutor, TabCompleter {

    private final PastequeMyLittleShopPlugin plugin;
    private final ShopManager shopManager;

    public MyShopAdminCommand(PastequeMyLittleShopPlugin plugin, ShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pastequemylittleshop.admin")) {
            sender.sendMessage(plugin.prefix() + plugin.color("&fTu n'as pas accès à cette commande."));
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
                sender.sendMessage(plugin.prefix() + plugin.color(plugin.getConfig().getString("messages.admin-shop-deleted").replace("%shop%", builder.toString()).replace("%owner%", owner)));
            } else {
                sender.sendMessage(plugin.message("messages.unknown-shop"));
            }
            return true;
        }
        sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.help-admin-delete")));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 1) out.add("delete");
        return out;
    }
}
