package fr.pasteque.skyblock.collection.model;

public enum CollectionCategory {

    FARMING("Agriculture", "&a", "WHEAT"),
    MINING("Minage", "&b", "DIAMOND_ORE"),
    COMBAT("Combat", "&c", "ROTTEN_FLESH"),
    FISHING("Peche", "&9", "RAW_FISH"),
    FORAGING("Bucheron", "&2", "LOG");

    private final String displayName;
    private final String color;
    private final String icon;

    CollectionCategory(String displayName, String color, String icon) {
        this.displayName = displayName;
        this.color = color;
        this.icon = icon;
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
}
