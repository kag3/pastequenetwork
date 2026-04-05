package fr.pasteque.skyblock.pet.model;

public enum PetType {

    WOLF("Loup", "&7", "BONE", "COMBAT"),
    BEE("Abeille", "&e", "YELLOW_FLOWER", "FARMING"),
    ENDERMAN("Enderman", "&5", "ENDER_PEARL", "COMBAT"),
    RABBIT("Lapin", "&f", "CARROT_ITEM", "FARMING"),
    OCELOT("Ocelot", "&6", "RAW_FISH", "FISHING"),
    GOLEM("Golem", "&8", "IRON_INGOT", "MINING"),
    BLAZE("Blaze", "&c", "BLAZE_ROD", "COMBAT"),
    SHEEP("Mouton", "&d", "WOOL", "FARMING");

    private final String displayName;
    private final String color;
    private final String iconMaterial;
    private final String skillAffinity;

    PetType(String displayName, String color, String iconMaterial, String skillAffinity) {
        this.displayName = displayName;
        this.color = color;
        this.iconMaterial = iconMaterial;
        this.skillAffinity = skillAffinity;
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

    public String getSkillAffinity() {
        return skillAffinity;
    }
}
