package fr.pasteque.skyblock.staff;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class StaffModeManager {

    private final PastequeSkyblockPlugin plugin;
    private final Set<UUID> inStaffMode = new HashSet<UUID>();
    private final HashMap<UUID, ItemStack[]> savedInventories = new HashMap<UUID, ItemStack[]>();
    private final HashMap<UUID, ItemStack[]> savedArmor = new HashMap<UUID, ItemStack[]>();
    private final HashMap<UUID, GameMode> savedGameMode = new HashMap<UUID, GameMode>();
    private final Set<UUID> frozen = new HashSet<UUID>();

    public StaffModeManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    //  Staff mode toggle
    // ------------------------------------------------------------------

    public void toggleStaffMode(Player player) {
        if (inStaffMode.contains(player.getUniqueId())) {
            disableStaffMode(player);
        } else {
            enableStaffMode(player);
        }
    }

    private void enableStaffMode(Player player) {
        UUID uuid = player.getUniqueId();

        // Save current state
        savedInventories.put(uuid, player.getInventory().getContents().clone());
        savedArmor.put(uuid, player.getInventory().getArmorContents().clone());
        savedGameMode.put(uuid, player.getGameMode());

        // Clear and set up
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.setGameMode(GameMode.CREATIVE);

        // Give staff items
        player.getInventory().setItem(0, createStaffItem(Material.COMPASS,
                "&c&lTeleportation",
                "&7Clic droit pour vous teleporter",
                "&7au joueur que vous regardez",
                "",
                "&8\u25B8 &5Staff Pasteque"));

        player.getInventory().setItem(1, createSkullItem(
                "&e&lInspection",
                "&7Clic droit sur un joueur",
                "&7pour voir son inventaire",
                "",
                "&8\u25B8 &5Staff Pasteque"));

        player.getInventory().setItem(2, createStaffItem(Material.ICE,
                "&b&lFreeze",
                "&7Clic droit sur un joueur",
                "&7pour le geler/degeler",
                "",
                "&8\u25B8 &5Staff Pasteque"));

        player.getInventory().setItem(3, createStaffItem(Material.BOOK,
                "&a&lRapports",
                "&7Clic droit pour ouvrir",
                "&7le panneau de rapports",
                "",
                "&8\u25B8 &5Staff Pasteque"));

        player.getInventory().setItem(4, createStaffItem(Material.BLAZE_ROD,
                "&6&lVanish",
                "&7Clic droit pour basculer",
                "&7le mode invisible",
                "",
                "&8\u25B8 &5Staff Pasteque"));

        player.getInventory().setItem(8, createStaffItem(Material.REDSTONE,
                "&c&lQuitter Staff",
                "&7Clic droit pour quitter",
                "&7le mode staff",
                "",
                "&8\u25B8 &5Staff Pasteque"));

        // Vanish
        setVanished(player, true);

        inStaffMode.add(uuid);
        player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&aMode Staff &2active &a!"));
    }

    private void disableStaffMode(Player player) {
        UUID uuid = player.getUniqueId();

        // Unvanish
        setVanished(player, false);

        // Restore inventory
        player.getInventory().clear();
        if (savedInventories.containsKey(uuid)) {
            player.getInventory().setContents(savedInventories.get(uuid));
            savedInventories.remove(uuid);
        }
        if (savedArmor.containsKey(uuid)) {
            player.getInventory().setArmorContents(savedArmor.get(uuid));
            savedArmor.remove(uuid);
        }
        if (savedGameMode.containsKey(uuid)) {
            player.setGameMode(savedGameMode.get(uuid));
            savedGameMode.remove(uuid);
        }

        inStaffMode.remove(uuid);
        player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cMode Staff &4desactive &c!"));
    }

    // ------------------------------------------------------------------
    //  Vanish
    // ------------------------------------------------------------------

    public void setVanished(Player player, boolean vanish) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) {
                continue;
            }
            if (vanish) {
                online.hidePlayer(player);
            } else {
                online.showPlayer(player);
            }
        }
        if (vanish) {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&7Vous etes maintenant &einvisible&7."));
        } else {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&7Vous etes maintenant &avisible&7."));
        }
    }

    public boolean isVanished(Player player) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player) || inStaffMode.contains(online.getUniqueId())) {
                continue;
            }
            if (!online.canSee(player)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    //  Freeze
    // ------------------------------------------------------------------

    public void freezePlayer(Player staff, Player target) {
        UUID targetId = target.getUniqueId();
        if (frozen.contains(targetId)) {
            frozen.remove(targetId);
            target.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&aVous avez ete &2degele&a !"));
            staff.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                    + "&e" + target.getName() + " &aa ete degele."));
        } else {
            frozen.add(targetId);
            target.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                    + "&cVous avez ete &4gele &cpar un moderateur ! Ne vous deconnectez pas."));
            staff.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                    + "&e" + target.getName() + " &ca ete gele."));
        }
    }

    public boolean isFrozen(UUID uuid) {
        return frozen.contains(uuid);
    }

    public void unfreezePlayer(UUID uuid) {
        frozen.remove(uuid);
    }

    // ------------------------------------------------------------------
    //  State checks
    // ------------------------------------------------------------------

    public boolean isInStaffMode(UUID uuid) {
        return inStaffMode.contains(uuid);
    }

    public void forceDisable(Player player) {
        if (inStaffMode.contains(player.getUniqueId())) {
            disableStaffMode(player);
        }
    }

    // ------------------------------------------------------------------
    //  Item creation helpers
    // ------------------------------------------------------------------

    private ItemStack createStaffItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(PastequeSkyblockPlugin.color(name));
        if (loreLines.length > 0) {
            java.util.List<String> lore = new java.util.ArrayList<String>();
            for (String line : loreLines) {
                lore.add(PastequeSkyblockPlugin.color(line));
            }
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSkullItem(String name, String... loreLines) {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(PastequeSkyblockPlugin.color(name));
        if (loreLines.length > 0) {
            java.util.List<String> lore = new java.util.ArrayList<String>();
            for (String line : loreLines) {
                lore.add(PastequeSkyblockPlugin.color(line));
            }
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }
}
