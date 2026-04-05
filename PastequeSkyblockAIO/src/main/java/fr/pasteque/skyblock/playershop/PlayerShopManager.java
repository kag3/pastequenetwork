package fr.pasteque.skyblock.playershop;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.gui.GuiHelper;
import fr.pasteque.skyblock.manager.EconomyManager;
import fr.pasteque.skyblock.model.Island;
import fr.pasteque.skyblock.playershop.model.PendingShopCreation;
import fr.pasteque.skyblock.playershop.model.Shop;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlayerShopManager {

    public static final String CREATE_GUI_TITLE = PastequeSkyblockPlugin.color("&2&lPasteque &5&lShop &8- &fCreer");
    public static final String VIEW_GUI_TITLE = PastequeSkyblockPlugin.color("&2&lPasteque &5&lShop &8- &fVoir");
    public static final String OWNER_GUI_TITLE = PastequeSkyblockPlugin.color("&2&lPasteque &5&lShop &8- &fGerer");

    private final PastequeSkyblockPlugin plugin;
    private final Map<UUID, PendingShopCreation> pendingCreations = new HashMap<UUID, PendingShopCreation>();
    private final Map<String, Shop> shopsByLocation = new HashMap<String, Shop>();
    private final Map<UUID, Map<String, Shop>> shopsByOwner = new HashMap<UUID, Map<String, Shop>>();
    private final File shopsFile;
    private final YamlConfiguration shopsConfig;
    private final Map<UUID, Shop> ownerViewSessions = new HashMap<UUID, Shop>();

    public PlayerShopManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.shopsFile = new File(plugin.getDataFolder(), "shops.yml");
        this.shopsConfig = YamlConfiguration.loadConfiguration(shopsFile);
    }

    public void load() {
        shopsByLocation.clear();
        shopsByOwner.clear();
        ConfigurationSection section = shopsConfig.getConfigurationSection("shops");
        if (section == null) return;
        for (String ownerKey : section.getKeys(false)) {
            UUID owner;
            try { owner = UUID.fromString(ownerKey); } catch (IllegalArgumentException ignored) { continue; }
            ConfigurationSection ownerSection = section.getConfigurationSection(ownerKey);
            if (ownerSection == null) continue;
            for (String shopKey : ownerSection.getKeys(false)) {
                ConfigurationSection shopSection = ownerSection.getConfigurationSection(shopKey);
                if (shopSection == null) continue;
                String world = shopSection.getString("world");
                if (world == null) continue;
                Location location = new Location(Bukkit.getWorld(world), shopSection.getInt("x"), shopSection.getInt("y"), shopSection.getInt("z"));
                ItemStack item = shopSection.getItemStack("item");
                if (item == null) continue;
                Shop shop = new Shop(owner, shopSection.getString("name", shopKey), location, item, shopSection.getDouble("price"), shopSection.getInt("stock"));
                registerShop(shop);
            }
        }
    }

    public void save() {
        shopsConfig.set("shops", null);
        for (Map.Entry<UUID, Map<String, Shop>> entry : shopsByOwner.entrySet()) {
            for (Shop shop : entry.getValue().values()) {
                String base = "shops." + entry.getKey().toString() + "." + normalize(shop.getName());
                shopsConfig.set(base + ".name", shop.getName());
                shopsConfig.set(base + ".world", shop.getSignLocation().getWorld().getName());
                shopsConfig.set(base + ".x", shop.getSignLocation().getBlockX());
                shopsConfig.set(base + ".y", shop.getSignLocation().getBlockY());
                shopsConfig.set(base + ".z", shop.getSignLocation().getBlockZ());
                shopsConfig.set(base + ".price", shop.getPrice());
                shopsConfig.set(base + ".stock", shop.getStock());
                shopsConfig.set(base + ".item", shop.getTemplate());
            }
        }
        try {
            shopsConfig.save(shopsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible d'enregistrer shops.yml : " + e.getMessage());
        }
    }

    public PendingShopCreation getPending(UUID player) { return pendingCreations.get(player); }
    public void removePending(UUID player) { pendingCreations.remove(player); }

    public PendingShopCreation openCreation(Player player, String shopName, SignPlacementUtil.SignPlacement placement) {
        Inventory inventory = Bukkit.createInventory(player, 54, CREATE_GUI_TITLE + PastequeSkyblockPlugin.color(" &7- &f" + trim(shopName)));
        GuiHelper.addTopBorder(inventory);
        inventory.setItem(49, GuiHelper.createItem(Material.EMERALD_BLOCK, "&a&lValider le depot des stocks"));
        inventory.setItem(45, GuiHelper.closeButton());
        GuiHelper.fillEmpty(inventory);
        PendingShopCreation pending = new PendingShopCreation(shopName, inventory, placement.getLocation(), placement.getMaterial(), placement.getWallFacing());
        pendingCreations.put(player.getUniqueId(), pending);
        player.openInventory(inventory);
        return pending;
    }

    public boolean canCreate(Player player) {
        Island island = plugin.getIslandManager().getIslandAt(player.getLocation());
        return island != null && island.getOwner().equals(player.getUniqueId());
    }

    public boolean ownerHasShopName(UUID owner, String shopName) {
        Map<String, Shop> byName = shopsByOwner.get(owner);
        return byName != null && byName.containsKey(normalize(shopName));
    }

    public boolean validatePendingDeposit(Player player) {
        PendingShopCreation pending = pendingCreations.get(player.getUniqueId());
        if (pending == null) return false;
        Inventory inventory = pending.getInventory();
        ItemStack template = null;
        int stock = 0;
        for (int slot = 0; slot < 45; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;
            if (template == null) {
                template = item.clone();
                template.setAmount(1);
            } else if (!template.isSimilar(one(item))) {
                return false;
            }
            stock += item.getAmount();
        }
        if (template == null || stock <= 0) return false;
        pending.setTemplate(template);
        pending.setStock(stock);
        pending.setWaitingPrice(true);
        return true;
    }

    public void cancelPending(Player player, boolean returnItems) {
        PendingShopCreation pending = pendingCreations.remove(player.getUniqueId());
        if (pending == null) return;
        if (returnItems) {
            Inventory inventory = pending.getInventory();
            for (int slot = 0; slot < 45; slot++) {
                ItemStack item = inventory.getItem(slot);
                if (item != null && item.getType() != Material.AIR) {
                    HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                    for (ItemStack overflowItem : overflow.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), overflowItem);
                    }
                }
            }
        }
    }

    public Shop finalizePending(Player player, double price) {
        PendingShopCreation pending = pendingCreations.remove(player.getUniqueId());
        if (pending == null || !pending.isWaitingPrice() || pending.getTemplate() == null || pending.getStock() <= 0) {
            return null;
        }
        placeSign(pending, price);
        Shop shop = new Shop(player.getUniqueId(), pending.getShopName(), pending.getSignLocation(), pending.getTemplate(), price, pending.getStock());
        registerShop(shop);
        save();
        return shop;
    }

    private void placeSign(PendingShopCreation pending, double price) {
        EconomyManager economy = plugin.getEconomyManager();
        Block block = pending.getSignLocation().getBlock();
        block.setType(pending.getSignMaterial());
        MaterialData data = block.getState().getData();
        if (data instanceof org.bukkit.material.Sign && pending.getSignMaterial() == Material.WALL_SIGN) {
            org.bukkit.material.Sign wall = (org.bukkit.material.Sign) data;
            wall.setFacingDirection(pending.getWallFacing());
            block.getState().setData(wall);
            block.getState().update(true, false);
        }
        Sign sign = (Sign) block.getState();
        sign.setLine(0, PastequeSkyblockPlugin.color("&2&l[SHOP]"));
        sign.setLine(1, PastequeSkyblockPlugin.color("&f" + trim(pending.getShopName())));
        sign.setLine(2, PastequeSkyblockPlugin.color("&7Stock: &a" + pending.getStock()));
        sign.setLine(3, PastequeSkyblockPlugin.color("&e" + economy.format(price)));
        sign.update(true);
    }

    public Shop getShopByLocation(Location location) {
        return location == null ? null : shopsByLocation.get(key(location));
    }

    public Shop getShopByName(UUID owner, String shopName) {
        Map<String, Shop> map = shopsByOwner.get(owner);
        return map == null ? null : map.get(normalize(shopName));
    }

    public Shop getShopByName(String ownerName, String shopName) {
        for (UUID owner : shopsByOwner.keySet()) {
            String candidate = Bukkit.getOfflinePlayer(owner).getName();
            if (candidate != null && candidate.equalsIgnoreCase(ownerName)) {
                return getShopByName(owner, shopName);
            }
        }
        return null;
    }

    public void openView(Player player, Shop shop) {
        EconomyManager economy = plugin.getEconomyManager();
        ownerViewSessions.remove(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(player, 27, VIEW_GUI_TITLE + PastequeSkyblockPlugin.color(" &7- &f" + trim(shop.getName())));
        GuiHelper.addTopBorder(inventory);
        GuiHelper.addBottomBorder(inventory);
        inventory.setItem(13, withName(shop.getTemplate(), "&a" + shop.getName(), Arrays.asList(
                PastequeSkyblockPlugin.color("&7Objet : &f" + readable(shop.getTemplate())),
                PastequeSkyblockPlugin.color("&7Stock : &a" + shop.getStock()),
                PastequeSkyblockPlugin.color("&7Prix : &e" + economy.format(shop.getPrice())),
                PastequeSkyblockPlugin.color("&8Clic droit pour acheter")
        )));
        inventory.setItem(18, GuiHelper.closeButton());
        GuiHelper.fillEmpty(inventory);
        player.openInventory(inventory);
    }

    public void openOwnerManage(Player player, Shop shop) {
        EconomyManager economy = plugin.getEconomyManager();
        ownerViewSessions.put(player.getUniqueId(), shop);
        Inventory inventory = Bukkit.createInventory(player, 54, OWNER_GUI_TITLE + PastequeSkyblockPlugin.color(" &7- &f" + trim(shop.getName())));
        GuiHelper.addTopBorder(inventory);
        inventory.setItem(13, withName(shop.getTemplate(), "&a" + shop.getName(), Arrays.asList(
                PastequeSkyblockPlugin.color("&7Objet : &f" + readable(shop.getTemplate())),
                PastequeSkyblockPlugin.color("&7Stock actuel : &a" + shop.getStock()),
                PastequeSkyblockPlugin.color("&7Prix : &e" + economy.format(shop.getPrice())),
                PastequeSkyblockPlugin.color("&8Deposez des items en bas pour ajouter du stock")
        )));
        inventory.setItem(45, GuiHelper.closeButton());
        inventory.setItem(49, GuiHelper.createItem(Material.EMERALD_BLOCK, "&a&lValider l'ajout de stock"));
        inventory.setItem(50, GuiHelper.createItem(Material.CHEST, "&e&lRetirer 1 stack"));
        inventory.setItem(51, GuiHelper.createItem(Material.HOPPER, "&c&lRetirer tout le stock"));
        GuiHelper.fillEmpty(inventory);
        player.openInventory(inventory);
    }

    public boolean handleOwnerInventory(Player player, Inventory inventory, Shop shop, int rawSlot) {
        if (rawSlot == 49) {
            ItemStack template = shop.getTemplate();
            int added = 0;
            for (int slot = 27; slot <= 44; slot++) {
                ItemStack item = inventory.getItem(slot);
                if (item == null || item.getType() == Material.AIR) continue;
                if (!one(item).isSimilar(template)) {
                    player.sendMessage(message("messages.invalid-mixed-stock"));
                    return true;
                }
                added += item.getAmount();
                inventory.setItem(slot, null);
            }
            if (added > 0) {
                shop.addStock(added);
                updateSign(shop);
                save();
                player.sendMessage(message("messages.stock-added"));
            }
            return true;
        }
        if (rawSlot == 50) {
            withdraw(player, shop, Math.min(64, shop.getStock()));
            return true;
        }
        if (rawSlot == 51) {
            withdraw(player, shop, shop.getStock());
            return true;
        }
        return false;
    }

    private void withdraw(Player player, Shop shop, int amount) {
        if (amount <= 0) {
            player.sendMessage(message("messages.stock-empty-return"));
            return;
        }
        ItemStack item = shop.getTemplate();
        int left = amount;
        while (left > 0) {
            int batch = Math.min(item.getMaxStackSize(), left);
            ItemStack stack = item.clone();
            stack.setAmount(batch);
            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
            if (!overflow.isEmpty()) {
                for (ItemStack overflowItem : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), overflowItem);
                }
            }
            left -= batch;
        }
        shop.takeStock(amount);
        updateSign(shop);
        save();
        player.sendMessage(message("messages.stock-withdrawn"));
    }

    public Shop getOpenOwnerSession(UUID uuid) {
        return ownerViewSessions.get(uuid);
    }

    public void clearOpenOwnerSession(UUID uuid, Inventory inventory) {
        Shop shop = ownerViewSessions.remove(uuid);
        if (shop == null || inventory == null) return;
        for (int slot = 27; slot <= 44; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                HashMap<Integer, ItemStack> overflow = Bukkit.getPlayer(uuid) != null ? Bukkit.getPlayer(uuid).getInventory().addItem(item) : new HashMap<Integer, ItemStack>();
                if (Bukkit.getPlayer(uuid) != null) {
                    for (ItemStack overflowItem : overflow.values()) {
                        Bukkit.getPlayer(uuid).getWorld().dropItemNaturally(Bukkit.getPlayer(uuid).getLocation(), overflowItem);
                    }
                }
            }
        }
    }

    public boolean buyOne(Player buyer, Shop shop) {
        EconomyManager economy = plugin.getEconomyManager();
        int buyAmount = Math.max(1, plugin.getConfig().getInt("shops.buy-amount", 1));
        if (shop.getStock() < buyAmount) {
            buyer.sendMessage(message("messages.no-stock"));
            return false;
        }
        if (!economy.take(buyer.getUniqueId(), shop.getPrice())) {
            buyer.sendMessage(message("messages.not-enough-pasteque"));
            return false;
        }
        economy.add(shop.getOwner(), shop.getPrice());
        ItemStack item = shop.getTemplate();
        int amountLeft = buyAmount;
        while (amountLeft > 0) {
            int batch = Math.min(item.getMaxStackSize(), amountLeft);
            ItemStack stack = item.clone();
            stack.setAmount(batch);
            HashMap<Integer, ItemStack> overflow = buyer.getInventory().addItem(stack);
            for (ItemStack overflowItem : overflow.values()) {
                buyer.getWorld().dropItemNaturally(buyer.getLocation(), overflowItem);
            }
            amountLeft -= batch;
        }
        shop.takeStock(buyAmount);
        updateSign(shop);
        save();
        buyer.sendMessage(prefix() + PastequeSkyblockPlugin.color(plugin.getConfig().getString("messages.bought").replace("%item%", readable(shop.getTemplate())).replace("%price%", economy.format(shop.getPrice()))));
        Player owner = Bukkit.getPlayer(shop.getOwner());
        if (owner != null) {
            owner.sendMessage(prefix() + PastequeSkyblockPlugin.color(plugin.getConfig().getString("messages.sold-owner").replace("%buyer%", buyer.getName()).replace("%item%", readable(shop.getTemplate())).replace("%price%", economy.format(shop.getPrice()))));
        }
        return true;
    }

    public boolean deleteShop(Player player, String shopName) {
        Shop shop = getShopByName(player.getUniqueId(), shopName);
        if (shop == null) return false;
        withdraw(player, shop, shop.getStock());
        removeShop(shop, true);
        player.sendMessage(prefix() + PastequeSkyblockPlugin.color(plugin.getConfig().getString("messages.shop-deleted").replace("%shop%", shop.getName())));
        return true;
    }

    public boolean adminDelete(String ownerName, String shopName) {
        Shop shop = getShopByName(ownerName, shopName);
        if (shop == null) return false;
        removeShop(shop, true);
        return true;
    }

    public void removeShop(Shop shop, boolean saveNow) {
        shopsByLocation.remove(shop.locationKey());
        Map<String, Shop> byName = shopsByOwner.get(shop.getOwner());
        if (byName != null) {
            byName.remove(normalize(shop.getName()));
            if (byName.isEmpty()) shopsByOwner.remove(shop.getOwner());
        }
        Block block = shop.getSignLocation().getBlock();
        if (block.getType() == Material.SIGN_POST || block.getType() == Material.WALL_SIGN) {
            block.setType(Material.AIR);
        }
        if (saveNow) save();
    }

    public void updateSign(Shop shop) {
        EconomyManager economy = plugin.getEconomyManager();
        Block block = shop.getSignLocation().getBlock();
        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            sign.setLine(0, PastequeSkyblockPlugin.color("&2&l[SHOP]"));
            sign.setLine(1, PastequeSkyblockPlugin.color("&f" + trim(shop.getName())));
            sign.setLine(2, PastequeSkyblockPlugin.color("&7Stock: &a" + shop.getStock()));
            sign.setLine(3, PastequeSkyblockPlugin.color("&e" + economy.format(shop.getPrice())));
            sign.update(true);
        }
    }

    // ── Helpers ──

    private void registerShop(Shop shop) {
        shopsByLocation.put(shop.locationKey(), shop);
        Map<String, Shop> byName = shopsByOwner.get(shop.getOwner());
        if (byName == null) {
            byName = new HashMap<String, Shop>();
            shopsByOwner.put(shop.getOwner(), byName);
        }
        byName.put(normalize(shop.getName()), shop);
    }

    private String key(Location location) {
        return location.getWorld().getName() + ";" + location.getBlockX() + ";" + location.getBlockY() + ";" + location.getBlockZ();
    }

    private ItemStack one(ItemStack stack) {
        ItemStack clone = stack.clone();
        clone.setAmount(1);
        return clone;
    }

    private String trim(String value) {
        return value.length() > 15 ? value.substring(0, 15) : value;
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ENGLISH).replace(' ', '_');
    }

    private String readable(ItemStack item) {
        return item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? ChatColor.stripColor(item.getItemMeta().getDisplayName()) : item.getType().name();
    }

    private ItemStack button(Material material, String name) {
        ItemStack item = new ItemStack(material, 1);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(PastequeSkyblockPlugin.color(name));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack withName(ItemStack base, String name, List<String> lore) {
        ItemStack item = base.clone();
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(PastequeSkyblockPlugin.color(name));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public String prefix() {
        return PastequeSkyblockPlugin.color("&2Pasteque &5Shop &8>> ");
    }

    public String message(String path) {
        return prefix() + PastequeSkyblockPlugin.color(plugin.getConfig().getString(path, path));
    }
}
