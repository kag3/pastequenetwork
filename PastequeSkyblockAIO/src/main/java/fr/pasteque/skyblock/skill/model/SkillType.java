package fr.pasteque.skyblock.skill.model;

public enum SkillType {
    COMBAT("Combat", "&c", "DIAMOND_SWORD"),
    MINING("Minage", "&b", "DIAMOND_PICKAXE"),
    FARMING("Agriculture", "&a", "DIAMOND_HOE"),
    FISHING("Peche", "&9", "FISHING_ROD"),
    ENCHANTING("Enchantement", "&d", "ENCHANTMENT_TABLE");

    private final String displayName;
    private final String color;
    private final String icon;

    SkillType(String displayName, String color, String icon) {
        this.displayName = displayName;
        this.color = color;
        this.icon = icon;
    }

    public String getDisplayName() { return displayName; }
    public String getColor() { return color; }
    public String getIcon() { return icon; }
}
