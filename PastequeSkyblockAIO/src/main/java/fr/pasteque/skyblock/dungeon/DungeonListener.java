package fr.pasteque.skyblock.dungeon;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.gui.GuiHelper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

public class DungeonListener implements Listener {

    private final PastequeSkyblockPlugin plugin;
    private final DungeonManager manager;

    public DungeonListener(PastequeSkyblockPlugin plugin, DungeonManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) return;
        if (manager.getDungeonWorld() == null) return;
        if (!entity.getWorld().equals(manager.getDungeonWorld())) return;

        Player killer = entity.getKiller();
        if (killer == null) return;

        DungeonInstance instance = manager.getPlayerInstance(killer.getUniqueId());
        if (instance == null) return;

        manager.onMobKill(instance, killer);
        GuiHelper.playClick(killer);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        DungeonInstance instance = manager.getPlayerInstance(player.getUniqueId());
        if (instance == null) return;

        int livesLeft = instance.consumeLife(player.getUniqueId());
        if (livesLeft > 0) {
            player.sendMessage(PastequeSkyblockPlugin.color(
                    plugin.getPrefix() + "&cVous etes mort ! &7Vies restantes: &e" + livesLeft));
        } else {
            player.sendMessage(PastequeSkyblockPlugin.color(
                    plugin.getPrefix() + "&c&lELIMINE ! &7Vous n'avez plus de vies."));
            // Check if all players eliminated
            if (instance.hasNoActivePlayers()) {
                manager.failDungeon(instance);
            }
        }

        // Clear drops in dungeon
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.setDroppedExp(0);
        event.getDrops().clear();
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        DungeonInstance instance = manager.getPlayerInstance(player.getUniqueId());
        if (instance == null) return;

        if (instance.getEliminated().contains(player.getUniqueId())) {
            // Eliminated: teleport to spawn
            event.setRespawnLocation(Bukkit.getWorlds().get(0).getSpawnLocation());
            // Remove from instance tracking
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    // Player stays mapped but eliminated — will be cleaned up on completion
                }
            }, 1L);
        } else {
            // Still has lives: respawn in dungeon entrance
            event.setRespawnLocation(instance.getSpawnPoint());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        DungeonInstance instance = manager.getPlayerInstance(uuid);
        if (instance == null) return;

        instance.getEliminated().add(uuid);
        if (instance.hasNoActivePlayers()) {
            manager.failDungeon(instance);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPvP(EntityDamageByEntityEvent event) {
        if (manager.getDungeonWorld() == null) return;
        if (!event.getEntity().getWorld().equals(manager.getDungeonWorld())) return;

        // Prevent PvP in dungeons
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (manager.getDungeonWorld() == null) return;
        if (event.getBlock().getWorld().equals(manager.getDungeonWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (manager.getDungeonWorld() == null) return;
        if (event.getBlock().getWorld().equals(manager.getDungeonWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() == null) return;
        String title = event.getInventory().getTitle();
        if (title == null) return;
        String plain = PastequeSkyblockPlugin.color("&2&lPasteque &5&lDonjons");
        if (!title.equals(plain)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        if (event.getCurrentItem().getType() == Material.STAINED_GLASS_PANE) return;

        GuiHelper.playClick(player);

        int slot = event.getRawSlot();
        DungeonType selected = null;
        if (slot == 11) selected = DungeonType.CRYPT;
        else if (slot == 13) selected = DungeonType.NETHER_FORTRESS;
        else if (slot == 15) selected = DungeonType.ENDER_SANCTUM;

        if (selected == null) return;

        // Check party
        DungeonParty party = manager.findPartyOf(player.getUniqueId());
        if (party == null) {
            party = manager.getOrCreateParty(player.getUniqueId());
        }

        if (!party.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(PastequeSkyblockPlugin.color(
                    plugin.getPrefix() + "&cSeul le leader du groupe peut lancer un donjon."));
            GuiHelper.playDeny(player);
            return;
        }

        if (party.size() < selected.getMinPlayers()) {
            player.sendMessage(PastequeSkyblockPlugin.color(
                    plugin.getPrefix() + "&cIl faut au minimum &e" + selected.getMinPlayers() + " joueurs &cpour ce donjon."));
            GuiHelper.playDeny(player);
            return;
        }

        if (!party.isReady(manager)) {
            player.sendMessage(PastequeSkyblockPlugin.color(
                    plugin.getPrefix() + "&cTous les membres doivent etre en ligne et libres."));
            GuiHelper.playDeny(player);
            return;
        }

        // Close GUI and start
        player.closeInventory();
        java.util.List<Player> members = new java.util.ArrayList<Player>();
        for (UUID mid : party.getMembers()) {
            Player p = Bukkit.getPlayer(mid);
            if (p != null) members.add(p);
        }

        manager.createInstance(selected, members);
    }
}
