package fr.pasteque.skyblock.listener;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.model.Island;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PvpListener implements Listener {
    private final PastequeSkyblockPlugin plugin;
    public PvpListener(PastequeSkyblockPlugin plugin) { this.plugin = plugin; }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;
        Player victim = (Player) event.getEntity();
        Player damager = (Player) event.getDamager();
        if (victim.getWorld() != null && victim.getWorld().getName().equalsIgnoreCase(plugin.getConfig().getString("external-arena-world-name", "skyblockarena"))) {
            return;
        }
        boolean allowed = plugin.getPvpManager().isPvp(victim.getLocation());
        if (!allowed && plugin.getIslandManager().isSkyblockWorld(victim.getLocation())) {
            Island island = plugin.getIslandManager().getIslandAt(victim.getLocation());
            allowed = island != null && island.isPvpEnabled();
        }
        if (!allowed) {
            event.setCancelled(true);
            if (!damager.equals(victim)) MessageUtil.send(damager, plugin.getPrefix(), "&fLe PvP n'est pas actif ici.");
            return;
        }
        plugin.getCombatManager().tag(damager, victim);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (victim.getWorld() != null && victim.getWorld().getName().equalsIgnoreCase(plugin.getConfig().getString("external-arena-world-name", "skyblockarena"))) {
            return;
        }
        Player killer = victim.getKiller();
        if (killer == null) return;
        boolean allowed = plugin.getPvpManager().isPvp(victim.getLocation());
        if (!allowed && plugin.getIslandManager().isSkyblockWorld(victim.getLocation())) {
            Island island = plugin.getIslandManager().getIslandAt(victim.getLocation());
            allowed = island != null && island.isPvpEnabled();
        }
        if (!allowed) return;
        double percent = plugin.getConfig().getDouble("combat.pvp-steal-percent", 0.10D);
        double cap = plugin.getConfig().getDouble("combat.pvp-steal-cap", 250.0D);
        double raw = plugin.getEconomyManager().getBalance(victim.getUniqueId()) * percent;
        double stolen = Math.min(cap, Math.max(0D, Math.round(raw * 100.0D) / 100.0D));
        if (stolen <= 0) return;
        plugin.getEconomyManager().take(victim.getUniqueId(), stolen);
        plugin.getEconomyManager().add(killer.getUniqueId(), stolen);
        MessageUtil.send(killer, plugin.getPrefix(), "&aTu as volé &e" + stolen + " " + plugin.getEconomyManager().getCurrencyName() + " &aà &f" + victim.getName());
        MessageUtil.send(victim, plugin.getPrefix(), "&5Tu as perdu &e" + stolen + " " + plugin.getEconomyManager().getCurrencyName() + " &5en zone PvP.");
    }
}
