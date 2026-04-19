package fr.pastequeworld.bedwars.listener;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.game.GameMode;
import fr.pastequeworld.bedwars.lobby.NPCManager;
import fr.pastequeworld.bedwars.player.BedWarsPlayer;
import fr.pastequeworld.bedwars.player.PlayerState;
import fr.pastequeworld.bedwars.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Interactions du lobby :
 *   - clic droit sur le selector -> menu de modes
 *   - clic droit sur le boussole retour -> message (Bungee)
 *   - clic droit sur NPC -> rejoint la queue du mode
 *   - Protection du lobby (pas de degats, pas de faim, pas de casse de blocks)
 */
public class LobbyListener implements Listener {

    private final BedWarsPlugin plugin;

    public LobbyListener(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isInLobby(Player player) {
        BedWarsPlayer bw = plugin.getPlayerDataManager().get(player);
        return bw != null && (bw.getState() == PlayerState.LOBBY || bw.getArena() == null);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK
                && event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return;

        if (!isInLobby(player)) return;

        event.setCancelled(true);

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
                // Reconnexion Bungee vers le hub principal
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
        if (!plugin.getNpcManager().isNPC(event.getRightClicked())) return;
        event.setCancelled(true);
        GameMode mode = plugin.getNpcManager().getModeOf(event.getRightClicked());
        if (mode == null) return;
        plugin.getQueueManager().join(event.getPlayer(), mode);
    }

    @EventHandler
    public void onNPCInteractAt(PlayerInteractAtEntityEvent event) {
        if (!plugin.getNpcManager().isNPC(event.getRightClicked())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (event.getView().getTitle() == null) return;
        String title = event.getView().getTitle();
        if (!title.contains("Choisissez un mode")) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;
        int slot = event.getRawSlot();
        if (slot == 11) plugin.getQueueManager().join(p, GameMode.SOLO);
        else if (slot == 13) plugin.getQueueManager().join(p, GameMode.DUO);
        else if (slot == 15) plugin.getQueueManager().join(p, GameMode.TEAMS);
        p.closeInventory();
    }

    @EventHandler
    public void onLobbyBreak(BlockBreakEvent event) {
        if (isInLobby(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onLobbyPlace(BlockPlaceEvent event) {
        if (isInLobby(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player p = (Player) event.getEntity();
        if (isInLobby(p)) event.setCancelled(true);
        BedWarsPlayer bw = plugin.getPlayerDataManager().get(p);
        if (bw != null && bw.getState() == PlayerState.QUEUEING) event.setCancelled(true);
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player p = (Player) event.getEntity();
        if (isInLobby(p)) event.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isInLobby(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        if (isInLobby(event.getPlayer())) event.setCancelled(true);
    }
}
