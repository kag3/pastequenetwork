package fr.pasteque.skyblock.listener;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ShopListener implements Listener {
    private final PastequeSkyblockPlugin plugin;

    public ShopListener(PastequeSkyblockPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onSignCreate(SignChangeEvent event) {
        String tag = plugin.getConfig().getString("shop.sign-tag", "[PSHOP]");
        if (!event.getLine(0).equalsIgnoreCase(tag)) return;
        if (!event.getPlayer().hasPermission("pastequeskyblock.admin")) {
            event.getPlayer().sendMessage(MessageUtil.prefix(plugin.getPrefix(), "&fTu n'es pas autoris\u00e9 \u00e0 cr\u00e9er ce shop."));
            return;
        }
        Material material = Material.matchMaterial(event.getLine(1));
        if (material == null) {
            event.getPlayer().sendMessage(MessageUtil.prefix(plugin.getPrefix(), "&fMat\u00e9riau invalide."));
            return;
        }
        event.setLine(0, tag);
        event.getPlayer().sendMessage(MessageUtil.prefix(plugin.getPrefix(), "&aPanneau shop cr\u00e9\u00e9."));
    }

    @EventHandler
    public void onShopClick(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Block block = event.getClickedBlock();
        if (!(block.getState() instanceof Sign)) return;
        Sign sign = (Sign) block.getState();
        if (!plugin.getShopManager().isShopSign(sign)) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        Material material = plugin.getShopManager().getMaterial(sign);
        int amount = plugin.getShopManager().getAmount(sign);
        if (material == null || amount <= 0) { MessageUtil.send(player, plugin.getPrefix(), "&fPanneau shop invalide."); return; }
        if (event.getAction().name().contains("RIGHT")) {
            double price = plugin.getShopManager().getBuyPrice(sign);
            if (price < 0) { MessageUtil.send(player, plugin.getPrefix(), "&fAucun achat disponible sur ce panneau."); return; }
            if (!plugin.getEconomyManager().take(player.getUniqueId(), price)) { MessageUtil.send(player, plugin.getPrefix(), "&fTu n'as pas assez de " + plugin.getEconomyManager().getCurrencyName() + "."); return; }
            player.getInventory().addItem(new ItemStack(material, amount));
            MessageUtil.send(player, plugin.getPrefix(), "&aAchat r\u00e9ussi : &f" + material.name() + " x" + amount + " &7pour &e" + price + " " + plugin.getEconomyManager().getCurrencyName());
        } else if (event.getAction().name().contains("LEFT")) {
            double price = plugin.getShopManager().getSellPrice(sign);
            if (price < 0) { MessageUtil.send(player, plugin.getPrefix(), "&fAucune revente disponible sur ce panneau."); return; }
            if (!removeItems(player, material, amount)) { MessageUtil.send(player, plugin.getPrefix(), "&fTu n'as pas assez d'objets pour vendre."); return; }
            plugin.getEconomyManager().add(player.getUniqueId(), price);
            MessageUtil.send(player, plugin.getPrefix(), "&aVente r\u00e9ussie : &f" + material.name() + " x" + amount + " &7pour &e" + price + " " + plugin.getEconomyManager().getCurrencyName());
        }
    }

    private boolean removeItems(Player player, Material material, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) if (item != null && item.getType() == material) count += item.getAmount();
        if (count < amount) return false;
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != material) continue;
            if (item.getAmount() <= remaining) { remaining -= item.getAmount(); contents[i] = null; }
            else { item.setAmount(item.getAmount() - remaining); remaining = 0; }
            if (remaining <= 0) break;
        }
        player.getInventory().setContents(contents);
        player.updateInventory();
        return true;
    }
}
