package fr.pasteque.skyblock.anticheat;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class AntiCheatListener implements Listener {
    private final PastequeSkyblockPlugin plugin;
    private final AntiCheatManager ac;

    public AntiCheatListener(PastequeSkyblockPlugin plugin, AntiCheatManager ac) {
        this.plugin = plugin;
        this.ac = ac;
    }

    // ── REACH ────────────────────────────────────────────────────────────────
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player) e.getDamager();
        if (p.hasPermission("pastequeskyblock.staff")) return;
        if (p.getGameMode() == GameMode.CREATIVE) return;
        Entity target = e.getEntity();
        double distSq = p.getEyeLocation().distanceSquared(target.getLocation());
        if (distSq > AntiCheatManager.MAX_REACH_SQ + 0.6) {
            ac.flag(p, "reach (" + String.format("%.2f", Math.sqrt(distSq)) + "b)", 3);
        }
    }

    // ── SPEED / FLY ─────────────────────────────────────────────────────────
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (p.hasPermission("pastequeskyblock.staff")) return;
        if (p.getGameMode() == GameMode.CREATIVE) return;
        if (p.isInsideVehicle() || p.getAllowFlight() || p.isFlying()) return;
        if (e.getFrom().getWorld() != e.getTo().getWorld()) return;

        UUID id = p.getUniqueId();
        Location last = ac.getLastLocation().get(id);
        long now = System.currentTimeMillis();
        Long lastMs = ac.getLastMove().get(id);
        ac.getLastLocation().put(id, e.getTo().clone());
        ac.getLastMove().put(id, now);

        if (last == null || lastMs == null) return;
        double dx = e.getTo().getX() - last.getX();
        double dy = e.getTo().getY() - last.getY();
        double dz = e.getTo().getZ() - last.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        long elapsed = Math.max(1L, now - lastMs);

        // Speed check: sustained >0.7 per tick = flag
        double speed = horiz / (elapsed / 50.0); // blocks per tick
        if (speed > AntiCheatManager.MAX_HORIZONTAL_PER_TICK && !p.hasPotionEffect(org.bukkit.potion.PotionEffectType.SPEED)) {
            ac.flag(p, "speed (" + String.format("%.2f", speed) + "b/t)", 2);
        }

        // Fly check: significant upward motion without a ground block below
        if (dy > 0.42 && !p.isOnGround()) {
            Location below = p.getLocation().clone().add(0, -1.1, 0);
            if (below.getBlock().getType() == org.bukkit.Material.AIR && !p.hasPotionEffect(org.bukkit.potion.PotionEffectType.JUMP)) {
                ac.flag(p, "fly (dy=" + String.format("%.2f", dy) + ")", 2);
            }
        }
    }

    // ── NO FALL ─────────────────────────────────────────────────────────────
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFall(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        // If fall damage was supposedly taken but player has 0 fall distance, flag
        if (p.getFallDistance() < 0.5f && e.getDamage() < 0.1) {
            ac.flag(p, "nofall (fd=" + p.getFallDistance() + ")", 1);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        ac.clear(e.getPlayer().getUniqueId());
    }
}
