package fr.pastequeworld.labyroyal.manager;

import fr.pastequeworld.labyroyal.game.Game;
import fr.pastequeworld.labyroyal.game.GameState;
import fr.pastequeworld.labyroyal.util.MessageUtil;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

public class BossBarManager {

    private final Game game;
    private final BossBar bossBar;

    public BossBarManager(Game game) {
        this.game = game;
        this.bossBar = Bukkit.createBossBar(
                MessageUtil.color("&eEn attente de joueurs..."),
                BarColor.YELLOW,
                BarStyle.SOLID
        );
        this.bossBar.setProgress(1.0);
    }

    public void addPlayer(Player player) {
        bossBar.addPlayer(player);
    }

    public void removePlayer(Player player) {
        bossBar.removePlayer(player);
    }

    public void update(int phaseTimer, int phaseDuration) {
        GameState state = game.getState();

        switch (state) {
            case WAITING:
                bossBar.setTitle(MessageUtil.color("&e\u231A En attente de joueurs... &7(" + game.getPlayers().size() + "/" + game.getMaxPlayers() + ")"));
                bossBar.setColor(BarColor.YELLOW);
                bossBar.setProgress(1.0);
                break;

            case STARTING:
                int countdown = game.getCountdown();
                bossBar.setTitle(MessageUtil.color("&6\u26a0 D\u00e9marrage dans &f" + countdown + "s"));
                bossBar.setColor(BarColor.YELLOW);
                bossBar.setProgress(Math.max(0.0, Math.min(1.0, (double) countdown / phaseDuration)));
                break;

            case PREPARATION:
                bossBar.setTitle(MessageUtil.color("&2\u2694 Pr\u00e9paration &7- &f" + MessageUtil.formatTime(phaseTimer) + " &7restantes"));
                bossBar.setColor(BarColor.GREEN);
                bossBar.setProgress(Math.max(0.0, Math.min(1.0, (double) phaseTimer / phaseDuration)));
                break;

            case PVP:
                if (game.isStormStarted()) {
                    bossBar.setTitle(MessageUtil.color("&5\u26a1 Temp\u00eate active &7- &c\u2694 Combat &7- &f" + MessageUtil.formatTime(phaseTimer)));
                    bossBar.setColor(BarColor.PURPLE);
                } else {
                    bossBar.setTitle(MessageUtil.color("&c\u2694 Phase de Combat &7- &f" + MessageUtil.formatTime(phaseTimer) + " &7restantes"));
                    bossBar.setColor(BarColor.RED);
                }
                bossBar.setProgress(Math.max(0.0, Math.min(1.0, (double) phaseTimer / phaseDuration)));
                break;

            case ENDING:
                bossBar.setTitle(MessageUtil.color("&6\u2726 Fin de la partie \u2726"));
                bossBar.setColor(BarColor.YELLOW);
                bossBar.setProgress(0.0);
                break;
        }
    }

    public void cleanup() {
        bossBar.removeAll();
    }
}
