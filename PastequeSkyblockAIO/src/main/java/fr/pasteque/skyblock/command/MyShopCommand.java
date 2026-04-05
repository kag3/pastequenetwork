package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.playershop.PlayerShopManager;
import fr.pasteque.skyblock.playershop.SignPlacementUtil;
import fr.pasteque.skyblock.playershop.model.PendingShopCreation;
import fr.pasteque.skyblock.playershop.model.Shop;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MyShopCommand implements CommandExecutor, TabCompleter {

    private final PastequeSkyblockPlugin plugin;
    private final PlayerShopManager shopManager;

    public MyShopCommand(PastequeSkyblockPlugin plugin, PlayerShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PastequeSkyblockPlugin.color("&2&lPasteque &8\u00bb &cCommande joueur uniquement."));
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        String sub = args[0].toLowerCase();
        if ("create".equals(sub)) {
            if (args.length < 2) {
                sendHelp(player);
                return true;
            }
            if (!shopManager.canCreate(player)) {
                player.sendMessage(shopManager.message("messages.only-owner-island"));
                return true;
            }
            String shopName = join(args, 1);
            if (shopManager.ownerHasShopName(player.getUniqueId(), shopName)) {
                player.sendMessage(shopManager.message("messages.existing-name"));
                return true;
            }
            Block target = getTarget(player);
            if (target == null || target.getType() == Material.AIR) {
                player.sendMessage(shopManager.message("messages.must-look-block"));
                return true;
            }
            SignPlacementUtil.SignPlacement placement = SignPlacementUtil.findPlacement(player, target);
            if (placement == null) {
                player.sendMessage(shopManager.message("messages.must-look-block"));
                return true;
            }
            shopManager.openCreation(player, shopName, placement);
            player.sendMessage(shopManager.message("messages.pending-created"));
            return true;
        }
        if ("price".equals(sub)) {
            if (args.length < 2) {
                sendHelp(player);
                return true;
            }
            PendingShopCreation pending = shopManager.getPending(player.getUniqueId());
            if (pending == null || !pending.isWaitingPrice()) {
                player.sendMessage(shopManager.message("messages.no-pending-shop"));
                return true;
            }
            double price;
            try {
                price = Double.parseDouble(args[1]);
            } catch (NumberFormatException exception) {
                player.sendMessage(shopManager.message("messages.invalid-price"));
                return true;
            }
            if (price <= 0D) {
                player.sendMessage(shopManager.message("messages.invalid-price"));
                return true;
            }
            Shop shop = shopManager.finalizePending(player, price);
            if (shop == null) {
                player.sendMessage(shopManager.message("messages.no-pending-shop"));
                return true;
            }
            player.sendMessage(shopManager.prefix() + PastequeSkyblockPlugin.color(plugin.getConfig().getString("messages.shop-created").replace("%shop%", shop.getName()).replace("%price%", plugin.getEconomyManager().format(price))));
            return true;
        }
        if ("delete".equals(sub)) {
            if (args.length < 2) {
                sendHelp(player);
                return true;
            }
            String shopName = join(args, 1);
            if (!shopManager.deleteShop(player, shopName)) {
                player.sendMessage(shopManager.message("messages.unknown-shop"));
            }
            return true;
        }
        sendHelp(player);
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(PastequeSkyblockPlugin.color(plugin.getConfig().getString("messages.help-header")));
        player.sendMessage(PastequeSkyblockPlugin.color(plugin.getConfig().getString("messages.help-create")));
        player.sendMessage(PastequeSkyblockPlugin.color(plugin.getConfig().getString("messages.help-price")));
        player.sendMessage(PastequeSkyblockPlugin.color(plugin.getConfig().getString("messages.help-delete")));
    }

    private String join(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) builder.append(' ');
            builder.append(args[i]);
        }
        return builder.toString();
    }

    @SuppressWarnings("deprecation")
    private Block getTarget(Player player) {
        return player.getTargetBlock((java.util.HashSet<Byte>) null, plugin.getConfig().getInt("shops.look-distance", 6));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 1) {
            out.add("create");
            out.add("price");
            out.add("delete");
        }
        return out;
    }
}
