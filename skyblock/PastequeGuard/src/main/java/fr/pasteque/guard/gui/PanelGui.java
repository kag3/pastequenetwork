package fr.pasteque.guard.gui;

import fr.pasteque.guard.PastequeGuardPlugin;
import fr.pasteque.guard.model.ReportEntry;
import fr.pasteque.guard.service.ReportService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PanelGui {

    public static final String HOLDER_TITLE = "PG_PANEL";

    public static Inventory create(PastequeGuardPlugin plugin, ReportService reportService) {
        int size = plugin.getConfig().getInt("reports.size", 54);
        String title = plugin.color(plugin.getConfig().getString("reports.gui-title", "&dPastequeGuard &f• &dSignalements"));
        Inventory inventory = Bukkit.createInventory(null, size, title);

        fillBorders(plugin, inventory);

        List<ReportEntry> reports = reportService.getPendingReports();
        if (reports.isEmpty()) {
            ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 6);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(plugin.color("&dAucun signalement"));
            List<String> lore = new ArrayList<String>();
            lore.add(plugin.color("&fLe panel est propre pour le moment."));
            meta.setLore(lore);
            item.setItemMeta(meta);
            inventory.setItem(22, item);
            return inventory;
        }

        int slot = 10;
        SimpleDateFormat format = new SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE);
        for (ReportEntry report : reports) {
            if (slot >= size - 9) {
                break;
            }
            if (slot % 9 == 8) {
                slot += 2;
            }
            Material material = Material.BOOK_AND_QUILL;
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(plugin.color("&dSignalement #" + report.getId()));
            List<String> lore = new ArrayList<String>();
            lore.add(plugin.color("&fAuteur : &d" + report.getAuthor()));
            lore.add(plugin.color("&fCible : &d" + report.getTarget()));
            lore.add(plugin.color("&fMotif : &d" + report.getReason()));
            lore.add(plugin.color("&fDate : &d" + format.format(new Date(report.getCreatedAt()))));
            lore.add(" ");
            lore.add(plugin.color("&fClic gauche : &dmarquer comme traité"));
            meta.setLore(lore);
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
            slot++;
        }

        return inventory;
    }

    private static void fillBorders(PastequeGuardPlugin plugin, Inventory inventory) {
        ItemStack border = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 2);
        ItemMeta meta = border.getItemMeta();
        meta.setDisplayName(plugin.color("&f"));
        border.setItemMeta(meta);
        int size = inventory.getSize();
        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, border);
            }
        }
    }
}
