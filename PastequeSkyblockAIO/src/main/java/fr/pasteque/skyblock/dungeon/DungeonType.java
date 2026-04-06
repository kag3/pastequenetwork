package fr.pasteque.skyblock.dungeon;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public enum DungeonType {

    CRYPT(
            "Crypte Oubliee",
            Material.MOSSY_COBBLESTONE,
            3, 5,
            10,
            "&c&lGardien de la Crypte",
            EntityType.SKELETON, // wither skeleton not in 1.9 enum — use skeleton
            2000, 5000,
            new EntityType[]{EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER}
    ),

    NETHER_FORTRESS(
            "Forteresse du Nether",
            Material.NETHER_BRICK,
            3, 5,
            15,
            "&c&lSeigneur des Flammes",
            EntityType.BLAZE,
            5000, 10000,
            new EntityType[]{EntityType.BLAZE, EntityType.SKELETON, EntityType.MAGMA_CUBE}
    ),

    ENDER_SANCTUM(
            "Sanctuaire de l'End",
            Material.ENDER_STONE,
            3, 5,
            20,
            "&c&lArchitecte du Vide",
            EntityType.ENDERMAN,
            10000, 20000,
            new EntityType[]{EntityType.ENDERMAN, EntityType.SILVERFISH}
    );

    private final String displayName;
    private final Material icon;
    private final int minPlayers;
    private final int maxPlayers;
    private final int timerMinutes;
    private final String bossName;
    private final EntityType bossType;
    private final double minReward;
    private final double maxReward;
    private final EntityType[] mobTypes;

    DungeonType(String displayName, Material icon, int minPlayers, int maxPlayers,
                int timerMinutes, String bossName, EntityType bossType,
                double minReward, double maxReward, EntityType[] mobTypes) {
        this.displayName = displayName;
        this.icon = icon;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.timerMinutes = timerMinutes;
        this.bossName = bossName;
        this.bossType = bossType;
        this.minReward = minReward;
        this.maxReward = maxReward;
        this.mobTypes = mobTypes;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getTimerMinutes() {
        return timerMinutes;
    }

    public String getBossName() {
        return bossName;
    }

    public EntityType getBossType() {
        return bossType;
    }

    public double getMinReward() {
        return minReward;
    }

    public double getMaxReward() {
        return maxReward;
    }

    public EntityType[] getMobTypes() {
        return mobTypes;
    }

    public String getDifficultyLabel() {
        switch (this) {
            case CRYPT:
                return "&aFacile";
            case NETHER_FORTRESS:
                return "&6Moyen";
            case ENDER_SANCTUM:
                return "&cDifficile";
            default:
                return "&7Inconnu";
        }
    }

    /**
     * Resolve a dungeon type from a user-friendly alias.
     */
    public static DungeonType fromAlias(String alias) {
        if (alias == null) {
            return null;
        }
        String lower = alias.toLowerCase();
        if (lower.equals("crypt") || lower.equals("crypte")) {
            return CRYPT;
        }
        if (lower.equals("nether") || lower.equals("fortress") || lower.equals("nether_fortress")) {
            return NETHER_FORTRESS;
        }
        if (lower.equals("ender") || lower.equals("end") || lower.equals("sanctum") || lower.equals("ender_sanctum")) {
            return ENDER_SANCTUM;
        }
        return null;
    }
}
