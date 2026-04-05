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

        // Layout parfaitement symetrique aere — 14 items repartis en 4+4+4+2
        // Row 1 (9-17)  : 10, 12, 14, 16
        // Row 2 (18-26) : 19, 21, 23, 25
        // Row 3 (27-35) : 28, 30, 32, 34
        // Row 4 (36-44) : 39, 41 (symetriques autour de 40)
        // Close button  : 49 (row 5 center)
        // Theme PASTEQUE (vert/lime/magenta) applique en fin de methode via decorate().

        // ── Row 1 : Ile / Arene / HDV / Boutique ────────────────────────
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

        // ── Row 2 : Competences / Collections / Animaux / Minions ───────
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

        // ── Row 3 : Duels / Pass / Slayers / Bounties ───────────────────
        inv.setItem(28, GuiHelper.createItem(Material.IRON_SWORD,
                "&5&lDuels",
                "&7Defie un joueur en 1v1",
                "&8\u25B8 Arenes privees, ELO",
                "",
                "&e\u25B6 Clic pour ouvrir!"));

        inv.setItem(30, GuiHelper.createItem(Material.PAPER,
                "&d&lPasse de Combat",
                "&7Saison 1 - Recompenses",
                "&8\u25B8 30 paliers a debloquer",
                "",
                "&e\u25B6 Clic pour ouvrir!"));

        inv.setItem(32, GuiHelper.createItem(Material.BLAZE_ROD,
                "&6&lSlayers",
                "&7Invoque et terrasse des boss",
                "&8\u25B8 5 types, niveaux 1-5",
                "",
                "&e\u25B6 Clic pour ouvrir!"));

        inv.setItem(34, GuiHelper.createItem(Material.GOLDEN_APPLE,
                "&e&lBounties",
                "&7Primes sur les joueurs",
                "",
                "&e\u25B6 Clic pour ouvrir!"));

        // ── Row 4 : Vente Sombre / Social (symetriques autour de 40) ────
        inv.setItem(39, GuiHelper.createItem(Material.NETHER_STAR,
                "&5&lVente Sombre",
                "&7Encheres sur items legendaires",
                "&8\u25B8 Toutes les 2 heures",
                "",
                "&e\u25B6 Clic pour ouvrir!"));

        inv.setItem(41, GuiHelper.createItem(Material.BOOK_AND_QUILL,
                "&a&lSocial",
                "&7Amis, ennemis, alliances",
                "",
                "&e\u25B6 Clic pour ouvrir!"));

        // ── Row 5 : bottom border + close ───────────────────────────────
        inv.setItem(49, GuiHelper.closeButton());
        GuiHelper.decorate(inv, GuiHelper.Theme.PASTEQUE);

        return inv;
    }

    /**
     * Ouvre le menu principal et joue le son de signature.
     */
    public static void open(Player player) {
        player.openInventory(create(player));
        GuiHelper.playOpen(player);
    }
}
