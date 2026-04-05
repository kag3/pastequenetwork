package fr.pasteque.skyblock.slayer.model;

import java.util.HashMap;
import java.util.Map;

public class PlayerSlayerData {

    private final Map<SlayerType, Integer> xp = new HashMap<SlayerType, Integer>();
    private final Map<SlayerType, Integer> level = new HashMap<SlayerType, Integer>();

    private SlayerType activeQuest;
    private int activeQuestTier;
    private int questKillCount;
    private int questKillTarget;

    public PlayerSlayerData() {
        for (SlayerType type : SlayerType.values()) {
            xp.put(type, 0);
            level.put(type, 0);
        }
    }

    // ------------------------------------------------------------------
    //  XP / Level
    // ------------------------------------------------------------------

    public int getXp(SlayerType type) {
        Integer val = xp.get(type);
        return val != null ? val : 0;
    }

    public void setXp(SlayerType type, int amount) {
        xp.put(type, amount);
    }

    public void addXp(SlayerType type, int amount) {
        xp.put(type, getXp(type) + amount);
    }

    public int getLevel(SlayerType type) {
        Integer val = level.get(type);
        return val != null ? val : 0;
    }

    public void setLevel(SlayerType type, int lvl) {
        level.put(type, lvl);
    }

    // ------------------------------------------------------------------
    //  Quest
    // ------------------------------------------------------------------

    public boolean hasActiveQuest() {
        return activeQuest != null;
    }

    public SlayerType getActiveQuest() {
        return activeQuest;
    }

    public int getActiveQuestTier() {
        return activeQuestTier;
    }

    public int getQuestKillCount() {
        return questKillCount;
    }

    public int getQuestKillTarget() {
        return questKillTarget;
    }

    public void startQuest(SlayerType type, int tier) {
        this.activeQuest = type;
        this.activeQuestTier = tier;
        this.questKillCount = 0;
        this.questKillTarget = tier * 10 + 5;
    }

    public void incrementKillCount() {
        this.questKillCount++;
    }

    public boolean isQuestComplete() {
        return activeQuest != null && questKillCount >= questKillTarget;
    }

    public void completeQuest() {
        this.activeQuest = null;
        this.activeQuestTier = 0;
        this.questKillCount = 0;
        this.questKillTarget = 0;
    }

    public String getProgress() {
        if (activeQuest == null) {
            return "Aucune quete active";
        }
        return activeQuest.getDisplayName() + " Tier " + activeQuestTier
                + " - " + questKillCount + "/" + questKillTarget + " kills";
    }

    // ------------------------------------------------------------------
    //  Raw maps for serialization
    // ------------------------------------------------------------------

    public Map<SlayerType, Integer> getXpMap() {
        return xp;
    }

    public Map<SlayerType, Integer> getLevelMap() {
        return level;
    }
}
