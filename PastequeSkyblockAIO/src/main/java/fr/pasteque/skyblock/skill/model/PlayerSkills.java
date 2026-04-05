package fr.pasteque.skyblock.skill.model;

import java.util.HashMap;
import java.util.Map;

public class PlayerSkills {

    public static final int MAX_LEVEL = 50;

    private final Map<SkillType, Integer> xp;
    private final Map<SkillType, Integer> levels;

    public PlayerSkills() {
        this.xp = new HashMap<SkillType, Integer>();
        this.levels = new HashMap<SkillType, Integer>();
        for (SkillType type : SkillType.values()) {
            xp.put(type, 0);
            levels.put(type, 0);
        }
    }

    public int getXp(SkillType type) {
        Integer val = xp.get(type);
        return val == null ? 0 : val;
    }

    public int getLevel(SkillType type) {
        Integer val = levels.get(type);
        return val == null ? 0 : val;
    }

    public void setLevel(SkillType type, int level) {
        levels.put(type, level);
    }

    /**
     * XP required to go from the given level to the next level.
     */
    public static int xpRequired(int level) {
        return level * 100 + 50;
    }

    /**
     * Adds XP to the given skill type.
     * @return true if the player levelled up
     */
    public boolean addXp(SkillType type, int amount) {
        int currentXp = getXp(type);
        int currentLevel = getLevel(type);
        if (currentLevel >= MAX_LEVEL) {
            return false;
        }
        currentXp += amount;
        boolean levelledUp = false;
        while (currentLevel < MAX_LEVEL && currentXp >= xpRequired(currentLevel)) {
            currentXp -= xpRequired(currentLevel);
            currentLevel++;
            levelledUp = true;
        }
        xp.put(type, currentXp);
        levels.put(type, currentLevel);
        return levelledUp;
    }

    public Map<SkillType, Integer> getXpMap() {
        return xp;
    }

    public Map<SkillType, Integer> getLevelsMap() {
        return levels;
    }
}
