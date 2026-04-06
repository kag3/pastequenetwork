package fr.pasteque.skyblock.dungeon;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DungeonInstance {

    public enum DungeonState {
        WAITING,
        ACTIVE,
        BOSS,
        COMPLETED,
        FAILED
    }

    private final String instanceId;
    private final DungeonType type;
    private final Set<UUID> players;
    private final UUID leader;
    private final Location origin;
    private final int slotIndex;

    private DungeonState state;
    private long startedAt;
    private int timerSeconds;
    private int currentWave;
    private int mobsRemaining;
    private int totalKills;

    /** Tracks remaining lives per player (each player starts with 3). */
    private final Map<UUID, Integer> lives;

    /** Players who have been fully eliminated (0 lives). */
    private final Set<UUID> eliminated;

    public DungeonInstance(String instanceId, DungeonType type, Set<UUID> players,
                           UUID leader, Location origin, int slotIndex) {
        this.instanceId = instanceId;
        this.type = type;
        this.players = new HashSet<UUID>(players);
        this.leader = leader;
        this.origin = origin;
        this.slotIndex = slotIndex;
        this.state = DungeonState.WAITING;
        this.startedAt = System.currentTimeMillis();
        this.timerSeconds = type.getTimerMinutes() * 60;
        this.currentWave = 0;
        this.mobsRemaining = 0;
        this.totalKills = 0;
        this.lives = new HashMap<UUID, Integer>();
        this.eliminated = new HashSet<UUID>();
        for (UUID uuid : players) {
            lives.put(uuid, 3);
        }
    }

    // =========================================================================
    //  Getters
    // =========================================================================

    public String getInstanceId() {
        return instanceId;
    }

    public DungeonType getType() {
        return type;
    }

    public Set<UUID> getPlayers() {
        return players;
    }

    public UUID getLeader() {
        return leader;
    }

    public Location getOrigin() {
        return origin;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public DungeonState getState() {
        return state;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public int getTimerSeconds() {
        return timerSeconds;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public int getMobsRemaining() {
        return mobsRemaining;
    }

    public int getTotalKills() {
        return totalKills;
    }

    public Map<UUID, Integer> getLives() {
        return lives;
    }

    public Set<UUID> getEliminated() {
        return eliminated;
    }

    // =========================================================================
    //  Setters
    // =========================================================================

    public void setState(DungeonState state) {
        this.state = state;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public void setCurrentWave(int currentWave) {
        this.currentWave = currentWave;
    }

    public void setMobsRemaining(int mobsRemaining) {
        this.mobsRemaining = mobsRemaining;
    }

    public void incrementTotalKills() {
        this.totalKills++;
    }

    // =========================================================================
    //  Logic
    // =========================================================================

    /**
     * Returns true if the dungeon timer has expired.
     */
    public boolean isExpired() {
        long elapsed = (System.currentTimeMillis() - startedAt) / 1000L;
        return elapsed >= timerSeconds;
    }

    /**
     * Returns the number of seconds remaining before the timer expires.
     */
    public int getRemainingSeconds() {
        long elapsed = (System.currentTimeMillis() - startedAt) / 1000L;
        int remaining = (int) (timerSeconds - elapsed);
        return remaining < 0 ? 0 : remaining;
    }

    /**
     * Consume one life for a player. Returns remaining lives (0 = eliminated).
     */
    public int consumeLife(UUID uuid) {
        Integer current = lives.get(uuid);
        if (current == null || current <= 0) {
            eliminated.add(uuid);
            return 0;
        }
        int remaining = current - 1;
        lives.put(uuid, remaining);
        if (remaining <= 0) {
            eliminated.add(uuid);
        }
        return remaining;
    }

    /**
     * Returns true if all non-eliminated players have left.
     */
    public boolean hasNoActivePlayers() {
        for (UUID uuid : players) {
            if (!eliminated.contains(uuid)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the spawn point for players inside this dungeon (entrance area).
     */
    public Location getSpawnPoint() {
        return origin.clone().add(15, 1, 2);
    }

    /**
     * Get the location for wave mob spawns based on wave number.
     * Rooms are laid out along the Z axis: room1 z+8, room2 z+16, room3 z+24, boss z+28.
     */
    public Location getWaveSpawnLocation(int wave) {
        if (wave <= 1) {
            return origin.clone().add(15, 1, 8);
        } else if (wave == 2) {
            return origin.clone().add(15, 1, 16);
        } else if (wave == 3) {
            return origin.clone().add(15, 1, 24);
        } else {
            // Boss room
            return origin.clone().add(15, 1, 28);
        }
    }
}
