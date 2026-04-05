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
     * Pro layout: leave empty slots genuinely empty (no black-glass wall) —
     * only fills the LEFT and RIGHT frame columns to keep visual symmetry.
     * This is what Hypixel/top-tier servers do.
     */
    public static void fillEmpty(Inventory inv) {
        ItemStack frame = glassPane(13, " "); // dark green (Pasteque)
        int rows = inv.getSize() / 9;
        for (int r = 1; r < rows - 1; r++) {
            int left = r * 9;
            int right = r * 9 + 8;
            if (inv.getItem(left) == null) inv.setItem(left, frame.clone());
            if (inv.getItem(right) == null) inv.setItem(right, frame.clone());
        }
    }

    /**
     * Top border: Pasteque-themed pattern with lime + magenta accents at
     * positions 0,4,8 and dark-green fill elsewhere. Symmetric, distinctive.
     */
    public static void addTopBorder(Inventory inv) {
        ItemStack base = glassPane(13, " ");     // dark green
        ItemStack accent1 = glassPane(5, " ");   // lime
        ItemStack accent2 = glassPane(2, " ");   // magenta
        for (int i = 0; i < 9; i++) inv.setItem(i, base.clone());
        inv.setItem(0, accent2.clone());
        inv.setItem(4, accent1.clone());
        inv.setItem(8, accent2.clone());
    }

    /**
     * Bottom border: mirrors top pattern for perfect symmetry.
     */
    public static void addBottomBorder(Inventory inv) {
        int start = inv.getSize() - 9;
        ItemStack base = glassPane(13, " ");
        ItemStack accent1 = glassPane(5, " ");
        ItemStack accent2 = glassPane(2, " ");
        for (int i = 0; i < 9; i++) inv.setItem(start + i, base.clone());
        inv.setItem(start, accent2.clone());
        inv.setItem(start + 4, accent1.clone());
        inv.setItem(start + 8, accent2.clone());
    }

    /**
     * Create a close button (BARRIER) with standard styling.
     */
    public static ItemStack closeButton() {
        return createItem(Material.REDSTONE_BLOCK, "&c&lFermer", "", "&7Cliquez pour fermer ce menu.");
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
