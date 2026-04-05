package fr.pasteque.skyblock.listener.guard;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.guard.GuardPanelGui;
import fr.pasteque.skyblock.guard.ReportService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GuardPanelListener implements Listener {

    private final PastequeSkyblockPlugin plugin;
    private final ReportService reportService;

    public GuardPanelListener(PastequeSkyblockPlugin plugin, ReportService reportService) {
        this.plugin = plugin;
        this.reportService = reportService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() == null || event.getView() == null) {
            return;
        }
        String title = event.getView().getTitle();
        if (!title.equals(GuardPanelGui.GUI_TITLE)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        // Close button (slot 49)
        if (event.getRawSlot() == 49) {
            player.closeInventory();
            return;
        }

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR || !event.getCurrentItem().hasItemMeta()) {
            return;
        }
        String display = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
        if (display == null || !display.startsWith("Signalement #")) {
            return;
        }
        String id = display.replace("Signalement #", "").trim();
        reportService.markHandled(id, player.getName());
        player.sendMessage(plugin.getGuardPrefix() + plugin.color("&fLe signalement &d#" + id + " &fa été classé comme traité."));
        player.openInventory(GuardPanelGui.create(plugin, reportService));
    }
}
