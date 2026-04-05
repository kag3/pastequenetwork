package fr.pasteque.skyblock.daily;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.gui.GuiHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class DailyRewardGui {

    public static final String TITLE = PastequeSkyblockPlugin.color("&6&lRecompense &e&lQuotidienne");

    private DailyRewardGui() {}

    public static void open(Player player, DailyRewardManager manager) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE);

        int streak = manager.getStreak(player.getUniqueId());
        boolean canClaim = manager.canClaim(player.getUniqueId());

        // Days 1..7 in slots 19..25
        int[] slots = { 19, 20, 21, 22, 23, 24, 25 };
        for (int day = 1; day <= 7; day++) {
            boolean claimed = day <= streak && !canClaim; // already done this cycle
            boolean current = canClaim && (day == ((streak % 7) + 1) || (streak == 0 && day == 1));
            Material mat;
            String name;
            List<String> lore = new ArrayList<String>();
            double reward = manager.getRewardForDay(day);
            if (claimed) {
                mat = Material.STAINED_GLASS_PANE; // will appear as pane — override below
                name = "&a&lJour " + day + " &7(reclame)";
                lore.add("");
                lore.add("&8\u258E &7Recompense");
                lore.add("&8\u25B8 &e" + ((long) reward) + " Pasteque");
                lore.add("");
                lore.add("&a\u2714 Deja reclame");
            } else if (current) {
                mat = Material.GOLD_INGOT;
                name = "&6&lJour " + day + " &e(aujourd'hui)";
                lore.add("");
                lore.add("&8\u258E &7Recompense");
                lore.add("&8\u25B8 &e" + ((long) reward) + " Pasteque");
                if (day == 7) {
                    lore.add("&8\u25B8 &6+ Nether Star Jackpot");
                }
                lore.add("");
                lore.add("&e\u25B6 Clic pour reclamer !");
            } else {
                mat = Material.IRON_INGOT;
                name = "&7&lJour " + day;
                lore.add("");
                lore.add("&8\u258E &7Recompense");
                lore.add("&8\u25B8 &e" + ((long) reward) + " Pasteque");
                if (day == 7) {
                    lore.add("&8\u25B8 &6+ Nether Star Jackpot");
                }
                lore.add("");
                lore.add("&7Verrouille");
            }
            ItemStack item;
            if (mat == Material.STAINED_GLASS_PANE) {
                item = GuiHelper.createItem(Material.STAINED_GLASS_PANE, 5, name, lore.toArray(new String[0])); // lime
            } else {
                item = GuiHelper.createItem(mat, name, lore.toArray(new String[0]));
            }
            inv.setItem(slots[day - 1], item);
        }

        // Info
        inv.setItem(4, GuiHelper.createItem(Material.WATCH,
                "&e&lVotre progression",
                "",
                "&8\u258E &7Statut",
                "&8\u25B8 &7Jour en cours: &e" + Math.max(1, streak + (canClaim ? 1 : 0)),
                "&8\u25B8 &7Prochaine recompense: " + (canClaim ? "&adisponible" : "&cdans < 24h"),
                "",
                "&7Revenez chaque jour pour augmenter",
                "&7votre streak jusqu'au jackpot du jour 7 !"));

        inv.setItem(40, GuiHelper.closeButton());
        GuiHelper.decorate(inv, GuiHelper.Theme.EVENT);
        player.openInventory(inv);
        GuiHelper.playOpen(player);
    }
}
