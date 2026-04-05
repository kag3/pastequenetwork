package fr.pasteque.skyblock.skill;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.gui.GuiHelper;
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

    public static final String TITLE = PastequeSkyblockPlugin.color("&2&lPasteque &5&lCompetences");

    private static final int[] SLOTS = {11, 12, 13, 14, 15};

    private SkillGui() {
    }

    public static Inventory create(PastequeSkyblockPlugin plugin, PlayerSkills skills) {
        Inventory inv = Bukkit.createInventory(null, 36, TITLE);

        // Row 1: skill items
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
                    type.getColor() + "&l" + type.getDisplayName() + " &fNiv. " + level
            ));

            List<String> lore = new ArrayList<String>();
            lore.add("");
            lore.add(PastequeSkyblockPlugin.color("&8\u258E &7Progression"));
            lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7XP: &f" + xp + "&8/&f" + required));
            lore.add(buildProgressBar(xp, required));
            lore.add("");
            lore.add(PastequeSkyblockPlugin.color("&8\u258E &7Bonus actif"));
            lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &a+" + bonus + "% &7de rendement"));
            lore.add("");
            lore.add(PastequeSkyblockPlugin.color("&e\u25B6 Competence passive!"));
            meta.setLore(lore);

            item.setItemMeta(meta);
            inv.setItem(SLOTS[i], item);
        }

        // Row 3: back button
        inv.setItem(27, GuiHelper.backButton());

        GuiHelper.decorate(inv, GuiHelper.Theme.SKILL);

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
