package fr.pastequeworld.labyroyal.listener;

import fr.pastequeworld.labyroyal.LabyRoyalPlugin;
import fr.pastequeworld.labyroyal.game.Game;
import fr.pastequeworld.labyroyal.game.GameState;
import fr.pastequeworld.labyroyal.game.PlayerData;
import fr.pastequeworld.labyroyal.util.MessageUtil;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

public class GameListener implements Listener {

    private final LabyRoyalPlugin plugin;

    public GameListener(LabyRoyalPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());
        if (game == null) return;

        // Suppress vanilla death message
        event.setDeathMessage(null);
        event.setKeepInventory(false);
        event.setKeepLevel(false);

        // Determine killer
        Player killer = player.getKiller();

        // Eliminate the player
        game.eliminatePlayer(player, killer, false);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());
        if (game == null) return;

        // Respawn as spectator at their death location
        event.setRespawnLocation(player.getLocation());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());
        if (game == null) return;

        // No damage in waiting/starting/ending phases
        if (game.getState() == GameState.WAITING
                || game.getState() == GameState.STARTING
                || game.getState() == GameState.ENDING) {
            event.setCancelled(true);
            return;
        }

        // Spectators can't take damage
        PlayerData data = game.getPlayers().get(player.getUniqueId());
        if (data != null && !data.isAlive()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        Game game = plugin.getGameManager().getPlayerGame(victim.getUniqueId());
        if (game == null) return;

        // Same game check
        Game attackerGame = plugin.getGameManager().getPlayerGame(attacker.getUniqueId());
        if (attackerGame != game) {
            event.setCancelled(true);
            return;
        }

        // No PvP in waiting/starting/ending
        if (game.getState() == GameState.WAITING
                || game.getState() == GameState.STARTING
                || game.getState() == GameState.ENDING) {
            event.setCancelled(true);
            return;
        }

        // In duo mode, no friendly fire
        if (game.getGameMode() == fr.pastequeworld.labyroyal.game.LabyGameMode.DUO) {
            PlayerData victimData = game.getPlayers().get(victim.getUniqueId());
            PlayerData attackerData = game.getPlayers().get(attacker.getUniqueId());
            if (victimData != null && attackerData != null
                    && victimData.getTeam() != null && attackerData.getTeam() != null
                    && victimData.getTeam().getId() == attackerData.getTeam().getId()) {
                event.setCancelled(true);
                MessageUtil.send(attacker, "&cVous ne pouvez pas attaquer votre coequipier !");
                return;
            }
        }

        // Dead players can't deal damage
        PlayerData attackerData = game.getPlayers().get(attacker.getUniqueId());
        if (attackerData != null && !attackerData.isAlive()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());
        if (game == null) return;

        game.removePlayer(player);
        plugin.getGameManager().removePlayerMapping(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());
        if (game == null) return;

        if (game.getState() == GameState.WAITING || game.getState() == GameState.STARTING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());
        if (game == null) return;

        if (game.getState() == GameState.WAITING || game.getState() == GameState.STARTING) {
            event.setCancelled(true);
            player.setFoodLevel(20);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());
        if (game == null) return;

        // Prevent falling into void
        if (player.getLocation().getY() < 0) {
            if (game.getState() == GameState.PREPARATION || game.getState() == GameState.PVP) {
                PlayerData data = game.getPlayers().get(player.getUniqueId());
                if (data != null && data.isAlive()) {
                    game.eliminatePlayer(player, null, false);
                }
            } else {
                // In lobby, teleport back
                player.teleport(player.getLocation().getWorld().getSpawnLocation());
            }
        }
    }

    @EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());
        if (game == null) return;

        if (game.getState() == GameState.WAITING
                || game.getState() == GameState.STARTING
                || game.getState() == GameState.ENDING) {
            event.setCancelled(true);
            return;
        }

        // During preparation, allow mining except bedrock/obsidian/barrier
        if (game.getState() == GameState.PREPARATION) {
            org.bukkit.Material type = event.getBlock().getType();
            if (type == org.bukkit.Material.BEDROCK
                    || type == org.bukkit.Material.OBSIDIAN
                    || type == org.bukkit.Material.BARRIER) {
                event.setCancelled(true);
                MessageUtil.sendActionBar(player, "&cCe bloc est indestructible !");
            }
        }
        // During PVP, mining fatigue handles it but add extra safety
    }

    @EventHandler
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());
        if (game == null) return;

        if (game.getState() == GameState.WAITING
                || game.getState() == GameState.STARTING
                || game.getState() == GameState.ENDING) {
            event.setCancelled(true);
            return;
        }

        // Prevent placing blocks above maze ceiling
        if (event.getBlock().getY() >= 68) {
            event.setCancelled(true);
            MessageUtil.sendActionBar(player, "&cVous ne pouvez pas placer de blocs ici !");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());
        if (game == null) return;

        if (game.getState() == GameState.WAITING || game.getState() == GameState.STARTING) {
            // Allow chest opening in lobby? No, cancel all
            if (event.getAction().name().contains("RIGHT")) {
                // Allow but prevent placement
            }
        }
    }

    @EventHandler
    public void onCraftItem(org.bukkit.event.inventory.CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());
        if (game == null) return;

        if (game.getState() == GameState.WAITING || game.getState() == GameState.STARTING) {
            event.setCancelled(true);
        }
    }
}
