package fr.pastequeworld.bedwars.listener;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.game.GameMode;
import fr.pastequeworld.bedwars.player.BedWarsPlayer;
import fr.pastequeworld.bedwars.player.PlayerState;
import fr.pastequeworld.bedwars.util.ColorUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Lobby BedWars : interactions + protection totale.
 *
 * Protections :
 *   - Pas de casse / pose de blocs
 *   - Pas de degats (fall, fire, void, PVP, suffocation...)
 *   - Pas de faim
 *   - Pas de pickup / drop
 *   - Pas d'explosion, pas de propagation de feu, pas de decompose de feuilles
 *   - Pas de spawn de mobs naturel
 *   - Pas de meteo
 *   - Pas de cliquage sur NPC / frames / stands
 *
 * Interactions :
 *   - NETHER_STAR -> menu de modes
 *   - BED -> retour au hub (BungeeCord)
 *   - Clic droit sur NPC villageois -> rejoint la queue du mode associe
 *   - Clic sur l'inventaire du menu -> rejoint le mode selectionne
 */
public class LobbyListener implements Listener {

    private final BedWarsPlugin plugin;

    public LobbyListener(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isLobbyWorld(World world) {
        if (world == null) return false;
        String lobby = plugin.getConfigManager().getLobbyWorldName();
        return lobby != null && lobby.equals(world.getName());
    }

    private boolean isInLobby(Player player) {
        if (player == null) return false;
        if (isLobbyWorld(player.getWorld())) return true;
        BedWarsPlayer bw = plugin.getPlayerDataManager().get(player);
        return bw != null && (bw.getState() == PlayerState.LOBBY || bw.getArena() == null);
    }

    // === Interactions ===

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK
                && event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (!isInLobby(player)) return;

        event.setCancelled(true);
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return;

        switch (item.getType()) {
            case NETHER_STAR:
                plugin.getLobbyManager().openModeSelector(player);
                break;
            case PAPER:
                player.sendMessage(ColorUtil.color("&7Vos statistiques arrivent prochainement !"));
                break;
            case CHEST:
                player.sendMessage(ColorUtil.color("&7La boutique cosmetique arrive prochainement !"));
                break;
            case BED:
                connectToHub(player);
                break;
            default:
                break;
        }
    }

    private void connectToHub(Player player) {
        try {
            java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream out = new java.io.DataOutputStream(b);
            out.writeUTF("Connect");
            out.writeUTF("hub");
            player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
            player.sendMessage(ColorUtil.color("&aReconnexion au hub..."));
        } catch (Exception ignored) {}
    }

    @EventHandler
    public void onNPCInteract(PlayerInteractEntityEvent event) {
        if (!plugin.getNpcManager().isNPC(event.getRightClicked())) {
            if (isInLobby(event.getPlayer())) event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        GameMode mode = plugin.getNpcManager().getModeOf(event.getRightClicked());
        if (mode == null) return;
        plugin.getQueueManager().join(event.getPlayer(), mode);
    }

    @EventHandler
    public void onNPCInteractAt(PlayerInteractAtEntityEvent event) {
        if (isInLobby(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (event.getView().getTitle() == null) return;
        String title = event.getView().getTitle();
        if (!title.contains("Choisissez un mode")) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        if (slot == 11) plugin.getQueueManager().join(p, GameMode.SOLO);
        else if (slot == 13) plugin.getQueueManager().join(p, GameMode.DUO);
        else if (slot == 15) plugin.getQueueManager().join(p, GameMode.TEAMS);
        p.closeInventory();
    }

    // === Protections blocs ===

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLobbyBreak(BlockBreakEvent event) {
        if (isLobbyWorld(event.getBlock().getWorld()) || isInLobby(event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLobbyPlace(BlockPlaceEvent event) {
        if (isLobbyWorld(event.getBlock().getWorld()) || isInLobby(event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (isInLobby(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (isInLobby(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onBurn(BlockBurnEvent event) {
        if (isLobbyWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler
    public void onIgnite(BlockIgniteEvent event) {
        if (isLobbyWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler
    public void onSpread(BlockSpreadEvent event) {
        if (isLobbyWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler
    public void onForm(BlockFormEvent event) {
        if (isLobbyWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler
    public void onFade(BlockFadeEvent event) {
        if (isLobbyWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (isLobbyWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    // === Protections degats / meteo / mobs ===

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            if (isLobbyWorld(event.getEntity().getWorld())) event.setCancelled(true);
            return;
        }
        Player p = (Player) event.getEntity();
        if (isInLobby(p)) { event.setCancelled(true); return; }
        BedWarsPlayer bw = plugin.getPlayerDataManager().get(p);
        if (bw != null && bw.getState() == PlayerState.QUEUEING) event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (isLobbyWorld(event.getEntity().getWorld())) event.setCancelled(true);
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player p = (Player) event.getEntity();
        if (isInLobby(p)) {
            event.setCancelled(true);
            p.setFoodLevel(20);
            p.setSaturation(20);
        }
    }

    @EventHandler
    public void onRegen(EntityRegainHealthEvent event) {
        if (isLobbyWorld(event.getEntity().getWorld())) {
            // on laisse passer pour un effet "heal auto", mais pas d'interet car pas de degats
        }
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        if (isInLobby(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isInLobby(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        if (isInLobby(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClickLobby(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();
        if (!isInLobby(p)) return;
        if (event.getView().getTitle() != null && event.getView().getTitle().contains("Choisissez un mode")) return;
        if (event.getClickedInventory() != null && event.getClickedInventory().equals(p.getInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        if (isLobbyWorld(event.getLocation().getWorld())) {
            event.blockList().clear();
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPrime(ExplosionPrimeEvent event) {
        if (isLobbyWorld(event.getEntity().getWorld())) event.setCancelled(true);
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!isLobbyWorld(event.getLocation().getWorld())) return;
        CreatureSpawnEvent.SpawnReason r = event.getSpawnReason();
        if (r == CreatureSpawnEvent.SpawnReason.NATURAL
                || r == CreatureSpawnEvent.SpawnReason.DEFAULT
                || r == CreatureSpawnEvent.SpawnReason.JOCKEY
                || r == CreatureSpawnEvent.SpawnReason.CHUNK_GEN
                || r == CreatureSpawnEvent.SpawnReason.VILLAGE_DEFENSE
                || r == CreatureSpawnEvent.SpawnReason.VILLAGE_INVASION
                || r == CreatureSpawnEvent.SpawnReason.SLIME_SPLIT) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTarget(EntityTargetEvent event) {
        if (isLobbyWorld(event.getEntity().getWorld())) event.setCancelled(true);
    }

    @EventHandler
    public void onProjectile(ProjectileLaunchEvent event) {
        if (isLobbyWorld(event.getEntity().getWorld()) && event.getEntity().getShooter() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onVehicleDamage(VehicleDamageEvent event) {
        if (isLobbyWorld(event.getVehicle().getWorld())) event.setCancelled(true);
    }

    @EventHandler
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        if (isLobbyWorld(event.getVehicle().getWorld())) event.setCancelled(true);
    }

    @EventHandler
    public void onHangingBreak(HangingBreakEvent event) {
        if (isLobbyWorld(event.getEntity().getWorld())) event.setCancelled(true);
    }

    @EventHandler
    public void onArmorStand(PlayerArmorStandManipulateEvent event) {
        if (isInLobby(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onWeather(WeatherChangeEvent event) {
        if (isLobbyWorld(event.getWorld()) && event.toWeatherState()) event.setCancelled(true);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World w = event.getWorld();
        if (isLobbyWorld(w)) {
            w.setStorm(false);
            w.setThundering(false);
            w.setGameRuleValue("doDaylightCycle", "false");
            w.setGameRuleValue("doWeatherCycle", "false");
            w.setGameRuleValue("doMobSpawning", "false");
            w.setGameRuleValue("doFireTick", "false");
            w.setGameRuleValue("mobGriefing", "false");
            w.setGameRuleValue("naturalRegeneration", "true");
            w.setGameRuleValue("showDeathMessages", "false");
            w.setGameRuleValue("announceAdvancements", "false");
            w.setTime(6000L);
        }
    }
}
