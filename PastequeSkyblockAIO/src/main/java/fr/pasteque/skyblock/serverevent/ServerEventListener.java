package fr.pasteque.skyblock.serverevent;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ServerEventListener implements Listener {
    private final PastequeSkyblockPlugin plugin;
    private final EventManager eventManager;

    public ServerEventListener(PastequeSkyblockPlugin plugin, EventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (e.getBlock().getType() == Material.OBSIDIAN) {
            ServerEvent sv = eventManager.get("meteor");
            if (sv instanceof MeteorShowerEvent) {
                if (((MeteorShowerEvent) sv).onMeteorBreak(e.getPlayer(), e.getBlock().getLocation())) {
                    e.setCancelled(true);
                    e.getBlock().setType(Material.AIR);
                }
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        if (e.getClickedBlock().getType() != Material.CHEST) return;
        ServerEvent sv = eventManager.get("treasure");
        if (sv instanceof TreasureHuntEvent) {
            ((TreasureHuntEvent) sv).onChestOpen(e.getPlayer(), e.getClickedBlock().getLocation());
        }
    }
}
