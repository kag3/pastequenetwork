package fr.pasteque.skyblock.combatpass.model;

public class PassTier {

    private final int tier;
    private final String rewardDescription;
    private final String material;
    private final int amount;
    private final double moneyReward;

    public PassTier(int tier, String rewardDescription, String material, int amount, double moneyReward) {
        this.tier = tier;
        this.rewardDescription = rewardDescription;
        this.material = material;
        this.amount = amount;
        this.moneyReward = moneyReward;
    }

    public int getTier() {
        return tier;
    }

    public String getRewardDescription() {
        return rewardDescription;
    }

    public String getMaterial() {
        return material;
    }

    public int getAmount() {
        return amount;
    }

    public double getMoneyReward() {
        return moneyReward;
    }
}
