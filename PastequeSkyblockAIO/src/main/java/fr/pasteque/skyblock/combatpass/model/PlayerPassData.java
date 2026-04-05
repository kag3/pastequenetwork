package fr.pasteque.skyblock.combatpass.model;

import java.util.HashSet;
import java.util.Set;

public class PlayerPassData {

    private int xp;
    private int currentTier;
    private final Set<Integer> claimedTiers;
    private boolean premium;

    public PlayerPassData() {
        this.xp = 0;
        this.currentTier = 0;
        this.claimedTiers = new HashSet<Integer>();
        this.premium = false;
    }

    public int getXp() {
        return xp;
    }

    public void addXp(int amount) {
        this.xp += amount;
        recalculateTier();
    }

    public int getCurrentTier() {
        return currentTier;
    }

    public boolean hasClaimed(int tier) {
        return claimedTiers.contains(tier);
    }

    public void claimTier(int tier) {
        claimedTiers.add(tier);
    }

    public boolean isPremium() {
        return premium;
    }

    public void setPremium(boolean premium) {
        this.premium = premium;
    }

    public Set<Integer> getClaimedTiers() {
        return claimedTiers;
    }

    /**
     * Recalculates the current tier based on XP.
     * Each tier requires tier * 500 XP (cumulative threshold).
     * Tier 1 = 500 XP, Tier 2 = 1000 XP, Tier 3 = 1500 XP, etc.
     */
    private void recalculateTier() {
        int tier = 0;
        for (int t = 1; t <= 30; t++) {
            if (xp >= t * 500) {
                tier = t;
            } else {
                break;
            }
        }
        this.currentTier = tier;
    }
}
