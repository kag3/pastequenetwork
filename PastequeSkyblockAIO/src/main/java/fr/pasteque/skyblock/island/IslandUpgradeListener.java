package fr.pasteque.skyblock.island;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.island.model.IslandPreset;
import fr.pasteque.skyblock.island.model.IslandUpgrade;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IslandUpgradeListener implements Listener {

    private final PastequeSkyblockPlugin plugin;
    private final IslandUpgradeManager upgradeManager;
    private final IslandWarpManager warpManager;
    private final Map<UUID, IslandPreset> pendingPresets = new HashMap<UUID, IslandPreset>();

    public IslandUpgradeListener(PastequeSkyblockPlugin plugin, IslandUpgradeManager upgradeManager, IslandWarpManager warpManager) {
        this.plugin = plugin;
        this.upgradeManager = upgradeManager;
        this.warpManager = warpManager;
    }

    public IslandPreset getPendingPreset(UUID uuid) {
        return pendingPresets.remove(uuid);
    }

    public boolean hasPendingPreset(UUID uuid) {
        return pendingPresets.containsKey(uuid);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();
        String title = inv.getTitle();

        if (title == null) {
            return;
        }

        // === Upgrade GUI ===
        if (title.equals(IslandUpgradeManager.GUI_TITLE)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= inv.getSize()) {
                return;
            }

            // Upgrade slots are 10-14
            IslandUpgrade upgrade = getUpgradeBySlot(slot);
            if (upgrade != null) {
                player.closeInventory();
                upgradeManager.upgrade(player, upgrade);
            }
            return;
        }

        // === Warp GUI ===
        if (title.equals(IslandWarpManager.WARP_GUI_TITLE)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= inv.getSize()) {
                return;
            }

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) {
                return;
            }

            String displayName = clicked.getItemMeta().getDisplayName();
            // Strip color codes to get the warp name
            String warpName = org.bukkit.ChatColor.stripColor(displayName);
            if (warpName == null || warpName.trim().isEmpty()) {
                return;
            }

            // Find the island owner from the warps - check whose warps contain this name
            player.closeInventory();
            // We need to find the owner. Look at all warps to find a match.
            for (UUID owner : getOwnerCandidates()) {
                List<fr.pasteque.skyblock.island.model.IslandWarp> ownerWarps = warpManager.getWarps(owner);
                for (fr.pasteque.skyblock.island.model.IslandWarp warp : ownerWarps) {
                    if (warp.getName().equals(warpName)) {
                        warpManager.warpTo(player, owner, warpName);
                        return;
                    }
                }
            }
            return;
        }

        // === Public Warps GUI ===
        if (title.equals(IslandWarpManager.PUBLIC_WARP_GUI_TITLE)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= inv.getSize()) {
                return;
            }

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) {
                return;
            }

            String displayName = clicked.getItemMeta().getDisplayName();
            String cleanName = org.bukkit.ChatColor.stripColor(displayName);

            // Navigation
            if (slot == 45 && cleanName != null && cleanName.contains("precedente")) {
                // Previous page - for simplicity, reopen page 0
                player.closeInventory();
                warpManager.openPublicWarpsGui(player, 0);
                return;
            }
            if (slot == 53 && cleanName != null && cleanName.contains("suivante")) {
                // Next page
                player.closeInventory();
                warpManager.openPublicWarpsGui(player, 1);
                return;
            }

            if (cleanName == null || cleanName.trim().isEmpty()) {
                return;
            }

            // Find and teleport
            player.closeInventory();
            List<Map.Entry<UUID, fr.pasteque.skyblock.island.model.IslandWarp>> publicWarps = warpManager.getPublicWarps();
            for (Map.Entry<UUID, fr.pasteque.skyblock.island.model.IslandWarp> entry : publicWarps) {
                if (entry.getValue().getName().equals(cleanName)) {
                    warpManager.warpTo(player, entry.getKey(), cleanName);
                    return;
                }
            }
            return;
        }

        // === Preset GUI ===
        if (title.equals(IslandPresetGui.GUI_TITLE)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= inv.getSize()) {
                return;
            }

            IslandPreset preset = getPresetBySlot(slot);
            if (preset != null) {
                pendingPresets.put(player.getUniqueId(), preset);
                player.closeInventory();
                MessageUtil.send(player, plugin.getPrefix(), "&aType d'ile &e" + preset.getDisplayName() + " &aselectionne !");
            }
        }
    }

    private IslandUpgrade getUpgradeBySlot(int slot) {
        switch (slot) {
            case 10: return IslandUpgrade.SIZE;
            case 11: return IslandUpgrade.MEMBERS;
            case 12: return IslandUpgrade.GENERATOR;
            case 13: return IslandUpgrade.SPAWNER;
            case 14: return IslandUpgrade.MINIONS;
            default: return null;
        }
    }

    private IslandPreset getPresetBySlot(int slot) {
        switch (slot) {
            case 10: return IslandPreset.CLASSIC;
            case 11: return IslandPreset.DESERT;
            case 12: return IslandPreset.JUNGLE;
            case 13: return IslandPreset.NETHER;
            case 14: return IslandPreset.ICE;
            case 15: return IslandPreset.MUSHROOM;
            default: return null;
        }
    }

    /**
     * Returns all known owner UUIDs that have warps.
     */
    private java.util.Set<UUID> getOwnerCandidates() {
        java.util.Set<UUID> owners = new java.util.HashSet<UUID>();
        // Iterate public warps to gather owners
        List<Map.Entry<UUID, fr.pasteque.skyblock.island.model.IslandWarp>> all = warpManager.getPublicWarps();
        for (Map.Entry<UUID, fr.pasteque.skyblock.island.model.IslandWarp> entry : all) {
            owners.add(entry.getKey());
        }
        return owners;
    }
}
