package fr.pasteque.skyblock.collection.model;

import java.util.ArrayList;
import java.util.List;

public class CollectionEntry {

    private final String id;
    private final String displayName;
    private final String material;
    private final CollectionCategory category;
    private final int[] tiers;
    private final String[] tierRewards;

    public CollectionEntry(String id, String displayName, String material,
                           CollectionCategory category, int[] tiers, String[] tierRewards) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.category = category;
        this.tiers = tiers;
        this.tierRewards = tierRewards;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMaterial() {
        return material;
    }

    public CollectionCategory getCategory() {
        return category;
    }

    public int[] getTiers() {
        return tiers;
    }

    public String[] getTierRewards() {
        return tierRewards;
    }

    public static List<CollectionEntry> createDefaults() {
        List<CollectionEntry> list = new ArrayList<CollectionEntry>();

        // ── FARMING ──────────────────────────────────────────────────────
        list.add(new CollectionEntry("wheat", "Ble", "WHEAT", CollectionCategory.FARMING,
                new int[]{50, 150, 500, 1500, 5000},
                new String[]{"100 Pasteques", "300 Pasteques", "700 Pasteques", "1500 Pasteques", "5000 Pasteques"}));
        list.add(new CollectionEntry("carrot", "Carotte", "CARROT_ITEM", CollectionCategory.FARMING,
                new int[]{50, 150, 500, 1500, 5000},
                new String[]{"100 Pasteques", "300 Pasteques", "700 Pasteques", "1500 Pasteques", "5000 Pasteques"}));
        list.add(new CollectionEntry("potato", "Pomme de Terre", "POTATO_ITEM", CollectionCategory.FARMING,
                new int[]{50, 150, 500, 1500, 5000},
                new String[]{"100 Pasteques", "300 Pasteques", "700 Pasteques", "1500 Pasteques", "5000 Pasteques"}));
        list.add(new CollectionEntry("sugar_cane", "Canne a Sucre", "SUGAR_CANE", CollectionCategory.FARMING,
                new int[]{50, 150, 500, 1500, 5000},
                new String[]{"100 Pasteques", "300 Pasteques", "700 Pasteques", "1500 Pasteques", "5000 Pasteques"}));
        list.add(new CollectionEntry("melon", "Melon", "MELON", CollectionCategory.FARMING,
                new int[]{50, 150, 500, 1500, 5000},
                new String[]{"100 Pasteques", "300 Pasteques", "700 Pasteques", "1500 Pasteques", "5000 Pasteques"}));
        list.add(new CollectionEntry("pumpkin", "Citrouille", "PUMPKIN", CollectionCategory.FARMING,
                new int[]{50, 150, 500, 1500, 5000},
                new String[]{"100 Pasteques", "300 Pasteques", "700 Pasteques", "1500 Pasteques", "5000 Pasteques"}));
        list.add(new CollectionEntry("cactus", "Cactus", "CACTUS", CollectionCategory.FARMING,
                new int[]{50, 150, 500, 1500, 5000},
                new String[]{"100 Pasteques", "300 Pasteques", "700 Pasteques", "1500 Pasteques", "5000 Pasteques"}));
        list.add(new CollectionEntry("nether_wart", "Verrues du Nether", "NETHER_STALK", CollectionCategory.FARMING,
                new int[]{50, 150, 500, 1500, 5000},
                new String[]{"100 Pasteques", "300 Pasteques", "700 Pasteques", "1500 Pasteques", "5000 Pasteques"}));

        // ── MINING ───────────────────────────────────────────────────────
        list.add(new CollectionEntry("cobblestone", "Pierre", "COBBLESTONE", CollectionCategory.MINING,
                new int[]{50, 200, 1000, 5000, 15000},
                new String[]{"100 Pasteques", "300 Pasteques", "700 Pasteques", "1500 Pasteques", "5000 Pasteques"}));
        list.add(new CollectionEntry("iron_ore", "Fer", "IRON_ORE", CollectionCategory.MINING,
                new int[]{25, 100, 300, 1000, 3000},
                new String[]{"100 Pasteques", "300 Pasteques", "700 Pasteques", "1500 Pasteques", "5000 Pasteques"}));
        list.add(new CollectionEntry("gold_ore", "Or", "GOLD_ORE", CollectionCategory.MINING,
                new int[]{25, 100, 300, 1000, 3000},
                new String[]{"100 Pasteques", "300 Pasteques", "700 Pasteques", "1500 Pasteques", "5000 Pasteques"}));
        list.add(new CollectionEntry("diamond", "Diamant", "DIAMOND", CollectionCategory.MINING,
                new int[]{10, 50, 150, 500, 1500},
                new String[]{"100 Pasteques", "300 Pasteques", "700 Pasteques", "1500 Pasteques", "5000 Pasteques"}));
        list.add(new CollectionEntry("emerald", "Emeraude", "EMERALD", CollectionCategory.MINING,
                new int[]{10, 50, 150, 500, 1500},
                new String[]{"100 Pasteques", "300 Pasteques", "700 Pasteques", "1500 Pasteques", "5000 Pasteques"}));
        list.add(new CollectionEntry("coal", "Charbon", "COAL", CollectionCategory.MINING,
                new int[]{50, 200, 1000, 5000, 15000},
                new String[]{"100 Pasteques", "300 Pasteques", "700 Pasteques", "1500 Pasteques", "5000 Pasteques"}));
        list.add(new CollectionEntry("lapis", "Lapis Lazuli", "INK_SACK", CollectionCategory.MINING,
                new int[]{25, 100, 300, 1000, 3000},
                new String[]{"100 Pasteques", "300 Pasteques", "700 Pasteques", "1500 Pasteques", "5000 Pasteques"}));
        list.add(new CollectionEntry("redstone", "Redstone", "REDSTONE", CollectionCategory.MINING,
                new int[]{25, 100, 300, 1000, 3000},
                new String[]{"100 Pasteques", "300 Pasteques", "700 Pasteques", "1500 Pasteques", "5000 Pasteques"}));

        // ── COMBAT ───────────────────────────────────────────────────────
        list.add(new CollectionEntry("rotten_flesh", "Chair Putrefiee", "ROTTEN_FLESH", CollectionCategory.COMBAT,
                new int[]{25, 100, 300, 1000, 3000},
                new String[]{"100 Pasteques", "300 Pasteques", "700 Pasteques", "1500 Pasteques", "5000 Pasteques"}));
        list.add(new CollectionEntry("bone", "Os", "BONE", CollectionCategory.COMBAT,
                new int[]{25, 100, 300, 1000, 3000},
                new String[]{"100 Pasteques", "300 Pasteques", "700 Pasteques", "1500 Pasteques", "5000 Pasteques"}));
        list.add(new CollectionEntry("spider_eye", "Oeil d'Araignee", "SPIDER_EYE", CollectionCategory.COMBAT,
                new int[]{25, 100, 300, 1000, 3000},
                new String[]{"100 Pasteques", "300 Pasteques", "700 Pasteques", "1500 Pasteques", "5000 Pasteques"}));
        list.add(new CollectionEntry("ender_pearl", "Perle de l'Ender", "ENDER_PEARL", CollectionCategory.COMBAT,
                new int[]{10, 50, 150, 500, 1500},
                new String[]{"100 Pasteques", "300 Pasteques", "700 Pasteques", "1500 Pasteques", "5000 Pasteques"}));
        list.add(new CollectionEntry("blaze_rod", "Baton de Blaze", "BLAZE_ROD", CollectionCategory.COMBAT,
                new int[]{10, 50, 150, 500, 1500},
                new String[]{"100 Pasteques", "300 Pasteques", "700 Pasteques", "1500 Pasteques", "5000 Pasteques"}));

        // ── FISHING ──────────────────────────────────────────────────────
        list.add(new CollectionEntry("raw_fish", "Poisson", "RAW_FISH", CollectionCategory.FISHING,
                new int[]{10, 50, 150, 500, 1500},
                new String[]{"100 Pasteques", "300 Pasteques", "700 Pasteques", "1500 Pasteques", "5000 Pasteques"}));

        return list;
    }
}
