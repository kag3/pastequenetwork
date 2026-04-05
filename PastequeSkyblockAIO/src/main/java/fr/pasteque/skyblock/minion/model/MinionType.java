package fr.pasteque.skyblock.minion.model;

public enum MinionType {

    WHEAT_MINION("Minion Ble", "WHEAT", "WHEAT", 60),
    COBBLE_MINION("Minion Pierre", "STONE", "COBBLESTONE", 30),
    IRON_MINION("Minion Fer", "IRON_ORE", "IRON_INGOT", 120),
    GOLD_MINION("Minion Or", "GOLD_ORE", "GOLD_INGOT", 180),
    DIAMOND_MINION("Minion Diamant", "DIAMOND_ORE", "DIAMOND", 300),
    WOOD_MINION("Minion Bois", "LOG", "LOG", 45),
    FISHING_MINION("Minion Peche", "FISHING_ROD", "RAW_FISH", 90),
    CACTUS_MINION("Minion Cactus", "CACTUS", "CACTUS", 60);

    private final String displayName;
    private final String iconMaterial;
    private final String produceMaterial;
    private final int intervalSeconds;

    MinionType(String displayName, String iconMaterial, String produceMaterial, int intervalSeconds) {
        this.displayName = displayName;
        this.iconMaterial = iconMaterial;
        this.produceMaterial = produceMaterial;
        this.intervalSeconds = intervalSeconds;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconMaterial() {
        return iconMaterial;
    }

    public String getProduceMaterial() {
        return produceMaterial;
    }

    public int getIntervalSeconds() {
        return intervalSeconds;
    }
}
