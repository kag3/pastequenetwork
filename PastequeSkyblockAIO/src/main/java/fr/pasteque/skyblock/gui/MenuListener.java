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
            // Row 1
            case 10: // Mon Ile
                player.closeInventory();
                player.performCommand("is");
                break;
            case 12: // Arene PvP
                player.closeInventory();
                player.performCommand("arena");
                break;
            case 14: // Hotel des Ventes
                player.closeInventory();
                player.performCommand("hdv");
                break;
            case 16: // Boutique
                player.closeInventory();
                player.performCommand("myshop");
                break;

            // Row 2
            case 19: // Competences
                player.closeInventory();
                player.performCommand("skills");
                break;
            case 21: // Collections
                player.closeInventory();
                player.performCommand("collection");
                break;
            case 23: // Animaux
                player.closeInventory();
                player.performCommand("pet");
                break;
            case 25: // Minions
                player.closeInventory();
                player.performCommand("minion");
                break;

            // Row 3
            case 20: // Duels
                player.closeInventory();
                player.performCommand("duel");
                break;
            case 22: // Passe de Combat
                player.closeInventory();
                player.performCommand("combatpass");
                break;
            case 24: // Slayers
                player.closeInventory();
                player.performCommand("slayer");
                break;

            // Row 4
            case 29: // Bounties
                player.closeInventory();
                player.performCommand("bounty list");
                break;
            case 31: // Vente Sombre
                player.closeInventory();
                player.performCommand("darkauction");
                break;
            case 33: // Social
                player.closeInventory();
                player.performCommand("friends");
                break;

            // Close button
            case 49:
                player.closeInventory();
                break;
            default:
                break;
        }
    }
}
