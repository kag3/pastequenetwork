package fr.pasteque.skyblock.gui;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("deprecation")
public final class GuiHelper {

    /**
     * Theme de decoration d'un GUI. Chaque categorie du serveur a son propre
     * jeu de couleurs de bordures et sa signature sonore. Cela evite le
     * modele uniforme magenta/lime sur toutes les interfaces et donne un
     * feeling "top 1 serveur skyblock mondial".
     *
     * <p>Codes data pour STAINED_GLASS_PANE (1.8):
     * 0 blanc, 1 orange, 2 magenta, 3 bleu clair, 4 jaune, 5 lime, 6 rose,
     * 7 gris, 8 gris clair, 9 cyan, 10 violet, 11 bleu, 12 marron,
     * 13 vert, 14 rouge, 15 noir.
     */
    public enum Theme {
        // Nom          base  acc1  acc2  corner  titleCol
        PASTEQUE(         13,    5,    2,      6,  "&d"), // vert/lime/magenta (menu principal)
        ISLAND(            5,   13,    4,     13,  "&a"), // lime/vert/jaune (iles, coop)
        PVP(              14,    1,   15,     15,  "&c"), // rouge/orange/noir (arene, duels)
        ECO(               4,    1,    5,      1,  "&6"), // jaune/orange (hdv, minion, eco)
        SOCIAL(            3,    6,    9,      6,  "&b"), // bleu clair/rose (amis, coop menu)
        ADMIN(            15,   14,    1,     14,  "&4"), // noir/rouge/orange (psky admin)
        EVENT(            10,    2,   11,      2,  "&5"), // violet/magenta/bleu (koth, daily, darkauction)
        SKILL(             9,    3,    5,      3,  "&b"), // cyan/bleu clair/lime (skills, collections, pets)
        GUARD(            15,   14,    7,     14,  "&c"); // noir/rouge/gris (moderation)

        public final int base;
        public final int accent1;
        public final int accent2;
        public final int corner;
        public final String titleColor;

        Theme(int base, int accent1, int accent2, int corner, String titleColor) {
            this.base = base;
            this.accent1 = accent1;
            this.accent2 = accent2;
            this.corner = corner;
            this.titleColor = titleColor;
        }
    }

    private GuiHelper() {
    }

    /**
     * Pro layout: leave empty slots genuinely empty (no black-glass wall) —
     * only fills the LEFT and RIGHT frame columns to keep visual symmetry.
     * This is what Hypixel/top-tier servers do.
     */
    public static void fillEmpty(Inventory inv) {
        fillEmpty(inv, Theme.PASTEQUE);
    }

    public static void fillEmpty(Inventory inv, Theme theme) {
        ItemStack frame = glassPane(theme.base, " ");
        int rows = inv.getSize() / 9;
        for (int r = 1; r < rows - 1; r++) {
            int left = r * 9;
            int right = r * 9 + 8;
            if (inv.getItem(left) == null) inv.setItem(left, frame.clone());
            if (inv.getItem(right) == null) inv.setItem(right, frame.clone());
        }
    }

    /**
     * Top border: pattern theme-aware avec coins accentues et centre accent1.
     */
    public static void addTopBorder(Inventory inv) {
        addTopBorder(inv, Theme.PASTEQUE);
    }

    public static void addTopBorder(Inventory inv, Theme theme) {
        ItemStack base = glassPane(theme.base, " ");
        ItemStack a1 = glassPane(theme.accent1, " ");
        ItemStack a2 = glassPane(theme.accent2, " ");
        ItemStack corner = glassPane(theme.corner, " ");
        for (int i = 0; i < 9; i++) inv.setItem(i, base.clone());
        inv.setItem(0, corner.clone());
        inv.setItem(2, a2.clone());
        inv.setItem(4, a1.clone());
        inv.setItem(6, a2.clone());
        inv.setItem(8, corner.clone());
    }

    /**
     * Bottom border: mirror du top pour une symetrie parfaite.
     */
    public static void addBottomBorder(Inventory inv) {
        addBottomBorder(inv, Theme.PASTEQUE);
    }

    public static void addBottomBorder(Inventory inv, Theme theme) {
        int start = inv.getSize() - 9;
        ItemStack base = glassPane(theme.base, " ");
        ItemStack a1 = glassPane(theme.accent1, " ");
        ItemStack a2 = glassPane(theme.accent2, " ");
        ItemStack corner = glassPane(theme.corner, " ");
        for (int i = 0; i < 9; i++) inv.setItem(start + i, base.clone());
        inv.setItem(start, corner.clone());
        inv.setItem(start + 2, a2.clone());
        inv.setItem(start + 4, a1.clone());
        inv.setItem(start + 6, a2.clone());
        inv.setItem(start + 8, corner.clone());
    }

    /**
     * Applique les 3 decorations (top/bottom/fill) en une seule ligne selon le theme.
     */
    public static void decorate(Inventory inv, Theme theme) {
        addTopBorder(inv, theme);
        addBottomBorder(inv, theme);
        fillEmpty(inv, theme);
    }

    // ─────────────────────────────────────────────────────────────────────
    // SONS — signature audio du serveur
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Bruitage leger joue a chaque clic dans un GUI du plugin.
     * Utilise Sound.valueOf en try/catch pour etre resistant aux differences
     * de nom entre versions (1.8 "CLICK", 1.9+ "UI_BUTTON_CLICK").
     */
    public static void playClick(Player player) {
        playSafe(player, new String[]{"UI_BUTTON_CLICK", "CLICK", "WOOD_CLICK"}, 0.7F, 1.2F);
    }

    /**
     * Bruitage d'ouverture de menu (plus doux que le clic).
     */
    public static void playOpen(Player player) {
        playSafe(player, new String[]{"BLOCK_CHEST_OPEN", "CHEST_OPEN", "NOTE_PLING"}, 0.8F, 1.3F);
    }

    /**
     * Bruitage de succes (achat, deblocage, recompense).
     */
    public static void playSuccess(Player player) {
        playSafe(player, new String[]{"ENTITY_PLAYER_LEVELUP", "LEVEL_UP", "ORB_PICKUP"}, 0.9F, 1.1F);
    }

    /**
     * Bruitage d'erreur / action refusee.
     */
    public static void playDeny(Player player) {
        playSafe(player, new String[]{"BLOCK_NOTE_BASS", "NOTE_BASS", "ITEM_BREAK"}, 0.8F, 0.7F);
    }

    /**
     * Bruitage discret de fermeture de menu.
     */
    public static void playClose(Player player) {
        playSafe(player, new String[]{"BLOCK_CHEST_CLOSE", "CHEST_CLOSE", "WOOD_CLICK"}, 0.6F, 1.0F);
    }

    private static void playSafe(Player player, String[] candidates, float volume, float pitch) {
        if (player == null) return;
        for (String name : candidates) {
            try {
                Sound sound = Sound.valueOf(name);
                player.playSound(player.getLocation(), sound, volume, pitch);
                return;
            } catch (Throwable ignored) {
                // Try next candidate
            }
        }
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

    // ─────────────────────────────────────────────────────────────────────
    // HELPERS DE FLUIDITE TEXTE
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Construit une lore "magazine" propre et aeree avec separateur, details
     * et call-to-action. Pattern reutilisable sur tous les items de GUI pour
     * une typographie coherente.
     *
     * @param description courte phrase d'introduction (1 ligne, italique grise)
     * @param details     liste de bullets (peuvent contenir &couleurs)
     * @param cta         call-to-action (ex: "Clic pour ouvrir")
     */
    public static List<String> fluidLore(String description, String[] details, String cta) {
        List<String> lore = new ArrayList<String>();
        lore.add("");
        if (description != null && description.length() > 0) {
            lore.add("&7&o" + description);
            lore.add("");
        }
        if (details != null) {
            for (String d : details) {
                if (d == null || d.length() == 0) continue;
                lore.add("&8\u25B8 &7" + d);
            }
            if (details.length > 0) lore.add("");
        }
        if (cta != null && cta.length() > 0) {
            lore.add("&e\u25B6 " + cta);
        }
        return lore;
    }

    /**
     * Ligne de separation decorative pour entetes de lore.
     */
    public static String separator() {
        return "&8&m\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500";
    }
}
