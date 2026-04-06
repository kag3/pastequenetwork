package fr.pasteque.skyblock.enchant;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public enum CustomEnchant {

    TELEKINESIS(
            "Telekinesis", "&b", 1, Material.ENDER_PEARL, 5000,
            new String[]{"&7Les items mines vont", "&7directement dans l'inventaire."}
    ),
    SMELTING_TOUCH(
            "Smelting Touch", "&6", 1, Material.FURNACE, 3000,
            new String[]{"&7Les minerais sont automatiquement", "&7fondus lors du minage."}
    ),
    LUMBERJACK(
            "Lumberjack", "&2", 3, Material.IRON_AXE, 2000,
            new String[]{"&7Coupe l'arbre entier.", "&7Rayon = niveau x2 + 1 blocs."}
    ),
    EXPERIENCE(
            "Experience", "&a", 3, Material.EXP_BOTTLE, 4000,
            new String[]{"&7Bonus d'XP au minage", "&7et lors des kills."}
    ),
    VENOM(
            "Venom", "&2", 3, Material.SPIDER_EYE, 6000,
            new String[]{"&7Empoisonne l'ennemi", "&7lors d'un coup."}
    ),
    LIFESTEAL(
            "Lifesteal", "&c", 3, Material.REDSTONE, 8000,
            new String[]{"&7Vole de la vie a", "&7chaque coup porte."}
    ),
    HASTE(
            "Haste", "&e", 3, Material.GOLD_PICKAXE, 3000,
            new String[]{"&7Augmente la vitesse", "&7de minage."}
    ),
    IMPLANTS(
            "Implants", "&a", 1, Material.WHEAT, 2000,
            new String[]{"&7Replante automatiquement", "&7les cultures cassees."}
    );

    private static final String ENCHANT_PREFIX = "\u00a77\u2726 ";
    private static final String[] ROMAN = {"I", "II", "III"};

    private final String displayName;
    private final String color;
    private final int maxLevel;
    private final Material icon;
    private final int baseCost;
    private final String[] description;

    CustomEnchant(String displayName, String color, int maxLevel, Material icon, int baseCost, String[] description) {
        this.displayName = displayName;
        this.color = color;
        this.maxLevel = maxLevel;
        this.icon = icon;
        this.baseCost = baseCost;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public Material getIcon() {
        return icon;
    }

    public int getBaseCost() {
        return baseCost;
    }

    public String[] getDescription() {
        return description;
    }

    /**
     * Returns the cost for a specific level.
     * For max-level-1 enchants the cost is flat; otherwise it scales with level.
     */
    public int getCost(int level) {
        return baseCost * level;
    }

    /**
     * Returns a roman numeral string for the given level (1-3).
     */
    public static String toRoman(int level) {
        if (level < 1 || level > 3) return String.valueOf(level);
        return ROMAN[level - 1];
    }

    /**
     * Returns the formatted lore line for this enchant at the given level.
     * Example: "§7✦ §bTelekinesis I"
     */
    public String getLoreLine(int level) {
        return PastequeSkyblockPlugin.color(ENCHANT_PREFIX + color + displayName + " " + toRoman(level));
    }

    /**
     * Returns the raw (color-stripped) identifier used for lore scanning.
     */
    private String getLoreTag() {
        return PastequeSkyblockPlugin.color(color + displayName);
    }

    /**
     * Checks whether the given item has this enchant applied (via lore scanning).
     */
    public boolean hasEnchant(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;
        String tag = getLoreTag();
        for (String line : meta.getLore()) {
            if (line.contains(tag)) return true;
        }
        return false;
    }

    /**
     * Reads the level of this enchant from the item lore.
     * Returns 0 if the enchant is not present.
     */
    public int getLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return 0;
        String tag = getLoreTag();
        for (String line : meta.getLore()) {
            if (!line.contains(tag)) continue;
            for (int lvl = ROMAN.length; lvl >= 1; lvl--) {
                if (line.endsWith(ROMAN[lvl - 1])) return lvl;
            }
            return 1;
        }
        return 0;
    }

    /**
     * Applies (or upgrades) this enchant on the given item at the specified level.
     * Removes any existing line for this enchant first, then appends the new line.
     */
    public void apply(ItemStack item, int level) {
        if (item == null) return;
        if (level < 1 || level > maxLevel) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<String> lore;
        if (meta.hasLore()) {
            lore = new ArrayList<String>(meta.getLore());
        } else {
            lore = new ArrayList<String>();
        }

        // Remove existing line for this enchant
        String tag = getLoreTag();
        List<String> cleaned = new ArrayList<String>();
        for (String line : lore) {
            if (!line.contains(tag)) {
                cleaned.add(line);
            }
        }

        // Add separator if this is the first enchant
        boolean hasEnchants = false;
        String sepColored = PastequeSkyblockPlugin.color("&8&m────────────────────");
        for (String line : cleaned) {
            if (line.contains("\u2726")) {
                hasEnchants = true;
                break;
            }
        }
        if (!hasEnchants) {
            cleaned.add("");
            cleaned.add(sepColored);
        }

        cleaned.add(getLoreLine(level));
        meta.setLore(cleaned);
        item.setItemMeta(meta);
    }

    /**
     * Removes this enchant from the given item.
     */
    public void remove(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return;
        String tag = getLoreTag();
        List<String> lore = new ArrayList<String>();
        for (String line : meta.getLore()) {
            if (!line.contains(tag)) {
                lore.add(line);
            }
        }

        // Remove separator if no enchants remain
        boolean hasEnchants = false;
        for (String line : lore) {
            if (line.contains("\u2726")) {
                hasEnchants = true;
                break;
            }
        }
        if (!hasEnchants) {
            String sepColored = PastequeSkyblockPlugin.color("&8&m────────────────────");
            List<String> final2 = new ArrayList<String>();
            for (String line : lore) {
                if (!line.equals(sepColored)) {
                    final2.add(line);
                }
            }
            // Trim trailing empty lines
            while (!final2.isEmpty() && final2.get(final2.size() - 1).trim().isEmpty()) {
                final2.remove(final2.size() - 1);
            }
            lore = final2;
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    /**
     * Finds a CustomEnchant by name (case-insensitive, supports underscores or display names).
     */
    public static CustomEnchant fromName(String name) {
        if (name == null) return null;
        String upper = name.toUpperCase().replace(" ", "_").replace("-", "_");
        for (CustomEnchant ce : values()) {
            if (ce.name().equals(upper)) return ce;
            if (ce.displayName.equalsIgnoreCase(name)) return ce;
        }
        return null;
    }
}
