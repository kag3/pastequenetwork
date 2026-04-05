package fr.pasteque.skyblock.combatpass;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.combatpass.model.PlayerPassData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.Inventory;

public class CombatPassListener implements Listener {

    private final PastequeSkyblockPlugin plugin;
    private final CombatPassManager manager;

    public CombatPassListener(PastequeSkyblockPlugin plugin, CombatPassManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    // =========================================================================
    //  GUI click handling
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory inv = event.getInventory();
        if (inv == null) return;

        String title = inv.getTitle();
        if (title == null) return;
        String stripped = ChatColor.stripColor(title);
        if (!stripped.startsWith("Pasteque Passe")) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        int page = CombatPassGui.pageFromTitle(title);
        int startTier = page * 7 + 1; // 7 tiers per page

        // Row 1 (slots 10-16): click on free tier to claim
        if (slot >= 10 && slot <= 16) {
            int tier = startTier + (slot - 10);
            if (tier >= 1 && tier <= 30) {
                manager.claimReward(player, tier, false);
                CombatPassGui.open(manager, player, page);
            }
            return;
        }

        // Row 3 (slots 28-34): click on premium tier to claim
        if (slot >= 28 && slot <= 34) {
            int tier = startTier + (slot - 28);
            if (tier >= 1 && tier <= 30) {
                manager.claimReward(player, tier, true);
                CombatPassGui.open(manager, player, page);
            }
            return;
        }

        // Row 4 (slots 37-43): claim buttons - try both free and premium
        if (slot >= 37 && slot <= 43) {
            int tier = startTier + (slot - 37);
            if (tier >= 1 && tier <= 30) {
                PlayerPassData data = manager.getData(player.getUniqueId());
                int freeKey = tier;
                int premiumKey = -tier;
                if (!data.hasClaimed(freeKey)) {
                    manager.claimReward(player, tier, false);
                }
                if (data.isPremium() && !data.hasClaimed(premiumKey)) {
                    manager.claimReward(player, tier, true);
                }
                CombatPassGui.open(manager, player, page);
            }
            return;
        }

        // Slot 45: previous page
        if (slot == 45 && page > 0) {
            CombatPassGui.open(manager, player, page - 1);
            return;
        }

        // Slot 53: next page
        if (slot == 53) {
            CombatPassGui.open(manager, player, page + 1);
            return;
        }

        // Slot 47: buy premium
        if (slot == 47) {
            PlayerPassData data = manager.getData(player.getUniqueId());
            if (!data.isPremium()) {
                double price = 25000;
                double balance = plugin.getEconomyManager().getBalance(player.getUniqueId());
                if (balance < price) {
                    player.sendMessage(PastequeSkyblockPlugin.color(
                            "&d&lPasse de Combat &7» &cTu n'as pas assez d'argent ! &7(Requis: &e25000$&7)"));
                } else {
                    plugin.getEconomyManager().take(player.getUniqueId(), price);
                    data.setPremium(true);
                    player.sendMessage(PastequeSkyblockPlugin.color(
                            "&d&lPasse de Combat &7» &a&lFelicitations ! &aTu as achete le &6Passe Premium &a!"));
                    CombatPassGui.open(manager, player, page);
                }
            }
            return;
        }
    }

    // =========================================================================
    //  XP sources
    // =========================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) return;
        Player killer = event.getEntity().getKiller();
        manager.addXp(killer, 10);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        manager.addXp(event.getPlayer(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            manager.addXp(event.getPlayer(), 5);
        }
    }
}
