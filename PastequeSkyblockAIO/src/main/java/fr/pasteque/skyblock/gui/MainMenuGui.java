package fr.pasteque.skyblock.gui;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainMenuGui {

    public static final String TITLE = PastequeSkyblockPlugin.color("&2&lMenu Principal");

    public static Inventory create(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // Fill empty slots with gray stained glass pane (data 7)
        ItemStack filler = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(PastequeSkyblockPlugin.color("&7"));
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Slot 4: NETHER_STAR - decoration
        inv.setItem(4, createItem(Material.NETHER_STAR, "&a&lMenu Principal", null));

        // Row 2 (slots 10-16)
        inv.setItem(10, createItem(Material.GRASS, "&a&lMon Ile", "&7Gerer ton ile"));
        inv.setItem(11, createItem(Material.DIAMOND, "&b&lCompetences", "&7Voir tes skills"));
        inv.setItem(12, createItem(Material.CHEST, "&e&lHotel des Ventes", "&7Acheter et vendre"));
        inv.setItem(13, createItem(Material.EMERALD, "&2&lBoutique", "&7Shops joueurs"));
        inv.setItem(14, createItem(Material.GOLD_INGOT, "&6&lCollections", "&7Progresse tes collections"));
        inv.setItem(15, createItem(Material.SKULL_ITEM, "&d&lPets", "&7Tes compagnons"));
        inv.setItem(16, createItem(Material.ARMOR_STAND, "&8&lMinions", "&7Tes minions"));

        // Row 3 (slots 20-24)
        inv.setItem(20, createItem(Material.IRON_SWORD, "&c&lArene PvP", "&7Combats et classements"));
        inv.setItem(21, createItem(Material.DIAMOND_SWORD, "&5&lDuels", "&7Defie un joueur"));
        inv.setItem(22, createItem(Material.BOOK, "&d&lPasse de Combat", "&7Saison 1"));
        inv.setItem(23, createItem(Material.PAPER, "&e&lBounties", "&7Primes sur joueurs"));
        inv.setItem(24, createItem(Material.IRON_CHESTPLATE, "&7&lELO Classement", "&7Ton rang"));

        // Row 4 (slots 29-31)
        inv.setItem(29, createItem(Material.REDSTONE, "&4&lVente Sombre", "&7Encheres rares"));
        inv.setItem(30, createItem(Material.BLAZE_ROD, "&6&lSlayers", "&7Boss a invoquer"));
        inv.setItem(31, createItem(Material.BOOK_AND_QUILL, "&f&lAmis & Social", "&7Amis, ennemis, alliances"));

        // Slot 40: BARRIER - close
        inv.setItem(40, createItem(Material.BARRIER, "&c&lFermer", null));

        return inv;
    }

    private static ItemStack createItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(PastequeSkyblockPlugin.color(name));
        if (lore != null) {
            List<String> loreList = new ArrayList<String>();
            loreList.add(PastequeSkyblockPlugin.color(lore));
            meta.setLore(loreList);
        }
        item.setItemMeta(meta);
        return item;
    }
}
