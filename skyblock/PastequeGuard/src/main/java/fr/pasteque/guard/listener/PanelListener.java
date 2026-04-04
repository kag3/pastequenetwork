package fr.pasteque.guard.listener;

import fr.pasteque.guard.PastequeGuardPlugin;
import fr.pasteque.guard.gui.PanelGui;
import fr.pasteque.guard.service.ReportService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class PanelListener implements Listener {

    private final PastequeGuardPlugin plugin;
    private final ReportService reportService;

    public PanelListener(PastequeGuardPlugin plugin, ReportService reportService) {
        this.plugin = plugin;
        this.reportService = reportService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() == null || event.getView() == null) {
            return;
        }
        String title = event.getView().getTitle();
        if (!title.equals(plugin.color(plugin.getConfig().getString("reports.gui-title", "&dPastequeGuard &f• &dSignalements")))) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR || !event.getCurrentItem().hasItemMeta()) {
            return;
        }
        String display = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
        if (display == null || !display.startsWith("Signalement #")) {
            return;
        }
        String id = display.replace("Signalement #", "").trim();
        reportService.markHandled(id, player.getName());
        player.sendMessage(plugin.getPrefix() + plugin.color("&fLe signalement &d#" + id + " &fa été classé comme traité."));
        player.openInventory(PanelGui.create(plugin, reportService));
    }
}
