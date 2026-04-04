package fr.pasteque.skyblockarena.listener;

import fr.pasteque.skyblockarena.PastequeSkyBlockArenaPlugin;
import fr.pasteque.skyblockarena.model.ArenaPlayerData;
import fr.pasteque.skyblockarena.service.ArenaLevelService;
import fr.pasteque.skyblockarena.service.ArenaWorldService;
import fr.pasteque.skyblockarena.service.CombatTagService;
import fr.pasteque.skyblockarena.service.EconomyBridge;
import fr.pasteque.skyblockarena.service.PlayerDataService;
import fr.pasteque.skyblockarena.service.SafeZoneService;
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

    private final PastequeSkyBlockArenaPlugin plugin;
    private final ArenaWorldService arenaWorldService;
    private final SafeZoneService safeZoneService;
    private final CombatTagService combatTagService;
    private final ArenaLevelService arenaLevelService;
    private final EconomyBridge economyBridge;
    private final PlayerDataService playerDataService;

    public ArenaCombatListener(PastequeSkyBlockArenaPlugin plugin, ArenaWorldService arenaWorldService, SafeZoneService safeZoneService, CombatTagService combatTagService, ArenaLevelService arenaLevelService, EconomyBridge economyBridge, PlayerDataService playerDataService) {
        this.plugin = plugin;
        this.arenaWorldService = arenaWorldService;
        this.safeZoneService = safeZoneService;
        this.combatTagService = combatTagService;
        this.arenaLevelService = arenaLevelService;
        this.economyBridge = economyBridge;
        this.playerDataService = playerDataService;
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
            attacker.sendMessage(plugin.color(plugin.prefix() + plugin.getConfig().getString("messages.no-pvp-safe", "&fLe combat n'est pas autorisé dans cette zone.")));
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
        Player killer = victim.getKiller();
        ArenaPlayerData victimData = playerDataService.get(victim);
        victimData.addDeath();
        event.setDeathMessage(null);
        if (killer != null) {
            ArenaPlayerData killerData = playerDataService.get(killer);
            killerData.addKill();
            arenaLevelService.addXp(killer, plugin.getConfig().getInt("kill-xp", 35));
            double victimBalance = economyBridge.getBalance(victim.getUniqueId(), victim.getName());
            double stolen = Math.floor(victimBalance * plugin.getConfig().getDouble("pvp-pasteque-steal-percent", 0.10D));
            if (stolen > 0) {
                economyBridge.withdraw(victim.getUniqueId(), victim.getName(), stolen);
                economyBridge.deposit(killer.getUniqueId(), killer.getName(), stolen);
            }
            String message = plugin.color(plugin.prefix() + plugin.getConfig().getString("messages.kill-message", "&d%victim% &fa été terrassé par &5%killer%&f.")
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
