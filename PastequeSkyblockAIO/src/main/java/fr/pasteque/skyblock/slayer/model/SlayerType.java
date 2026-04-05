package fr.pasteque.skyblock.slayer.model;

public enum SlayerType {

    ZOMBIE("Revenant", "&2", "ROTTEN_FLESH", "ZOMBIE",
            new int[]{50, 150, 500, 1500, 5000},
            new int[]{5, 25, 100, 500, 2000}),
    SPIDER("Tarantule", "&a", "SPIDER_EYE", "SPIDER",
            new int[]{25, 100, 350, 1000, 3000},
            new int[]{10, 50, 200, 750, 3000}),
    WOLF("Sven", "&b", "BONE", "WOLF",
            new int[]{30, 120, 400, 1200, 4000},
            new int[]{15, 75, 250, 1000, 4000}),
    ENDERMAN("Voidgloom", "&5", "ENDER_PEARL", "ENDERMAN",
            new int[]{100, 300, 800, 2500, 8000},
            new int[]{25, 100, 400, 1500, 5000}),
    BLAZE("Inferno", "&6", "BLAZE_ROD", "BLAZE",
            new int[]{75, 250, 700, 2000, 6000},
            new int[]{20, 80, 300, 1200, 4500});

    private final String displayName;
    private final String color;
    private final String iconMaterial;
    private final String mobType;
    private final int[] xpPerTier;
    private final int[] costPerTier;

    SlayerType(String displayName, String color, String iconMaterial, String mobType,
               int[] xpPerTier, int[] costPerTier) {
        this.displayName = displayName;
        this.color = color;
        this.iconMaterial = iconMaterial;
        this.mobType = mobType;
        this.xpPerTier = xpPerTier;
        this.costPerTier = costPerTier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }

    public String getIconMaterial() {
        return iconMaterial;
    }

    public String getMobType() {
        return mobType;
    }

    public int[] getXpPerTier() {
        return xpPerTier;
    }

    public int[] getCostPerTier() {
        return costPerTier;
    }

    public int getXpForTier(int tier) {
        if (tier < 1 || tier > xpPerTier.length) {
            return 0;
        }
        return xpPerTier[tier - 1];
    }

    public int getCostForTier(int tier) {
        if (tier < 1 || tier > costPerTier.length) {
            return 0;
        }
        return costPerTier[tier - 1];
    }

    public static SlayerType fromMobType(String mobType) {
        for (SlayerType type : values()) {
            if (type.mobType.equalsIgnoreCase(mobType)) {
                return type;
            }
        }
        return null;
    }

    public static SlayerType fromName(String name) {
        for (SlayerType type : values()) {
            if (type.name().equalsIgnoreCase(name) || type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
