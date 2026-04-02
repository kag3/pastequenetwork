package fr.pastequeworld.labyroyal.listener;

import fr.pastequeworld.labyroyal.LabyRoyalPlugin;
import fr.pastequeworld.labyroyal.game.Game;
import fr.pastequeworld.labyroyal.game.GameState;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Full 1.8 PVP combat system bypass for 1.9.4.
 * - Removes attack cooldown (max attack speed)
 * - Removes sweep attacks
 * - Disables off-hand (shield, items)
 * - Jitterclick deals proper damage like 1.8
 */
public class CombatListener implements Listener {

    private final LabyRoyalPlugin plugin;

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

    // ==================== 1.8 DAMAGE (NO COOLDOWN PENALTY) ====================

    /**
     * Cancel sweep attacks and ensure full damage on every hit.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        final Player attacker = (Player) event.getDamager();
        Game game = plugin.getGameManager().getPlayerGame(attacker.getUniqueId());
        if (game == null) return;
        if (game.getState() != GameState.PREPARATION && game.getState() != GameState.PVP) return;

        // Cancel sweep attack damage entirely (1.9 mechanic)
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            event.setCancelled(true);
            return;
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
}
