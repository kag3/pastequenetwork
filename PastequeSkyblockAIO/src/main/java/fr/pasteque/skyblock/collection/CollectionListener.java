package fr.pasteque.skyblock.collection;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.collection.model.CollectionCategory;
import fr.pasteque.skyblock.collection.model.CollectionEntry;
import fr.pasteque.skyblock.collection.model.PlayerCollections;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class CollectionListener implements Listener {

    private final CollectionManager collectionManager;

    /* ── Block type to collection material mapping ── */
    private static final Map<Material, String> BLOCK_COLLECTIONS = new HashMap<Material, String>();
    /* ── Drop material to collection material mapping (for combat) ── */
    private static final Map<Material, String> DROP_COLLECTIONS = new HashMap<Material, String>();

    static {
        // Mining
        BLOCK_COLLECTIONS.put(Material.STONE, "COBBLESTONE");
        BLOCK_COLLECTIONS.put(Material.COBBLESTONE, "COBBLESTONE");
        BLOCK_COLLECTIONS.put(Material.IRON_ORE, "IRON_ORE");
        BLOCK_COLLECTIONS.put(Material.GOLD_ORE, "GOLD_ORE");
        BLOCK_COLLECTIONS.put(Material.DIAMOND_ORE, "DIAMOND");
        BLOCK_COLLECTIONS.put(Material.EMERALD_ORE, "EMERALD");
        BLOCK_COLLECTIONS.put(Material.COAL_ORE, "COAL");
        BLOCK_COLLECTIONS.put(Material.LAPIS_ORE, "INK_SACK");
        BLOCK_COLLECTIONS.put(Material.REDSTONE_ORE, "REDSTONE");

        // Farming
        BLOCK_COLLECTIONS.put(Material.CROPS, "WHEAT");
        BLOCK_COLLECTIONS.put(Material.CARROT, "CARROT_ITEM");
        BLOCK_COLLECTIONS.put(Material.POTATO, "POTATO_ITEM");
        BLOCK_COLLECTIONS.put(Material.SUGAR_CANE_BLOCK, "SUGAR_CANE");
        BLOCK_COLLECTIONS.put(Material.MELON_BLOCK, "MELON");
        BLOCK_COLLECTIONS.put(Material.PUMPKIN, "PUMPKIN");
        BLOCK_COLLECTIONS.put(Material.CACTUS, "CACTUS");
        BLOCK_COLLECTIONS.put(Material.NETHER_WARTS, "NETHER_STALK");

        // Foraging
        BLOCK_COLLECTIONS.put(Material.LOG, "LOG");
        BLOCK_COLLECTIONS.put(Material.LOG_2, "LOG");

        // Combat drops
        DROP_COLLECTIONS.put(Material.ROTTEN_FLESH, "ROTTEN_FLESH");
        DROP_COLLECTIONS.put(Material.BONE, "BONE");
        DROP_COLLECTIONS.put(Material.SPIDER_EYE, "SPIDER_EYE");
        DROP_COLLECTIONS.put(Material.ENDER_PEARL, "ENDER_PEARL");
        DROP_COLLECTIONS.put(Material.BLAZE_ROD, "BLAZE_ROD");
    }

    public CollectionListener(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    // ── Mining / Farming / Foraging ──────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material mat = event.getBlock().getType();
        String collectionMat = BLOCK_COLLECTIONS.get(mat);
        if (collectionMat != null) {
            collectionManager.addCollection(player, collectionMat, 1);
        }
    }

    // ── Combat ───────────────────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        for (ItemStack drop : event.getDrops()) {
            if (drop == null) {
                continue;
            }
            String collectionMat = DROP_COLLECTIONS.get(drop.getType());
            if (collectionMat != null) {
                collectionManager.addCollection(killer, collectionMat, drop.getAmount());
            }
        }
    }

    // ── Fishing ──────────────────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            if (event.getCaught() instanceof Item) {
                Item caughtItem = (Item) event.getCaught();
                collectionManager.addCollection(event.getPlayer(), "RAW_FISH",
                        caughtItem.getItemStack().getAmount());
            } else {
                collectionManager.addCollection(event.getPlayer(), "RAW_FISH", 1);
            }
        }
    }

    // ── GUI clicks ───────────────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() == null || !(event.getWhoClicked() instanceof Player)) {
            return;
        }
        String title = event.getInventory().getTitle();
        Player player = (Player) event.getWhoClicked();

        // Main collection menu
        if (CollectionManager.MAIN_TITLE.equals(title)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            int[] catSlots = {10, 11, 12, 13, 14};
            CollectionCategory[] categories = CollectionCategory.values();
            for (int i = 0; i < catSlots.length && i < categories.length; i++) {
                if (slot == catSlots[i]) {
                    collectionManager.openCategoryGui(player, categories[i]);
                    return;
                }
            }
            return;
        }

        // Category menus
        if (title.startsWith(CollectionManager.CATEGORY_TITLE_PREFIX)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            int size = event.getInventory().getSize();

            // Back button
            if (slot == size - 5) {
                collectionManager.openCollectionGui(player);
                return;
            }

            // Collection item clicked - try to claim reward
            if (slot >= 9 && slot < size - 9) {
                // Figure out which category we're in
                CollectionCategory category = null;
                for (CollectionCategory cat : CollectionCategory.values()) {
                    String catTitle = CollectionManager.CATEGORY_TITLE_PREFIX
                            + PastequeSkyblockPlugin.color(cat.getColor() + cat.getDisplayName());
                    if (title.equals(catTitle)) {
                        category = cat;
                        break;
                    }
                }
                if (category == null) {
                    return;
                }

                // Find the entry at this slot
                int index = slot - 9;
                int count = 0;
                CollectionEntry targetEntry = null;
                for (CollectionEntry entry : collectionManager.getEntries()) {
                    if (entry.getCategory() == category) {
                        if (count == index) {
                            targetEntry = entry;
                            break;
                        }
                        count++;
                    }
                }
                if (targetEntry == null) {
                    return;
                }

                // Try to claim the next available tier
                PlayerCollections pc = collectionManager.getPlayerCollections(player.getUniqueId());
                int claimed = pc.getClaimedTier(targetEntry.getId());
                int playerCount = pc.getCount(targetEntry.getMaterial());
                int[] tiers = targetEntry.getTiers();

                int nextClaimable = -1;
                for (int i = claimed; i < tiers.length; i++) {
                    if (playerCount >= tiers[i]) {
                        nextClaimable = i + 1;
                    } else {
                        break;
                    }
                }

                if (nextClaimable > claimed) {
                    // Claim all unclaimed tiers up to the highest reached
                    double totalReward = 0;
                    for (int t = claimed + 1; t <= nextClaimable; t++) {
                        totalReward += collectionManager.getTierReward(targetEntry, t);
                    }
                    pc.claimTier(targetEntry.getId(), nextClaimable);
                    collectionManager.getPlugin().getEconomyManager().add(player.getUniqueId(), totalReward);
                    player.sendMessage(PastequeSkyblockPlugin.color(
                            "&6&l>> &aRecompense recuperee ! &e+"
                                    + collectionManager.getPlugin().getEconomyManager().format(totalReward)
                    ));
                    // Refresh the GUI
                    collectionManager.openCategoryGui(player, category);
                } else {
                    player.sendMessage(PastequeSkyblockPlugin.color(
                            "&cAucune recompense a recuperer pour cette collection."
                    ));
                }
            }
        }
    }
}
