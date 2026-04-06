package fr.pasteque.skyblock.farming;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.gui.GuiHelper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class FarmingListener implements Listener {

    private final FarmingManager manager;
    private final PastequeSkyblockPlugin plugin;

    public FarmingListener(PastequeSkyblockPlugin plugin, FarmingManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        startGrowthTicker();
    }

    // ── Growth Ticker ───────────────────────────────────────────────────

    private void startGrowthTicker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                List<String> toRemove = new ArrayList<String>();
                for (Map.Entry<String, CropLocation> entry : manager.getCrops().entrySet()) {
                    CropLocation cropLoc = entry.getValue();
                    if (cropLoc.isFullyGrown()) {
                        continue;
                    }

                    Location loc = cropLoc.toBukkitLocation();
                    if (loc == null || !loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                        continue;
                    }

                    Block block = loc.getBlock();
                    // Verify the block is still a valid crop block
                    if (!isValidCropBlock(block, cropLoc.getType())) {
                        toRemove.add(entry.getKey());
                        continue;
                    }

                    // Increment growth stage
                    int newStage = cropLoc.getGrowthStage() + 1;
                    cropLoc.setGrowthStage(newStage);

                    // Update block data to reflect growth
                    updateBlockGrowth(block, cropLoc.getType(), newStage);
                }

                for (String key : toRemove) {
                    manager.removeCrop(key);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    // ── Block Break ─────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        String key = CropLocation.toKey(block.getLocation());
        CropLocation cropLoc = manager.getCrop(key);

        if (cropLoc == null) {
            return;
        }

        Player player = event.getPlayer();
        event.setCancelled(true);
        event.getBlock().setType(Material.AIR);

        if (cropLoc.isFullyGrown()) {
            // Drop harvest
            ItemStack harvest = manager.createHarvestItem(cropLoc.getType(), 1);
            block.getWorld().dropItemNaturally(block.getLocation(), harvest);

            // Give XP
            manager.addFarmingXp(player.getUniqueId(), cropLoc.getType().getXpReward());

            player.sendMessage(PastequeSkyblockPlugin.color(
                    "&2&lPasteque &8\u00bb &aVous avez recolte " + cropLoc.getType().getColoredName() + "&a!"));

            // Auto-replant check (Implants enchant in lore)
            if (hasImplantsEnchant(player)) {
                autoReplant(block, cropLoc.getType());
                player.sendMessage(PastequeSkyblockPlugin.color(
                        "&2&lPasteque &8\u00bb &7Auto-replantation (Implants)..."));
            } else {
                manager.removeCrop(key);
            }
        } else {
            // Not fully grown, just remove
            manager.removeCrop(key);
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "&2&lPasteque &8\u00bb &cCette culture n'etait pas encore prete!"));
        }
    }

    // ── Block Place (Seeds) ─────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();

        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return;
        }

        List<String> lore = item.getItemMeta().getLore();
        CustomCrop crop = null;
        for (String line : lore) {
            crop = CustomCrop.fromSeedId(line);
            if (crop != null) {
                break;
            }
        }

        if (crop == null) {
            return;
        }

        // Cancel normal placement; we handle it ourselves
        event.setCancelled(true);

        // Check farming level requirement
        if (crop.getRequiredLevel() > 0 && manager.getFarmingLevel(player.getUniqueId()) < crop.getRequiredLevel()) {
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "&2&lPasteque &8\u00bb &cVous devez etre &eNiveau " + crop.getRequiredLevel()
                            + " &cen farming pour planter cette graine."));
            GuiHelper.playDeny(player);
            return;
        }

        // Check block below
        Block placed = event.getBlockPlaced();
        Block below = placed.getRelative(BlockFace.DOWN);

        if (!isValidSoilFor(crop, below)) {
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "&2&lPasteque &8\u00bb &cVous ne pouvez pas planter ici! Verifiez le bloc en dessous."));
            GuiHelper.playDeny(player);
            return;
        }

        // Place the crop block
        placed.setType(crop.getPlantBlock());
        placed.setData((byte) 0);

        // Register crop location
        CropLocation cropLoc = new CropLocation(placed.getLocation(), crop);
        manager.registerCrop(cropLoc);

        // Consume one seed from hand
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.setItemInHand(null);
        }

        player.sendMessage(PastequeSkyblockPlugin.color(
                "&2&lPasteque &8\u00bb &aVous avez plante " + crop.getColoredName() + "&a!"));
        GuiHelper.playClick(player);
    }

    // ── Right-Click Bone Meal Acceleration ──────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack hand = player.getItemInHand();

        if (hand == null || hand.getType() != Material.INK_SACK || hand.getDurability() != 15) {
            // Not bone meal (dye data value 15 = white = bone meal in 1.9)
            return;
        }

        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }

        String key = CropLocation.toKey(clicked.getLocation());
        CropLocation cropLoc = manager.getCrop(key);

        if (cropLoc == null || cropLoc.isFullyGrown()) {
            return;
        }

        event.setCancelled(true);

        // Advance growth by 1 stage
        int newStage = cropLoc.getGrowthStage() + 1;
        cropLoc.setGrowthStage(newStage);
        updateBlockGrowth(clicked, cropLoc.getType(), newStage);

        // Consume bone meal
        if (hand.getAmount() > 1) {
            hand.setAmount(hand.getAmount() - 1);
        } else {
            player.setItemInHand(null);
        }

        // Green particles effect
        try { clicked.getWorld().playEffect(clicked.getLocation(), org.bukkit.Effect.valueOf("HAPPY_VILLAGER"), 1); } catch (Throwable ignored) {}

        if (cropLoc.isFullyGrown()) {
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "&2&lPasteque &8\u00bb &aLa culture est prete a etre recoltee!"));
        }
    }

    // ── GUI Click Handling ──────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Inventory inv = event.getInventory();
        String title = inv.getTitle();

        if (title == null) {
            return;
        }

        if (title.equals(FarmingManager.GUI_TITLE)) {
            handleFarmingGuiClick(event);
        } else if (title.equals(FarmingManager.SEED_SHOP_TITLE)) {
            handleSeedShopClick(event);
        }
    }

    private void handleFarmingGuiClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot < 0 || slot >= 54) {
            return;
        }

        // Seed shop button (slot 40)
        if (slot == 40) {
            GuiHelper.playClick(player);
            manager.openSeedShop(player);
            return;
        }

        // Sell button (slot 42)
        if (slot == 42) {
            GuiHelper.playClick(player);
            player.closeInventory();
            manager.sellCrops(player);
            return;
        }

        // Close button (slot 49)
        if (slot == 49) {
            GuiHelper.playClick(player);
            player.closeInventory();
            return;
        }
    }

    private void handleSeedShopClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot < 0 || slot >= 54) {
            return;
        }

        // Back button (slot 48)
        if (slot == 48) {
            GuiHelper.playClick(player);
            manager.openFarmingGui(player);
            return;
        }

        // Close button (slot 49)
        if (slot == 49) {
            GuiHelper.playClick(player);
            player.closeInventory();
            return;
        }

        // Seed slots
        int[] seedSlots = {10, 12, 14, 16, 28, 30};
        CustomCrop[] allCrops = CustomCrop.values();
        for (int i = 0; i < seedSlots.length && i < allCrops.length; i++) {
            if (slot == seedSlots[i]) {
                buySeed(player, allCrops[i]);
                return;
            }
        }
    }

    private void buySeed(Player player, CustomCrop crop) {
        UUID uuid = player.getUniqueId();

        // Level check
        if (crop.getRequiredLevel() > 0 && manager.getFarmingLevel(uuid) < crop.getRequiredLevel()) {
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "&2&lPasteque &8\u00bb &cVous devez etre &eNiveau " + crop.getRequiredLevel()
                            + " &cen farming pour acheter ces graines."));
            GuiHelper.playDeny(player);
            return;
        }

        // Balance check
        double balance = plugin.getEconomyManager().getBalance(uuid);
        if (balance < crop.getSeedPrice()) {
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "&2&lPasteque &8\u00bb &cVous n'avez pas assez de pasteques! Prix: &6"
                            + crop.getSeedPrice() + " &c(solde: &6" + (int) balance + "&c)"));
            GuiHelper.playDeny(player);
            return;
        }

        // Purchase
        plugin.getEconomyManager().take(uuid, crop.getSeedPrice());
        ItemStack seedItem = manager.createSeedItem(crop, 1);
        player.getInventory().addItem(seedItem);

        player.sendMessage(PastequeSkyblockPlugin.color(
                "&2&lPasteque &8\u00bb &aVous avez achete " + crop.getSeedName()
                        + " &apour &6" + crop.getSeedPrice() + " pasteques&a!"));
        GuiHelper.playSuccess(player);

        // Refresh shop GUI
        manager.openSeedShop(player);
    }

    // ── Helper Methods ──────────────────────────────────────────────────

    private boolean isValidCropBlock(Block block, CustomCrop crop) {
        Material type = block.getType();
        Material expected = crop.getPlantBlock();

        // Allow AIR for crops that may have been broken externally
        return type == expected || type == Material.AIR;
    }

    private boolean isValidSoilFor(CustomCrop crop, Block below) {
        Material soilType = below.getType();

        switch (crop) {
            case ENCHANTED_MELON:
            case CRYSTAL_WHEAT:
            case PASTEQUE_ROYALE:
                return soilType == Material.SOIL; // farmland
            case GOLDEN_CACTUS:
                return soilType == Material.SAND;
            case ENCHANTED_SUGAR_CANE:
                if (soilType != Material.SAND && soilType != Material.GRASS && soilType != Material.DIRT) {
                    return false;
                }
                // Check for adjacent water
                return hasAdjacentWater(below);
            case LAVA_NETHERWART:
                return soilType == Material.SOUL_SAND;
            default:
                return false;
        }
    }

    private boolean hasAdjacentWater(Block block) {
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        for (BlockFace face : faces) {
            Material adj = block.getRelative(face).getType();
            if (adj == Material.WATER || adj == Material.STATIONARY_WATER) {
                return true;
            }
        }
        return false;
    }

    private void updateBlockGrowth(Block block, CustomCrop crop, int stage) {
        int maxStage = crop.getMaxGrowthStage();
        if (stage > maxStage) {
            stage = maxStage;
        }

        // Map growth stage to block data value (0-7 for crops like wheat)
        int dataValue;
        switch (crop) {
            case ENCHANTED_MELON:
            case CRYSTAL_WHEAT:
            case PASTEQUE_ROYALE:
                // Wheat-like: data 0->7, map 4 stages to 0, 2, 5, 7
                int[] wheatStages = {0, 2, 5, 7};
                dataValue = stage < wheatStages.length ? wheatStages[stage] : 7;
                block.setData((byte) dataValue);
                break;
            case LAVA_NETHERWART:
                // Nether wart: data 0->3, map 4 stages to 0, 1, 2, 3
                dataValue = stage < 4 ? stage : 3;
                block.setData((byte) dataValue);
                break;
            case GOLDEN_CACTUS:
            case ENCHANTED_SUGAR_CANE:
                // These grow as full blocks; no data change needed for growth
                // Visual feedback: could stack blocks but keeping simple
                break;
        }
    }

    private boolean hasImplantsEnchant(Player player) {
        ItemStack held = player.getItemInHand();
        if (held == null || !held.hasItemMeta() || !held.getItemMeta().hasLore()) {
            return false;
        }
        List<String> lore = held.getItemMeta().getLore();
        for (String line : lore) {
            if (line.contains("Implants")) {
                return true;
            }
        }
        return false;
    }

    private void autoReplant(Block block, CustomCrop crop) {
        // Re-place the crop block
        block.setType(crop.getPlantBlock());
        block.setData((byte) 0);

        // Register new crop at same location
        CropLocation newCrop = new CropLocation(block.getLocation(), crop);
        manager.registerCrop(newCrop);
    }
}
