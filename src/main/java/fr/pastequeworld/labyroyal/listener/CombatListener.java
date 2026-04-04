package fr.pastequeworld.labyroyal.listener;

import fr.pastequeworld.labyroyal.LabyRoyalPlugin;
import fr.pastequeworld.labyroyal.game.Game;
import fr.pastequeworld.labyroyal.game.GameState;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Full 1.8 PVP combat system bypass for 1.9.4.
 * - Removes attack cooldown (max attack speed)
 * - Removes sweep attacks
 * - Disables off-hand (shield, items)
 * - Jitterclick deals proper damage like 1.8
 */
public class CombatListener implements Listener {

    private final LabyRoyalPlugin plugin;
    private final java.util.Set<java.util.UUID> recentlyHit = new java.util.HashSet<java.util.UUID>();

    public CombatListener(LabyRoyalPlugin plugin) {
        this.plugin = plugin;

        // Periodic task: enforce max attack speed + clear off-hand for all in-game players
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());
                    if (game == null) continue;

                    setMaxAttackSpeed(player);

                    // Force clear off-hand
                    ItemStack offhand = player.getInventory().getItemInOffHand();
                    if (offhand != null && offhand.getType() != Material.AIR) {
                        // Move to main inventory or drop
                        int empty = player.getInventory().firstEmpty();
                        if (empty != -1 && empty != 40) {
                            player.getInventory().setItem(empty, offhand);
                        } else {
                            player.getWorld().dropItemNaturally(player.getLocation(), offhand);
                        }
                        player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    // ==================== REMOVE ATTACK COOLDOWN ====================

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        setMaxAttackSpeed(event.getPlayer());
    }

    @EventHandler
    public void onItemSwitch(PlayerItemHeldEvent event) {
        final Player player = event.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    setMaxAttackSpeed(player);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    /**
     * Sets GENERIC_ATTACK_SPEED to 1024 — effectively removes the cooldown bar.
     */
    public void setMaxAttackSpeed(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
        if (attr != null) {
            attr.setBaseValue(1024.0);
        }
    }

    // ==================== DISABLE OFF-HAND ====================

    /**
     * Cancel F key swap.
     */
    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());
        if (game != null) {
            event.setCancelled(true);
        }
    }

    /**
     * Cancel clicking on the off-hand slot (slot 40) in inventory.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());
        if (game == null) return;

        // Slot 40 = off-hand slot in player inventory
        if (event.getSlot() == 40 && event.getSlotType() == InventoryType.SlotType.QUICKBAR) {
            event.setCancelled(true);
        }

        // Also block shift-clicking shields into off-hand
        if (event.isShiftClick() && event.getCurrentItem() != null
                && event.getCurrentItem().getType() == Material.SHIELD) {
            event.setCancelled(true);
        }
    }

    // ==================== 1.8 DAMAGE + KNOCKBACK ====================

    /**
     * Cancel sweep attacks, ensure full damage, apply 1.8 knockback.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        final Player attacker = (Player) event.getDamager();
        Game game = plugin.getGameManager().getPlayerGame(attacker.getUniqueId());
        if (game == null) return;
        if (game.getState() != GameState.PREPARATION && game.getState() != GameState.PVP) return;

        // Cancel sweep attack damage (1.11+ cause, safe check for 1.9.4)
        try {
            if (event.getCause() == EntityDamageEvent.DamageCause.valueOf("ENTITY_SWEEP_ATTACK")) {
                event.setCancelled(true);
                return;
            }
        } catch (IllegalArgumentException ignored) {
            // ENTITY_SWEEP_ATTACK doesn't exist in 1.9.4
        }

        // Apply 1.8 knockback to the victim
        if (event.getEntity() instanceof Player) {
            final Player victim = (Player) event.getEntity();

            // Mark victim so we cancel vanilla KB in PlayerVelocityEvent
            recentlyHit.add(victim.getUniqueId());

            // Calculate knockback enchant level
            ItemStack weapon = attacker.getInventory().getItemInMainHand();
            int kbLevel = 0;
            if (weapon != null && weapon.containsEnchantment(Enchantment.KNOCKBACK)) {
                kbLevel = weapon.getEnchantmentLevel(Enchantment.KNOCKBACK);
            }

            // Sprint hitting adds +1 KB level in 1.8
            final boolean sprinting = attacker.isSprinting();
            final int finalKbLevel = kbLevel + (sprinting ? 1 : 0);

            // Apply custom KB on next tick (overrides vanilla)
            final double attackerX = attacker.getLocation().getX();
            final double attackerZ = attacker.getLocation().getZ();

            new BukkitRunnable() {
                @Override
                public void run() {
                    recentlyHit.remove(victim.getUniqueId());
                    if (!victim.isOnline() || !attacker.isOnline()) return;

                    // Direction from attacker to victim (horizontal only)
                    double dx = victim.getLocation().getX() - attackerX;
                    double dz = victim.getLocation().getZ() - attackerZ;
                    double dist = Math.sqrt(dx * dx + dz * dz);

                    if (dist < 0.001) {
                        // Fallback: use attacker's look direction
                        Vector dir = attacker.getLocation().getDirection();
                        dx = dir.getX();
                        dz = dir.getZ();
                        dist = Math.sqrt(dx * dx + dz * dz);
                    }

                    // Normalize
                    dx /= dist;
                    dz /= dist;

                    // 1.8 knockback: good vertical "pop" + moderate horizontal push
                    double horizontalKB = 0.32;
                    double verticalKB = 0.37;

                    // Each KB level adds more horizontal push + slight vertical
                    horizontalKB += finalKbLevel * 0.36;
                    verticalKB += finalKbLevel * 0.07;

                    // Cap vertical KB like vanilla 1.8
                    if (verticalKB > 0.45) verticalKB = 0.45;

                    Vector kb = new Vector(dx * horizontalKB, verticalKB, dz * horizontalKB);
                    victim.setVelocity(kb);
                }
            }.runTaskLater(plugin, 1L);
        }

        // Reset attack cooldown after each hit for seamless jitterclick
        new BukkitRunnable() {
            @Override
            public void run() {
                if (attacker.isOnline()) {
                    setMaxAttackSpeed(attacker);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    /**
     * Cancel vanilla velocity changes from combat so our custom KB takes over.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVelocity(PlayerVelocityEvent event) {
        if (recentlyHit.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
