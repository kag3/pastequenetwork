package fr.pasteque.skyblock.gui;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

@SuppressWarnings("deprecation")
public class MainMenuGui {

    public static final String TITLE = PastequeSkyblockPlugin.color("&2&lPasteque &5&lMenu");

    public static Inventory create(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // Row 0: decorative border (alternating green/purple glass)
        GuiHelper.addTopBorder(inv);

        // ── Row 1 ────────────────────────────────────────────────────────
        inv.setItem(10, GuiHelper.createItem(Material.GRASS,
                "&a&lMon Ile",
                "&7Gere ton ile skyblock",
                "&8\u25B8 Niveau, membres, parametres",
                "",
                "&e\u25B6 Clic pour ouvrir!"));

        inv.setItem(12, GuiHelper.createItem(Material.DIAMOND_SWORD,
                "&c&lArene PvP",
                "&7Combats dans l'arene",
                "&8\u25B8 Kits, classement, duels",
                "",
                "&e\u25B6 Clic pour ouvrir!"));

        inv.setItem(14, GuiHelper.createItem(Material.CHEST,
                "&e&lHotel des Ventes",
                "&7Achete et vends des items",
                "&8\u25B8 Encheres, recherche",
                "",
                "&e\u25B6 Clic pour ouvrir!"));

        inv.setItem(16, GuiHelper.createItem(Material.EMERALD,
                "&a&lBoutique",
                "&7Boutiques des joueurs",
                "",
                "&e\u25B6 Clic pour ouvrir!"));

        // ── Row 2 ────────────────────────────────────────────────────────
        inv.setItem(19, GuiHelper.createItem(Material.BOOK_AND_QUILL,
                "&d&lCompetences",
                "&7Tes 5 arbres de competences",
                "&8\u25B8 Combat, Minage, Peche...",
                "",
                "&e\u25B6 Clic pour ouvrir!"));

        inv.setItem(21, GuiHelper.createItem(Material.GOLD_INGOT,
                "&6&lCollections",
                "&7Progresse tes collections",
                "&8\u25B8 Debloquer des recompenses",
                "",
                "&e\u25B6 Clic pour ouvrir!"));

        inv.setItem(23, GuiHelper.createItem(Material.SKULL_ITEM, 3,
                "&d&lAnimaux",
                "&7Tes compagnons de combat",
                "&8\u25B8 Bonus passifs uniques",
                "",
                "&e\u25B6 Clic pour ouvrir!"));

        inv.setItem(25, GuiHelper.createItem(Material.BREWING_STAND_ITEM,
                "&8&lMinions",
                "&7Tes ouvriers automatiques",
                "&8\u25B8 Farm, mine, peche...",
                "",
                "&e\u25B6 Clic pour ouvrir!"));

        // ── Row 3 ────────────────────────────────────────────────────────
        inv.setItem(20, GuiHelper.createItem(Material.IRON_SWORD,
                "&5&lDuels",
                "&7Defie un joueur en 1v1",
                "&8\u25B8 Arenes privees, ELO",
                "",
                "&e\u25B6 Clic pour ouvrir!"));

        inv.setItem(22, GuiHelper.createItem(Material.PAPER,
                "&d&lPasse de Combat",
                "&7Saison 1 - Recompenses",
                "&8\u25B8 30 paliers a debloquer",
                "",
                "&e\u25B6 Clic pour ouvrir!"));

        inv.setItem(24, GuiHelper.createItem(Material.BLAZE_ROD,
                "&6&lSlayers",
                "&7Invoque et terrasse des boss",
                "&8\u25B8 5 types, niveaux 1-5",
                "",
                "&e\u25B6 Clic pour ouvrir!"));

        // ── Row 4 ────────────────────────────────────────────────────────
        inv.setItem(29, GuiHelper.createItem(Material.GOLDEN_APPLE,
                "&e&lBounties",
                "&7Primes sur les joueurs",
                "",
                "&e\u25B6 Clic pour ouvrir!"));

        inv.setItem(31, GuiHelper.createItem(Material.NETHER_STAR,
                "&5&lVente Sombre",
                "&7Encheres sur items legendaires",
                "&8\u25B8 Toutes les 2 heures",
                "",
                "&e\u25B6 Clic pour ouvrir!"));

        inv.setItem(33, GuiHelper.createItem(Material.BOOK_AND_QUILL,
                "&a&lSocial",
                "&7Amis, ennemis, alliances",
                "",
                "&e\u25B6 Clic pour ouvrir!"));

        // ── Row 5: decorative border + close button ──────────────────────
        GuiHelper.addBottomBorder(inv);
        inv.setItem(49, GuiHelper.closeButton());

        // Fill remaining empty slots with black glass
        GuiHelper.fillEmpty(inv);

        return inv;
    }
}
