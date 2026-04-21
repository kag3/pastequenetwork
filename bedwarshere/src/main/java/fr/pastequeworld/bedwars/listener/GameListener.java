package fr.pastequeworld.bedwars.listener;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.game.Arena;
import fr.pastequeworld.bedwars.game.GameState;
import fr.pastequeworld.bedwars.player.BedWarsPlayer;
import fr.pastequeworld.bedwars.player.PlayerState;
import fr.pastequeworld.bedwars.team.Team;
import fr.pastequeworld.bedwars.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Listeners de gameplay : bed destroy, kill, void death, block place protection,
 * fireballs, TNT, bridge eggs, etc.
 */
public class GameListener implements Listener {

    private final BedWarsPlugin plugin;
    private final Set<Location> placedBlocks = new HashSet<Location>(); // evite de casser les blocs de map
    private final Set<UUID> bridgeEggs = new HashSet<UUID>();

    public GameListener(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.getArenaManager().getArenaOfPlayer(player);
        if (arena == null) return;
        if (arena.getState() != GameState.RUNNING) return;
        if (player.getLocation().getY() < plugin.getConfigManager().getVoidDeathY()) {
            // Void death
            BedWarsPlayer bw = plugin.getPlayerDataManager().get(player);
            if (bw == null || !bw.isPlayingOrRespawning()) return;
            Player killer = null;
            arena.handleDeath(player, killer, null);
            player.teleport(bw.getTeam() != null && bw.getTeam().getSpawnLocation() != null
                    ? bw.getTeam().getSpawnLocation() : arena.getQueueSpawn());
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Arena arena = plugin.getArenaManager().getArenaOfPlayer(victim);
        if (arena == null) return;
        Player killer = victim.getKiller();
        arena.handleDeath(victim, killer, event);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.getArenaManager().getArenaOfPlayer(player);
        if (arena == null) return;
        BedWarsPlayer bw = plugin.getPlayerDataManager().get(player);
        if (bw == null) return;
        if (bw.getState() == PlayerState.SPECTATING) {
            event.setRespawnLocation(arena.getQueueSpawn());
        } else if (bw.getTeam() != null) {
            event.setRespawnLocation(arena.getQueueSpawn());
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.getArenaManager().getArenaOfPlayer(player);
        if (arena == null) return;

        Block block = event.getBlock();
        Material type = block.getType();

        // Destruction du lit
        if (type == Material.BED_BLOCK || type == Material.BED) {
            Team team = arena.getTeamAt(block.getLocation());
            if (team == null) {
                event.setCancelled(true);
                return;
            }
            // Peut pas casser son propre lit
            Team myTeam = arena.getTeamOf(player);
            if (myTeam == team) {
                event.setCancelled(true);
                player.sendMessage(ColorUtil.color("&cVous ne pouvez pas casser votre propre lit !"));
                return;
            }
            if (!team.isBedAlive()) {
                event.setCancelled(true);
                return;
            }
            arena.onBedBroken(team, player);
            // Force le bloc a disparaitre sans drop
            event.setCancelled(true);
            block.setType(Material.AIR);
            // Casser aussi la 2eme partie du lit (bed a deux blocs)
            for (org.bukkit.block.BlockFace face : new org.bukkit.block.BlockFace[]{
                    org.bukkit.block.BlockFace.NORTH, org.bukkit.block.BlockFace.SOUTH,
                    org.bukkit.block.BlockFace.EAST, org.bukkit.block.BlockFace.WEST}) {
                org.bukkit.block.Block rel = block.getRelative(face);
                if (rel.getType() == Material.BED_BLOCK || rel.getType() == Material.BED) {
                    rel.setType(Material.AIR);
                }
            }
            return;
        }

        // Ne peut casser que ce qu'il a place
        if (!placedBlocks.contains(block.getLocation())) {
            event.setCancelled(true);
        } else {
            placedBlocks.remove(block.getLocation());
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.getArenaManager().getArenaOfPlayer(player);
        if (arena == null) return;
        if (arena.getState() != GameState.RUNNING) {
            event.setCancelled(true);
            return;
        }
        placedBlocks.add(event.getBlock().getLocation());
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        Arena arena = plugin.getArenaManager().getArenaByWorld(event.getLocation().getWorld());
        if (arena == null) return;
        // Pas de degats aux lits
        Iterator<Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            Block b = it.next();
            if (b.getType() == Material.BED_BLOCK || b.getType() == Material.BED) {
                it.remove();
                continue;
            }
            if (!placedBlocks.contains(b.getLocation())) {
                it.remove();
            } else {
                placedBlocks.remove(b.getLocation());
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Arena arena = plugin.getArenaManager().getArenaByWorld(event.getEntity().getWorld());
        if (arena == null) return;
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();

        Player damager = null;
        if (event.getDamager() instanceof Player) damager = (Player) event.getDamager();
        else if (event.getDamager() instanceof Projectile) {
            Projectile proj = (Projectile) event.getDamager();
            if (proj.getShooter() instanceof Player) damager = (Player) proj.getShooter();
        } else if (event.getDamager() instanceof TNTPrimed) {
            // pas d'attribution du damager directement
        }

        if (damager == null || damager == victim) return;

        BedWarsPlayer bwV = plugin.getPlayerDataManager().get(victim);
        BedWarsPlayer bwD = plugin.getPlayerDataManager().get(damager);
        if (bwV == null || bwD == null) return;
        if (bwV.getTeam() != null && bwV.getTeam() == bwD.getTeam()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        Arena arena = plugin.getArenaManager().getArenaOfPlayer(player);
        if (arena == null || arena.getState() != GameState.RUNNING) return;

        org.bukkit.inventory.ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null) return;
        Material type = item.getType();

        if (type == Material.FIREBALL) {
            event.setCancelled(true);
            if (player.getInventory().getItemInMainHand().getAmount() > 1) {
                player.getInventory().getItemInMainHand().setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
            Fireball fb = player.launchProjectile(Fireball.class);
            fb.setIsIncendiary(false);
            fb.setYield(3f);
            Vector dir = player.getLocation().getDirection().multiply(0.9);
            fb.setVelocity(dir);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GHAST_SHOOT, 1f, 1f);
        } else if (type == Material.EGG) {
            // Bridge egg
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                    && item.getItemMeta().getDisplayName().contains("pont")) {
                // laissez l'action normale de lancer l'egg
                org.bukkit.entity.Egg egg = player.launchProjectile(org.bukkit.entity.Egg.class);
                bridgeEggs.add(egg.getUniqueId());
                // marquer la couleur de l'equipe pour le pont
                event.setCancelled(true);
                if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
                else player.getInventory().setItemInMainHand(null);
            }
        }
    }

    @EventHandler
    public void onProjectile(ProjectileLaunchEvent event) {
        // Ici on pourrait attacher un tracker a un bridge egg pour poser de la laine
        // a chaque tick le long de la trajectoire. Implementation simplifiee :
        // dans un premier temps, les eggs sont remplaces par une perle de l'ender like.
    }
}
