package fr.pasteque.skyblock.collection.model;

import java.util.HashMap;
import java.util.Map;

public class PlayerCollections {

    private final Map<String, Integer> counts;
    private final Map<String, Integer> claimedTiers;

    public PlayerCollections() {
        this.counts = new HashMap<String, Integer>();
        this.claimedTiers = new HashMap<String, Integer>();
    }

    public int getCount(String material) {
        Integer count = counts.get(material);
        return count == null ? 0 : count;
    }

    public void addCount(String material, int amount) {
        counts.put(material, getCount(material) + amount);
    }

    public int getClaimedTier(String entryId) {
        Integer tier = claimedTiers.get(entryId);
        return tier == null ? 0 : tier;
    }

    public void claimTier(String entryId, int tier) {
        claimedTiers.put(entryId, tier);
    }

    public Map<String, Integer> getCounts() {
        return counts;
    }

    public Map<String, Integer> getClaimedTiers() {
        return claimedTiers;
    }
}
