package fr.pastequeworld.bedwars.generator;

import org.bukkit.ChatColor;
import org.bukkit.Material;

/**
 * Types de generateurs. Chaque type porte sa config YAML
 * (intervalles, tier, drops) dans config.yml section "generators".
 */
public enum GeneratorType {
    IRON("iron", Material.IRON_INGOT, ChatColor.WHITE, "Fer", false),
    GOLD("gold", Material.GOLD_INGOT, ChatColor.GOLD, "Or", false),
    DIAMOND("diamond", Material.DIAMOND, ChatColor.AQUA, "Diamant", true),
    EMERALD("emerald", Material.EMERALD, ChatColor.GREEN, "Emeraude", true);

    private final String configKey;
    private final Material itemMaterial;
    private final ChatColor color;
    private final String displayName;
    private final boolean tiered;

    GeneratorType(String configKey, Material itemMaterial, ChatColor color,
                  String displayName, boolean tiered) {
        this.configKey = configKey;
        this.itemMaterial = itemMaterial;
        this.color = color;
        this.displayName = displayName;
        this.tiered = tiered;
    }

    public String getConfigKey() { return configKey; }
    public Material getItemMaterial() { return itemMaterial; }
    public ChatColor getColor() { return color; }
    public String getDisplayName() { return displayName; }
    public boolean isTiered() { return tiered; }
}
