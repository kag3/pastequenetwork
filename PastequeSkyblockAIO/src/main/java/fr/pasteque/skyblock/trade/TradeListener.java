package fr.pasteque.skyblock.trade;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.gui.GuiHelper;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TradeListener implements Listener {

    private final PastequeSkyblockPlugin plugin;
    private final TradeManager tradeManager;

    /**
     * Players whose inventory close should be ignored (because we are
     * programmatically closing it, not the player).
     */
    private final Set<UUID> closingByPlugin;

    public TradeListener(PastequeSkyblockPlugin plugin, TradeManager tradeManager) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
        this.closingByPlugin = new HashSet<UUID>();
    }

    private boolean isTradeGui(Inventory inv) {
        if (inv == null) {
            return false;
        }
        String title = inv.getTitle();
        return title != null && title.startsWith(PastequeSkyblockPlugin.color("&2&lPasteque &5&lEchange"));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Inventory topInv = event.getView().getTopInventory();

        if (!isTradeGui(topInv)) {
            return;
        }

        TradeSession session = tradeManager.getSession(player.getUniqueId());
        if (session == null) {
            event.setCancelled(true);
            return;
        }

        // If trade is in countdown or completed/cancelled, block everything
        if (session.getState() == TradeSession.TradeState.COUNTDOWN
                || session.getState() == TradeSession.TradeState.COMPLETED
                || session.getState() == TradeSession.TradeState.CANCELLED) {
            event.setCancelled(true);
            return;
        }

        int rawSlot = event.getRawSlot();

        // Click in player's own inventory (below trade GUI)
        if (rawSlot >= 54) {
            // Allow picking up items from own inventory to place in trade
            // Only if clicking with an item, and they want to place it
            // We let them pick it up; they'll place it in the trade area
            return;
        }

        // All clicks in trade GUI are cancelled by default, we handle logic manually
        event.setCancelled(true);

        int row = rawSlot / 9;
        int col = rawSlot % 9;

        UUID myId = player.getUniqueId();
        boolean isP1 = myId.equals(session.getPlayer1());

        // Cancel button (slot 49)
        if (rawSlot == 49) {
            GuiHelper.playClick(player);
            closingByPlugin.add(session.getPlayer1());
            closingByPlugin.add(session.getPlayer2());
            tradeManager.cancelTrade(myId);
            return;
        }

        // Separator column (col 4) — do nothing
        if (col == 4) {
            return;
        }

        // Status items (row 4, cols 1 and 7) — do nothing
        if (row == 4 && (col == 1 || col == 7)) {
            return;
        }

        // Decoration glass (row 0, row 4 cols 0,2,6,8, row 5) — do nothing
        if (row == 0 || row == 5) {
            return;
        }
        if (row == 4 && (col == 0 || col == 2 || col == 6 || col == 8)) {
            return;
        }

        // My lock button: slot 39 (row 4, col 3) for the viewer
        if (rawSlot == 39) {
            handleLockClick(player, session);
            return;
        }

        // Other player's lock button: slot 41 (row 4, col 5) — read only
        if (rawSlot == 41) {
            return;
        }

        // My item slots: left side cols 0-3, rows 1-3
        if (row >= 1 && row <= 3 && col >= 0 && col <= 3) {
            handleMySlotClick(player, session, row, col, event);
            return;
        }

        // Other player's item slots: right side cols 5-8, rows 1-3 — read only
        if (row >= 1 && row <= 3 && col >= 5 && col <= 8) {
            // Can't interact with other player's items
            return;
        }
    }

    private void handleLockClick(Player player, TradeSession session) {
        UUID myId = player.getUniqueId();

        if (session.isLocked(myId)) {
            // Unlock
            session.unlock(myId);
            // Cancel countdown if it was running
            if (session.getCountdownTaskId() != -1) {
                org.bukkit.Bukkit.getScheduler().cancelTask(session.getCountdownTaskId());
                session.setCountdownTaskId(-1);
            }
            GuiHelper.playClick(player);
            tradeManager.updateTradeGui(session);

            Player other = org.bukkit.Bukkit.getPlayer(session.getOther(myId));
            if (other != null && other.isOnline()) {
                other.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                        + "&cL'autre joueur a deverrouille son offre !"));
            }
        } else {
            // Lock
            session.lock(myId);
            GuiHelper.playClick(player);
            tradeManager.updateTradeGui(session);

            // Check anti-scam warning: if other player offers nothing
            UUID otherId = session.getOther(myId);
            if (session.countItems(otherId) == 0 && session.countItems(myId) > 0) {
                player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                        + "&c\u26A0 ATTENTION: L'autre joueur n'offre rien en echange !"));
                GuiHelper.playDeny(player);
            }

            if (session.areBothLocked()) {
                tradeManager.startCountdown(session);
            }
        }
    }

    private void handleMySlotClick(Player player, TradeSession session, int row, int col,
                                   InventoryClickEvent event) {
        UUID myId = player.getUniqueId();

        // If locked, can't modify
        if (session.isLocked(myId)) {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                    + "&cDeverrouillez d'abord votre offre pour modifier les items."));
            GuiHelper.playDeny(player);
            return;
        }

        int itemIndex = (row - 1) * 4 + col;
        ItemStack[] myItems = session.getItems(myId);
        ItemStack cursor = event.getCursor();

        if (myItems[itemIndex] != null) {
            // Remove item from trade → give back to player
            ItemStack removed = session.removeItem(myId, itemIndex);
            if (removed != null) {
                player.getInventory().addItem(removed);
                GuiHelper.playClick(player);
            }
        } else if (cursor != null && cursor.getType() != Material.AIR) {
            // Place item from cursor into trade slot
            ItemStack toPlace = cursor.clone();
            session.addItem(myId, toPlace, itemIndex);
            // Clear cursor
            event.setCursor(null);
            GuiHelper.playClick(player);
        } else {
            // Empty slot, no cursor — do nothing
            return;
        }

        // Anti-scam: check if other player offers nothing
        UUID otherId = session.getOther(myId);
        if (session.countItems(otherId) == 0 && session.countItems(myId) > 0) {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                    + "&c\u26A0 ATTENTION: L'autre joueur n'offre rien en echange !"));
        }

        tradeManager.updateTradeGui(session);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Inventory topInv = event.getView().getTopInventory();
        if (!isTradeGui(topInv)) {
            return;
        }

        // Check if any dragged slot is in the trade GUI area
        for (int slot : event.getRawSlots()) {
            if (slot < 54) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (closingByPlugin.remove(playerId)) {
            return;
        }

        Inventory inv = event.getInventory();
        if (!isTradeGui(inv)) {
            return;
        }

        TradeSession session = tradeManager.getSession(playerId);
        if (session == null) {
            return;
        }

        // If trade is already completed or cancelled, do nothing
        if (session.getState() == TradeSession.TradeState.COMPLETED
                || session.getState() == TradeSession.TradeState.CANCELLED) {
            return;
        }

        // Player closed the GUI → cancel the trade
        UUID otherId = session.getOther(playerId);
        closingByPlugin.add(otherId);
        tradeManager.cancelTrade(playerId);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        // Cancel active trade
        if (tradeManager.hasActiveTrade(playerId)) {
            UUID otherId = tradeManager.getSession(playerId).getOther(playerId);
            closingByPlugin.add(playerId);
            closingByPlugin.add(otherId);
            tradeManager.cancelTrade(playerId);
        }

        // Clean up pending requests
        tradeManager.cleanupPendingRequests(playerId);
    }
}
