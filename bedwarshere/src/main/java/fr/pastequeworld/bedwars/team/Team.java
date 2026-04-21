package fr.pastequeworld.bedwars.team;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Equipe en partie. Contient :
 *   - les membres (joueurs vivants + eliminies)
 *   - la position du spawn, du lit, des generateurs de l'equipe
 *   - les upgrades actifs (sharpness, protection, haste, healpool, trap)
 *   - l'etat du lit (vivant ou detruit)
 */
public class Team {

    private final TeamColor color;

    private final Set<UUID> members = new LinkedHashSet<UUID>();
    private final Set<UUID> aliveMembers = new HashSet<UUID>();
    private final Set<UUID> finalKilledMembers = new HashSet<UUID>();

    private Location spawnLocation;
    private Location bedLocation;
    private Location shopLocation;
    private Location upgradeLocation;
    private Location ironGeneratorLocation;
    private Location goldGeneratorLocation;

    private boolean bedAlive = true;

    // Upgrades
    private int sharpnessLevel;
    private int protectionLevel;
    private int hasteLevel;
    private boolean healPool;
    private boolean dragonBuff;

    public Team(TeamColor color) {
        this.color = color;
    }

    public TeamColor getColor() { return color; }

    public Set<UUID> getMembers() { return members; }

    public Set<UUID> getAliveMembers() { return aliveMembers; }

    public boolean contains(UUID uuid) { return members.contains(uuid); }

    public boolean contains(Player player) { return members.contains(player.getUniqueId()); }

    public void addMember(UUID uuid) {
        members.add(uuid);
        aliveMembers.add(uuid);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        aliveMembers.remove(uuid);
    }

    public void markDead(UUID uuid) {
        aliveMembers.remove(uuid);
        if (!bedAlive) finalKilledMembers.add(uuid);
    }

    public void markAlive(UUID uuid) {
        aliveMembers.add(uuid);
    }

    public boolean isEliminated() {
        return !bedAlive && aliveMembers.isEmpty();
    }

    public boolean isBedAlive() { return bedAlive; }

    public void setBedAlive(boolean alive) { this.bedAlive = alive; }

    public Location getSpawnLocation() { return spawnLocation; }
    public void setSpawnLocation(Location loc) { this.spawnLocation = loc; }

    public Location getBedLocation() { return bedLocation; }
    public void setBedLocation(Location loc) { this.bedLocation = loc; }

    public Location getShopLocation() { return shopLocation; }
    public void setShopLocation(Location loc) { this.shopLocation = loc; }

    public Location getUpgradeLocation() { return upgradeLocation; }
    public void setUpgradeLocation(Location loc) { this.upgradeLocation = loc; }

    public Location getIronGeneratorLocation() { return ironGeneratorLocation; }
    public void setIronGeneratorLocation(Location loc) { this.ironGeneratorLocation = loc; }

    public Location getGoldGeneratorLocation() { return goldGeneratorLocation; }
    public void setGoldGeneratorLocation(Location loc) { this.goldGeneratorLocation = loc; }

    public int getSharpnessLevel() { return sharpnessLevel; }
    public void setSharpnessLevel(int level) { this.sharpnessLevel = level; }

    public int getProtectionLevel() { return protectionLevel; }
    public void setProtectionLevel(int level) { this.protectionLevel = level; }

    public int getHasteLevel() { return hasteLevel; }
    public void setHasteLevel(int level) { this.hasteLevel = level; }

    public boolean hasHealPool() { return healPool; }
    public void setHealPool(boolean enabled) { this.healPool = enabled; }

    public boolean hasDragonBuff() { return dragonBuff; }
    public void setDragonBuff(boolean enabled) { this.dragonBuff = enabled; }

    public int size() { return members.size(); }

    public int aliveSize() { return aliveMembers.size(); }
}
