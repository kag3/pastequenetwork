package fr.pasteque.skyblock.npc;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;

public class NpcListener implements Listener {

    private final PastequeSkyblockPlugin plugin;
    private final NpcManager npcManager;

    public NpcListener(PastequeSkyblockPlugin plugin, NpcManager npcManager) {
        this.plugin = plugin;
        this.npcManager = npcManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEntityEvent e) {
        if (!npcManager.isNpc(e.getRightClicked())) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        String action = npcManager.findActionByEntity(e.getRightClicked());
        if (action == null || action.isEmpty()) return;
        executeAction(p, action);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent e) {
        if (npcManager.isNpc(e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPvp(EntityDamageByEntityEvent e) {
        if (npcManager.isNpc(e.getEntity())) {
            e.setCancelled(true);
            // Treat left-click as right-click on NPCs (convenience)
            if (e.getDamager() instanceof Player) {
                String action = npcManager.findActionByEntity(e.getEntity());
                if (action != null && !action.isEmpty()) {
                    executeAction((Player) e.getDamager(), action);
                }
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        npcManager.respawnInChunk(e.getChunk());
    }

    private void executeAction(Player player, String action) {
        if (action.startsWith("command:")) {
            String cmd = action.substring("command:".length()).replace("{player}", player.getName());
            Bukkit.dispatchCommand(player, cmd);
        } else if (action.startsWith("console:")) {
            String cmd = action.substring("console:".length()).replace("{player}", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        } else if (action.startsWith("msg:")) {
            player.sendMessage(PastequeSkyblockPlugin.color(action.substring("msg:".length())));
        }
    }
}
