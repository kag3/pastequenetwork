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

        // Bruitage universel de clic sur le menu principal (hors vitres deco)
        org.bukkit.inventory.ItemStack current = event.getCurrentItem();
        if (current != null && current.getType() != org.bukkit.Material.AIR
                && current.getType() != org.bukkit.Material.STAINED_GLASS_PANE) {
            if (slot == 49) {
                fr.pasteque.skyblock.gui.GuiHelper.playClose(player);
            } else {
                fr.pasteque.skyblock.gui.GuiHelper.playClick(player);
            }
        }

        // Slots alignes sur le layout 4+4+4+2 de MainMenuGui
        switch (slot) {
            // Row 1: Ile / Arene / HDV / Boutique
            case 10: player.closeInventory(); player.performCommand("is"); break;
            case 12: player.closeInventory(); player.performCommand("arena"); break;
            case 14: player.closeInventory(); player.performCommand("hdv"); break;
            case 16: player.closeInventory(); player.performCommand("myshop"); break;

            // Row 2: Competences / Collections / Animaux / Minions
            case 19: player.closeInventory(); player.performCommand("skills"); break;
            case 21: player.closeInventory(); player.performCommand("collection"); break;
            case 23: player.closeInventory(); player.performCommand("pet"); break;
            case 25: player.closeInventory(); player.performCommand("minions"); break;

            // Row 3: Duels / Combat Pass / Slayers / Bounties
            case 28: player.closeInventory(); player.performCommand("duel"); break;
            case 30: player.closeInventory(); player.performCommand("combatpass"); break;
            case 32: player.closeInventory(); player.performCommand("slayer"); break;
            case 34: player.closeInventory(); player.performCommand("bounty list"); break;

            // Row 4: Vente Sombre / Social
            case 39: player.closeInventory(); player.performCommand("darkauction"); break;
            case 41: player.closeInventory(); player.performCommand("friends"); break;

            // Close button
            case 49:
                player.closeInventory();
                break;
            default:
                break;
        }
    }
}
