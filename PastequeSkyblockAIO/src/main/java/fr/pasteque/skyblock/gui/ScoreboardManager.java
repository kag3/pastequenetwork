package fr.pasteque.skyblock.gui;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.arena.model.ArenaPlayerData;
import fr.pasteque.skyblock.combatpass.model.PlayerPassData;
import fr.pasteque.skyblock.model.Island;
import fr.pasteque.skyblock.pet.model.PlayerPet;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.UUID;

public class ScoreboardManager {

    private final PastequeSkyblockPlugin plugin;
    private final HashMap<UUID, Scoreboard> playerBoards = new HashMap<UUID, Scoreboard>();

    // 15 unique invisible color code strings (one per scoreboard line)
    private static final String[] ENTRIES = {
        ChatColor.BLACK.toString(),                                    // 0
        ChatColor.DARK_BLUE.toString(),                                // 1
        ChatColor.DARK_GREEN.toString(),                               // 2
        ChatColor.DARK_AQUA.toString(),                                // 3
        ChatColor.DARK_RED.toString(),                                 // 4
        ChatColor.DARK_PURPLE.toString(),                              // 5
        ChatColor.GOLD.toString(),                                     // 6
        ChatColor.GRAY.toString(),                                     // 7
        ChatColor.DARK_GRAY.toString(),                                // 8
        ChatColor.BLUE.toString(),                                     // 9
        ChatColor.GREEN.toString(),                                    // 10
        ChatColor.AQUA.toString(),                                     // 11
        ChatColor.RED.toString(),                                      // 12
        ChatColor.LIGHT_PURPLE.toString(),                             // 13
        ChatColor.YELLOW.toString()                                    // 14
    };

    public ScoreboardManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    public void createScoreboard(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("sidebar", "dummy");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.setDisplayName(PastequeSkyblockPlugin.color("&2&lPasteque &5&lSkyblock"));

        for (int i = 0; i < ENTRIES.length; i++) {
            Team team = board.registerNewTeam("line_" + i);
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
        // --- Gather data ---
        double money = plugin.getEconomyManager().getBalance(player.getUniqueId());
        String moneyStr = plugin.getEconomyManager().format(money);

        Island island = plugin.getIslandManager().getIslandByPlayer(player.getUniqueId());
        int islandLevel = island != null ? plugin.getIslandManager().getLevel(island) : 0;

        int kills = 0;
        int arenaLevel = 0;
        try {
            ArenaPlayerData arenaData = plugin.getPlayerDataService().get(player);
            if (arenaData != null) {
                kills = arenaData.getKills();
                arenaLevel = arenaData.getLevel();
            }
        } catch (Exception ignored) {
        }

        // Combat pass tier
        int passTier = 0;
        try {
            if (plugin.getCombatPassManager() != null) {
                PlayerPassData passData = plugin.getCombatPassManager().getData(player.getUniqueId());
                if (passData != null) {
                    passTier = passData.getCurrentTier();
                }
            }
        } catch (Exception ignored) {
        }

        // Pet name
        String petName = "Aucun";
        try {
            if (plugin.getPetManager() != null) {
                PlayerPet activePet = plugin.getPetManager().getActivePet(player.getUniqueId());
                if (activePet != null) {
                    petName = activePet.getType().getDisplayName();
                }
            }
        } catch (Exception ignored) {
        }

        // --- Set lines (score 0 = bottom, score 14 = top) ---

        // Score 14: blank
        setLine(board, 14, "");
        // Score 13: Profil header
        setLine(board, 13, PastequeSkyblockPlugin.color("&8\u258E &7Profil"));
        // Score 12: Argent
        setLine(board, 12, PastequeSkyblockPlugin.color("&8  \u2726 &7Argent: &a$" + moneyStr));
        // Score 11: Niveau
        setLine(board, 11, PastequeSkyblockPlugin.color("&8  \u2726 &7Niveau: &b" + islandLevel));
        // Score 10: blank
        setLine(board, 10, "");
        // Score 9: Combat header
        setLine(board, 9, PastequeSkyblockPlugin.color("&8\u258E &7Combat"));
        // Score 8: Kills
        setLine(board, 8, PastequeSkyblockPlugin.color("&8  \u2694 &7Kills: &c" + kills));
        // Score 7: Arene
        setLine(board, 7, PastequeSkyblockPlugin.color("&8  \u2694 &7Arene: &d" + arenaLevel));
        // Score 6: blank
        setLine(board, 6, "");
        // Score 5: Progression header
        setLine(board, 5, PastequeSkyblockPlugin.color("&8\u258E &7Progression"));
        // Score 4: Passe
        setLine(board, 4, PastequeSkyblockPlugin.color("&8  \u2605 &7Passe: &dTier " + passTier));
        // Score 3: Pet
        setLine(board, 3, PastequeSkyblockPlugin.color("&8  \u2605 &7Pet: &e" + petName));
        // Score 2: blank
        setLine(board, 2, "");
        // Score 1: Server IP
        setLine(board, 1, PastequeSkyblockPlugin.color("&2play&8.&5pasteque&8.&2world"));
        // Score 0: blank
        setLine(board, 0, "");
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
            // Carry over last color codes into suffix so colors don't break
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
