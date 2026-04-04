package fr.pasteque.skyblock.arena.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ArenaPlayerData {

    private final UUID uuid;
    private String name;
    private int level;
    private int xp;
    private int kills;
    private int deaths;
    private int activitySeconds;
    private int streakIntervals;
    private boolean pendingCombatPenalty;
    private double pendingPenaltyAmount;
    private final Set<String> purchasedKits = new HashSet<String>();

    public ArenaPlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }
    public int getKills() { return kills; }
    public void setKills(int kills) { this.kills = kills; }
    public int getDeaths() { return deaths; }
    public void setDeaths(int deaths) { this.deaths = deaths; }
    public int getActivitySeconds() { return activitySeconds; }
    public void setActivitySeconds(int activitySeconds) { this.activitySeconds = activitySeconds; }
    public int getStreakIntervals() { return streakIntervals; }
    public void setStreakIntervals(int streakIntervals) { this.streakIntervals = streakIntervals; }
    public boolean isPendingCombatPenalty() { return pendingCombatPenalty; }
    public void setPendingCombatPenalty(boolean pendingCombatPenalty) { this.pendingCombatPenalty = pendingCombatPenalty; }
    public double getPendingPenaltyAmount() { return pendingPenaltyAmount; }
    public void setPendingPenaltyAmount(double pendingPenaltyAmount) { this.pendingPenaltyAmount = pendingPenaltyAmount; }
    public Set<String> getPurchasedKits() { return purchasedKits; }

    public void addKill() { this.kills++; }
    public void addDeath() { this.deaths++; }
    public void addXp(int amount) { this.xp += Math.max(0, amount); }
    public void addActivitySecond() { this.activitySeconds++; }

    public double getRatio() {
        if (deaths <= 0) {
            return kills;
        }
        return ((double) kills) / deaths;
    }
}
