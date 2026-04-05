package fr.pasteque.skyblock.arena;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DuelService {

    private final PastequeSkyblockPlugin plugin;
    private final ArenaWorldService arenaWorldService;
    private final EloService eloService;

    /** challenger UUID -> target UUID */
    private final HashMap<UUID, UUID> pendingInvites = new HashMap<UUID, UUID>();
    /** player UUID -> opponent UUID (both directions) */
    private final HashMap<UUID, UUID> activeDuels = new HashMap<UUID, UUID>();
    private final Set<UUID> inDuel = new HashSet<UUID>();

    /** Saved locations for teleporting back */
    private final HashMap<UUID, Location> savedLocations = new HashMap<UUID, Location>();

    public DuelService(PastequeSkyblockPlugin plugin, ArenaWorldService arenaWorldService, EloService eloService) {
        this.plugin = plugin;
        this.arenaWorldService = arenaWorldService;
        this.eloService = eloService;
    }

    // =========================================================================
    //  Invite
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
    //  Accept
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

        // Mark as in duel
        inDuel.add(challengerId);
        inDuel.add(targetId);
        activeDuels.put(challengerId, targetId);
        activeDuels.put(targetId, challengerId);

        // Save locations
        savedLocations.put(challengerId, challenger.getLocation().clone());
        savedLocations.put(targetId, target.getLocation().clone());

        // Teleport both to arena spawn offset
        World arenaWorld = arenaWorldService.getArenaWorld();
        Location spawn = arenaWorldService.getConfiguredSpawn(arenaWorld);
        final Location loc1 = spawn.clone().add(10, 0, 0);
        final Location loc2 = spawn.clone().add(-10, 0, 0);

        challenger.teleport(loc1);
        target.teleport(loc2);

        final Player finalTarget = target;
        final String prefix = plugin.getPrefix();

        // Countdown 5 seconds
        challenger.sendMessage(PastequeSkyblockPlugin.color(prefix + "&eDuel contre &d" + target.getName() + "&e !"));
        target.sendMessage(PastequeSkyblockPlugin.color(prefix + "&eDuel contre &d" + challenger.getName() + "&e !"));

        for (int i = 5; i >= 1; i--) {
            final int count = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    String msg = PastequeSkyblockPlugin.color("&e&l" + count + "...");
                    if (challenger.isOnline()) challenger.sendMessage(msg);
                    if (finalTarget.isOnline()) finalTarget.sendMessage(msg);
                }
            }.runTaskLater(plugin, 20L * (5 - i));
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (challenger.isOnline() && finalTarget.isOnline()) {
                    String goMsg = PastequeSkyblockPlugin.color(prefix + "&a&lCOMBATTEZ !");
                    challenger.sendMessage(goMsg);
                    finalTarget.sendMessage(goMsg);
                }
            }
        }.runTaskLater(plugin, 20L * 5);
    }

    // =========================================================================
    //  End duel
    // =========================================================================

    public void endDuel(Player winner, Player loser) {
        UUID winnerId = winner.getUniqueId();
        UUID loserId = loser.getUniqueId();

        if (!inDuel.contains(winnerId) || !inDuel.contains(loserId)) {
            return;
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

        // Broadcast
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getUniqueId().equals(winnerId) && !p.getUniqueId().equals(loserId)) {
                p.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&d" + winner.getName() + " &fa remporte le duel contre &d" + loser.getName() + "&f !"));
            }
        }

        // Teleport back
        Location savedWinner = savedLocations.remove(winnerId);
        Location savedLoser = savedLocations.remove(loserId);
        if (savedWinner != null) winner.teleport(savedWinner);
        if (savedLoser != null && loser.isOnline()) loser.teleport(savedLoser);

        // Clean up
        inDuel.remove(winnerId);
        inDuel.remove(loserId);
        activeDuels.remove(winnerId);
        activeDuels.remove(loserId);

        eloService.save();
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
        UUID opponentId = activeDuels.get(uuid);
        inDuel.remove(uuid);
        activeDuels.remove(uuid);
        savedLocations.remove(uuid);

        if (opponentId != null) {
            inDuel.remove(opponentId);
            activeDuels.remove(opponentId);
            Location saved = savedLocations.remove(opponentId);
            Player opponent = Bukkit.getPlayer(opponentId);
            if (opponent != null && opponent.isOnline()) {
                opponent.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cLe duel a ete annule (adversaire deconnecte)."));
                if (saved != null) {
                    opponent.teleport(saved);
                }
            }
        }
    }
}
