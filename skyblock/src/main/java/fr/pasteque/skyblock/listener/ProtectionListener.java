package fr.pasteque.skyblock.listener;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.model.CoopIsland;
import fr.pasteque.skyblock.model.Island;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class ProtectionListener implements Listener {
    private final PastequeSkyblockPlugin plugin;
    public ProtectionListener(PastequeSkyblockPlugin plugin) { this.plugin = plugin; }
    private boolean isProtected(Location location) { return plugin.getIslandManager().isSkyblockWorld(location) || plugin.getCoopManager().isCoopWorld(location); }

    private boolean canBuild(Player player, Location location) {
        if (player.hasPermission("pastequeskyblock.bypass")) return true;
        if (plugin.getCoopManager().isCoopWorld(location)) return plugin.getCoopManager().canBuild(player.getUniqueId(), location);
        return plugin.getIslandManager().canBuild(player, location);
    }

    private boolean canOpen(Player player, Location location) {
        if (player.hasPermission("pastequeskyblock.bypass")) return true;
        if (plugin.getCoopManager().isCoopWorld(location)) return plugin.getCoopManager().canBuild(player.getUniqueId(), location);
        return plugin.getIslandManager().canOpen(player, location);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (isProtected(event.getBlock().getLocation()) && !canBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            MessageUtil.send(event.getPlayer(), plugin.getPrefix(), "&cTu ne peux pas casser ici.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (isProtected(event.getBlock().getLocation()) && !canBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            MessageUtil.send(event.getPlayer(), plugin.getPrefix(), "&cTu ne peux pas construire ici.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || !isProtected(block.getLocation())) return;
        Material type = block.getType();
        if (type == Material.CHEST || type == Material.TRAPPED_CHEST || type == Material.FURNACE || type == Material.BURNING_FURNACE || type == Material.HOPPER || type == Material.DISPENSER || type == Material.DROPPER || type == Material.BREWING_STAND || type == Material.ENCHANTMENT_TABLE) {
            if (!canOpen(event.getPlayer(), block.getLocation())) { event.setCancelled(true); MessageUtil.send(event.getPlayer(), plugin.getPrefix(), "&cTu n'as pas accès à ce bloc."); }
        }
    }

    @EventHandler(ignoreCancelled = true) public void onBurn(BlockBurnEvent event) { if (isProtected(event.getBlock().getLocation())) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true) public void onIgnite(BlockIgniteEvent event) { if (isProtected(event.getBlock().getLocation())) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true)
    public void onFromTo(BlockFromToEvent event) {
        if (isProtected(event.getBlock().getLocation()) || isProtected(event.getToBlock().getLocation())) {
            Island from = plugin.getIslandManager().getIslandAt(event.getBlock().getLocation()); Island to = plugin.getIslandManager().getIslandAt(event.getToBlock().getLocation());
            CoopIsland cFrom = plugin.getCoopManager().getIslandAt(event.getBlock().getLocation()); CoopIsland cTo = plugin.getCoopManager().getIslandAt(event.getToBlock().getLocation());
            if ((from != null || to != null) && (from == null || to == null || from != to)) event.setCancelled(true);
            if ((cFrom != null || cTo != null) && (cFrom == null || cTo == null || cFrom != cTo)) event.setCancelled(true);
        }
    }
    @EventHandler(ignoreCancelled = true)
    public void onPiston(BlockPistonExtendEvent event) {
        if (!isProtected(event.getBlock().getLocation())) return;
        Island source = plugin.getIslandManager().getIslandAt(event.getBlock().getLocation());
        CoopIsland coopSource = plugin.getCoopManager().getIslandAt(event.getBlock().getLocation());
        for (Block block : event.getBlocks()) {
            Location toLoc = block.getRelative(event.getDirection()).getLocation();
            Island dest = plugin.getIslandManager().getIslandAt(toLoc);
            CoopIsland coopDest = plugin.getCoopManager().getIslandAt(toLoc);
            if (source != null && (dest == null || source != dest)) { event.setCancelled(true); return; }
            if (coopSource != null && (coopDest == null || coopSource != coopDest)) { event.setCancelled(true); return; }
        }
    }
    @EventHandler(ignoreCancelled = true) public void onExplode(EntityExplodeEvent event) { if (event.getLocation() != null && isProtected(event.getLocation())) event.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onVisitorMove(PlayerMoveEvent event) {
        if (event.getTo() == null || !isProtected(event.getTo())) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;
        Player player = event.getPlayer();
        Island island = plugin.getIslandManager().getIslandAt(event.getTo());
        if (island != null) {
            if (island.getBanned().contains(player.getUniqueId())) { player.teleport(plugin.getWorldManager().getServerSpawn()); MessageUtil.send(player, plugin.getPrefix(), "&cTu es banni de cette île."); return; }
            if (!plugin.getIslandManager().canVisit(player, island) && !player.hasPermission("pastequeskyblock.bypass")) { player.teleport(plugin.getWorldManager().getServerSpawn()); MessageUtil.send(player, plugin.getPrefix(), "&cCette île est privée."); }
            return;
        }
        CoopIsland coop = plugin.getCoopManager().getIslandAt(event.getTo());
        if (coop != null && !coop.isMember(player.getUniqueId()) && !player.hasPermission("pastequeskyblock.bypass")) {
            player.teleport(plugin.getWorldManager().getServerSpawn());
            MessageUtil.send(player, plugin.getPrefix(), "&cCette île coop n'est pas accessible.");
        }
    }
}
