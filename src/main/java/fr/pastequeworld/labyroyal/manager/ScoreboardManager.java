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
        Objective obj = board.registerNewObjective("labyroyal", "dummy",
                ChatColor.translateAlternateColorCodes('&', "&6&l\u2726 LabyRoyal \u2726"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(board);
        update(player);
    }

    public void update(Player player) {
        Scoreboard board = player.getScoreboard();
        Objective obj = board.getObjective("labyroyal");
        if (obj == null) return;

        // Clear old entries
        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }

        GameState state = game.getState();
        PlayerData data = game.getPlayers().get(player.getUniqueId());

        int line = 15;

        setScore(obj, "&8&m                    ", line--);
        setScore(obj, "&fMode: &e" + game.getGameMode().getDisplayName(), line--);
        setScore(obj, "", line--);

        switch (state) {
            case WAITING:
                setScore(obj, "&fJoueurs: &a" + game.getPlayers().size() + "&7/&a" + game.getMaxPlayers(), line--);
                setScore(obj, "&fMin: &e" + game.getMinPlayers() + " joueurs", line--);
                setScore(obj, " ", line--);
                setScore(obj, "&7En attente...", line--);
                break;

            case STARTING:
                setScore(obj, "&fJoueurs: &a" + game.getPlayers().size() + "&7/&a" + game.getMaxPlayers(), line--);
                setScore(obj, " ", line--);
                setScore(obj, "&fDepart dans: &c" + game.getCountdown() + "s", line--);
                break;

            case PREPARATION:
                setScore(obj, "&fPhase: &6Preparation", line--);
                setScore(obj, "&fTemps: &e" + MessageUtil.formatTime(game.getPhaseTimer()), line--);
                setScore(obj, " ", line--);
                setScore(obj, "&fJoueurs en vie: &a" + game.getAliveCount(), line--);
                if (data != null) {
                    setScore(obj, "&fKills: &c" + data.getKills(), line--);
                }
                setScore(obj, "  ", line--);
                setScore(obj, "&7\u2794 Minez et preparez-vous !", line--);
                break;

            case PVP:
                setScore(obj, "&fPhase: &c\u2694 Combat", line--);
                setScore(obj, "&fTemps: &e" + MessageUtil.formatTime(game.getPhaseTimer()), line--);
                setScore(obj, " ", line--);
                setScore(obj, "&fJoueurs en vie: &a" + game.getAliveCount(), line--);
                if (data != null) {
                    setScore(obj, "&fKills: &c" + data.getKills(), line--);
                }
                setScore(obj, "  ", line--);
                if (game.isStormStarted()) {
                    setScore(obj, "&5\u26a1 Tempete: &dActive", line--);
                } else {
                    setScore(obj, "&5\u26a1 Tempete: &7Bientot...", line--);
                }
                break;

            case ENDING:
                setScore(obj, "&6&lFIN DE PARTIE", line--);
                if (data != null) {
                    setScore(obj, " ", line--);
                    setScore(obj, "&fVos kills: &c" + data.getKills(), line--);
                }
                break;
        }

        line--;
        setScore(obj, "&8&m                     ", line--);
        setScore(obj, "&epastequeworld", line);
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
        String colored = ChatColor.translateAlternateColorCodes('&', text);
        // Ensure uniqueness by padding with invisible chars if needed
        while (obj.getScoreboard().getEntries().contains(colored)) {
            colored += ChatColor.RESET;
        }
        if (colored.length() > 40) {
            colored = colored.substring(0, 40);
        }
        obj.getScore(colored).setScore(score);
    }
}
