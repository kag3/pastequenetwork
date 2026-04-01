package fr.pastequeworld.labyroyal.manager;

import fr.pastequeworld.labyroyal.game.Game;
import fr.pastequeworld.labyroyal.game.GameState;
import fr.pastequeworld.labyroyal.game.PlayerData;
import fr.pastequeworld.labyroyal.util.MessageUtil;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Collection;

public class ScoreboardManager {

    private final Game game;

    public ScoreboardManager(Game game) {
        this.game = game;
    }

    public void setup(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("labyroyale", "dummy");
        obj.setDisplayName(color("&2&lpasteque&7.&d&lworld"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(board);
        update(player);
    }

    public void update(Player player) {
        Scoreboard board = player.getScoreboard();
        Objective obj = board.getObjective("labyroyale");
        if (obj == null) return;

        // Clear old entries
        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }

        GameState state = game.getState();
        PlayerData data = game.getPlayers().get(player.getUniqueId());
        String playerName = (data != null) ? data.getName() : player.getName();

        int line = 10;

        setScore(obj, "&8&m                    ", line--);

        // Joueur
        setScore(obj, "&7Joueur: &f" + playerName, line--);

        setScore(obj, " ", line--);

        // Serveur
        setScore(obj, "&7Serveur: &fLabyRoyale&2#" + game.getId(), line--);

        setScore(obj, "  ", line--);

        // Phase
        switch (state) {
            case WAITING:
                setScore(obj, "&7Phase: &eAttente", line--);
                break;
            case STARTING:
                setScore(obj, "&7Phase: &eDemarrage &f(" + game.getCountdown() + "s)", line--);
                break;
            case PREPARATION:
                setScore(obj, "&7Phase: &2Preparation", line--);
                break;
            case PVP:
                setScore(obj, "&7Phase: &cCombat", line--);
                break;
            case ENDING:
                setScore(obj, "&7Phase: &6Fin", line--);
                break;
        }

        setScore(obj, "   ", line--);

        // Temps (elapsed timer counting up)
        setScore(obj, "&7Temps: &f" + MessageUtil.formatTime(game.getElapsedTime()), line--);

        setScore(obj, "    ", line--);

        // Joueurs restants
        int alive = game.getAliveCount();
        int total = game.getMaxPlayers();
        if (state == GameState.WAITING || state == GameState.STARTING) {
            setScore(obj, "&7Joueurs: &2" + game.getPlayers().size() + "&7/&2" + total, line--);
        } else {
            setScore(obj, "&7Joueurs: &2" + alive + "&7/&2" + total + " restants", line--);
        }

        setScore(obj, "&8&m                     ", line);
    }

    public void updateAll(Collection<? extends Player> players) {
        for (Player player : players) {
            update(player);
        }
    }

    public void remove(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    private void setScore(Objective obj, String text, int score) {
        String colored = color(text);
        // Ensure uniqueness
        while (obj.getScoreboard().getEntries().contains(colored)) {
            colored += ChatColor.RESET;
        }
        if (colored.length() > 40) {
            colored = colored.substring(0, 40);
        }
        obj.getScore(colored).setScore(score);
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
