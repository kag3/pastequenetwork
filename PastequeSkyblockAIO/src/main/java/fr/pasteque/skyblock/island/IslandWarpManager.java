package fr.pasteque.skyblock.island;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.gui.GuiHelper;
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

@SuppressWarnings("deprecation")
public class IslandWarpManager {

    public static final String WARP_GUI_TITLE = PastequeSkyblockPlugin.color("&2&lPasteque &5&lWarps");
    public static final String PUBLIC_WARP_GUI_TITLE = PastequeSkyblockPlugin.color("&2&lPasteque &5&lWarps Publics");

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
        Inventory gui = Bukkit.createInventory(null, 36, WARP_GUI_TITLE);
        List<IslandWarp> ownerWarps = getWarps(islandOwner);

        // Row 1: warp items
        int slot = 10;
        for (IslandWarp warp : ownerWarps) {
            if (slot > 16) break;

            List<String> lore = new ArrayList<String>();
            lore.add("");
            lore.add(PastequeSkyblockPlugin.color("&8\u258E &7Details"));
            lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Monde: &f" + warp.getWorldName()));
            lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Position: &f" + (int) warp.getX() + ", " + (int) warp.getY() + ", " + (int) warp.getZ()));
            lore.add(PastequeSkyblockPlugin.color(warp.isPublic() ? "&8\u25B8 &aPublic" : "&8\u25B8 &cPrive"));
            lore.add("");
            lore.add(PastequeSkyblockPlugin.color("&e\u25B6 Clic pour teleporter!"));

            ItemStack item = GuiHelper.createItem(Material.ENDER_PEARL,
                    "&a&l" + warp.getName());
            ItemMeta meta = item.getItemMeta();
            List<String> coloredLore = new ArrayList<String>();
            for (String line : lore) {
                coloredLore.add(line);
            }
            meta.setLore(coloredLore);
            item.setItemMeta(meta);

            gui.setItem(slot, item);
            slot++;
        }

        // Row 3: back button
        gui.setItem(27, GuiHelper.backButton());

        // Fill remaining with black glass
        GuiHelper.decorate(gui, GuiHelper.Theme.ISLAND);

        player.openInventory(gui);
        GuiHelper.playOpen(player);
    }

    public void openPublicWarpsGui(Player player, int page) {
        Inventory gui = Bukkit.createInventory(null, 54, PUBLIC_WARP_GUI_TITLE);


        List<Map.Entry<UUID, IslandWarp>> publicWarps = getPublicWarps();
        int itemsPerPage = 28; // slots 10-16, 19-25, 28-34, 37-43
        int startIndex = page * itemsPerPage;

        int[] contentSlots = new int[]{
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        };

        for (int i = startIndex; i < publicWarps.size() && (i - startIndex) < contentSlots.length; i++) {
            Map.Entry<UUID, IslandWarp> entry = publicWarps.get(i);
            UUID owner = entry.getKey();
            IslandWarp warp = entry.getValue();

            OfflinePlayer ownerPlayer = Bukkit.getOfflinePlayer(owner);
            String ownerName = ownerPlayer.getName() != null ? ownerPlayer.getName() : owner.toString().substring(0, 8);

            ItemStack item = GuiHelper.createItem(Material.ENDER_PEARL,
                    "&a&l" + warp.getName(),
                    "",
                    "&8\u258E &7Details",
                    "&8\u25B8 &7Proprietaire: &f" + ownerName,
                    "&8\u25B8 &7Position: &f" + (int) warp.getX() + ", " + (int) warp.getY() + ", " + (int) warp.getZ(),
                    "",
                    "&e\u25B6 Clic pour teleporter!");

            gui.setItem(contentSlots[i - startIndex], item);
        }

        // Navigation arrows
        if (page > 0) {
            gui.setItem(45, GuiHelper.createItem(Material.ARROW,
                    "&e&l\u2190 Page precedente",
                    "&7Page " + page));
        }
        if ((page + 1) * itemsPerPage < publicWarps.size()) {
            gui.setItem(53, GuiHelper.createItem(Material.ARROW,
                    "&e&lPage suivante \u2192",
                    "&7Page " + (page + 2)));
        }

        // Close button
        gui.setItem(49, GuiHelper.closeButton());

        // Fill remaining with black glass
        GuiHelper.decorate(gui, GuiHelper.Theme.ISLAND);

        player.openInventory(gui);
        GuiHelper.playOpen(player);
    }

    public void openPublicWarpsGui(Player player) {
        openPublicWarpsGui(player, 0);
    }
}
