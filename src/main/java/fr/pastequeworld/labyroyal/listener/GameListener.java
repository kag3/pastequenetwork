package fr.pastequeworld.labyroyal.listener;

import fr.pastequeworld.labyroyal.LabyRoyalPlugin;
import fr.pastequeworld.labyroyal.game.Game;
import fr.pastequeworld.labyroyal.game.GameState;
import fr.pastequeworld.labyroyal.game.LabyGameMode;
import fr.pastequeworld.labyroyal.game.PlayerData;
import fr.pastequeworld.labyroyal.util.MessageUtil;

import org.bukkit.Bukkit;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;

public class GameListener implements Listener {

    private final LabyRoyalPlugin plugin;

    public GameListener(LabyRoyalPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        final Player player = event.getEntity();
        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());
        if (game == null) return;

        event.setDeathMessage(null);
        event.setKeepInventory(false);
        event.setKeepLevel(false);

        Player killer = player.getKiller();
        game.eliminatePlayer(player, killer, false);

        // Skip death screen - force respawn on next tick
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline() && player.isDead()) {
                    player.spigot().respawn();
                }
            }
        }, 1L);
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

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        final Player player = event.getPlayer();

        // Hide all in-game players from this new player (and vice versa)
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                // If this player is not in any game, hide all game players
                if (plugin.getGameManager().getPlayerGame(player.getUniqueId()) != null) return;

                for (Game game : plugin.getGameManager().getGames()) {
                    for (Player gamePlayer : game.getOnlinePlayers()) {
                        player.hidePlayer(gamePlayer);
                        gamePlayer.hidePlayer(player);
                    }
                }
            }
        }, 2L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
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

        org.bukkit.block.Block block = event.getBlock();
        int bx = block.getX(), by = block.getY(), bz = block.getZ();

        if (game.getState() == GameState.PVP) {
            // Only player-placed blocks can be broken during PVP
            if (game.isPlayerPlaced(bx, by, bz)) {
                game.unmarkPlayerPlaced(bx, by, bz);
                // Allow break
            } else {
                event.setCancelled(true);
                MessageUtil.sendActionBar(player, "&cVous ne pouvez casser que les blocs pos\u00e9s par des joueurs !");
            }
            return;
        }

        if (game.getState() == GameState.PREPARATION) {
            Material type = block.getType();
            if (type == Material.BEDROCK
                    || type == Material.OBSIDIAN
                    || type == Material.BARRIER
                    || type == Material.SEA_LANTERN) {
                event.setCancelled(true);
                MessageUtil.sendActionBar(player, "&cCe bloc est indestructible !");
                return;
            }

            // Duel mode: auto-smelt ores (UHC Fast)
            if (game.getGameMode() == LabyGameMode.DUEL) {
                Material smelted = getSmeltedDrop(type);
                if (smelted != null) {
                    event.setCancelled(true);
                    block.setType(Material.AIR);
                    block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5),
                            new org.bukkit.inventory.ItemStack(smelted));
                    // Give XP like normal mining
                    player.giveExp(1);
                }
            }
        }
    }

    /**
     * Returns the smelted version of an ore, or null if not an ore.
     */
    private Material getSmeltedDrop(Material type) {
        switch (type) {
            case IRON_ORE: return Material.IRON_INGOT;
            case GOLD_ORE: return Material.GOLD_INGOT;
            default: return null;
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

        int maxBuildY = plugin.getConfig().getInt("maze.base-y", 60)
                + plugin.getConfig().getInt("maze.wall-height", 12);
        if (event.getBlock().getY() >= maxBuildY) {
            event.setCancelled(true);
            MessageUtil.sendActionBar(player, "&cVous ne pouvez pas placer de blocs ici !");
            return;
        }

        // Track player-placed blocks so they can be broken during PVP
        org.bukkit.block.Block block = event.getBlock();
        game.markPlayerPlaced(block.getX(), block.getY(), block.getZ());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());
        if (game == null) return;

        // Bed item: right-click to return to hub (only in queue)
        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            if (player.getInventory().getItemInMainHand() != null
                    && player.getInventory().getItemInMainHand().getType() == Material.BED) {
                event.setCancelled(true);
                if (game.getState() == GameState.WAITING || game.getState() == GameState.STARTING) {
                    game.removePlayer(player);
                    plugin.getGameManager().removePlayerMapping(player.getUniqueId());
                }
                return;
            }
        }

        // Block chest/container opening during countdown freeze
        if (game.isCountdownFrozen()) {
            if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
                    && event.getClickedBlock() != null) {
                Material type = event.getClickedBlock().getType();
                if (type == Material.CHEST || type == Material.TRAPPED_CHEST
                        || type == Material.ENDER_CHEST || type == Material.FURNACE
                        || type == Material.WORKBENCH || type == Material.ENCHANTMENT_TABLE
                        || type == Material.ANVIL || type == Material.HOPPER
                        || type == Material.DROPPER || type == Material.DISPENSER) {
                    event.setCancelled(true);
                    MessageUtil.sendActionBar(player, "&cAttendez le signal de d\u00e9part !");
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());
        if (game == null) return;

        // Block moving the bed item in WAITING/STARTING
        if (game.getState() == GameState.WAITING || game.getState() == GameState.STARTING) {
            event.setCancelled(true);
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
