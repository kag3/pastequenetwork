package fr.pasteque.skyblock.listener;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.playershop.PlayerShopManager;
import fr.pasteque.skyblock.playershop.model.PendingShopCreation;
import fr.pasteque.skyblock.playershop.model.Shop;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;

public class PlayerShopListener implements Listener {

    private final PastequeSkyblockPlugin plugin;
    private final PlayerShopManager shopManager;

    public PlayerShopListener(PastequeSkyblockPlugin plugin, PlayerShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        if (block.getType() != Material.SIGN_POST && block.getType() != Material.WALL_SIGN) {
            return;
        }
        Shop shop = shopManager.getShopByLocation(block.getLocation());
        if (shop == null) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (player.getUniqueId().equals(shop.getOwner())) {
                shopManager.openOwnerManage(player, shop);
            } else {
                shopManager.buyOne(player, shop);
            }
            return;
        }
        if (player.getUniqueId().equals(shop.getOwner())) {
            shopManager.openOwnerManage(player, shop);
        } else {
            shopManager.openView(player, shop);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        String title = inventory.getTitle();
        if (title == null) {
            return;
        }

        if (title.startsWith(PlayerShopManager.CREATE_GUI_TITLE)) {
            int raw = event.getRawSlot();
            if (raw >= inventory.getSize()) {
                return;
            }
            if (raw == 49) {
                event.setCancelled(true);
                if (!shopManager.validatePendingDeposit(player)) {
                    player.sendMessage(shopManager.message("messages.invalid-mixed-stock"));
                    return;
                }
                PendingShopCreation pending = shopManager.getPending(player.getUniqueId());
                if (pending != null) {
                    player.closeInventory();
                    player.sendMessage(shopManager.message("messages.pending-price"));
                }
                return;
            }
            if (raw >= 45) {
                event.setCancelled(true);
            }
            return;
        }

        if (title.startsWith(PlayerShopManager.VIEW_GUI_TITLE)) {
            event.setCancelled(true);
            return;
        }

        if (title.startsWith(PlayerShopManager.OWNER_GUI_TITLE)) {
            int raw = event.getRawSlot();
            if (raw >= inventory.getSize()) {
                return;
            }
            if (raw < 27 || raw >= 45) {
                event.setCancelled(true);
            }
            Shop shop = shopManager.getOpenOwnerSession(player.getUniqueId());
            if (shop == null) {
                return;
            }
            if (raw >= 45) {
                shopManager.handleOwnerInventory(player, inventory, shop, raw);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();
        String title = inventory.getTitle();
        if (title == null) {
            return;
        }
        if (title.startsWith(PlayerShopManager.CREATE_GUI_TITLE)) {
            PendingShopCreation pending = shopManager.getPending(player.getUniqueId());
            if (pending != null && !pending.isWaitingPrice()) {
                shopManager.cancelPending(player, true);
                player.sendMessage(shopManager.message("messages.creation-cancelled"));
            }
            return;
        }
        if (title.startsWith(PlayerShopManager.OWNER_GUI_TITLE)) {
            shopManager.clearOpenOwnerSession(player.getUniqueId(), inventory);
        }
    }
}
