package fr.pasteque.skyblock.gui;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("deprecation")
public final class GuiHelper {

    private GuiHelper() {
    }

    /**
     * Fill all empty (null) slots with black stained glass panes (data 15).
     */
    public static void fillEmpty(Inventory inv) {
        ItemStack filler = glassPane(15, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler.clone());
            }
        }
    }

    /**
     * Create the decorative top row border with alternating dark green (13) and purple (10) glass.
     * Assumes the inventory has at least 9 slots.
     */
    public static void addTopBorder(Inventory inv) {
        for (int i = 0; i < 9; i++) {
            short color = (i % 2 == 0) ? (short) 13 : (short) 10;
            inv.setItem(i, glassPane(color, " "));
        }
    }

    /**
     * Create the decorative bottom row border with alternating dark green (13) and purple (10) glass.
     * Works for any inventory size.
     */
    public static void addBottomBorder(Inventory inv) {
        int start = inv.getSize() - 9;
        for (int i = 0; i < 9; i++) {
            short color = (i % 2 == 0) ? (short) 13 : (short) 10;
            inv.setItem(start + i, glassPane(color, " "));
        }
    }

    /**
     * Create a close button (BARRIER) with standard styling.
     */
    public static ItemStack closeButton() {
        return createItem(Material.BARRIER, "&c&lFermer", "", "&7Cliquez pour fermer ce menu.");
    }

    /**
     * Create a back button (ARROW) with standard styling.
     */
    public static ItemStack backButton() {
        return createItem(Material.ARROW, "&c&lRetour", "", "&7Cliquez pour revenir en arriere.");
    }

    /**
     * Create a stained glass pane with the given data value and display name.
     */
    public static ItemStack glassPane(int dataValue, String name) {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) dataValue);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(PastequeSkyblockPlugin.color(name));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create an item with a display name and lore lines.
     */
    public static ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(PastequeSkyblockPlugin.color(name));
        if (lore != null && lore.length > 0) {
            List<String> loreList = new ArrayList<String>();
            for (String line : lore) {
                loreList.add(PastequeSkyblockPlugin.color(line));
            }
            meta.setLore(loreList);
        }
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create an item with a data value, display name, and lore lines.
     */
    public static ItemStack createItem(Material mat, int dataValue, String name, String... lore) {
        ItemStack item = new ItemStack(mat, 1, (short) dataValue);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(PastequeSkyblockPlugin.color(name));
        if (lore != null && lore.length > 0) {
            List<String> loreList = new ArrayList<String>();
            for (String line : lore) {
                loreList.add(PastequeSkyblockPlugin.color(line));
            }
            meta.setLore(loreList);
        }
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create an item with a specific amount, display name, and lore lines.
     */
    public static ItemStack createItemWithAmount(Material mat, int amount, String name, String... lore) {
        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(PastequeSkyblockPlugin.color(name));
        if (lore != null && lore.length > 0) {
            List<String> loreList = new ArrayList<String>();
            for (String line : lore) {
                loreList.add(PastequeSkyblockPlugin.color(line));
            }
            meta.setLore(loreList);
        }
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create an item with data value, specific amount, display name, and lore lines.
     */
    public static ItemStack createItemWithAmount(Material mat, int dataValue, int amount, String name, String... lore) {
        ItemStack item = new ItemStack(mat, amount, (short) dataValue);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(PastequeSkyblockPlugin.color(name));
        if (lore != null && lore.length > 0) {
            List<String> loreList = new ArrayList<String>();
            for (String line : lore) {
                loreList.add(PastequeSkyblockPlugin.color(line));
            }
            meta.setLore(loreList);
        }
        item.setItemMeta(meta);
        return item;
    }
}
