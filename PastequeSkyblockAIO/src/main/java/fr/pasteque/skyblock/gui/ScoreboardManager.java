package fr.pasteque.skyblock.gui;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.arena.model.ArenaPlayerData;
import fr.pasteque.skyblock.model.Island;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import org.bukkit.OfflinePlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {

    private final PastequeSkyblockPlugin plugin;
    private final HashMap<UUID, Scoreboard> playerBoards = new HashMap<UUID, Scoreboard>();

    // Invisible entries used as unique team members (one per line)
    private static final String[] ENTRIES = {
        ChatColor.BLACK.toString(),
        ChatColor.DARK_BLUE.toString(),
        ChatColor.DARK_GREEN.toString(),
        ChatColor.DARK_AQUA.toString(),
        ChatColor.DARK_RED.toString(),
        ChatColor.DARK_PURPLE.toString(),
        ChatColor.GOLD.toString(),
        ChatColor.GRAY.toString(),
        ChatColor.DARK_GRAY.toString(),
        ChatColor.BLUE.toString(),
        ChatColor.GREEN.toString()
    };

    public ScoreboardManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public void createScoreboard(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("sidebar", "dummy");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.setDisplayName(PastequeSkyblockPlugin.color("&2&lPasteque &a&lSkyblock"));

        // Lines from bottom (score 0) to top (score 10)
        // Score 0:  "" (blank)
        // Score 1:  "&fArgent: &a%money%"
        // Score 2:  "&fNiveau ile: &b%island_level%"
        // Score 3:  "" (blank)
        // Score 4:  "&fKills: &c%kills%"
        // Score 5:  "&fNiveau arene: &d%arena_level%"
        // Score 6:  "" (blank)
        // Score 7:  "&fPasse: &dTier %pass_tier%"
        // Score 8:  "&fPet: &e%pet_name%"
        // Score 9:  "" (blank)
        // Score 10: "&epastequenetwork.fr"

        for (int i = 0; i < ENTRIES.length; i++) {
            Team team = board.registerNewTeam("line_" + i);
            @SuppressWarnings("deprecation")
            OfflinePlayer fakePlayer = Bukkit.getOfflinePlayer(ENTRIES[i]);
            team.addPlayer(fakePlayer);
            obj.getScore(fakePlayer).setScore(i);
        }

        playerBoards.put(player.getUniqueId(), board);
        updateLines(player, board);
        player.setScoreboard(board);
    }

    public void updateScoreboard(Player player) {
        Scoreboard board = playerBoards.get(player.getUniqueId());
        if (board == null) {
            return;
        }
        updateLines(player, board);
    }

    private void updateLines(Player player, Scoreboard board) {
        // Gather data
        double money = plugin.getEconomyManager().getBalance(player.getUniqueId());
        String moneyStr = plugin.getEconomyManager().format(money);

        Island island = plugin.getIslandManager().getIslandByPlayer(player.getUniqueId());
        int islandLevel = island != null ? plugin.getIslandManager().getLevel(island) : 0;

        ArenaPlayerData arenaData = plugin.getPlayerDataService().get(player);
        int kills = arenaData.getKills();
        int arenaLevel = arenaData.getLevel();

        // Combat pass tier - try to get from config/manager
        int passTier = 0;
        try {
            passTier = arenaData.getLevel(); // fallback
        } catch (Exception ignored) {
        }

        // Pet name
        String petName = "Aucun";

        // Set team prefixes (lines bottom to top: score 0..10)
        setLine(board, 0, "");                                                       // blank
        setLine(board, 1, PastequeSkyblockPlugin.color("&fArgent: &a" + moneyStr));
        setLine(board, 2, PastequeSkyblockPlugin.color("&fNiveau ile: &b" + islandLevel));
        setLine(board, 3, "");                                                       // blank
        setLine(board, 4, PastequeSkyblockPlugin.color("&fKills: &c" + kills));
        setLine(board, 5, PastequeSkyblockPlugin.color("&fNiveau arene: &d" + arenaLevel));
        setLine(board, 6, "");                                                       // blank
        setLine(board, 7, PastequeSkyblockPlugin.color("&fPasse: &dTier " + passTier));
        setLine(board, 8, PastequeSkyblockPlugin.color("&fPet: &e" + petName));
        setLine(board, 9, "");                                                       // blank
        setLine(board, 10, PastequeSkyblockPlugin.color("&epastequenetwork.fr"));
    }

    private void setLine(Scoreboard board, int score, String text) {
        Team team = board.getTeam("line_" + score);
        if (team == null) {
            return;
        }
        // Split text into prefix (max 16) and suffix (max 16)
        String prefix;
        String suffix = "";
        if (text.length() <= 16) {
            prefix = text;
        } else {
            prefix = text.substring(0, 16);
            // Carry over color codes to suffix
            String lastColors = ChatColor.getLastColors(prefix);
            suffix = lastColors + text.substring(16);
            if (suffix.length() > 16) {
                suffix = suffix.substring(0, 16);
            }
        }
        team.setPrefix(prefix);
        team.setSuffix(suffix);
    }

    public void removeScoreboard(Player player) {
        playerBoards.remove(player.getUniqueId());
    }

    public void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (playerBoards.containsKey(player.getUniqueId())) {
                updateScoreboard(player);
            }
        }
    }
}
