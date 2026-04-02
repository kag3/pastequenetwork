package fr.pastequeworld.labyroyal.listener;

import fr.pastequeworld.labyroyal.LabyRoyalPlugin;
import fr.pastequeworld.labyroyal.game.Game;
import fr.pastequeworld.labyroyal.game.GameState;
import fr.pastequeworld.labyroyal.game.LabyGameMode;
import fr.pastequeworld.labyroyal.game.PlayerData;
import fr.pastequeworld.labyroyal.util.MessageUtil;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
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

        event.setDeathMessage(null);
        event.setKeepInventory(false);
        event.setKeepLevel(false);

        Player killer = player.getKiller();
        game.eliminatePlayer(player, killer, false);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());
        if (game == null) return;

        event.setRespawnLocation(player.getLocation());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());
        if (game == null) return;

        if (game.getState() == GameState.WAITING
                || game.getState() == GameState.STARTING
                || game.getState() == GameState.ENDING) {
            event.setCancelled(true);
            return;
        }

        PlayerData data = game.getPlayers().get(player.getUniqueId());
        if (data != null && !data.isAlive()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof Player)) return;
        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        Game game = plugin.getGameManager().getPlayerGame(victim.getUniqueId());
        if (game == null) return;

        Game attackerGame = plugin.getGameManager().getPlayerGame(attacker.getUniqueId());
        if (attackerGame != game) {
            event.setCancelled(true);
            return;
        }

        if (game.getState() == GameState.WAITING
                || game.getState() == GameState.STARTING
                || game.getState() == GameState.ENDING) {
            event.setCancelled(true);
            return;
        }

        if (game.getGameMode() == LabyGameMode.DUO) {
            PlayerData victimData = game.getPlayers().get(victim.getUniqueId());
            PlayerData attackerData = game.getPlayers().get(attacker.getUniqueId());
            if (victimData != null && attackerData != null
                    && victimData.getTeam() != null && attackerData.getTeam() != null
                    && victimData.getTeam().getId() == attackerData.getTeam().getId()) {
                event.setCancelled(true);
                MessageUtil.send(attacker, "&cVous ne pouvez pas attaquer votre co\u00e9quipier !");
                return;
            }
        }

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
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

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

        if (player.getLocation().getY() < 0) {
            if (game.getState() == GameState.PREPARATION || game.getState() == GameState.PVP) {
                PlayerData data = game.getPlayers().get(player.getUniqueId());
                if (data != null && data.isAlive()) {
                    game.eliminatePlayer(player, null, false);
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());
        if (game == null) return;

        if (game.getState() == GameState.WAITING
                || game.getState() == GameState.STARTING
                || game.getState() == GameState.ENDING) {
            event.setCancelled(true);
            return;
        }

        if (game.getState() == GameState.PREPARATION) {
            Material type = event.getBlock().getType();
            if (type == Material.BEDROCK
                    || type == Material.OBSIDIAN
                    || type == Material.BARRIER) {
                event.setCancelled(true);
                MessageUtil.sendActionBar(player, "&cCe bloc est indestructible !");
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());
        if (game == null) return;

        if (game.getState() == GameState.WAITING
                || game.getState() == GameState.STARTING
                || game.getState() == GameState.ENDING) {
            event.setCancelled(true);
            return;
        }

        if (event.getBlock().getY() >= 68) {
            event.setCancelled(true);
            MessageUtil.sendActionBar(player, "&cVous ne pouvez pas placer de blocs ici !");
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());
        if (game == null) return;

        if (game.getState() == GameState.WAITING || game.getState() == GameState.STARTING) {
            event.setCancelled(true);
        }
    }
}
