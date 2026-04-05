package fr.pasteque.skyblock.listener.arena;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.arena.ArenaWorldService;
import fr.pasteque.skyblock.arena.SafeZoneService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ArenaProtectionListener implements Listener {

    private final PastequeSkyblockPlugin plugin;
    private final ArenaWorldService arenaWorldService;
    private final SafeZoneService safeZoneService;
    private final Set<String> blockedCommands = new HashSet<String>(Arrays.asList("/shop", "/sell"));

    public ArenaProtectionListener(PastequeSkyblockPlugin plugin, ArenaWorldService arenaWorldService, SafeZoneService safeZoneService) {
        this.plugin = plugin;
        this.arenaWorldService = arenaWorldService;
        this.safeZoneService = safeZoneService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!arenaWorldService.isArenaWorld(event.getBlock().getWorld())) {
            return;
        }
        if (isPrivileged(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(plugin.color(plugin.getPrefix() + plugin.getConfig().getString("messages.no-build", "&fLa construction est fermee dans l'arene.")));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!arenaWorldService.isArenaWorld(event.getBlock().getWorld())) {
            return;
        }
        if (isPrivileged(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(plugin.color(plugin.getPrefix() + plugin.getConfig().getString("messages.no-break", "&fLa casse est fermee dans l'arene.")));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null || !arenaWorldService.isArenaWorld(event.getClickedBlock().getWorld())) {
            return;
        }
        if (isPrivileged(event.getPlayer())) {
            return;
        }
        Block block = event.getClickedBlock();
        if (isAllowedInteraction(block.getType())) {
            return;
        }
        if (safeZoneService.isSafe(block.getLocation()) && event.getPlayer().getItemInHand() != null && event.getPlayer().getItemInHand().getType() != Material.AIR) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!blockedCommands.contains(event.getMessage().toLowerCase())) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(plugin.color(plugin.getPrefix() + plugin.getConfig().getString("messages.no-command", "&fCette commande est desactivee ici. Passez au hub pour l'utiliser.")));
    }

    private boolean isAllowedInteraction(Material material) {
        String name = material.name();
        return name.contains("DOOR") || name.contains("BUTTON") || name.contains("PRESSURE_PLATE") || name.contains("FENCE_GATE") || name.contains("TRAP_DOOR");
    }

    private boolean isPrivileged(Player player) {
        return player.hasPermission("pastequearena.admin") || player.hasPermission("pastequearena.bypass");
    }
}
