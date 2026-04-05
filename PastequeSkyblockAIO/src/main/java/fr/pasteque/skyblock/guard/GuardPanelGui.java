package fr.pasteque.skyblock.guard;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.guard.model.ReportEntry;
import fr.pasteque.skyblock.gui.GuiHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GuardPanelGui {

    public static final String GUI_TITLE = PastequeSkyblockPlugin.color("&2&lPasteque &5&lSignalements");

    public static Inventory create(PastequeSkyblockPlugin plugin, ReportService reportService) {
        Inventory inventory = Bukkit.createInventory(null, 54, GUI_TITLE);

        GuiHelper.addTopBorder(inventory);
        GuiHelper.addBottomBorder(inventory);

        List<ReportEntry> reports = reportService.getPendingReports();
        if (reports.isEmpty()) {
            inventory.setItem(22, GuiHelper.createItem(Material.STAINED_GLASS_PANE, 5,
                    "&a&lAucun signalement",
                    "",
                    "&8▎ &7Informations",
                    "&8▸ &7Le panel est propre pour le moment.",
                    "&8▸ &7Aucun rapport en attente."));
        } else {
            int slot = 10;
            SimpleDateFormat format = new SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE);
            for (ReportEntry report : reports) {
                if (slot >= 45) {
                    break;
                }
                if (slot % 9 == 0 || slot % 9 == 8) {
                    slot++;
                    continue;
                }

                // Place colored glass behind: red (14) for unresolved
                inventory.setItem(slot, GuiHelper.createItem(Material.BOOK_AND_QUILL,
                        "&c&lSignalement #" + report.getId(),
                        "",
                        "&8▎ &7Details du signalement",
                        "&8▸ &7Auteur: &d" + report.getAuthor(),
                        "&8▸ &7Cible: &c" + report.getTarget(),
                        "&8▸ &7Motif: &e" + report.getReason(),
                        "&8▸ &7Date: &b" + format.format(new Date(report.getCreatedAt())),
                        "",
                        "&a▶ Clic pour marquer comme traite"));
                slot++;
            }
        }

        inventory.setItem(49, GuiHelper.closeButton());

        GuiHelper.fillEmpty(inventory);

        return inventory;
    }
}
