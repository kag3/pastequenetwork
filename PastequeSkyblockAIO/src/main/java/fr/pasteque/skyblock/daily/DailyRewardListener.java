package fr.pasteque.skyblock.daily;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;

public class DailyRewardListener implements Listener {

    private final PastequeSkyblockPlugin plugin;
    private final DailyRewardManager manager;

    public DailyRewardListener(PastequeSkyblockPlugin plugin, DailyRewardManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getItemInHand() == null) return;
        if (!manager.isDailyChestItem(event.getItemInHand())) return;
        // Register the block as a daily chest
        Block placed = event.getBlockPlaced();
        if (placed.getType() != Material.ENDER_CHEST) return;
        manager.registerChest(placed.getLocation());
        event.getPlayer().sendMessage(PastequeSkyblockPlugin.color(
                plugin.getPrefix() + "&aCoffre quotidien configure ! &7Hologramme installe au-dessus."));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block b = event.getClickedBlock();
        if (b == null || b.getType() != Material.ENDER_CHEST) return;
        if (!manager.isDailyChestBlock(b.getLocation())) return;
        event.setCancelled(true);
        DailyRewardGui.open(event.getPlayer(), manager);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block b = event.getBlock();
        if (b.getType() != Material.ENDER_CHEST) return;
        if (!manager.isDailyChestBlock(b.getLocation())) return;
        // Only admins can break a daily chest
        if (!event.getPlayer().hasPermission("pastequeskyblock.admin") && !event.getPlayer().isOp()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(PastequeSkyblockPlugin.color("&cVous ne pouvez pas casser un coffre quotidien."));
            return;
        }
        manager.unregisterChest(b.getLocation());
        event.getPlayer().sendMessage(PastequeSkyblockPlugin.color(
                plugin.getPrefix() + "&aCoffre quotidien retire."));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGuiClick(InventoryClickEvent event) {
        if (event.getInventory() == null) return;
        String title = event.getInventory().getTitle();
        if (title == null || !title.equals(DailyRewardGui.TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();
        if (event.getCurrentItem() == null) return;
        // Clicking a GOLD_INGOT slot = claim today
        if (event.getCurrentItem().getType() == Material.GOLD_INGOT) {
            int day = manager.claim(p);
            if (day == 0) {
                p.sendMessage(PastequeSkyblockPlugin.color("&cVous avez deja reclame votre recompense aujourd'hui !"));
            }
            p.closeInventory();
            return;
        }
        if (event.getCurrentItem().getType() == Material.REDSTONE_BLOCK) {
            p.closeInventory();
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        // Respawn holograms in loaded chunks
        manager.respawnAllHolograms();
    }
}
