package fr.pasteque.skyblock.gui;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.arena.model.ArenaPlayerData;
import fr.pasteque.skyblock.combatpass.model.PlayerPassData;
import fr.pasteque.skyblock.model.Island;
import fr.pasteque.skyblock.pet.model.PlayerPet;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

/**
 * Scoreboard dynamique contextuel — le contenu change selon le monde
 * dans lequel se trouve le joueur:
 *
 * - Spawn/Overworld : Profil general (argent, ile, quetes, pet)
 * - Ile Skyblock    : Stats d'ile (niveau, membres en ligne, upgrades)
 * - Arene PvP       : Stats de combat (kills, ELO, killstreak, kit)
 * - KOTH            : KOTH en cours (temps restant, position, leader)
 * - Donjons         : Donjon en cours (vague, mobs restants, timer)
 */
@SuppressWarnings("deprecation")
public class ScoreboardManager {

    private final PastequeSkyblockPlugin plugin;
    private final HashMap<UUID, Scoreboard> playerBoards = new HashMap<UUID, Scoreboard>();
    private final HashMap<UUID, String> lastContext = new HashMap<UUID, String>();
    private long tickCount = 0;

    // 15 unique invisible color code strings (one per scoreboard line)
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
        ChatColor.GREEN.toString(),
        ChatColor.AQUA.toString(),
        ChatColor.RED.toString(),
        ChatColor.LIGHT_PURPLE.toString(),
        ChatColor.YELLOW.toString()
    };

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE);

    public ScoreboardManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

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

    private String detectContext(Player player) {
        World w = player.getWorld();
        String worldName = w.getName().toLowerCase();

        if (worldName.contains("arena") || worldName.contains("pvp")) {
            return "arena";
        }
        if (worldName.contains("koth")) {
            return "koth";
        }
        if (worldName.contains("dungeon")) {
            return "dungeon";
        }
        if (worldName.contains("island") || worldName.contains("skyblock")) {
            // Check if player is on their own island or visiting
            Island island = plugin.getIslandManager().getIslandByPlayer(player.getUniqueId());
            if (island != null) {
                return "island";
            }
            return "island_visit";
        }
        if (worldName.contains("coop")) {
            return "island";
        }
        // Default: spawn/hub/overworld
        return "spawn";
    }

    private void updateLines(Player player, Scoreboard board) {
        String context = detectContext(player);

        // Animated title: cycle colors every 2 ticks (6 seconds at 3s refresh)
        Objective obj = board.getObjective("sidebar");
        String[] titleFrames;
        switch (context) {
            case "arena":
                titleFrames = new String[]{"&c&lPasteque &4&lArene", "&4&lPasteque &c&lArene", "&c&l\u2694 &4&lArene PvP &c&l\u2694"};
                break;
            case "koth":
                titleFrames = new String[]{"&c&l\u265b &4&lKOTH &c&l\u265b", "&4&l\u265b &c&lKOTH &4&l\u265b", "&e&l\u265b &6&lKOTH &e&l\u265b"};
                break;
            case "dungeon":
                titleFrames = new String[]{"&5&lPasteque &d&lDonjon", "&d&lPasteque &5&lDonjon", "&5&l\u2620 &d&lDonjon &5&l\u2620"};
                break;
            case "island":
            case "island_visit":
                titleFrames = new String[]{"&a&lPasteque &2&lSkyblock", "&2&lPasteque &a&lSkyblock"};
                break;
            default:
                titleFrames = new String[]{"&2&lPasteque &5&lSkyblock", "&5&lPasteque &2&lSkyblock", "&a&lPasteque &d&lSkyblock"};
                break;
        }
        if (obj != null) {
            int frame = (int)(tickCount % titleFrames.length);
            obj.setDisplayName(PastequeSkyblockPlugin.color(titleFrames[frame]));
        }

        switch (context) {
            case "arena":
                updateArenaBoard(player, board);
                break;
            case "koth":
                updateKothBoard(player, board);
                break;
            case "dungeon":
                updateDungeonBoard(player, board);
                break;
            case "island":
            case "island_visit":
                updateIslandBoard(player, board, context);
                break;
            default:
                updateSpawnBoard(player, board);
                break;
        }

        lastContext.put(player.getUniqueId(), context);
    }

    // =========================================================================
    //  SPAWN / HUB scoreboard
    // =========================================================================
    private void updateSpawnBoard(Player player, Scoreboard board) {
        double money = plugin.getEconomyManager().getBalance(player.getUniqueId());
        String moneyStr = plugin.getEconomyManager().format(money);
        Island island = plugin.getIslandManager().getIslandByPlayer(player.getUniqueId());
        int islandLevel = island != null ? plugin.getIslandManager().getLevel(island) : 0;

        String petName = "Aucun";
        try {
            if (plugin.getPetManager() != null) {
                PlayerPet pet = plugin.getPetManager().getActivePet(player.getUniqueId());
                if (pet != null) petName = pet.getType().getDisplayName();
            }
        } catch (Exception ignored) {}

        int online = Bukkit.getOnlinePlayers().size();
        String date = DATE_FMT.format(new Date());

        // Active events
        String eventInfo = "&7Aucun";
        try {
            int activeEvents = plugin.getEventManager().getActiveCount();
            if (activeEvents > 0) {
                for (fr.pasteque.skyblock.serverevent.ServerEvent ev : plugin.getEventManager().getAll()) {
                    if (ev.isActive()) {
                        long rem = ev.getRemainingSeconds();
                        eventInfo = "&a" + ev.getDisplayName() + " &7(" + formatTime(rem) + ")";
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}

        setLine(board, 14, "");
        setLine(board, 13, c("&7" + date + " &8| &7" + online + " en ligne"));
        setLine(board, 12, "");
        setLine(board, 11, c("&8\u258E &f&lProfil"));
        setLine(board, 10, c("&8  \u2726 &7Pasteques: &a" + moneyStr));
        setLine(board, 9,  c("&8  \u2726 &7Ile: &bNiv." + islandLevel));
        setLine(board, 8,  c("&8  \u2726 &7Pet: &e" + petName));
        setLine(board, 7,  "");
        setLine(board, 6,  c("&8\u258E &f&lEvenement"));
        if (eventInfo.startsWith("&a")) {
            // Blink the event line between two colors
            String blinkEvent = (tickCount % 2 == 0) ? eventInfo : eventInfo.replace("&a", "&e");
            setLine(board, 5, c("&8  \u2694 " + blinkEvent));
        } else {
            setLine(board, 5, c("&8  \u2694 " + eventInfo));
        }
        setLine(board, 4,  "");
        setLine(board, 3,  c("&8\u258E &f&lServeur"));
        setLine(board, 2,  c("&8  \u2605 &7Joueurs: &f" + online));
        String ip = (tickCount % 3 == 0) ? "&2play&8.&5pasteque&8.&2world"
                  : (tickCount % 3 == 1) ? "&a play&8.&d pasteque&8.&a world"
                  : "&2\u2764 &5pasteque&8.&2world &5\u2764";
        setLine(board, 1,  c(ip));
        setLine(board, 0,  "");
    }

    // =========================================================================
    //  ISLAND scoreboard
    // =========================================================================
    private void updateIslandBoard(Player player, Scoreboard board, String ctx) {
        double money = plugin.getEconomyManager().getBalance(player.getUniqueId());
        String moneyStr = plugin.getEconomyManager().format(money);
        Island island = plugin.getIslandManager().getIslandByPlayer(player.getUniqueId());
        int islandLevel = island != null ? plugin.getIslandManager().getLevel(island) : 0;

        // Count online members on island world
        int membersOnline = 0;
        if (player.getWorld() != null) {
            for (Player p : player.getWorld().getPlayers()) {
                membersOnline++;
            }
        }

        // Minion count
        int minionCount = 0;
        try {
            if (plugin.getMinionManager() != null) {
                java.util.List<?> minions = plugin.getMinionManager().getMinions(player.getUniqueId());
                if (minions != null) minionCount = minions.size();
            }
        } catch (Exception ignored) {}

        String petName = "Aucun";
        try {
            if (plugin.getPetManager() != null) {
                PlayerPet pet = plugin.getPetManager().getActivePet(player.getUniqueId());
                if (pet != null) petName = pet.getType().getDisplayName();
            }
        } catch (Exception ignored) {}

        setLine(board, 14, "");
        setLine(board, 13, c("&8\u258E &a&lMon Ile"));
        setLine(board, 12, c("&8  \u2726 &7Niveau: &b" + islandLevel));
        setLine(board, 11, c("&8  \u2726 &7Joueurs ici: &f" + membersOnline));
        setLine(board, 10, c("&8  \u2726 &7Minions: &e" + minionCount));
        setLine(board, 9,  "");
        setLine(board, 8,  c("&8\u258E &a&lInventaire"));
        setLine(board, 7,  c("&8  \u2726 &7Pasteques: &a" + moneyStr));
        setLine(board, 6,  c("&8  \u2726 &7Pet: &e" + petName));
        setLine(board, 5,  "");
        setLine(board, 4,  c("&8\u258E &a&lAstuces"));
        setLine(board, 3,  c("&8  \u25B8 &7/is upgrade &8- Ameliorer"));
        setLine(board, 2,  c("&8  \u25B8 &7/menu &8- Menu principal"));
        String ip = (tickCount % 3 == 0) ? "&2play&8.&5pasteque&8.&2world"
                  : (tickCount % 3 == 1) ? "&a play&8.&d pasteque&8.&a world"
                  : "&2\u2764 &5pasteque&8.&2world &5\u2764";
        setLine(board, 1,  c(ip));
        setLine(board, 0,  "");
    }

    // =========================================================================
    //  ARENA PVP scoreboard
    // =========================================================================
    private void updateArenaBoard(Player player, Scoreboard board) {
        int kills = 0, deaths = 0, arenaLevel = 0, killStreak = 0;
        int elo = 1000;
        String kit = "Aucun";

        try {
            ArenaPlayerData data = plugin.getPlayerDataService().get(player);
            if (data != null) {
                kills = data.getKills();
                deaths = data.getDeaths();
                arenaLevel = data.getLevel();
            }
        } catch (Exception ignored) {}

        try {
            if (plugin.getEloService() != null) {
                elo = plugin.getEloService().getElo(player.getUniqueId());
            }
        } catch (Exception ignored) {}

        try {
            if (plugin.getKillStreakService() != null) {
                killStreak = plugin.getKillStreakService().getStreak(player.getUniqueId());
            }
        } catch (Exception ignored) {}

        double kd = deaths > 0 ? (double) kills / deaths : kills;
        String kdStr = String.format("%.2f", kd);

        setLine(board, 14, "");
        setLine(board, 13, c("&8\u258E &c&lCombat"));
        setLine(board, 12, c("&8  \u2694 &7Kills: &c" + kills));
        setLine(board, 11, c("&8  \u2694 &7Morts: &7" + deaths));
        setLine(board, 10, c("&8  \u2694 &7K/D: &e" + kdStr));
        setLine(board, 9,  "");
        setLine(board, 8,  c("&8\u258E &c&lClassement"));
        setLine(board, 7,  c("&8  \u2605 &7ELO: &6" + elo));
        setLine(board, 6,  c("&8  \u2605 &7Niveau: &d" + arenaLevel));
        setLine(board, 5,  c("&8  \u2605 &7Serie: &a" + killStreak + " kills"));
        setLine(board, 4,  "");
        setLine(board, 3,  c("&8\u258E &c&lAstuces"));
        setLine(board, 2,  c("&8  \u25B8 &7/arenakit &8- Kits"));
        String ip = (tickCount % 3 == 0) ? "&2play&8.&5pasteque&8.&2world"
                  : (tickCount % 3 == 1) ? "&a play&8.&d pasteque&8.&a world"
                  : "&2\u2764 &5pasteque&8.&2world &5\u2764";
        setLine(board, 1,  c(ip));
        setLine(board, 0,  "");
    }

    // =========================================================================
    //  KOTH scoreboard
    // =========================================================================
    private void updateKothBoard(Player player, Scoreboard board) {
        long remaining = 0;
        String leader = "Personne";
        boolean playerInZone = false;

        try {
            fr.pasteque.skyblock.serverevent.ServerEvent koth = plugin.getEventManager().get("koth");
            if (koth != null && koth.isActive()) {
                remaining = koth.getRemainingSeconds();
                // Try to get leader info
                if (koth instanceof fr.pasteque.skyblock.serverevent.KingOfTheHillEvent) {
                    fr.pasteque.skyblock.serverevent.KingOfTheHillEvent k =
                            (fr.pasteque.skyblock.serverevent.KingOfTheHillEvent) koth;
                    org.bukkit.Location zone = k.getZoneCenter();
                    if (zone != null && player.getWorld().equals(zone.getWorld())) {
                        double dx = player.getLocation().getX() - zone.getX();
                        double dz = player.getLocation().getZ() - zone.getZ();
                        playerInZone = Math.abs(dx) <= 8.5 && Math.abs(dz) <= 8.5;
                    }
                }
            }
        } catch (Exception ignored) {}

        int playersInWorld = player.getWorld().getPlayers().size();

        setLine(board, 14, "");
        setLine(board, 13, c("&8\u258E &4&lKing of the Hill"));
        setLine(board, 12, c("&8  \u23F1 &7Temps: &e" + formatTime(remaining)));
        setLine(board, 11, c("&8  \u2694 &7Joueurs: &c" + playersInWorld));
        setLine(board, 10, "");
        setLine(board, 9,  c("&8\u258E &4&lVotre statut"));
        String zoneText;
        if (playerInZone) {
            zoneText = (tickCount % 2 == 0) ? "&a&lOUI \u2605" : "&e&lOUI \u2605";
        } else {
            zoneText = "&c&lNON";
        }
        setLine(board, 8,  c("&8  \u25B8 &7Dans la zone: " + zoneText));
        setLine(board, 7,  "");
        setLine(board, 6,  c("&8\u258E &4&lRecompenses"));
        setLine(board, 5,  c("&8  &61er &7- 10000 Pasteques"));
        setLine(board, 4,  c("&8  &e2eme &7- 5000 Pasteques"));
        setLine(board, 3,  c("&8  &c3eme &7- 2500 Pasteques"));
        setLine(board, 2,  "");
        String ip = (tickCount % 3 == 0) ? "&2play&8.&5pasteque&8.&2world"
                  : (tickCount % 3 == 1) ? "&a play&8.&d pasteque&8.&a world"
                  : "&2\u2764 &5pasteque&8.&2world &5\u2764";
        setLine(board, 1,  c(ip));
        setLine(board, 0,  "");
    }

    // =========================================================================
    //  DUNGEON scoreboard
    // =========================================================================
    private void updateDungeonBoard(Player player, Scoreboard board) {
        // Try to get dungeon instance info
        String dungeonName = "Inconnu";
        int wave = 0, mobsLeft = 0;
        long remaining = 0;
        int partySize = 0;

        try {
            // Access via reflection-safe pattern — dungeon manager may not exist yet
            java.lang.reflect.Method m = plugin.getClass().getMethod("getDungeonManager");
            Object dm = m.invoke(plugin);
            if (dm != null) {
                java.lang.reflect.Method gi = dm.getClass().getMethod("getPlayerInstance", UUID.class);
                Object inst = gi.invoke(dm, player.getUniqueId());
                if (inst != null) {
                    dungeonName = (String) inst.getClass().getMethod("getTypeName").invoke(inst);
                    wave = (Integer) inst.getClass().getMethod("getCurrentWave").invoke(inst);
                    mobsLeft = (Integer) inst.getClass().getMethod("getMobsRemaining").invoke(inst);
                    remaining = (Long) inst.getClass().getMethod("getRemainingSeconds").invoke(inst);
                    partySize = (Integer) inst.getClass().getMethod("getPlayerCount").invoke(inst);
                }
            }
        } catch (Exception ignored) {}

        setLine(board, 14, "");
        setLine(board, 13, c("&8\u258E &5&lDonjon"));
        setLine(board, 12, c("&8  \u2726 &7Type: &d" + dungeonName));
        setLine(board, 11, c("&8  \u2726 &7Groupe: &f" + partySize + " joueurs"));
        setLine(board, 10, "");
        setLine(board, 9,  c("&8\u258E &5&lProgression"));
        setLine(board, 8,  c("&8  \u2694 &7Vague: &e" + wave + "/4"));
        setLine(board, 7,  c("&8  \u2694 &7Mobs restants: &c" + mobsLeft));
        setLine(board, 6,  c("&8  \u23F1 &7Temps: &e" + formatTime(remaining)));
        setLine(board, 5,  "");
        setLine(board, 4,  c("&8\u258E &5&lAstuces"));
        setLine(board, 3,  c("&8  \u25B8 &7Tuez tous les mobs !"));
        setLine(board, 2,  c("&8  \u25B8 &7Le boss est en vague 4"));
        String ip = (tickCount % 3 == 0) ? "&2play&8.&5pasteque&8.&2world"
                  : (tickCount % 3 == 1) ? "&a play&8.&d pasteque&8.&a world"
                  : "&2\u2764 &5pasteque&8.&2world &5\u2764";
        setLine(board, 1,  c(ip));
        setLine(board, 0,  "");
    }

    // =========================================================================
    //  Utility
    // =========================================================================

    private String formatTime(long seconds) {
        if (seconds <= 0) return "0:00";
        long m = seconds / 60;
        long s = seconds % 60;
        return m + ":" + (s < 10 ? "0" : "") + s;
    }

    private static String c(String text) {
        return PastequeSkyblockPlugin.color(text);
    }

    private void setLine(Scoreboard board, int score, String text) {
        Team team = board.getTeam("line_" + score);
        if (team == null) {
            return;
        }
        String prefix;
        String suffix = "";
        if (text.length() <= 16) {
            prefix = text;
        } else {
            prefix = text.substring(0, 16);
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
        lastContext.remove(player.getUniqueId());
    }

    public void updateAll() {
        tickCount++;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (playerBoards.containsKey(player.getUniqueId())) {
                updateScoreboard(player);
            }
        }
    }
}
