package fr.pasteque.skyblock.staff;

import fr.pasteque.skyblock.gui.GuiHelper;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class StaffListener implements Listener {

    private static final String INSPECT_TITLE = "Inspection: ";

    private final PastequeSkyblockPlugin plugin;
    private final StaffModeManager manager;

    public StaffListener(PastequeSkyblockPlugin plugin, StaffModeManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    // ------------------------------------------------------------------
    //  Right-click staff items
    // ------------------------------------------------------------------

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!manager.isInStaffMode(player.getUniqueId())) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack hand = player.getItemInHand();
        if (hand == null || hand.getType() == Material.AIR) {
            return;
        }

        Material type = hand.getType();

        // Compass - teleport to looked-at player
        if (type == Material.COMPASS) {
            event.setCancelled(true);
            handleCompassTeleport(player);
            return;
        }

        // Book - reports panel
        if (type == Material.BOOK) {
            event.setCancelled(true);
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                    + "&aPanneau de rapports - Utilisez &e/pg reports &apour voir les rapports."));
            return;
        }

        // Blaze rod - vanish toggle
        if (type == Material.BLAZE_ROD) {
            event.setCancelled(true);
            boolean currentlyVanished = manager.isVanished(player);
            manager.setVanished(player, !currentlyVanished);
            return;
        }

        // Redstone - exit staff mode
        if (type == Material.REDSTONE) {
            event.setCancelled(true);
            manager.toggleStaffMode(player);
            return;
        }
    }

    // ------------------------------------------------------------------
    //  Right-click entity (skull = inspect, ice = freeze)
    // ------------------------------------------------------------------

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!manager.isInStaffMode(player.getUniqueId())) {
            return;
        }

        Entity rightClicked = event.getRightClicked();
        if (!(rightClicked instanceof Player)) {
            return;
        }

        Player target = (Player) rightClicked;
        ItemStack hand = player.getItemInHand();
        if (hand == null || hand.getType() == Material.AIR) {
            return;
        }

        // Skull - inspect player inventory
        if (hand.getType() == Material.SKULL_ITEM) {
            event.setCancelled(true);
            openInspection(player, target);
            return;
        }

        // Ice - freeze player
        if (hand.getType() == Material.ICE) {
            event.setCancelled(true);
            manager.freezePlayer(player, target);
            return;
        }
    }

    // ------------------------------------------------------------------
    //  Freeze - prevent movement
    // ------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!manager.isFrozen(event.getPlayer().getUniqueId())) {
            return;
        }

        // Allow head rotation but not position change
        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            event.setTo(event.getFrom());
        }
    }

    // ------------------------------------------------------------------
    //  Quit - restore inventory
    // ------------------------------------------------------------------

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (manager.isInStaffMode(player.getUniqueId())) {
            manager.forceDisable(player);
        }
        manager.unfreezePlayer(player.getUniqueId());
    }

    // ------------------------------------------------------------------
    //  Protect staff items
    // ------------------------------------------------------------------

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();

        // Cancel interaction with inspection inventory
        if (event.getInventory() != null && event.getInventory().getTitle() != null
                && event.getInventory().getTitle().startsWith(PastequeSkyblockPlugin.color(INSPECT_TITLE))) {
            event.setCancelled(true);
            return;
        }

        // Protect staff mode inventory
        if (manager.isInStaffMode(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------

    private void handleCompassTeleport(Player player) {
        // Find nearest player in line of sight (within 100 blocks)
        List<Entity> nearby = player.getNearbyEntities(100, 100, 100);
        Player closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : nearby) {
            if (!(entity instanceof Player)) {
                continue;
            }
            Player target = (Player) entity;
            if (target.equals(player)) {
                continue;
            }
            // Check if target is roughly in line of sight direction
            org.bukkit.util.Vector direction = player.getLocation().getDirection().normalize();
            org.bukkit.util.Vector toTarget = target.getLocation().toVector()
                    .subtract(player.getLocation().toVector()).normalize();
            double dot = direction.dot(toTarget);
            if (dot > 0.9) { // Within roughly 25 degree cone
                double dist = player.getLocation().distanceSquared(target.getLocation());
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = target;
                }
            }
        }

        if (closest != null) {
            player.teleport(closest.getLocation());
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                    + "&aTeleporte a &e" + closest.getName() + "&a."));
        } else {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                    + "&cAucun joueur trouve dans votre direction."));
        }
    }

    private void openInspection(Player staff, Player target) {
        Inventory inv = Bukkit.createInventory(null, 54,
                PastequeSkyblockPlugin.color(INSPECT_TITLE + "&e" + target.getName()));

        // Copy target's inventory (slots 0-35)
        ItemStack[] contents = target.getInventory().getContents();
        for (int i = 0; i < contents.length && i < 36; i++) {
            if (contents[i] != null) {
                inv.setItem(i, contents[i].clone());
            }
        }

        // Armor in slots 36-39
        ItemStack[] armor = target.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null) {
                inv.setItem(36 + i, armor[i].clone());
            }
        }

        // Info head at slot 49
        ItemStack info = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        org.bukkit.inventory.meta.ItemMeta meta = info.getItemMeta();
        meta.setDisplayName(PastequeSkyblockPlugin.color("&e" + target.getName()));
        java.util.List<String> lore = new java.util.ArrayList<String>();
        lore.add(PastequeSkyblockPlugin.color("&7Vie: &c" + (int) target.getHealth() + "/" + (int) target.getMaxHealth()));
        lore.add(PastequeSkyblockPlugin.color("&7Nourriture: &6" + target.getFoodLevel() + "/20"));
        lore.add(PastequeSkyblockPlugin.color("&7GameMode: &a" + target.getGameMode().name()));
        lore.add(PastequeSkyblockPlugin.color("&7Monde: &b" + target.getWorld().getName()));
        meta.setLore(lore);
        info.setItemMeta(meta);
        inv.setItem(49, info);

        staff.openInventory(inv);
        GuiHelper.playOpen(staff);
    }
}
