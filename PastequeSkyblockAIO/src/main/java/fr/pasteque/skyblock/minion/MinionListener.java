package fr.pasteque.skyblock.minion;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.minion.model.MinionType;
import fr.pasteque.skyblock.minion.model.PlacedMinion;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.List;

public class MinionListener implements Listener {

    private final MinionManager minionManager;

    public MinionListener(MinionManager minionManager) {
        this.minionManager = minionManager;
    }

    // ── Right-click villager minion ───────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager)) {
            return;
        }
        Villager villager = (Villager) event.getRightClicked();
        if (villager.getCustomName() == null || !villager.getCustomName().contains(MinionManager.MINION_TAG)) {
            return;
        }
        event.setCancelled(true);

        Player player = event.getPlayer();
        PlacedMinion minion = minionManager.getMinionAt(villager.getLocation());
        if (minion == null) {
            player.sendMessage(PastequeSkyblockPlugin.color("&cMinion introuvable."));
            return;
        }

        // Only owner can interact
        if (!minion.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(PastequeSkyblockPlugin.color("&cCe minion ne vous appartient pas !"));
            return;
        }

        minionManager.openMinionGui(player, minion);
    }

    // ── GUI interactions ─────────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() == null || !(event.getWhoClicked() instanceof Player)) {
            return;
        }
        String title = event.getInventory().getTitle();
        if (!title.startsWith(MinionManager.GUI_TITLE_PREFIX)) {
            return;
        }
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        // Find the minion this GUI belongs to
        PlacedMinion targetMinion = null;
        List<PlacedMinion> minions = minionManager.getMinions(player.getUniqueId());
        for (PlacedMinion minion : minions) {
            String expectedTitle = MinionManager.GUI_TITLE_PREFIX
                    + PastequeSkyblockPlugin.color("&e" + minion.getType().getDisplayName());
            if (title.equals(expectedTitle)) {
                targetMinion = minion;
                break;
            }
        }

        if (targetMinion == null) {
            return;
        }

        // Back button (slot 18)
        if (slot == 18) {
            player.closeInventory();
            return;
        }

        // Close button (slot 26)
        if (slot == 26) {
            player.closeInventory();
            return;
        }

        // Collect button (slot 12)
        if (slot == 12) {
            if (targetMinion.getStorageCount() == 0) {
                player.sendMessage(PastequeSkyblockPlugin.color("&cLe stockage du minion est vide !"));
                return;
            }
            targetMinion.collectAll(player);
            player.sendMessage(PastequeSkyblockPlugin.color("&6&l>> &aObjets recuperes avec succes !"));
            minionManager.openMinionGui(player, targetMinion);
            return;
        }

        // Upgrade button (slot 14)
        if (slot == 14) {
            if (targetMinion.getLevel() >= 5) {
                player.sendMessage(PastequeSkyblockPlugin.color("&cCe minion est deja au niveau maximum !"));
                return;
            }
            int nextLevel = targetMinion.getLevel() + 1;
            double cost = minionManager.getMinionPrice(targetMinion.getType(), nextLevel);
            if (!minionManager.getPlugin().getEconomyManager().take(player.getUniqueId(), cost)) {
                player.sendMessage(PastequeSkyblockPlugin.color(
                        "&cVous n'avez pas assez de " + minionManager.getPlugin().getEconomyManager().getCurrencyName() + " !"
                ));
                return;
            }
            targetMinion.setLevel(nextLevel);
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "&6&l>> &aMinion ameliore au niveau &e" + nextLevel + " &a!"
            ));
            // Update villager name
            Location loc = targetMinion.getLocation();
            if (loc != null && loc.getWorld() != null) {
                for (Entity entity : loc.getChunk().getEntities()) {
                    if (entity instanceof Villager) {
                        Villager v = (Villager) entity;
                        if (v.getCustomName() != null
                                && v.getCustomName().contains(MinionManager.MINION_TAG)) {
                            double dx = v.getLocation().getX() - loc.getX();
                            double dy = v.getLocation().getY() - loc.getY();
                            double dz = v.getLocation().getZ() - loc.getZ();
                            if (dx * dx + dy * dy + dz * dz < 2.25) {
                                v.setCustomName(PastequeSkyblockPlugin.color(
                                        "&6" + targetMinion.getType().getDisplayName()
                                                + " &7[Niv." + nextLevel + "]"
                                ) + MinionManager.MINION_TAG);
                                break;
                            }
                        }
                    }
                }
            }
            minionManager.openMinionGui(player, targetMinion);
        }
    }

    // ── Chunk load - respawn villagers ────────────────────────────────────

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        minionManager.respawnMinionsInChunk(event.getChunk());
    }
}
