package fr.pasteque.skyblockarena.listener;

import fr.pasteque.skyblockarena.PastequeSkyBlockArenaPlugin;
import fr.pasteque.skyblockarena.service.ArenaKitService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ArenaKitListener implements Listener {

    private final PastequeSkyBlockArenaPlugin plugin;
    private final ArenaKitService arenaKitService;

    public ArenaKitListener(PastequeSkyBlockArenaPlugin plugin, ArenaKitService arenaKitService) {
        this.plugin = plugin;
        this.arenaKitService = arenaKitService;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory() == null || event.getInventory().getTitle() == null || !event.getInventory().getTitle().equals(ArenaKitService.GUI_TITLE)) {
            return;
        }
        event.setCancelled(true);
        if (event.getWhoClicked() instanceof org.bukkit.entity.Player && event.getRawSlot() == 13) {
            arenaKitService.tryPurchase((org.bukkit.entity.Player) event.getWhoClicked());
            event.getWhoClicked().closeInventory();
        }
    }
}
