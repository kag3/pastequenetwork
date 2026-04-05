package fr.pasteque.skyblock.island;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.island.model.IslandWarp;
import fr.pasteque.skyblock.manager.DataFile;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IslandWarpManager {

    public static final String WARP_GUI_TITLE = PastequeSkyblockPlugin.color("&2&lWarps d'Ile");
    public static final String PUBLIC_WARP_GUI_TITLE = PastequeSkyblockPlugin.color("&2&lWarps Publics");

    private final PastequeSkyblockPlugin plugin;
    private final DataFile dataFile;
    private final Map<UUID, List<IslandWarp>> warps = new HashMap<UUID, List<IslandWarp>>();

    public IslandWarpManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new DataFile(plugin, "island-warps.yml");
        load();
    }

    public void load() {
        warps.clear();
        ConfigurationSection section = dataFile.getConfig().getConfigurationSection("warps");
        if (section == null) {
            return;
        }
        for (String uuidStr : section.getKeys(false)) {
            UUID owner;
            try {
                owner = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                continue;
            }
            List<IslandWarp> warpList = new ArrayList<IslandWarp>();
            ConfigurationSection ownerSection = section.getConfigurationSection(uuidStr);
            if (ownerSection != null) {
                for (String warpName : ownerSection.getKeys(false)) {
                    ConfigurationSection ws = ownerSection.getConfigurationSection(warpName);
                    if (ws == null) continue;
                    IslandWarp warp = new IslandWarp(
                            warpName,
                            ws.getString("world", "world"),
                            ws.getDouble("x"),
                            ws.getDouble("y"),
                            ws.getDouble("z"),
                            (float) ws.getDouble("yaw"),
                            (float) ws.getDouble("pitch"),
                            ws.getBoolean("public", false)
                    );
                    warpList.add(warp);
                }
            }
            warps.put(owner, warpList);
        }
    }

    public void save() {
        dataFile.getConfig().set("warps", null);
        for (Map.Entry<UUID, List<IslandWarp>> entry : warps.entrySet()) {
            String basePath = "warps." + entry.getKey().toString();
            for (IslandWarp warp : entry.getValue()) {
                String path = basePath + "." + warp.getName();
                dataFile.getConfig().set(path + ".world", warp.getWorldName());
                dataFile.getConfig().set(path + ".x", warp.getX());
                dataFile.getConfig().set(path + ".y", warp.getY());
                dataFile.getConfig().set(path + ".z", warp.getZ());
                dataFile.getConfig().set(path + ".yaw", (double) warp.getYaw());
                dataFile.getConfig().set(path + ".pitch", (double) warp.getPitch());
                dataFile.getConfig().set(path + ".public", warp.isPublic());
            }
        }
        dataFile.save();
    }

    public void createWarp(Player player, String name) {
        UUID owner = player.getUniqueId();
        int maxWarps = plugin.getConfig().getInt("island.max-warps", 3);

        List<IslandWarp> ownerWarps = warps.get(owner);
        if (ownerWarps == null) {
            ownerWarps = new ArrayList<IslandWarp>();
            warps.put(owner, ownerWarps);
        }

        if (ownerWarps.size() >= maxWarps) {
            MessageUtil.send(player, plugin.getPrefix(), "&cTu as atteint la limite de &e" + maxWarps + " &cwarps !");
            return;
        }

        // Check duplicate name
        for (IslandWarp existing : ownerWarps) {
            if (existing.getName().equalsIgnoreCase(name)) {
                MessageUtil.send(player, plugin.getPrefix(), "&cUn warp avec ce nom existe deja !");
                return;
            }
        }

        Location loc = player.getLocation();
        IslandWarp warp = new IslandWarp(
                name,
                loc.getWorld().getName(),
                loc.getX(),
                loc.getY(),
                loc.getZ(),
                loc.getYaw(),
                loc.getPitch(),
                false
        );
        ownerWarps.add(warp);
        save();

        MessageUtil.send(player, plugin.getPrefix(), "&aWarp &e" + name + " &acree avec succes !");
    }

    public void deleteWarp(Player player, String name) {
        UUID owner = player.getUniqueId();
        List<IslandWarp> ownerWarps = warps.get(owner);
        if (ownerWarps == null || ownerWarps.isEmpty()) {
            MessageUtil.send(player, plugin.getPrefix(), "&cTu n'as aucun warp !");
            return;
        }

        IslandWarp toRemove = null;
        for (IslandWarp warp : ownerWarps) {
            if (warp.getName().equalsIgnoreCase(name)) {
                toRemove = warp;
                break;
            }
        }

        if (toRemove == null) {
            MessageUtil.send(player, plugin.getPrefix(), "&cAucun warp trouve avec le nom &e" + name + "&c.");
            return;
        }

        ownerWarps.remove(toRemove);
        save();
        MessageUtil.send(player, plugin.getPrefix(), "&aWarp &e" + name + " &asupprime !");
    }

    public List<IslandWarp> getWarps(UUID owner) {
        List<IslandWarp> list = warps.get(owner);
        if (list == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(list);
    }

    public List<Map.Entry<UUID, IslandWarp>> getPublicWarps() {
        List<Map.Entry<UUID, IslandWarp>> publicWarps = new ArrayList<Map.Entry<UUID, IslandWarp>>();
        for (Map.Entry<UUID, List<IslandWarp>> entry : warps.entrySet()) {
            final UUID owner = entry.getKey();
            for (final IslandWarp warp : entry.getValue()) {
                if (warp.isPublic()) {
                    publicWarps.add(new Map.Entry<UUID, IslandWarp>() {
                        @Override public UUID getKey() { return owner; }
                        @Override public IslandWarp getValue() { return warp; }
                        @Override public IslandWarp setValue(IslandWarp value) { throw new UnsupportedOperationException(); }
                    });
                }
            }
        }
        return publicWarps;
    }

    public void warpTo(Player player, UUID owner, String name) {
        List<IslandWarp> ownerWarps = warps.get(owner);
        if (ownerWarps == null) {
            MessageUtil.send(player, plugin.getPrefix(), "&cCe joueur n'a aucun warp.");
            return;
        }

        IslandWarp target = null;
        for (IslandWarp warp : ownerWarps) {
            if (warp.getName().equalsIgnoreCase(name)) {
                target = warp;
                break;
            }
        }

        if (target == null) {
            MessageUtil.send(player, plugin.getPrefix(), "&cWarp &e" + name + " &cnon trouve.");
            return;
        }

        Location loc = target.toLocation(Bukkit.getServer());
        if (loc == null) {
            MessageUtil.send(player, plugin.getPrefix(), "&cLe monde de ce warp n'est pas charge.");
            return;
        }

        player.teleport(loc);
        MessageUtil.send(player, plugin.getPrefix(), "&aTeleporte au warp &e" + name + " &a!");
    }

    public void openWarpGui(Player player, UUID islandOwner) {
        Inventory gui = Bukkit.createInventory(null, 27, WARP_GUI_TITLE);
        List<IslandWarp> ownerWarps = getWarps(islandOwner);

        // Fill with black glass
        ItemStack filler = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        for (int s = 0; s < 27; s++) {
            gui.setItem(s, filler);
        }

        int slot = 10;
        for (IslandWarp warp : ownerWarps) {
            if (slot > 16) break;
            ItemStack item = new ItemStack(Material.ENDER_PEARL, 1);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(PastequeSkyblockPlugin.color("&a" + warp.getName()));

            List<String> lore = new ArrayList<String>();
            lore.add(PastequeSkyblockPlugin.color("&7Monde: &f" + warp.getWorldName()));
            lore.add(PastequeSkyblockPlugin.color("&7Position: &f" + (int) warp.getX() + ", " + (int) warp.getY() + ", " + (int) warp.getZ()));
            lore.add(PastequeSkyblockPlugin.color(warp.isPublic() ? "&aPublic" : "&cPrive"));
            lore.add("");
            lore.add(PastequeSkyblockPlugin.color("&e> Cliquez pour vous teleporter"));
            meta.setLore(lore);
            item.setItemMeta(meta);
            gui.setItem(slot, item);
            slot++;
        }

        player.openInventory(gui);
    }

    public void openPublicWarpsGui(Player player, int page) {
        Inventory gui = Bukkit.createInventory(null, 54, PUBLIC_WARP_GUI_TITLE);

        // Fill with black glass
        ItemStack filler = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        for (int s = 0; s < 54; s++) {
            gui.setItem(s, filler);
        }

        List<Map.Entry<UUID, IslandWarp>> publicWarps = getPublicWarps();
        int itemsPerPage = 45;
        int startIndex = page * itemsPerPage;

        for (int i = startIndex; i < publicWarps.size() && (i - startIndex) < itemsPerPage; i++) {
            Map.Entry<UUID, IslandWarp> entry = publicWarps.get(i);
            UUID owner = entry.getKey();
            IslandWarp warp = entry.getValue();

            OfflinePlayer ownerPlayer = Bukkit.getOfflinePlayer(owner);
            String ownerName = ownerPlayer.getName() != null ? ownerPlayer.getName() : owner.toString().substring(0, 8);

            ItemStack item = new ItemStack(Material.ENDER_PEARL, 1);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(PastequeSkyblockPlugin.color("&a" + warp.getName()));

            List<String> lore = new ArrayList<String>();
            lore.add(PastequeSkyblockPlugin.color("&7Proprietaire: &f" + ownerName));
            lore.add(PastequeSkyblockPlugin.color("&7Position: &f" + (int) warp.getX() + ", " + (int) warp.getY() + ", " + (int) warp.getZ()));
            lore.add("");
            lore.add(PastequeSkyblockPlugin.color("&e> Cliquez pour vous teleporter"));
            meta.setLore(lore);
            item.setItemMeta(meta);

            gui.setItem(i - startIndex, item);
        }

        // Navigation arrows
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW, 1);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.setDisplayName(PastequeSkyblockPlugin.color("&e<< Page precedente"));
            prev.setItemMeta(prevMeta);
            gui.setItem(45, prev);
        }
        if ((page + 1) * itemsPerPage < publicWarps.size()) {
            ItemStack next = new ItemStack(Material.ARROW, 1);
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.setDisplayName(PastequeSkyblockPlugin.color("&ePage suivante >>"));
            next.setItemMeta(nextMeta);
            gui.setItem(53, next);
        }

        player.openInventory(gui);
    }

    public void openPublicWarpsGui(Player player) {
        openPublicWarpsGui(player, 0);
    }
}
