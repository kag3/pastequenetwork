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
        inv.setItem(10, GuiHelper.fluidItem(Material.GRASS,
                "&a&lMon Ile",
                "Ton royaume flottant t'attend.",
                new String[]{"Niveau et experience d'ile", "Gestion des membres et coop", "Ameliorations et warps"},
                "Clic pour acceder a ton ile"));

        inv.setItem(12, GuiHelper.fluidItem(Material.DIAMOND_SWORD,
                "&c&lArene PvP",
                "Prouve ta valeur au combat.",
                new String[]{"Kits equilibres a selectionner", "Classement ELO competitif", "Duels et tournois"},
                "Clic pour entrer dans l'arene"));

        inv.setItem(14, GuiHelper.fluidItem(Material.CHEST,
                "&e&lHotel des Ventes",
                "Marche central du serveur.",
                new String[]{"Mets en vente tes tresors", "Recherche par categorie", "Systeme d'encheres en temps reel"},
                "Clic pour ouvrir l'HDV"));

        inv.setItem(16, GuiHelper.fluidItem(Material.EMERALD,
                "&a&lBoutiques Joueurs",
                "Les shops tenus par la communaute.",
                new String[]{"Visite les meilleurs vendeurs", "Cree ta propre boutique", "Prix libres et negociables"},
                "Clic pour parcourir"));

        // ── Row 2 : Competences / Collections / Animaux / Minions ───────
        inv.setItem(19, GuiHelper.fluidItem(Material.BOOK_AND_QUILL,
                "&d&lCompetences",
                "Cinq arbres pour te specialiser.",
                new String[]{"Combat, Minage, Peche, Foresterie, Farming", "Bonus passifs exclusifs", "Defis de maitrise a debloquer"},
                "Clic pour progresser"));

        inv.setItem(21, GuiHelper.fluidItem(Material.GOLD_INGOT,
                "&6&lCollections",
                "Collectionne tout, recompense-toi.",
                new String[]{"Debloque des paliers uniques", "Recettes et objets exclusifs", "Progression persistante"},
                "Clic pour consulter"));

        inv.setItem(23, GuiHelper.fluidItem(Material.SKULL_ITEM, 3,
                "&d&lAnimaux",
                "Des compagnons fideles a tes cotes.",
                new String[]{"Bonus passifs uniques par pet", "Montee en niveau individuelle", "Invoque le pet de ton choix"},
                "Clic pour gerer tes pets"));

        inv.setItem(25, GuiHelper.fluidItem(Material.BREWING_STAND_ITEM,
                "&8&lMinions",
                "Tes ouvriers automatiques infatigables.",
                new String[]{"Farm, mine, peche, bucheronnage", "Ameliorations de vitesse et stockage", "Revenus passifs 24/7"},
                "Clic pour deployer"));

        // ── Row 3 : Duels / Pass / Slayers / Bounties ───────────────────
        inv.setItem(28, GuiHelper.fluidItem(Material.IRON_SWORD,
                "&5&lDuels",
                "Affronte un adversaire en 1v1.",
                new String[]{"Arenes privees sans interruption", "Classement ELO individuel", "Parie des pasteques sur le resultat"},
                "Clic pour lancer un defi"));

        inv.setItem(30, GuiHelper.fluidItem(Material.PAPER,
                "&d&lPasse de Combat",
                "Saison 1 - recompenses exclusives.",
                new String[]{"30 paliers gratuits et premium", "Cosmetiques, pasteques, pets", "Defis hebdomadaires pour progresser"},
                "Clic pour voir ta progression"));

        inv.setItem(32, GuiHelper.fluidItem(Material.BLAZE_ROD,
                "&6&lSlayers",
                "Invoque et terrasse des boss legendaires.",
                new String[]{"5 types de slayers distincts", "Niveaux 1 a 5 de difficulte", "Drops rares et XP massive"},
                "Clic pour partir en chasse"));

        inv.setItem(34, GuiHelper.fluidItem(Material.GOLDEN_APPLE,
                "&e&lBounties",
                "Des primes sur les meilleurs joueurs.",
                new String[]{"Depose ta tete de gibier", "Chasse les tetes les plus cheres", "Recompenses en pasteques d'or"},
                "Clic pour voir les primes"));

        // ── Row 4 : Vente Sombre / Social (symetriques autour de 40) ────
        inv.setItem(39, GuiHelper.fluidItem(Material.NETHER_STAR,
                "&5&lVente Sombre",
                "Encheres mysterieuses d'items rarissimes.",
                new String[]{"Nouvelle vente toutes les 2 heures", "Items legendaires exclusifs", "Enchere publique ou privee"},
                "Clic pour participer"));

        inv.setItem(41, GuiHelper.fluidItem(Material.BOOK_AND_QUILL,
                "&a&lSocial",
                "Ton reseau sur le serveur.",
                new String[]{"Amis, ennemis et alliances", "Messages prives et parties", "Statut et historique"},
                "Clic pour gerer"));

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
