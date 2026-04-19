package fr.pastequeworld.bedwars.player;

import fr.pastequeworld.bedwars.game.Arena;
import fr.pastequeworld.bedwars.team.Team;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Donnees de session d'un joueur. Recree a chaque connexion,
 * et reset entre chaque partie (sauf stats agregees).
 */
public class BedWarsPlayer {

    private final UUID uuid;
    private final String name;

    private PlayerState state = PlayerState.LOBBY;
    private Arena arena;
    private Team team;

    private int pickaxeLevel;   // 0 = aucune, 1 = bois, 2 = pierre, 3 = fer, 4 = diam
    private int axeLevel;

    private boolean magicMilk;  // true = ne declenche pas les pieges ennemis
    private long magicMilkExpires;

    // Stats session (agregees sur la partie en cours)
    private int kills;
    private int finalKills;
    private int deaths;
    private int bedsBroken;
    private boolean winner;

    public BedWarsPlayer(Player player) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }

    public PlayerState getState() { return state; }
    public void setState(PlayerState state) { this.state = state; }

    public Arena getArena() { return arena; }
    public void setArena(Arena arena) { this.arena = arena; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    public int getPickaxeLevel() { return pickaxeLevel; }
    public void setPickaxeLevel(int level) { this.pickaxeLevel = level; }

    public int getAxeLevel() { return axeLevel; }
    public void setAxeLevel(int level) { this.axeLevel = level; }

    public boolean hasMagicMilk() {
        return magicMilk && System.currentTimeMillis() < magicMilkExpires;
    }

    public void setMagicMilk(int seconds) {
        this.magicMilk = true;
        this.magicMilkExpires = System.currentTimeMillis() + seconds * 1000L;
    }

    public void clearMagicMilk() {
        this.magicMilk = false;
        this.magicMilkExpires = 0;
    }

    public int getKills() { return kills; }
    public void incrementKills() { this.kills++; }

    public int getFinalKills() { return finalKills; }
    public void incrementFinalKills() { this.finalKills++; }

    public int getDeaths() { return deaths; }
    public void incrementDeaths() { this.deaths++; }

    public int getBedsBroken() { return bedsBroken; }
    public void incrementBedsBroken() { this.bedsBroken++; }

    public boolean isWinner() { return winner; }
    public void setWinner(boolean winner) { this.winner = winner; }

    public boolean isPlayingOrRespawning() {
        return state == PlayerState.PLAYING || state == PlayerState.RESPAWNING;
    }

    public boolean isSpectator() { return state == PlayerState.SPECTATING; }

    public boolean isInArena() { return arena != null; }

    public void resetGameSession() {
        this.arena = null;
        this.team = null;
        this.pickaxeLevel = 0;
        this.axeLevel = 0;
        this.magicMilk = false;
        this.magicMilkExpires = 0;
        this.kills = 0;
        this.finalKills = 0;
        this.deaths = 0;
        this.bedsBroken = 0;
        this.winner = false;
        this.state = PlayerState.LOBBY;
    }
}
