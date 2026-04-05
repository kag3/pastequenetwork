package fr.pasteque.skyblock.slayer;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.slayer.model.SlayerType;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class SlayerListener implements Listener {

    private final PastequeSkyblockPlugin plugin;
    private final SlayerManager manager;

    public SlayerListener(PastequeSkyblockPlugin plugin, SlayerManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        if (killer == null) {
            return;
        }

        // Check if this is a tracked slayer boss
        UUID entityId = entity.getUniqueId();
        UUID ownerId = manager.getBossOwner(entityId);
        if (ownerId != null) {
            SlayerType bossType = manager.getBossType(entityId);
            Integer bossTier = manager.getBossTier(entityId);
            if (bossType != null && bossTier != null) {
                // Only the quest owner gets credit
                if (killer.getUniqueId().equals(ownerId)) {
                    manager.onBossDeath(killer, bossType, bossTier);
                } else {
                    killer.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                            + "&cCe n'etait pas votre boss slayer !"));
                }
            }
            manager.removeBoss(entityId);
            return;
        }

        // Normal mob kill - check for slayer quest progress
        String mobType = entity.getType().name();
        manager.recordKill(killer, mobType);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() == null) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        String title = event.getInventory().getTitle();
        if (title == null) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Main slayer GUI
        if (title.equals(PastequeSkyblockPlugin.color(SlayerManager.SLAYER_GUI_TITLE))) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }
            if (event.getCurrentItem().getType() == Material.STAINED_GLASS_PANE) {
                return;
            }

            // Slots 11-15 map to slayer types
            int slot = event.getRawSlot();
            if (slot >= 11 && slot <= 15) {
                int index = slot - 11;
                SlayerType[] types = SlayerType.values();
                if (index < types.length) {
                    player.closeInventory();
                    manager.openTierGui(player, types[index]);
                }
            }
            return;
        }

        // Tier selection GUI
        String tierPrefix = PastequeSkyblockPlugin.color(SlayerManager.TIER_GUI_PREFIX);
        if (title.startsWith(tierPrefix)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }
            if (event.getCurrentItem().getType() == Material.STAINED_GLASS_PANE) {
                return;
            }

            int slot = event.getRawSlot();

            // Back button
            if (slot == 22 && event.getCurrentItem().getType() == Material.ARROW) {
                player.closeInventory();
                manager.openSlayerGui(player);
                return;
            }

            // Tier slots 11-15
            if (slot >= 11 && slot <= 15) {
                int tier = slot - 10;
                // Find which slayer type from title
                String typeName = title.substring(tierPrefix.length());
                SlayerType type = null;
                for (SlayerType st : SlayerType.values()) {
                    if (typeName.equals(st.getDisplayName())) {
                        type = st;
                        break;
                    }
                }
                if (type != null && event.getCurrentItem().getType() == Material.SLIME_BALL) {
                    player.closeInventory();
                    manager.startQuest(player, type, tier);
                }
            }
        }
    }
}
