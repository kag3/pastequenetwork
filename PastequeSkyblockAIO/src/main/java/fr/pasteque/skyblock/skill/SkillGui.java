package fr.pasteque.skyblock.skill;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.skill.model.PlayerSkills;
import fr.pasteque.skyblock.skill.model.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class SkillGui {

    public static final String TITLE = PastequeSkyblockPlugin.color("&2&lCompetences");

    private static final int[] SLOTS = {10, 11, 12, 13, 14};

    private SkillGui() {
    }

    public static Inventory create(PastequeSkyblockPlugin plugin, PlayerSkills skills) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        // Fill border with gray stained glass panes (durability 7 = gray)
        ItemStack filler = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler.clone());
        }

        SkillType[] types = SkillType.values();
        for (int i = 0; i < types.length && i < SLOTS.length; i++) {
            SkillType type = types[i];
            int level = skills.getLevel(type);
            int xp = skills.getXp(type);
            int required = PlayerSkills.xpRequired(level);
            int bonus = level * 2;

            Material iconMat = Material.matchMaterial(type.getIcon());
            if (iconMat == null) {
                iconMat = Material.BEDROCK;
            }
            ItemStack item = new ItemStack(iconMat);
            ItemMeta meta = item.getItemMeta();

            meta.setDisplayName(PastequeSkyblockPlugin.color(
                    type.getColor() + type.getDisplayName() + " &7Niv. " + level
            ));

            List<String> lore = new ArrayList<String>();
            lore.add(PastequeSkyblockPlugin.color("&7XP: &f" + xp + "/" + required));
            lore.add(PastequeSkyblockPlugin.color("&7Bonus: &a+" + bonus + "%"));
            lore.add("");
            lore.add(buildProgressBar(xp, required));
            meta.setLore(lore);

            item.setItemMeta(meta);
            inv.setItem(SLOTS[i], item);
        }

        return inv;
    }

    private static String buildProgressBar(int xp, int required) {
        int totalBars = 20;
        int filled;
        if (required <= 0) {
            filled = totalBars;
        } else {
            filled = (int) ((double) xp / required * totalBars);
            if (filled > totalBars) {
                filled = totalBars;
            }
        }

        StringBuilder bar = new StringBuilder();
        bar.append("&a");
        for (int i = 0; i < filled; i++) {
            bar.append("|");
        }
        bar.append("&7");
        for (int i = filled; i < totalBars; i++) {
            bar.append("|");
        }
        return PastequeSkyblockPlugin.color(bar.toString());
    }
}
