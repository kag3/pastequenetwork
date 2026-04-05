package fr.pasteque.skyblock.arena;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DuelService {

    private final PastequeSkyblockPlugin plugin;
    private final ArenaWorldService arenaWorldService;
    private final EloService eloService;

    /** challenger UUID -> target UUID */
    private final HashMap<UUID, UUID> pendingInvites = new HashMap<UUID, UUID>();
    /** player UUID -> opponent UUID (both directions, for 1v1) */
    private final HashMap<UUID, UUID> activeDuels = new HashMap<UUID, UUID>();
    private final Set<UUID> inDuel = new HashSet<UUID>();

    /** Saved locations for teleporting back */
    private final HashMap<UUID, Location> savedLocations = new HashMap<UUID, Location>();
    /** Saved inventories for restoring after duel */
    private final HashMap<UUID, ItemStack[]> savedInventories = new HashMap<UUID, ItemStack[]>();
    private final HashMap<UUID, ItemStack[]> savedArmor = new HashMap<UUID, ItemStack[]>();
    /** Saved game modes */
    private final HashMap<UUID, GameMode> savedGameModes = new HashMap<UUID, GameMode>();

    /** player UUID -> matchId for arena cleanup */
    private final HashMap<UUID, Integer> playerMatchIds = new HashMap<UUID, Integer>();
    /** player UUID -> spawn offset for that match's map */
    private final HashMap<UUID, Integer> playerSpawnOffsets = new HashMap<UUID, Integer>();

    /** 2v2: player UUID -> team index (1 or 2) */
    private final HashMap<UUID, Integer> teamMembership = new HashMap<UUID, Integer>();
    /** 2v2: matchId -> list of alive players on team 1 */
    private final HashMap<Integer, List<UUID>> teamAlive1 = new HashMap<Integer, List<UUID>>();
    /** 2v2: matchId -> list of alive players on team 2 */
    private final HashMap<Integer, List<UUID>> teamAlive2 = new HashMap<Integer, List<UUID>>();
    /** 2v2: matchId -> all players in that match */
    private final HashMap<Integer, List<UUID>> matchPlayers = new HashMap<Integer, List<UUID>>();

    /** Tracks whether an end-duel sequence is already running to prevent double calls */
    private final Set<Integer> endingMatches = new HashSet<Integer>();

    public DuelService(PastequeSkyblockPlugin plugin, ArenaWorldService arenaWorldService, EloService eloService) {
        this.plugin = plugin;
        this.arenaWorldService = arenaWorldService;
        this.eloService = eloService;
    }

    // =========================================================================
    //  Invite (1v1)
    // =========================================================================

    public void invite(Player challenger, Player target) {
        if (challenger.getUniqueId().equals(target.getUniqueId())) {
            challenger.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cVous ne pouvez pas vous defier vous-meme."));
            return;
        }
        if (inDuel.contains(challenger.getUniqueId())) {
            challenger.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cVous etes deja en duel."));
            return;
        }
        if (inDuel.contains(target.getUniqueId())) {
            challenger.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&c" + target.getName() + " est deja en duel."));
            return;
        }

        pendingInvites.put(challenger.getUniqueId(), target.getUniqueId());

        challenger.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&fDemande de duel envoyee a &d" + target.getName() + "&f."));

        target.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&d" + challenger.getName() + " &fvous defie en duel !"));
        target.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&fTapez &a/duel accept &fpour accepter."));

        // Expire after 30 seconds
        final UUID challengerId = challenger.getUniqueId();
        final UUID targetId = target.getUniqueId();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingInvites.containsKey(challengerId) && pendingInvites.get(challengerId).equals(targetId)) {
                    pendingInvites.remove(challengerId);
                    Player c = Bukkit.getPlayer(challengerId);
                    if (c != null) {
                        c.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cVotre demande de duel a expire."));
                    }
                }
            }
        }.runTaskLater(plugin, 20L * 30L);
    }

    // =========================================================================
    //  Accept (1v1)
    // =========================================================================

    public void accept(Player target) {
        UUID targetId = target.getUniqueId();

        // Find who invited this target
        UUID challengerId = null;
        for (Map.Entry<UUID, UUID> entry : pendingInvites.entrySet()) {
            if (entry.getValue().equals(targetId)) {
                challengerId = entry.getKey();
                break;
            }
        }

        if (challengerId == null) {
            target.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cVous n'avez aucune demande de duel en attente."));
            return;
        }

        final Player challenger = Bukkit.getPlayer(challengerId);
        if (challenger == null || !challenger.isOnline()) {
            target.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cLe joueur n'est plus en ligne."));
            pendingInvites.remove(challengerId);
            return;
        }

        pendingInvites.remove(challengerId);

        // Allocate match arena
        final int matchId = arenaWorldService.allocateMatchId();
        final int spawnOffset = arenaWorldService.createMatchArena(matchId);

        // Mark as in duel
        inDuel.add(challengerId);
        inDuel.add(targetId);
        activeDuels.put(challengerId, targetId);
        activeDuels.put(targetId, challengerId);
        playerMatchIds.put(challengerId, matchId);
        playerMatchIds.put(targetId, matchId);
        playerSpawnOffsets.put(challengerId, spawnOffset);
        playerSpawnOffsets.put(targetId, spawnOffset);

        // Save state
        savePlayerState(challenger);
        savePlayerState(target);

        // Teleport both to their spawn points in the match arena
        Location spawn1 = arenaWorldService.getSpawn1(matchId, spawnOffset);
        Location spawn2 = arenaWorldService.getSpawn2(matchId, spawnOffset);

        challenger.teleport(spawn1);
        target.teleport(spawn2);

        // Set survival mode
        challenger.setGameMode(GameMode.SURVIVAL);
        target.setGameMode(GameMode.SURVIVAL);

        // Clear inventories before giving kit
        challenger.getInventory().clear();
        challenger.getInventory().setArmorContents(new ItemStack[4]);
        target.getInventory().clear();
        target.getInventory().setArmorContents(new ItemStack[4]);

        // Heal players
        challenger.setHealth(challenger.getMaxHealth());
        challenger.setFoodLevel(20);
        challenger.setSaturation(20.0f);
        target.setHealth(target.getMaxHealth());
        target.setFoodLevel(20);
        target.setSaturation(20.0f);

        // Apply freeze effect (SLOW 255 + JUMP_BOOST 128)
        challenger.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 6, 255));
        challenger.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20 * 6, 128));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 6, 255));
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20 * 6, 128));

        // Countdown with titles
        final Player finalTarget = target;
        final Player finalChallenger = challenger;

        // "5" yellow
        new BukkitRunnable() {
            @Override
            public void run() {
                sendTitle(finalChallenger, PastequeSkyblockPlugin.color("&e&l5"), "");
                sendTitle(finalTarget, PastequeSkyblockPlugin.color("&e&l5"), "");
            }
        }.runTaskLater(plugin, 20L);

        // "4" yellow
        new BukkitRunnable() {
            @Override
            public void run() {
                sendTitle(finalChallenger, PastequeSkyblockPlugin.color("&e&l4"), "");
                sendTitle(finalTarget, PastequeSkyblockPlugin.color("&e&l4"), "");
            }
        }.runTaskLater(plugin, 20L * 2);

        // "3" gold
        new BukkitRunnable() {
            @Override
            public void run() {
                sendTitle(finalChallenger, PastequeSkyblockPlugin.color("&6&l3"), "");
                sendTitle(finalTarget, PastequeSkyblockPlugin.color("&6&l3"), "");
            }
        }.runTaskLater(plugin, 20L * 3);

        // "2" red
        new BukkitRunnable() {
            @Override
            public void run() {
                sendTitle(finalChallenger, PastequeSkyblockPlugin.color("&c&l2"), "");
                sendTitle(finalTarget, PastequeSkyblockPlugin.color("&c&l2"), "");
            }
        }.runTaskLater(plugin, 20L * 4);

        // "1" dark_red
        new BukkitRunnable() {
            @Override
            public void run() {
                sendTitle(finalChallenger, PastequeSkyblockPlugin.color("&4&l1"), "");
                sendTitle(finalTarget, PastequeSkyblockPlugin.color("&4&l1"), "");
            }
        }.runTaskLater(plugin, 20L * 5);

        // "COMBAT !" green - remove slowness, give kit
        new BukkitRunnable() {
            @Override
            public void run() {
                if (finalChallenger.isOnline() && finalTarget.isOnline()) {
                    sendTitle(finalChallenger, PastequeSkyblockPlugin.color("&a&lCOMBAT !"), "");
                    sendTitle(finalTarget, PastequeSkyblockPlugin.color("&a&lCOMBAT !"), "");

                    // Remove freeze effects
                    finalChallenger.removePotionEffect(PotionEffectType.SLOW);
                    finalChallenger.removePotionEffect(PotionEffectType.JUMP);
                    finalTarget.removePotionEffect(PotionEffectType.SLOW);
                    finalTarget.removePotionEffect(PotionEffectType.JUMP);

                    // Give kit
                    giveDuelKit(finalChallenger);
                    giveDuelKit(finalTarget);
                }
            }
        }.runTaskLater(plugin, 20L * 6);
    }

    // =========================================================================
    //  2v2 Team Duel
    // =========================================================================

    public void startTeamDuel(Player[] team1, Player[] team2) {
        // Allocate match arena
        final int matchId = arenaWorldService.allocateMatchId();
        final int spawnOffset = arenaWorldService.createMatchArena(matchId);

        List<UUID> alive1 = new ArrayList<UUID>();
        List<UUID> alive2 = new ArrayList<UUID>();
        List<UUID> allPlayers = new ArrayList<UUID>();

        // Setup team 1
        for (int i = 0; i < team1.length; i++) {
            Player p = team1[i];
            UUID pid = p.getUniqueId();
            inDuel.add(pid);
            playerMatchIds.put(pid, matchId);
            playerSpawnOffsets.put(pid, spawnOffset);
            teamMembership.put(pid, 1);
            alive1.add(pid);
            allPlayers.add(pid);
            savePlayerState(p);

            // Offset team members along Z axis
            Location spawn = arenaWorldService.getSpawn1(matchId, spawnOffset);
            spawn.add(0, 0, i * 3);
            p.teleport(spawn);
            p.setGameMode(GameMode.SURVIVAL);
            p.getInventory().clear();
            p.getInventory().setArmorContents(new ItemStack[4]);
            p.setHealth(p.getMaxHealth());
            p.setFoodLevel(20);
            p.setSaturation(20.0f);

            // Freeze
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 6, 255));
            p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20 * 6, 128));
        }

        // Setup team 2
        for (int i = 0; i < team2.length; i++) {
            Player p = team2[i];
            UUID pid = p.getUniqueId();
            inDuel.add(pid);
            playerMatchIds.put(pid, matchId);
            playerSpawnOffsets.put(pid, spawnOffset);
            teamMembership.put(pid, 2);
            alive2.add(pid);
            allPlayers.add(pid);
            savePlayerState(p);

            Location spawn = arenaWorldService.getSpawn2(matchId, spawnOffset);
            spawn.add(0, 0, i * 3);
            p.teleport(spawn);
            p.setGameMode(GameMode.SURVIVAL);
            p.getInventory().clear();
            p.getInventory().setArmorContents(new ItemStack[4]);
            p.setHealth(p.getMaxHealth());
            p.setFoodLevel(20);
            p.setSaturation(20.0f);

            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 6, 255));
            p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20 * 6, 128));
        }

        teamAlive1.put(matchId, alive1);
        teamAlive2.put(matchId, alive2);
        matchPlayers.put(matchId, allPlayers);

        // Countdown with titles for all players
        final List<UUID> allIds = new ArrayList<UUID>(allPlayers);

        // "5" yellow
        new BukkitRunnable() {
            @Override
            public void run() {
                sendTitleToAll(allIds, PastequeSkyblockPlugin.color("&e&l5"), "");
            }
        }.runTaskLater(plugin, 20L);

        // "4" yellow
        new BukkitRunnable() {
            @Override
            public void run() {
                sendTitleToAll(allIds, PastequeSkyblockPlugin.color("&e&l4"), "");
            }
        }.runTaskLater(plugin, 20L * 2);

        // "3" gold
        new BukkitRunnable() {
            @Override
            public void run() {
                sendTitleToAll(allIds, PastequeSkyblockPlugin.color("&6&l3"), "");
            }
        }.runTaskLater(plugin, 20L * 3);

        // "2" red
        new BukkitRunnable() {
            @Override
            public void run() {
                sendTitleToAll(allIds, PastequeSkyblockPlugin.color("&c&l2"), "");
            }
        }.runTaskLater(plugin, 20L * 4);

        // "1" dark_red
        new BukkitRunnable() {
            @Override
            public void run() {
                sendTitleToAll(allIds, PastequeSkyblockPlugin.color("&4&l1"), "");
            }
        }.runTaskLater(plugin, 20L * 5);

        // "COMBAT !" green
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uid : allIds) {
                    Player p = Bukkit.getPlayer(uid);
                    if (p != null && p.isOnline()) {
                        sendTitle(p, PastequeSkyblockPlugin.color("&a&lCOMBAT !"), "");
                        p.removePotionEffect(PotionEffectType.SLOW);
                        p.removePotionEffect(PotionEffectType.JUMP);
                        giveDuelKit(p);
                    }
                }
            }
        }.runTaskLater(plugin, 20L * 6);
    }

    // =========================================================================
    //  End duel (1v1)
    // =========================================================================

    public void endDuel(final Player winner, final Player loser) {
        final UUID winnerId = winner.getUniqueId();
        final UUID loserId = loser.getUniqueId();

        if (!inDuel.contains(winnerId) && !inDuel.contains(loserId)) {
            return;
        }

        // Check if this is a team duel
        if (teamMembership.containsKey(loserId)) {
            endTeamDuelDeath(winner, loser);
            return;
        }

        Integer matchIdObj = playerMatchIds.get(winnerId);
        if (matchIdObj == null) {
            matchIdObj = playerMatchIds.get(loserId);
        }
        final int matchId = matchIdObj != null ? matchIdObj.intValue() : -1;

        // Prevent double end
        if (matchId != -1 && endingMatches.contains(matchId)) {
            return;
        }
        if (matchId != -1) {
            endingMatches.add(matchId);
        }

        // Update ELO
        eloService.updateElo(winnerId, loserId);

        int winnerElo = eloService.getElo(winnerId);
        int loserElo = eloService.getElo(loserId);

        // Reward winner
        double reward = plugin.getConfig().getDouble("duel.reward", 200.0);
        plugin.getEconomyManager().add(winnerId, reward);

        // Messages
        winner.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&a&lVictoire ! &fVous avez gagne le duel contre &d" + loser.getName() + "&f. &a+" + (int) reward + " " + plugin.getEconomyManager().getCurrencyName()));
        winner.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&fELO : &d" + winnerElo + " &f(" + eloService.getRankColor(winnerElo) + eloService.getRank(winnerElo) + "&f)"));

        if (loser.isOnline()) {
            loser.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&c&lDefaite ! &fVous avez perdu le duel contre &d" + winner.getName() + "&f."));
            loser.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&fELO : &d" + loserElo + " &f(" + eloService.getRankColor(loserElo) + eloService.getRank(loserElo) + "&f)"));
        }

        // Titles
        sendTitle(winner, PastequeSkyblockPlugin.color("&a&lVICTOIRE"), PastequeSkyblockPlugin.color("&7GG bien joue !"));
        if (loser.isOnline()) {
            sendTitle(loser, PastequeSkyblockPlugin.color("&c&lDEFAITE"), PastequeSkyblockPlugin.color("&7Prochaine fois..."));
        }

        // Broadcast
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getUniqueId().equals(winnerId) && !p.getUniqueId().equals(loserId)) {
                p.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&d" + winner.getName() + " &fa remporte le duel contre &d" + loser.getName() + "&f !"));
            }
        }

        // Spawn fireworks at winner (3 fireworks, 1 second apart)
        final Location fwLoc = winner.getLocation().clone();
        for (int i = 0; i < 3; i++) {
            final int delay = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    spawnFirework(fwLoc);
                }
            }.runTaskLater(plugin, 20L * delay);
        }

        // Both players enter spectator mode for 5 seconds
        winner.setGameMode(GameMode.ADVENTURE);
        if (loser.isOnline()) {
            loser.setGameMode(GameMode.ADVENTURE);
            // Respawn loser health so they can spectate
            loser.setHealth(loser.getMaxHealth());
        }

        // After 5 seconds: restore and teleport back
        final int finalMatchId = matchId;
        new BukkitRunnable() {
            @Override
            public void run() {
                restoreAndCleanup(winnerId);
                restoreAndCleanup(loserId);

                // Destroy match arena
                if (finalMatchId != -1) {
                    arenaWorldService.destroyMatchArena(finalMatchId);
                    endingMatches.remove(finalMatchId);
                }

                eloService.save();
            }
        }.runTaskLater(plugin, 20L * 5);
    }

    // =========================================================================
    //  End team duel (2v2) - called when a player dies
    // =========================================================================

    private void endTeamDuelDeath(final Player killer, final Player dead) {
        UUID deadId = dead.getUniqueId();
        Integer matchIdObj = playerMatchIds.get(deadId);
        if (matchIdObj == null) return;
        final int matchId = matchIdObj.intValue();

        Integer teamNum = teamMembership.get(deadId);
        if (teamNum == null) return;

        // Remove dead player from alive list
        List<UUID> alive;
        if (teamNum.intValue() == 1) {
            alive = teamAlive1.get(matchId);
        } else {
            alive = teamAlive2.get(matchId);
        }
        if (alive != null) {
            alive.remove(deadId);
        }

        // Put dead player in spectator
        dead.setGameMode(GameMode.ADVENTURE);
        dead.setHealth(dead.getMaxHealth());
        sendTitle(dead, PastequeSkyblockPlugin.color("&c&lELIMINE"), PastequeSkyblockPlugin.color("&7Regardez vos coequipiers..."));

        // Check if the entire team is eliminated
        if (alive != null && alive.isEmpty()) {
            // Prevent double end
            if (endingMatches.contains(matchId)) {
                return;
            }
            endingMatches.add(matchId);

            // Determine winning team
            List<UUID> winnerAlive;
            List<UUID> loserTeamAll;
            if (teamNum.intValue() == 1) {
                winnerAlive = teamAlive2.get(matchId);
                loserTeamAll = teamAlive1.get(matchId); // already empty
            } else {
                winnerAlive = teamAlive1.get(matchId);
                loserTeamAll = teamAlive2.get(matchId); // already empty
            }

            List<UUID> allInMatch = matchPlayers.get(matchId);
            if (allInMatch == null) return;

            // Titles and messages for all players
            for (UUID uid : allInMatch) {
                Player p = Bukkit.getPlayer(uid);
                if (p == null || !p.isOnline()) continue;

                Integer pTeam = teamMembership.get(uid);
                if (pTeam == null) continue;

                if (pTeam.intValue() != teamNum.intValue()) {
                    // Winner team
                    sendTitle(p, PastequeSkyblockPlugin.color("&a&lVICTOIRE"), PastequeSkyblockPlugin.color("&7GG bien joue !"));
                    p.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&a&lVictoire ! &fVotre equipe a remporte le duel 2v2 !"));

                    // Fireworks
                    final Location fwLoc = p.getLocation().clone();
                    for (int i = 0; i < 3; i++) {
                        final int delay = i;
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                spawnFirework(fwLoc);
                            }
                        }.runTaskLater(plugin, 20L * delay);
                    }
                } else {
                    // Loser team
                    sendTitle(p, PastequeSkyblockPlugin.color("&c&lDEFAITE"), PastequeSkyblockPlugin.color("&7Prochaine fois..."));
                    p.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&c&lDefaite ! &fVotre equipe a perdu le duel 2v2."));
                }

                // ELO update: each loser loses to each winner
                p.setGameMode(GameMode.ADVENTURE);
            }

            // Update ELO for all combinations
            List<UUID> winnerIds = new ArrayList<UUID>();
            List<UUID> loserIds = new ArrayList<UUID>();
            for (UUID uid : allInMatch) {
                Integer pTeam = teamMembership.get(uid);
                if (pTeam == null) continue;
                if (pTeam.intValue() != teamNum.intValue()) {
                    winnerIds.add(uid);
                } else {
                    loserIds.add(uid);
                }
            }
            for (UUID wid : winnerIds) {
                for (UUID lid : loserIds) {
                    eloService.updateElo(wid, lid);
                }
            }

            // Reward winners
            double reward = plugin.getConfig().getDouble("duel.reward", 200.0);
            for (UUID wid : winnerIds) {
                plugin.getEconomyManager().add(wid, reward);
            }

            // Broadcast
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!allInMatch.contains(p.getUniqueId())) {
                    p.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&dUne equipe a remporte un duel 2v2 !"));
                }
            }

            // After 5 seconds: restore all and cleanup
            final List<UUID> allIds = new ArrayList<UUID>(allInMatch);
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (UUID uid : allIds) {
                        restoreAndCleanup(uid);
                        teamMembership.remove(uid);
                    }
                    teamAlive1.remove(matchId);
                    teamAlive2.remove(matchId);
                    matchPlayers.remove(matchId);
                    arenaWorldService.destroyMatchArena(matchId);
                    endingMatches.remove(matchId);
                    eloService.save();
                }
            }.runTaskLater(plugin, 20L * 5);
        }
    }

    // =========================================================================
    //  State queries
    // =========================================================================

    public boolean isDueling(UUID uuid) {
        return inDuel.contains(uuid);
    }

    public UUID getOpponent(UUID uuid) {
        return activeDuels.get(uuid);
    }

    public void cancelDuel(UUID uuid) {
        if (!inDuel.contains(uuid)) {
            return;
        }

        // Handle team duel cancel
        if (teamMembership.containsKey(uuid)) {
            cancelTeamDuel(uuid);
            return;
        }

        UUID opponentId = activeDuels.get(uuid);
        Integer matchIdObj = playerMatchIds.get(uuid);

        restoreAndCleanup(uuid);

        if (opponentId != null) {
            restoreAndCleanup(opponentId);
            Player opponent = Bukkit.getPlayer(opponentId);
            if (opponent != null && opponent.isOnline()) {
                opponent.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cLe duel a ete annule (adversaire deconnecte)."));
            }
        }

        if (matchIdObj != null) {
            arenaWorldService.destroyMatchArena(matchIdObj.intValue());
            endingMatches.remove(matchIdObj);
        }
    }

    private void cancelTeamDuel(UUID uuid) {
        Integer matchIdObj = playerMatchIds.get(uuid);
        if (matchIdObj == null) return;
        int matchId = matchIdObj.intValue();

        List<UUID> allInMatch = matchPlayers.get(matchId);
        if (allInMatch == null) return;

        for (UUID uid : allInMatch) {
            restoreAndCleanup(uid);
            teamMembership.remove(uid);
            Player p = Bukkit.getPlayer(uid);
            if (p != null && p.isOnline()) {
                p.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cLe duel 2v2 a ete annule (joueur deconnecte)."));
            }
        }

        teamAlive1.remove(matchId);
        teamAlive2.remove(matchId);
        matchPlayers.remove(matchId);
        arenaWorldService.destroyMatchArena(matchId);
        endingMatches.remove(matchId);
    }

    // =========================================================================
    //  Player state save/restore
    // =========================================================================

    private void savePlayerState(Player player) {
        UUID uuid = player.getUniqueId();
        savedLocations.put(uuid, player.getLocation().clone());
        savedInventories.put(uuid, player.getInventory().getContents().clone());
        savedArmor.put(uuid, player.getInventory().getArmorContents().clone());
        savedGameModes.put(uuid, player.getGameMode());
    }

    private void restoreAndCleanup(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);

        // Restore game mode
        GameMode savedMode = savedGameModes.remove(uuid);

        // Restore inventory
        ItemStack[] inv = savedInventories.remove(uuid);
        ItemStack[] armor = savedArmor.remove(uuid);

        // Teleport back
        Location savedLoc = savedLocations.remove(uuid);

        if (player != null && player.isOnline()) {
            // Restore gamemode first
            if (savedMode != null) {
                player.setGameMode(savedMode);
            } else {
                player.setGameMode(GameMode.SURVIVAL);
            }

            // Clear current inventory and restore saved
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]);
            if (inv != null) {
                player.getInventory().setContents(inv);
            }
            if (armor != null) {
                player.getInventory().setArmorContents(armor);
            }

            // Heal
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20.0f);

            // Remove lingering effects
            player.removePotionEffect(PotionEffectType.SLOW);
            player.removePotionEffect(PotionEffectType.JUMP);

            // Teleport back
            if (savedLoc != null) {
                player.teleport(savedLoc);
            }
        }

        // Clean maps
        inDuel.remove(uuid);
        activeDuels.remove(uuid);
        playerMatchIds.remove(uuid);
        playerSpawnOffsets.remove(uuid);
    }

    // =========================================================================
    //  Kit
    // =========================================================================

    private void giveDuelKit(Player player) {
        ItemStack helmet = new ItemStack(Material.IRON_HELMET);
        helmet.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1);
        ItemStack chestplate = new ItemStack(Material.IRON_CHESTPLATE);
        chestplate.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1);
        ItemStack leggings = new ItemStack(Material.IRON_LEGGINGS);
        leggings.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1);
        ItemStack boots = new ItemStack(Material.IRON_BOOTS);
        boots.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1);

        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);

        player.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
        ItemStack bow = new ItemStack(Material.BOW);
        bow.addUnsafeEnchantment(Enchantment.ARROW_KNOCKBACK, 1);
        player.getInventory().addItem(bow);
        player.getInventory().addItem(new ItemStack(Material.ARROW, 16));
        player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 3));
        player.getInventory().addItem(new ItemStack(Material.GRILLED_PORK, 16));
    }

    // =========================================================================
    //  Title helper (reflection for 1.9.4)
    // =========================================================================

    private void sendTitle(Player player, String title, String subtitle) {
        if (player == null || !player.isOnline()) return;
        try {
            player.getClass().getMethod("sendTitle", String.class, String.class).invoke(player, title, subtitle);
        } catch (Exception e) {
            // Fallback to chat message
            player.sendMessage(title);
        }
    }

    private void sendTitleToAll(List<UUID> uuids, String title, String subtitle) {
        for (UUID uid : uuids) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null && p.isOnline()) {
                sendTitle(p, title, subtitle);
            }
        }
    }

    // =========================================================================
    //  Firework helper
    // =========================================================================

    private void spawnFirework(Location loc) {
        Firework fw = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
            .with(FireworkEffect.Type.BALL_LARGE)
            .withColor(Color.LIME, Color.PURPLE)
            .withFade(Color.WHITE)
            .trail(true)
            .flicker(true)
            .build());
        meta.setPower(1);
        fw.setFireworkMeta(meta);
    }
}
