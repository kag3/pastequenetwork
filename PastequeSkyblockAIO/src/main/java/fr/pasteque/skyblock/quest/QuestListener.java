package fr.pasteque.skyblock.quest;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.gui.GuiHelper;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

public class QuestListener implements Listener {

    private final PastequeSkyblockPlugin plugin;
    private final QuestManager manager;

    public QuestListener(PastequeSkyblockPlugin plugin, QuestManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    // =========================================================================
    //  First join
    // =========================================================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore()) {
            manager.assignFirstJoinQuests(player.getUniqueId());
        }
        // Check REACH_LEVEL/MONEY quests on every join
        manager.checkReachLevelQuests(player.getUniqueId());
    }

    // =========================================================================
    //  Block break
    // =========================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String blockType = event.getBlock().getType().name();
        manager.advanceQuest(player.getUniqueId(), QuestType.BREAK_BLOCK, blockType, 1);
        // Also check money quests
        manager.checkReachLevelQuests(player.getUniqueId());
    }

    // =========================================================================
    //  Block place
    // =========================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        String blockType = event.getBlock().getType().name();
        manager.advanceQuest(player.getUniqueId(), QuestType.PLACE_BLOCK, blockType, 1);

        // Also check for item-in-hand name for special targets (e.g. seeds placed as crops)
        ItemStack hand = event.getItemInHand();
        if (hand != null && hand.getType() != Material.AIR) {
            String handType = hand.getType().name();
            if (!handType.equals(blockType)) {
                manager.advanceQuest(player.getUniqueId(), QuestType.PLACE_BLOCK, handType, 1);
            }
        }
    }

    // =========================================================================
    //  Entity death (kill mob)
    // =========================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        String entityType = entity.getType().name();
        manager.advanceQuest(killer.getUniqueId(), QuestType.KILL_MOB, entityType, 1);
    }

    // =========================================================================
    //  Craft item
    // =========================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        ItemStack result = event.getRecipe().getResult();
        if (result == null || result.getType() == Material.AIR) return;

        // Calculate the actual amount crafted (shift-click can craft multiple)
        int amount = result.getAmount();
        if (event.isShiftClick()) {
            // For shift-click, calculate maximum possible crafts
            int maxCrafts = 64;
            for (ItemStack ingredient : event.getInventory().getMatrix()) {
                if (ingredient != null && ingredient.getType() != Material.AIR) {
                    int possible = ingredient.getAmount();
                    if (possible < maxCrafts) {
                        maxCrafts = possible;
                    }
                }
            }
            amount = result.getAmount() * maxCrafts;
        }

        String itemType = result.getType().name();
        manager.advanceQuest(player.getUniqueId(), QuestType.CRAFT_ITEM, itemType, amount);
    }

    // =========================================================================
    //  Fishing
    // =========================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Player player = event.getPlayer();
        manager.advanceQuest(player.getUniqueId(), QuestType.FISH, "FISH", 1);
    }

    // =========================================================================
    //  World change (VISIT_LOCATION)
    // =========================================================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName().toLowerCase();
        manager.advanceQuest(player.getUniqueId(), QuestType.VISIT_LOCATION, worldName, 1);
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

        Player player = (Player) event.getWhoClicked();

        // Main quest GUI
        if (stripped.equals("Pasteque Missions")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= 54) return;

            // Close button
            if (slot == 49) {
                player.closeInventory();
                GuiHelper.playClick(player);
                return;
            }

            // Quest slots: 10-16, 19-25, 28-34
            int[] slots = {10, 11, 12, 13, 14, 15, 16,
                           19, 20, 21, 22, 23, 24, 25,
                           28, 29, 30, 31, 32, 33, 34};
            int questIndex = -1;
            for (int i = 0; i < slots.length; i++) {
                if (slots[i] == slot) {
                    questIndex = i;
                    break;
                }
            }
            if (questIndex < 0) return;

            // Determine which quest was clicked
            UUID uuid = player.getUniqueId();
            Map<String, QuestProgress> map = manager.getActiveQuests(uuid) != null
                    ? buildOrderedMap(uuid) : null;
            if (map == null) return;

            String questId = getQuestIdByIndex(uuid, questIndex);
            if (questId == null) return;

            QuestProgress qp = manager.getProgress(uuid, questId);
            if (qp != null && qp.isCompleted() && !qp.isClaimed()) {
                // Directly claim from main GUI
                boolean success = manager.claimReward(uuid, questId);
                if (success) {
                    GuiHelper.playSuccess(player);
                } else {
                    GuiHelper.playDeny(player);
                }
                manager.openQuestGui(player);
            } else {
                // Open detail view
                GuiHelper.playClick(player);
                manager.openQuestDetail(player, questId);
            }
            return;
        }

        // Detail quest GUI
        if (stripped.startsWith("Mission: ")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= 27) return;

            // Back button
            if (slot == 18) {
                GuiHelper.playClick(player);
                manager.openQuestGui(player);
                return;
            }

            // Claim button (slot 22)
            if (slot == 22) {
                // Find the quest from the title
                String questId = findQuestIdFromTitle(stripped);
                if (questId != null) {
                    boolean success = manager.claimReward(player.getUniqueId(), questId);
                    if (success) {
                        GuiHelper.playSuccess(player);
                        manager.openQuestGui(player);
                    } else {
                        GuiHelper.playDeny(player);
                        player.sendMessage(PastequeSkyblockPlugin.color(
                                "&2&lPasteque &5&lMissions &8\u00bb &cVous ne pouvez pas reclamer cette recompense."));
                    }
                }
                return;
            }
        }
    }

    // =========================================================================
    //  Utility
    // =========================================================================

    /**
     * Builds the ordered list of quest IDs the same way openQuestGui does,
     * and returns the quest ID at the given index.
     */
    private String getQuestIdByIndex(UUID uuid, int index) {
        java.util.List<String> allIds = new java.util.ArrayList<String>();
        Map<String, QuestProgress> progressMap = new java.util.HashMap<String, QuestProgress>();

        // Get all assigned quest IDs
        for (Quest q : manager.getActiveQuests(uuid)) {
            progressMap.put(q.getId(), manager.getProgress(uuid, q.getId()));
        }

        // Also add claimed quests
        for (Map.Entry<String, Quest> entry : manager.getQuests().entrySet()) {
            QuestProgress qp = manager.getProgress(uuid, entry.getKey());
            if (qp != null && !allIds.contains(entry.getKey())) {
                allIds.add(entry.getKey());
            }
        }

        // Add locked chain quests
        java.util.List<String> extras = new java.util.ArrayList<String>();
        for (String id : allIds) {
            Quest q = manager.getQuest(id);
            if (q != null && q.getNextQuestId() != null && q.getNextQuestId().length() > 0) {
                String nextId = q.getNextQuestId();
                if (!allIds.contains(nextId) && !extras.contains(nextId)
                        && manager.getQuest(nextId) != null) {
                    extras.add(nextId);
                }
            }
        }
        allIds.addAll(extras);

        if (index >= 0 && index < allIds.size()) {
            return allIds.get(index);
        }
        return null;
    }

    private Map<String, QuestProgress> buildOrderedMap(UUID uuid) {
        Map<String, QuestProgress> result = new java.util.HashMap<String, QuestProgress>();
        for (Quest q : manager.getActiveQuests(uuid)) {
            result.put(q.getId(), manager.getProgress(uuid, q.getId()));
        }
        return result;
    }

    /**
     * Finds a quest ID by matching the quest name in the detail GUI title.
     */
    private String findQuestIdFromTitle(String strippedTitle) {
        // Title is "Mission: <quest name>" — strip prefix
        String name = strippedTitle.substring("Mission: ".length()).trim();
        for (Map.Entry<String, Quest> entry : manager.getQuests().entrySet()) {
            String questName = ChatColor.stripColor(PastequeSkyblockPlugin.color(entry.getValue().getName()));
            if (questName.equals(name)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
