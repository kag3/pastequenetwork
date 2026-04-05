package fr.pasteque.skyblock.leaderboard;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.gui.GuiHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class LeaderboardGui {

    public static final String TITLE = PastequeSkyblockPlugin.color("&2&lTop &5&lClassements");

    private LeaderboardGui() {}

    public static void open(Player player, LeaderboardManager manager) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        GuiHelper.addTopBorder(inv);
        GuiHelper.addBottomBorder(inv);

        // Header
        inv.setItem(4, GuiHelper.createItem(Material.BEACON,
                "&2&lClassements du serveur",
                "",
                "&7Les meilleurs joueurs dans chaque categorie."));

        // Top Money (columns 19,28,37)
        renderColumn(inv, 19, Material.GOLD_INGOT, "&6&l\u2726 TOP MONEY", manager.getTopMoney(5), " Pasteque");
        renderColumn(inv, 22, Material.DIAMOND_SWORD, "&c&l\u265b TOP ELO", manager.getTopElo(5), " ELO");
        renderColumn(inv, 25, Material.GRASS, "&a&l\u2766 TOP ILE", manager.getTopIslands(5), " niv");

        inv.setItem(49, GuiHelper.closeButton());
        GuiHelper.fillEmpty(inv);
        player.openInventory(inv);
    }

    private static void renderColumn(Inventory inv, int headSlot, Material mat, String title, List<LeaderboardManager.Entry> entries, String unit) {
        List<String> lore = new ArrayList<String>();
        lore.add("");
        if (entries.isEmpty()) {
            lore.add(PastequeSkyblockPlugin.color("&7Aucun joueur classe."));
        } else {
            String[] ranks = { "&6&l1.", "&e&l2.", "&f&l3.", "&7&l4.", "&7&l5." };
            for (int i = 0; i < entries.size(); i++) {
                LeaderboardManager.Entry e = entries.get(i);
                lore.add(PastequeSkyblockPlugin.color(ranks[i] + " &f" + e.name + " &8- &e" + e.value + unit));
            }
        }
        ItemStack item = GuiHelper.createItem(mat, title, lore.toArray(new String[0]));
        inv.setItem(headSlot, item);
    }
}
