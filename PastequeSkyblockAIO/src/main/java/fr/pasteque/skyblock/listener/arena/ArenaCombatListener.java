package fr.pasteque.skyblock.listener.arena;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.arena.ArenaLevelService;
import fr.pasteque.skyblock.arena.ArenaWorldService;
import fr.pasteque.skyblock.arena.CombatTagService;
import fr.pasteque.skyblock.arena.DuelService;
import fr.pasteque.skyblock.arena.PlayerDataService;
import fr.pasteque.skyblock.arena.SafeZoneService;
import fr.pasteque.skyblock.arena.model.ArenaPlayerData;
import fr.pasteque.skyblock.manager.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class ArenaCombatListener implements Listener {

    private final PastequeSkyblockPlugin plugin;
    private final ArenaWorldService arenaWorldService;
    private final SafeZoneService safeZoneService;
    private final CombatTagService combatTagService;
    private final ArenaLevelService arenaLevelService;
    private final PlayerDataService playerDataService;
    private final DuelService duelService;

    public ArenaCombatListener(PastequeSkyblockPlugin plugin, ArenaWorldService arenaWorldService, SafeZoneService safeZoneService, CombatTagService combatTagService, ArenaLevelService arenaLevelService, PlayerDataService playerDataService, DuelService duelService) {
        this.plugin = plugin;
        this.arenaWorldService = arenaWorldService;
        this.safeZoneService = safeZoneService;
        this.combatTagService = combatTagService;
        this.arenaLevelService = arenaLevelService;
        this.playerDataService = playerDataService;
        this.duelService = duelService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player) event.getEntity();
        if (!arenaWorldService.isArenaWorld(victim.getWorld())) {
            return;
        }
        Player attacker = findAttacker(event.getDamager());
        if (attacker == null || attacker.equals(victim)) {
            return;
        }
        if (safeZoneService.isSafe(victim.getLocation()) || safeZoneService.isSafe(attacker.getLocation())) {
            event.setCancelled(true);
            attacker.sendMessage(plugin.color(plugin.getPrefix() + plugin.getConfig().getString("messages.no-pvp-safe", "&fLe combat n'est pas autorise dans cette zone.")));
            return;
        }
        combatTagService.tag(attacker, victim);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (!arenaWorldService.isArenaWorld(victim.getWorld())) {
            return;
        }

        // Check if the dead player is in an active duel
        if (duelService.isDueling(victim.getUniqueId())) {
            Player killer = victim.getKiller();
            if (killer != null) {
                // Cancel normal death processing for duel participants
                event.setDeathMessage(null);
                event.getDrops().clear();
                event.setDroppedExp(0);
                event.setKeepInventory(true);
                event.setKeepLevel(true);

                // End the duel with winner and loser
                duelService.endDuel(killer, victim);
                return;
            }
            // If killed by environment in duel, find opponent as winner
            java.util.UUID opponentId = duelService.getOpponent(victim.getUniqueId());
            if (opponentId != null) {
                Player opponent = org.bukkit.Bukkit.getPlayer(opponentId);
                if (opponent != null && opponent.isOnline()) {
                    event.setDeathMessage(null);
                    event.getDrops().clear();
                    event.setDroppedExp(0);
                    event.setKeepInventory(true);
                    event.setKeepLevel(true);

                    duelService.endDuel(opponent, victim);
                    return;
                }
            }
            // Fallback: cancel the duel
            duelService.cancelDuel(victim.getUniqueId());
            event.setDeathMessage(null);
            event.getDrops().clear();
            event.setDroppedExp(0);
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            return;
        }

        // Normal arena death processing (non-duel)
        ArenaPlayerData victimData = playerDataService.get(victim);
        victimData.addDeath();
        event.setDeathMessage(null);
        Player killer = victim.getKiller();
        if (killer != null) {
            ArenaPlayerData killerData = playerDataService.get(killer);
            killerData.addKill();
            arenaLevelService.addXp(killer, plugin.getConfig().getInt("kill-xp", 35));
            EconomyManager eco = plugin.getEconomyManager();
            double victimBalance = eco.getBalance(victim.getUniqueId());
            double stolen = Math.floor(victimBalance * plugin.getConfig().getDouble("pvp-pasteque-steal-percent", 0.10D));
            if (stolen > 0) {
                eco.take(victim.getUniqueId(), stolen);
                eco.add(killer.getUniqueId(), stolen);
            }
            String message = plugin.color(plugin.getPrefix() + plugin.getConfig().getString("messages.kill-message", "&d%victim% &fa ete terrasse par &5%killer%&f.")
                    .replace("%victim%", victim.getName())
                    .replace("%killer%", killer.getName()));
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (arenaWorldService.isArenaWorld(online.getWorld())) {
                    online.sendMessage(message);
                }
            }
        }
    }

    private Player findAttacker(Entity entity) {
        if (entity instanceof Player) {
            return (Player) entity;
        }
        if (entity instanceof Arrow) {
            Arrow arrow = (Arrow) entity;
            if (arrow.getShooter() instanceof Player) {
                return (Player) arrow.getShooter();
            }
        }
        return null;
    }
}
