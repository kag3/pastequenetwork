package fr.pasteque.skyblock.minion;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.gui.GuiHelper;
import fr.pasteque.skyblock.minion.model.MinionType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI d'achat de minions. Montre tous les MinionType avec leur prix (5000 de base).
 * Click = acheter + donner l'item minion au joueur.
 */
@SuppressWarnings("deprecation")
public final class MinionShopGui {

    public static final String TITLE = PastequeSkyblockPlugin.color("&2&lBoutique &5&lMinions");

    private static final double BASE_PRICE = 5000.0;

    private MinionShopGui() {}

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        GuiHelper.addTopBorder(inv);
        GuiHelper.addBottomBorder(inv);

        // Info center-top
        ItemStack info = GuiHelper.createItem(Material.EMERALD,
                "&a&lBoutique Minions",
                "",
                "&7Les minions farment pour vous",
                "&7automatiquement, meme hors ligne !",
                "",
                "&e\u25B6 Cliquez sur un minion pour l'acheter");
        inv.setItem(4, info);

        // Slots for minions: centered aesthetically
        int[] slots = { 19, 20, 21, 22, 23, 24, 25, 28 };
        MinionType[] types = MinionType.values();
        int i = 0;
        for (MinionType t : types) {
            if (i >= slots.length) break;
            Material mat = Material.matchMaterial(t.getIconMaterial());
            if (mat == null) mat = Material.MONSTER_EGG;
            List<String> lore = new ArrayList<String>();
            lore.add("");
            lore.add("&8\u258E &7Statistiques");
            lore.add("&8\u25B8 &7Produit: &e" + t.getProduceMaterial());
            lore.add("&8\u25B8 &7Intervalle: &e" + t.getIntervalSeconds() + "s");
            lore.add("");
            lore.add("&8\u258E &7Prix");
            lore.add("&8\u25B8 &6" + ((long) BASE_PRICE) + " Pasteque");
            lore.add("");
            lore.add("&e\u25B6 Clic gauche pour acheter");
            ItemStack item = GuiHelper.createItem(mat,
                    "&6&l" + t.getDisplayName(),
                    lore.toArray(new String[0]));
            // tag with minion type name via lore last line (hidden mechanism)
            ItemMeta meta = item.getItemMeta();
            List<String> l = meta.getLore();
            l.add(PastequeSkyblockPlugin.color("&0&k" + t.name())); // hidden tag
            meta.setLore(l);
            item.setItemMeta(meta);
            inv.setItem(slots[i], item);
            i++;
        }

        // Money display
        PastequeSkyblockPlugin plugin = (PastequeSkyblockPlugin)
                org.bukkit.Bukkit.getPluginManager().getPlugin("PastequeSkyblockAIO");
        double balance = plugin.getEconomyManager().getBalance(player.getUniqueId());
        inv.setItem(40, GuiHelper.createItem(Material.GOLD_INGOT,
                "&6&lVotre solde",
                "",
                "&8\u25B8 &e" + ((long) balance) + " Pasteque"));

        inv.setItem(49, GuiHelper.closeButton());
        GuiHelper.fillEmpty(inv);
        player.openInventory(inv);
    }

    public static double getPrice(MinionType type) {
        return BASE_PRICE;
    }

    /**
     * Extrait le MinionType a partir d'un ItemStack du shop (via le tag cache dans le lore).
     */
    public static MinionType extractType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return null;
        for (String line : meta.getLore()) {
            String stripped = org.bukkit.ChatColor.stripColor(line);
            if (stripped == null) continue;
            for (MinionType t : MinionType.values()) {
                if (stripped.equals(t.name())) return t;
            }
        }
        return null;
    }

    /**
     * Cree l'item-minion placable qu'on donne au joueur apres achat.
     * Cet item sera utilise lors du BlockPlaceEvent pour spawner un minion.
     */
    public static ItemStack createMinionItem(MinionType type) {
        ItemStack item = new ItemStack(Material.MONSTER_EGG, 1, (short) 120);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(PastequeSkyblockPlugin.color("&6&l" + type.getDisplayName()));
        List<String> lore = new ArrayList<String>();
        lore.add(PastequeSkyblockPlugin.color("&7Posez ce minion sur votre ile"));
        lore.add(PastequeSkyblockPlugin.color("&7pour farmer automatiquement !"));
        lore.add("");
        lore.add(PastequeSkyblockPlugin.color("&8\u258E &7Type"));
        lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &e" + type.getProduceMaterial()));
        lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Intervalle: &e" + type.getIntervalSeconds() + "s"));
        lore.add("");
        lore.add(PastequeSkyblockPlugin.color("&0&k" + type.name())); // hidden tag
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
