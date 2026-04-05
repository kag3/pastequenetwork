package fr.pasteque.skyblock.pvp;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Restaure le PvP style 1.8 sur Spigot 1.9.4 :
 * - Pas de cooldown d'attaque (attack speed infinie)
 * - Pas d'offhand / bouclier
 * - Knockback classique bien ajusté (style LabyRoyale)
 * - Pas de sweep attack
 * - Dégâts classiques sans reduction de cooldown
 */
public class OldPvPListener implements Listener {

    private final PastequeSkyblockPlugin plugin;

    /* ── Config values ── */
    private final double baseKnockback;
    private final double sprintKnockback;
    private final double verticalKnockback;
    private final double knockbackEnchantMultiplier;
    private final boolean disableOffhand;
    private final boolean disableSweep;
    private final boolean disableShields;

    public OldPvPListener(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.baseKnockback = plugin.getConfig().getDouble("old-pvp.base-knockback", 0.4D);
        this.sprintKnockback = plugin.getConfig().getDouble("old-pvp.sprint-knockback", 0.45D);
        this.verticalKnockback = plugin.getConfig().getDouble("old-pvp.vertical-knockback", 0.36D);
        this.knockbackEnchantMultiplier = plugin.getConfig().getDouble("old-pvp.knockback-enchant-multiplier", 0.5D);
        this.disableOffhand = plugin.getConfig().getBoolean("old-pvp.disable-offhand", true);
        this.disableSweep = plugin.getConfig().getBoolean("old-pvp.disable-sweep", true);
        this.disableShields = plugin.getConfig().getBoolean("old-pvp.disable-shields", true);
    }

    // =========================================================================
    //  Remove attack cooldown on join (set attack speed to max via attribute)
    // =========================================================================

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        removeAttackCooldown(event.getPlayer());
    }

    /**
     * Uses reflection to set generic.attackSpeed to 1024 (effectively no cooldown).
     * Works on Spigot 1.9+ where the Attribute API exists.
     */
    public void removeAttackCooldown(final Player player) {
        // Delay 1 tick to ensure player is fully loaded
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // Try the Bukkit Attribute API (Spigot 1.9+)
                    Object attribute = null;
                    try {
                        Class<?> attributeClass = Class.forName("org.bukkit.attribute.Attribute");
                        Object[] constants = attributeClass.getEnumConstants();
                        for (Object constant : constants) {
                            if (constant.toString().equals("GENERIC_ATTACK_SPEED")) {
                                attribute = constant;
                                break;
                            }
                        }
                    } catch (ClassNotFoundException ignored) {
                        return; // Attribute API not available
                    }

                    if (attribute == null) return;

                    // player.getAttribute(Attribute.GENERIC_ATTACK_SPEED)
                    java.lang.reflect.Method getAttributeMethod = player.getClass().getMethod("getAttribute", attribute.getClass().getDeclaringClass() != null ? attribute.getClass().getDeclaringClass() : attribute.getClass());
                    // Actually, getAttribute takes Attribute enum
                    Class<?> attributeEnum = Class.forName("org.bukkit.attribute.Attribute");
                    getAttributeMethod = null;
                    for (java.lang.reflect.Method m : player.getClass().getMethods()) {
                        if (m.getName().equals("getAttribute") && m.getParameterTypes().length == 1) {
                            getAttributeMethod = m;
                            break;
                        }
                    }

                    if (getAttributeMethod == null) return;

                    Object attributeInstance = getAttributeMethod.invoke(player, attribute);
                    if (attributeInstance == null) return;

                    // attributeInstance.setBaseValue(1024.0)
                    java.lang.reflect.Method setBaseValue = attributeInstance.getClass().getMethod("setBaseValue", double.class);
                    setBaseValue.invoke(attributeInstance, 1024.0D);
                } catch (Throwable ignored) {
                    // Fallback: silently fail
                }
            }
        }.runTaskLater(plugin, 2L);
    }

    // =========================================================================
    //  Custom Knockback (1.8 style, like LabyRoyale)
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        final Player attacker = (Player) event.getDamager();
        final LivingEntity victim = (LivingEntity) event.getEntity();

        // ── Disable sweep attack damage ──
        if (disableSweep) {
            try {
                EntityDamageEvent.DamageCause cause = event.getCause();
                if (cause.name().equals("ENTITY_SWEEP_ATTACK")) {
                    event.setCancelled(true);
                    return;
                }
            } catch (Throwable ignored) {}
        }

        // ── Disable shield blocking ──
        if (disableShields && victim instanceof Player) {
            Player victimPlayer = (Player) victim;
            ItemStack offhand = getOffhandItem(victimPlayer);
            if (offhand != null && offhand.getType().name().equals("SHIELD")) {
                // Remove blocking
                try {
                    java.lang.reflect.Method method = victimPlayer.getClass().getMethod("getInventory");
                    Object inv = method.invoke(victimPlayer);
                    java.lang.reflect.Method setOffhand = inv.getClass().getMethod("setItemInOffHand", ItemStack.class);
                    setOffhand.invoke(inv, new ItemStack(Material.AIR));
                } catch (Throwable ignored) {}
            }
        }

        // ── Apply custom knockback (1 tick later to override vanilla) ──
        new BukkitRunnable() {
            @Override
            public void run() {
                if (victim.isDead()) return;

                // Direction from attacker to victim
                Vector direction = victim.getLocation().toVector().subtract(attacker.getLocation().toVector());
                if (direction.lengthSquared() < 0.001D) {
                    direction = attacker.getLocation().getDirection();
                }
                direction.setY(0).normalize();

                // Base knockback
                double horizontal = baseKnockback;

                // Sprint bonus (1.8 sprint-hit = big knockback)
                if (attacker.isSprinting()) {
                    horizontal += sprintKnockback;
                }

                // Knockback enchantment bonus
                ItemStack weapon = attacker.getItemInHand();
                if (weapon != null && weapon.containsEnchantment(Enchantment.KNOCKBACK)) {
                    int level = weapon.getEnchantmentLevel(Enchantment.KNOCKBACK);
                    horizontal += level * knockbackEnchantMultiplier;
                }

                double vertical = verticalKnockback;

                // Apply
                Vector knockback = direction.multiply(horizontal);
                knockback.setY(vertical);

                // Cap vertical to prevent flying
                if (knockback.getY() > 0.5D) {
                    knockback.setY(0.5D);
                }

                victim.setVelocity(knockback);
            }
        }.runTaskLater(plugin, 1L);
    }

    // =========================================================================
    //  Disable Offhand (no shields, no dual-wielding)
    // =========================================================================

    /**
     * Block placing items in offhand slot (slot 40)
     */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!disableOffhand) return;
        if (!(event.getWhoClicked() instanceof Player)) return;

        // Offhand slot in player inventory = slot 40 in raw slot, or crafting type check
        if (event.getSlotType() != null) {
            try {
                // The offhand slot is rawSlot 45 in the player inventory view
                if (event.getRawSlot() == 45) {
                    event.setCancelled(true);
                    return;
                }
            } catch (Throwable ignored) {}
        }

        // Also prevent shift-clicking shields into offhand
        if (event.isShiftClick() && event.getCurrentItem() != null) {
            String typeName = event.getCurrentItem().getType().name();
            if (typeName.equals("SHIELD")) {
                event.setCancelled(true);
            }
        }
    }

    // =========================================================================
    //  Disable Shield Crafting (optional)
    // =========================================================================

    @EventHandler(ignoreCancelled = true)
    public void onCraft(org.bukkit.event.inventory.CraftItemEvent event) {
        if (!disableShields) return;
        if (event.getRecipe() == null || event.getRecipe().getResult() == null) return;
        if (event.getRecipe().getResult().getType().name().equals("SHIELD")) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player) {
                ((Player) event.getWhoClicked()).sendMessage(
                    PastequeSkyblockPlugin.color("&c&lPvP 1.8 &8» &7Les boucliers sont desactives sur ce serveur.")
                );
            }
        }
    }

    // =========================================================================
    //  Helper
    // =========================================================================

    private ItemStack getOffhandItem(Player player) {
        try {
            java.lang.reflect.Method method = player.getInventory().getClass().getMethod("getItemInOffHand");
            return (ItemStack) method.invoke(player.getInventory());
        } catch (Throwable ignored) {
            return null;
        }
    }
}
