package fr.pasteque.skyblock.farming;

import org.bukkit.Material;

public enum CustomCrop {

    ENCHANTED_MELON(
            "Melon Enchante",
            "&a",
            Material.MELON_SEEDS,
            Material.MELON_STEM,
            600,  // 30s * 20 ticks = 600 ticks per stage, 4 stages
            50.0,
            Material.MELON,
            4,
            0,
            500,
            5,
            new String[]{
                    "&7Un melon infuse de magie.",
                    "&7Plantez des graines enchantees",
                    "&7sur un bloc de terre labouree."
            }
    ),

    GOLDEN_CACTUS(
            "Cactus Dore",
            "&6",
            Material.CACTUS,
            Material.CACTUS,
            800,
            80.0,
            Material.CACTUS,
            4,
            0,
            800,
            8,
            new String[]{
                    "&7Un cactus recouvert d'or.",
                    "&7Plantez un cactus dore",
                    "&7sur du sable."
            }
    ),

    CRYSTAL_WHEAT(
            "Ble Cristallin",
            "&b",
            Material.SEEDS,
            Material.CROPS,
            600,
            30.0,
            Material.WHEAT,
            4,
            0,
            200,
            5,
            new String[]{
                    "&7Du ble aux reflets cristallins.",
                    "&7Plantez des graines cristallines",
                    "&7sur de la terre labouree."
            }
    ),

    ENCHANTED_SUGAR_CANE(
            "Canne a Sucre Enchantee",
            "&d",
            Material.SUGAR_CANE,
            Material.SUGAR_CANE_BLOCK,
            700,
            60.0,
            Material.SUGAR_CANE,
            4,
            0,
            600,
            7,
            new String[]{
                    "&7Une canne a sucre magique.",
                    "&7Plantez sur du sable ou de",
                    "&7l'herbe pres de l'eau."
            }
    ),

    LAVA_NETHERWART(
            "Verrue du Nether Ardente",
            "&c",
            Material.NETHER_STALK,
            Material.NETHER_WARTS,
            900,
            100.0,
            Material.NETHER_STALK,
            4,
            0,
            1500,
            12,
            new String[]{
                    "&7Une verrue baignee de lave.",
                    "&7Plantez sur du sable des ames."
            }
    ),

    PASTEQUE_ROYALE(
            "Pasteque Royale",
            "&5",
            Material.MELON_SEEDS,
            Material.MELON_STEM,
            1200,
            200.0,
            Material.MELON_BLOCK,
            4,
            5,
            5000,
            20,
            new String[]{
                    "&7La culture ultime de Pasteque.",
                    "&7Necessite le niveau 5 en farming.",
                    "&7Plantez sur de la terre labouree."
            }
    );

    private final String displayName;
    private final String color;
    private final Material baseMaterial;
    private final Material plantBlock;
    private final int growthTicks;
    private final double sellPrice;
    private final Material icon;
    private final int maxGrowthStage;
    private final int requiredLevel;
    private final int seedPrice;
    private final int xpReward;
    private final String[] loreDesc;

    CustomCrop(String displayName, String color, Material baseMaterial, Material plantBlock,
               int growthTicks, double sellPrice, Material icon, int maxGrowthStage,
               int requiredLevel, int seedPrice, int xpReward, String[] loreDesc) {
        this.displayName = displayName;
        this.color = color;
        this.baseMaterial = baseMaterial;
        this.plantBlock = plantBlock;
        this.growthTicks = growthTicks;
        this.sellPrice = sellPrice;
        this.icon = icon;
        this.maxGrowthStage = maxGrowthStage;
        this.requiredLevel = requiredLevel;
        this.seedPrice = seedPrice;
        this.xpReward = xpReward;
        this.loreDesc = loreDesc;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }

    public Material getBaseMaterial() {
        return baseMaterial;
    }

    public Material getPlantBlock() {
        return plantBlock;
    }

    public int getGrowthTicks() {
        return growthTicks;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public Material getIcon() {
        return icon;
    }

    public int getMaxGrowthStage() {
        return maxGrowthStage;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public int getSeedPrice() {
        return seedPrice;
    }

    public int getXpReward() {
        return xpReward;
    }

    public String[] getLoreDesc() {
        return loreDesc;
    }

    public String getColoredName() {
        return color + "&l" + displayName;
    }

    public String getSeedName() {
        return color + "Graine: " + displayName;
    }

    public String getHarvestName() {
        return color + "&l" + displayName;
    }

    /**
     * Identifiant unique utilise dans le lore des items pour reconnaitre
     * les recoltes et graines custom.
     */
    public String getHarvestId() {
        return "&8ID: HARVEST_" + name();
    }

    public String getSeedId() {
        return "&8ID: SEED_" + name();
    }

    /**
     * Cherche un CustomCrop a partir d'une ligne d'identifiant dans le lore.
     */
    public static CustomCrop fromHarvestId(String loreLine) {
        if (loreLine == null) {
            return null;
        }
        for (CustomCrop crop : values()) {
            if (loreLine.contains("HARVEST_" + crop.name())) {
                return crop;
            }
        }
        return null;
    }

    public static CustomCrop fromSeedId(String loreLine) {
        if (loreLine == null) {
            return null;
        }
        for (CustomCrop crop : values()) {
            if (loreLine.contains("SEED_" + crop.name())) {
                return crop;
            }
        }
        return null;
    }
}
