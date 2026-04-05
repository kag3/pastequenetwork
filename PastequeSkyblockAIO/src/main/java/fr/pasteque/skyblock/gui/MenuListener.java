package fr.pasteque.skyblock.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class MenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (event.getInventory() == null || event.getInventory().getTitle() == null) {
            return;
        }
        if (!event.getInventory().getTitle().equals(MainMenuGui.TITLE)) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        switch (slot) {
            case 10:
                player.closeInventory();
                player.performCommand("is");
                break;
            case 11:
                player.closeInventory();
                player.performCommand("skills");
                break;
            case 12:
                player.closeInventory();
                player.performCommand("hdv");
                break;
            case 13:
                player.closeInventory();
                player.performCommand("myshop");
                break;
            case 14:
                player.closeInventory();
                player.performCommand("collection");
                break;
            case 15:
                player.closeInventory();
                player.performCommand("pet");
                break;
            case 16:
                // Minions - no command yet, placeholder
                break;
            case 20:
                player.closeInventory();
                player.performCommand("arena");
                break;
            case 21:
                player.closeInventory();
                player.performCommand("duel");
                break;
            case 22:
                player.closeInventory();
                player.performCommand("combatpass");
                break;
            case 23:
                player.closeInventory();
                player.performCommand("bounty list");
                break;
            case 24:
                player.closeInventory();
                player.performCommand("elo");
                break;
            case 29:
                player.closeInventory();
                player.performCommand("darkauction");
                break;
            case 30:
                player.closeInventory();
                player.performCommand("slayer");
                break;
            case 31:
                player.closeInventory();
                player.performCommand("friends");
                break;
            case 40:
                player.closeInventory();
                break;
            default:
                break;
        }
    }
}
