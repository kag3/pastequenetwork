package fr.pastequeworld.bedwars.ui;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.game.Arena;
import fr.pastequeworld.bedwars.game.GameState;
import fr.pastequeworld.bedwars.player.BedWarsPlayer;
import fr.pastequeworld.bedwars.team.Team;
import fr.pastequeworld.bedwars.util.ColorUtil;
import fr.pastequeworld.bedwars.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scoreboard Hypixel-style.
 *
 * Lobby :
 *   BEDWARS
 *   -------
 *   pasteque.world
 *
 *   Coins: <amount>
 *   Grade: <rank>
 *
 *   Mode: <Solo/Duo/Teams>
 *
 *   19/04/26   [SRV01]
 *   -------
 *
 * In-Game :
 *   BEDWARS                  (titre)
 *   ---
 *   19/04/26   [BW-123]
 *   <ServerName>
 *
 *   Prochain event: <name>
 *   dans XX:XX
 *
 *   <team_icon> Votre equipe
 *   <team> <status> <alive>
 *
 *   Kills: <x>
 *   Final Kills: <x>
 *   Beds: <x>
 *
 *   pasteque.world
 */
public class ScoreboardManager {

    private final BedWarsPlugin plugin;
    private final ConcurrentHashMap<UUID, Scoreboard> boards = new ConcurrentHashMap<UUID, Scoreboard>();
    private final ConcurrentHashMap<String, String> arenaCodes = new ConcurrentHashMap<String, String>();

    private int refreshTaskId = -1;

    public ScoreboardManager(BedWarsPlugin plugin) {
        this.plugin = plugin;
        this.refreshTaskId = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                tickAll();
            }
        }, 20L, 20L).getTaskId();
    }

    public void stop() {
        if (refreshTaskId != -1) Bukkit.getScheduler().cancelTask(refreshTaskId);
    }

    private void tickAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            BedWarsPlayer bw = plugin.getPlayerDataManager().get(p);
            if (bw == null || bw.getArena() == null) {
                showLobby(p);
            }
        }
        for (Arena arena : plugin.getArenaManager().getArenas()) refresh(arena);
        plugin.getNpcManager().refreshHologramCounts();
    }

    public void showLobby(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("bwlobby", "dummy");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.setDisplayName(ColorUtil.color("&e&lBEDWARS"));

        List<String> lines = new ArrayList<String>();
        lines.add("&7" + dateLabel() + "    &8" + "LOBBY");
        lines.add("");
        lines.add("&fModes:");
        lines.add("  &aSolo &7- 4x1");
        lines.add("  &bDuo &7- 4x2");
        lines.add("  &cTeams &7- 4x4");
        lines.add("");
        lines.add("&fJoueurs en ligne: &a" + Bukkit.getOnlinePlayers().size());
        lines.add("");
        lines.add("&e" + plugin.getConfigManager().getServerName());

        applyLines(board, obj, lines);
        player.setScoreboard(board);
        boards.put(player.getUniqueId(), board);
    }

    public void refresh(Arena arena) {
        GameState state = arena.getState();
        List<String> lines = new ArrayList<String>();

        if (state == GameState.WAITING || state == GameState.STARTING) {
            buildWaitingLines(arena, lines);
        } else if (state == GameState.RUNNING) {
            buildGameLines(arena, lines);
        } else if (state == GameState.ENDING) {
            buildEndingLines(arena, lines);
        } else {
            return;
        }

        for (Player p : arena.onlinePlayers()) {
            Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective obj = board.registerNewObjective("bwgame", "dummy");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            obj.setDisplayName(ColorUtil.color("&e&lBEDWARS"));
            applyLines(board, obj, new ArrayList<String>(lines));
            p.setScoreboard(board);
            boards.put(p.getUniqueId(), board);
        }
    }

    private void buildWaitingLines(Arena arena, List<String> lines) {
        lines.add("&7" + dateLabel() + "    &8" + codeFor(arena));
        lines.add("");
        lines.add("&fMap: &a" + arena.getTemplate().getDisplayName());
        lines.add("&fMode: &a" + arena.getMode().name());
        lines.add("");
        lines.add("&fJoueurs: &a" + arena.getPlayers().size() + "/" + plugin.getConfigManager().getMode(arena.getMode()).getMaxPlayers());
        if (arena.getState() == GameState.STARTING) {
            lines.add("");
            lines.add("&aDemarre dans: &f" + arena.getStartCountdown() + "s");
        } else {
            lines.add("");
            lines.add("&7En attente...");
        }
        lines.add("");
        lines.add("&e" + plugin.getConfigManager().getServerName());
    }

    private void buildGameLines(Arena arena, List<String> lines) {
        EventsTimelineManager.NextEventInfo next = plugin.getArenaManager()
                .getEventsTimelineManager().getNextEvent(arena);
        lines.add("&7" + dateLabel() + "    &8" + codeFor(arena));
        lines.add("");
        lines.add("&aProchain event &7(&f" + TimeUtil.format(next.secondsRemaining) + "&7)");
        lines.add("&7 " + next.name);
        lines.add("");
        for (Team team : arena.getTeams()) {
            String status;
            if (team.isEliminated()) {
                status = "&c\u2717";
            } else if (!team.isBedAlive()) {
                status = "&c" + team.aliveSize();
            } else {
                status = "&a\u2713";
            }
            String you = "";
            lines.add(team.getColor().getChatColor() + team.getColor().getPrefix().toUpperCase()
                    + " &f" + team.getColor().getFrenchName() + " " + status + " " + you);
        }
        lines.add("");
        lines.add("&e" + plugin.getConfigManager().getServerName());
    }

    private void buildEndingLines(Arena arena, List<String> lines) {
        lines.add("&7" + dateLabel() + "    &8" + codeFor(arena));
        lines.add("");
        lines.add("&6&lPARTIE TERMINEE !");
        lines.add("");
        lines.add("&7Merci d'avoir joue !");
        lines.add("");
        lines.add("&e" + plugin.getConfigManager().getServerName());
    }

    /**
     * Applique des lignes sur le scoreboard avec des colors codes uniques pour eviter les collisions
     * (Bukkit ne permet pas deux entry identiques dans un objective).
     */
    private void applyLines(Scoreboard board, Objective obj, List<String> lines) {
        int score = lines.size();
        int i = 0;
        for (String line : lines) {
            String key = uniquify(line, i++);
            obj.getScore(key).setScore(score--);
        }
    }

    private String uniquify(String line, int idx) {
        String colored = ColorUtil.color(line);
        if (colored.length() == 0) colored = " ";
        // Ajoute une combinaison invisible de color codes pour forcer l'unicite
        String unique = ChatColor.RESET + "";
        for (int k = 0; k <= idx; k++) unique += ChatColor.RESET + "";
        String combined = unique + colored;
        if (combined.length() > 40) combined = combined.substring(0, 40);
        return combined;
    }

    private String dateLabel() {
        return new SimpleDateFormat("dd/MM/yy").format(new Date());
    }

    private String codeFor(Arena arena) {
        String code = arenaCodes.get(arena.getId());
        if (code == null) {
            Random r = new Random();
            String alphanum = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
            StringBuilder sb = new StringBuilder(5);
            for (int i = 0; i < 5; i++) sb.append(alphanum.charAt(r.nextInt(alphanum.length())));
            code = sb.toString();
            arenaCodes.put(arena.getId(), code);
        }
        return code;
    }
}
