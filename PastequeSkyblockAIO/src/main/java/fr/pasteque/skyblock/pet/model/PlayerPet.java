package fr.pasteque.skyblock.pet.model;

public class PlayerPet {

    private final PetType type;
    private int level;
    private int xp;
    private boolean active;

    public PlayerPet(PetType type) {
        this.type = type;
        this.level = 1;
        this.xp = 0;
        this.active = false;
    }

    public PlayerPet(PetType type, int level, int xp, boolean active) {
        this.type = type;
        this.level = level;
        this.xp = xp;
        this.active = active;
    }

    public PetType getType() {
        return type;
    }

    public int getLevel() {
        return level;
    }

    public int getXp() {
        return xp;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Adds XP and handles level-up. Level formula: level * 50 XP to next level.
     */
    public void addXp(int amount) {
        this.xp += amount;
        while (level < 100 && xp >= getXpRequired()) {
            xp -= getXpRequired();
            level++;
        }
        if (level >= 100) {
            level = 100;
        }
    }

    /**
     * XP required for the next level.
     */
    public int getXpRequired() {
        return level * 50;
    }

    /**
     * Returns the bonus percentage (level / 10.0). Level 100 = +10.0% bonus.
     */
    public double getBonus() {
        return level / 10.0;
    }
}
