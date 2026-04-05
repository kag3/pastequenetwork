package fr.pasteque.skyblock.island.model;

public enum IslandUpgrade {

    SIZE("Taille d'ile", new int[]{320, 400, 500, 640}, new double[]{0, 50000, 150000, 500000}),
    MEMBERS("Places membres", new int[]{4, 6, 8, 12}, new double[]{0, 25000, 75000, 200000}),
    GENERATOR("Generateur", new int[]{1, 2, 3, 4}, new double[]{0, 100000, 300000, 750000}),
    SPAWNER("Limite spawners", new int[]{2, 4, 8, 16}, new double[]{0, 75000, 250000, 600000}),
    MINIONS("Limite minions", new int[]{5, 10, 15, 25}, new double[]{0, 50000, 200000, 500000});

    private final String displayName;
    private final int[] tiers;
    private final double[] costs;

    IslandUpgrade(String displayName, int[] tiers, double[] costs) {
        this.displayName = displayName;
        this.tiers = tiers;
        this.costs = costs;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getTierValue(int level) {
        if (level < 0 || level >= tiers.length) {
            return tiers[tiers.length - 1];
        }
        return tiers[level];
    }

    public double getCost(int level) {
        if (level < 0 || level >= costs.length) {
            return costs[costs.length - 1];
        }
        return costs[level];
    }

    public int getMaxLevel() {
        return tiers.length - 1;
    }
}
