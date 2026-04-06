package fr.pasteque.skyblock.enchant;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.CropState;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Crops;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class EnchantListener implements Listener {

    private final PastequeSkyblockPlugin plugin;

    public EnchantListener(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  BlockBreakEvent — Telekinesis, Smelting Touch, Lumberjack,
    //                     Experience (mining), Implants
    // ─────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = player.getItemInHand();
        if (hand == null || hand.getType() == Material.AIR) return;

        Block block = event.getBlock();
        Material blockType = block.getType();

        // ── LUMBERJACK ──
        if (CustomEnchant.LUMBERJACK.hasEnchant(hand)) {
            if (isLog(blockType)) {
                int level = CustomEnchant.LUMBERJACK.getLevel(hand);
                int radius = level * 2 + 1;
                breakConnectedLogs(block, radius, player, hand);
            }
        }

        // ── SMELTING TOUCH ──
        boolean smelting = CustomEnchant.SMELTING_TOUCH.hasEnchant(hand);

        // ── TELEKINESIS ──
        boolean telekinesis = CustomEnchant.TELEKINESIS.hasEnchant(hand);

        if (smelting || telekinesis) {
            Collection<ItemStack> drops = block.getDrops(hand);
            List<ItemStack> finalDrops = new ArrayList<ItemStack>();
            for (ItemStack drop : drops) {
                if (smelting) {
                    finalDrops.add(smelt(drop));
                } else {
                    finalDrops.add(drop);
                }
            }

            // Cancel the event and manually break the block to suppress default drops
            // (setDropItems does not exist in 1.9.4)
            final Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);
            event.setCancelled(true);
            block.setType(Material.AIR);

            if (telekinesis) {
                for (ItemStack drop : finalDrops) {
                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(drop);
                    for (ItemStack left : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), left);
                    }
                }
            } else {
                // Smelting only: drop smelted items on the ground
                for (ItemStack drop : finalDrops) {
                    block.getWorld().dropItemNaturally(dropLoc, drop);
                }
            }
        }

        // ── EXPERIENCE (mining) ──
        if (CustomEnchant.EXPERIENCE.hasEnchant(hand)) {
            int level = CustomEnchant.EXPERIENCE.getLevel(hand);
            double multiplier = 1.0 + level * 0.5;
            int baseXp = event.getExpToDrop();
            event.setExpToDrop((int) (baseXp * multiplier));
        }

        // ── IMPLANTS ──
        if (CustomEnchant.IMPLANTS.hasEnchant(hand)) {
            if (isCrop(blockType)) {
                final Material cropType = blockType;
                final Block cropBlock = block;
                plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() {
                        cropBlock.setType(cropType);
                        BlockState state = cropBlock.getState();
                        if (state.getData() instanceof Crops) {
                            Crops crops = (Crops) state.getData();
                            crops.setState(CropState.SEEDED);
                            state.setData(crops);
                            state.update(true);
                        }
                    }
                }, 1L);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  PlayerItemHeldEvent — Haste
    // ─────────────────────────────────────────────────────────────────────

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        if (newItem != null && newItem.getType() != Material.AIR && CustomEnchant.HASTE.hasEnchant(newItem)) {
            int level = CustomEnchant.HASTE.getLevel(newItem);
            player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, Integer.MAX_VALUE, level - 1, true), true);
        } else {
            player.removePotionEffect(PotionEffectType.FAST_DIGGING);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  EntityDamageByEntityEvent — Venom, Lifesteal
    // ─────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player attacker = (Player) event.getDamager();
        ItemStack hand = attacker.getItemInHand();
        if (hand == null || hand.getType() == Material.AIR) return;

        Entity victim = event.getEntity();

        // ── VENOM ──
        if (CustomEnchant.VENOM.hasEnchant(hand) && victim instanceof LivingEntity) {
            int level = CustomEnchant.VENOM.getLevel(hand);
            int durationTicks = level * 20; // level seconds
            ((LivingEntity) victim).addPotionEffect(new PotionEffect(PotionEffectType.POISON, durationTicks, 0, true), true);
        }

        // ── LIFESTEAL ──
        if (CustomEnchant.LIFESTEAL.hasEnchant(hand)) {
            int level = CustomEnchant.LIFESTEAL.getLevel(hand);
            double healAmount = level * 1.0; // level * 0.5 hearts = level * 1.0 health points
            double newHealth = Math.min(attacker.getMaxHealth(), attacker.getHealth() + healAmount);
            attacker.setHealth(newHealth);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  EntityDeathEvent — Experience (kill bonus)
    // ─────────────────────────────────────────────────────────────────────

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        ItemStack hand = killer.getItemInHand();
        if (hand == null || hand.getType() == Material.AIR) return;

        if (CustomEnchant.EXPERIENCE.hasEnchant(hand)) {
            int level = CustomEnchant.EXPERIENCE.getLevel(hand);
            double multiplier = 1.0 + level * 0.5;
            int baseXp = event.getDroppedExp();
            event.setDroppedExp((int) (baseXp * multiplier));
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Utility methods
    // ─────────────────────────────────────────────────────────────────────

    private boolean isLog(Material mat) {
        return mat == Material.LOG || mat == Material.LOG_2;
    }

    private boolean isCrop(Material mat) {
        return mat == Material.CROPS
                || mat == Material.CARROT
                || mat == Material.POTATO
                || mat == Material.NETHER_WARTS
                || mat.name().equals("BEETROOT_BLOCK");
    }

    /**
     * Breaks all connected logs within the given radius using BFS.
     */
    private void breakConnectedLogs(Block origin, int radius, Player player, ItemStack tool) {
        Set<Block> visited = new HashSet<Block>();
        Queue<Block> queue = new LinkedList<Block>();
        queue.add(origin);
        visited.add(origin);

        boolean hasTelekinesis = CustomEnchant.TELEKINESIS.hasEnchant(tool);

        while (!queue.isEmpty()) {
            Block current = queue.poll();
            if (current != origin) {
                Collection<ItemStack> drops = current.getDrops(tool);
                current.setType(Material.AIR);
                for (ItemStack drop : drops) {
                    if (hasTelekinesis) {
                        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(drop);
                        for (ItemStack left : leftover.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), left);
                        }
                    } else {
                        current.getWorld().dropItemNaturally(current.getLocation(), drop);
                    }
                }
            }

            // Check all 26 neighbors (3x3x3 cube minus center)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        Block neighbor = current.getRelative(dx, dy, dz);
                        if (visited.contains(neighbor)) continue;
                        if (!isLog(neighbor.getType())) continue;
                        // Check within radius from origin
                        int distX = Math.abs(neighbor.getX() - origin.getX());
                        int distY = Math.abs(neighbor.getY() - origin.getY());
                        int distZ = Math.abs(neighbor.getZ() - origin.getZ());
                        if (distX <= radius && distY <= radius && distZ <= radius) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }
    }

    /**
     * Converts an ore drop to its smelted form.
     */
    private ItemStack smelt(ItemStack item) {
        if (item == null) return item;
        Material type = item.getType();
        if (type == Material.IRON_ORE) {
            return new ItemStack(Material.IRON_INGOT, item.getAmount());
        } else if (type == Material.GOLD_ORE) {
            return new ItemStack(Material.GOLD_INGOT, item.getAmount());
        } else if (type == Material.COBBLESTONE) {
            return new ItemStack(Material.STONE, item.getAmount());
        } else if (type == Material.SAND) {
            return new ItemStack(Material.GLASS, item.getAmount());
        }
        return item;
    }
}
