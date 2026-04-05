package fr.pasteque.skyblock.island.model;

public enum IslandPreset {

    CLASSIC("Classique", "&a", "GRASS", "Ile standard avec arbre et coffre"),
    DESERT("Desert", "&e", "SAND", "Ile de sable avec cactus"),
    JUNGLE("Jungle", "&2", "LOG:3", "Ile jungle avec lianes"),
    NETHER("Nether", "&c", "NETHERRACK", "Ile infernale avec lave"),
    ICE("Glaciale", "&b", "ICE", "Ile de glace et neige"),
    MUSHROOM("Champignon", "&d", "HUGE_MUSHROOM_1", "Ile champignon rare");

    private final String displayName;
    private final String color;
    private final String icon;
    private final String description;

    IslandPreset(String displayName, String color, String icon, String description) {
        this.displayName = displayName;
        this.color = color;
        this.icon = icon;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }

    public String getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }
}
