package fr.pasteque.skyblock.farming;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.gui.GuiHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FarmingManager {

    public static final String GUI_TITLE = "\u00a72\u00a7lPasteque \u00a75\u00a7lFarming";
    public static final String SEED_SHOP_TITLE = "\u00a72\u00a7lPasteque \u00a75\u00a7lSeed Shop";

    private static final int[] XP_THRESHOLDS = {
            0, 100, 300, 600, 1000, 2000, 3500, 5500, 8000, 12000, 20000
    };

    private final PastequeSkyblockPlugin plugin;
    private final File file;

    private final Map<UUID, Integer> farmingLevels = new HashMap<UUID, Integer>();
    private final Map<UUID, Integer> farmingXp = new HashMap<UUID, Integer>();
    private final Map<String, CropLocation> crops = new HashMap<String, CropLocation>();

    public FarmingManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "farming-data.yml");
    }

    // ── Persistence ─────────────────────────────────────────────────────

    public void load() {
        farmingLevels.clear();
        farmingXp.clear();
        crops.clear();

        if (!file.exists()) {
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        // Player data
        ConfigurationSection players = cfg.getConfigurationSection("players");
        if (players != null) {
            for (String uuidStr : players.getKeys(false)) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(uuidStr);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                ConfigurationSection sec = players.getConfigurationSection(uuidStr);
                if (sec == null) {
                    continue;
                }
                farmingLevels.put(uuid, sec.getInt("level", 0));
                farmingXp.put(uuid, sec.getInt("xp", 0));
            }
        }

        // Crop locations
        ConfigurationSection cropsSec = cfg.getConfigurationSection("crops");
        if (cropsSec != null) {
            for (String key : cropsSec.getKeys(false)) {
                CropLocation loc = CropLocation.loadFrom(cropsSec.getConfigurationSection(key));
                if (loc != null) {
                    crops.put(loc.getKey(), loc);
                }
            }
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();

        // Player data
        for (Map.Entry<UUID, Integer> entry : farmingLevels.entrySet()) {
            String base = "players." + entry.getKey().toString();
            cfg.set(base + ".level", entry.getValue());
            cfg.set(base + ".xp", farmingXp.containsKey(entry.getKey()) ? farmingXp.get(entry.getKey()) : 0);
        }

        // Crop locations
        int index = 0;
        for (Map.Entry<String, CropLocation> entry : crops.entrySet()) {
            ConfigurationSection sec = cfg.createSection("crops.c" + index);
            entry.getValue().saveTo(sec);
            index++;
        }

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("[Farming] Impossible de sauvegarder farming-data.yml: " + e.getMessage());
        }
    }

    // ── XP & Levels ─────────────────────────────────────────────────────

    public int getFarmingLevel(UUID uuid) {
        Integer level = farmingLevels.get(uuid);
        return level != null ? level : 0;
    }

    public int getFarmingXp(UUID uuid) {
        Integer xp = farmingXp.get(uuid);
        return xp != null ? xp : 0;
    }

    public int getXpForNextLevel(int currentLevel) {
        if (currentLevel >= XP_THRESHOLDS.length - 1) {
            return -1; // Max level
        }
        return XP_THRESHOLDS[currentLevel + 1];
    }

    public void addFarmingXp(UUID uuid, int amount) {
        int currentXp = getFarmingXp(uuid) + amount;
        int currentLevel = getFarmingLevel(uuid);
        farmingXp.put(uuid, currentXp);

        // Check for level up
        while (currentLevel < XP_THRESHOLDS.length - 1 && currentXp >= XP_THRESHOLDS[currentLevel + 1]) {
            currentLevel++;
            farmingLevels.put(uuid, currentLevel);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(PastequeSkyblockPlugin.color(
                        "&2&lPasteque &8\u00bb &a&lFELICITATIONS! &eVous etes maintenant &6Farming Niveau " + currentLevel + "&e!"));
                GuiHelper.playSuccess(player);
            }
            Bukkit.broadcastMessage(PastequeSkyblockPlugin.color(
                    "&2&lPasteque &8\u00bb &e" + (player != null ? player.getName() : uuid.toString())
                            + " &7a atteint le &6Farming Niveau " + currentLevel + "&7!"));
        }
    }

    // ── Crop Location Management ────────────────────────────────────────

    public void registerCrop(CropLocation cropLoc) {
        crops.put(cropLoc.getKey(), cropLoc);
    }

    public CropLocation getCrop(String key) {
        return crops.get(key);
    }

    public void removeCrop(String key) {
        crops.remove(key);
    }

    public Map<String, CropLocation> getCrops() {
        return crops;
    }

    // ── Item Creation ───────────────────────────────────────────────────

    public ItemStack createHarvestItem(CustomCrop crop, int amount) {
        ItemStack item = new ItemStack(crop.getIcon(), amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(PastequeSkyblockPlugin.color(crop.getHarvestName()));
        List<String> lore = new ArrayList<String>();
        lore.add("");
        for (String line : crop.getLoreDesc()) {
            lore.add(PastequeSkyblockPlugin.color(line));
        }
        lore.add("");
        lore.add(PastequeSkyblockPlugin.color("&7Prix de vente: &6" + (int) crop.getSellPrice() + " pasteques"));
        lore.add(PastequeSkyblockPlugin.color(crop.getHarvestId()));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createSeedItem(CustomCrop crop, int amount) {
        ItemStack item = new ItemStack(crop.getBaseMaterial(), amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(PastequeSkyblockPlugin.color(crop.getSeedName()));
        List<String> lore = new ArrayList<String>();
        lore.add("");
        lore.add(PastequeSkyblockPlugin.color("&7Plantez pour faire pousser:"));
        lore.add(PastequeSkyblockPlugin.color("  " + crop.getColoredName()));
        lore.add("");
        if (crop.getRequiredLevel() > 0) {
            lore.add(PastequeSkyblockPlugin.color("&c\u26A0 Niveau requis: &e" + crop.getRequiredLevel()));
        }
        lore.add(PastequeSkyblockPlugin.color(crop.getSeedId()));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ── GUIs ────────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    public void openFarmingGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);

        UUID uuid = player.getUniqueId();
        int level = getFarmingLevel(uuid);
        int xp = getFarmingXp(uuid);
        int nextXp = getXpForNextLevel(level);

        // Row 1: Player info (slot 4)
        String xpBar = buildXpBar(xp, nextXp);
        String nextLevelStr = nextXp == -1 ? "&6NIVEAU MAX" : "&7Prochain: &e" + nextXp + " XP";
        ItemStack playerInfo = GuiHelper.fluidItem(Material.SKULL_ITEM, 3,
                "&a&lVotre Profil Farming",
                "Vos statistiques de farming",
                new String[]{
                        "&7Niveau: &a" + level + " / 10",
                        "&7XP: &e" + xp,
                        nextLevelStr,
                        xpBar
                },
                null);
        inv.setItem(4, playerInfo);

        // Row 2-3: Custom crops (slots 10-16 for first 6)
        CustomCrop[] allCrops = CustomCrop.values();
        int[] cropSlots = {10, 12, 14, 16, 28, 30};
        for (int i = 0; i < allCrops.length && i < cropSlots.length; i++) {
            CustomCrop crop = allCrops[i];
            String levelReq = crop.getRequiredLevel() > 0
                    ? "&cNiveau requis: " + crop.getRequiredLevel()
                    : "&aPas de niveau requis";
            List<String> details = new ArrayList<String>();
            details.add("&7Vente: &6" + (int) crop.getSellPrice() + " pasteques");
            details.add(levelReq);
            details.add("&7XP par recolte: &e" + crop.getXpReward());
            for (String desc : crop.getLoreDesc()) {
                details.add(desc);
            }
            String[] detailsArr = details.toArray(new String[details.size()]);

            boolean unlocked = level >= crop.getRequiredLevel();
            ItemStack cropItem = GuiHelper.fluidItem(crop.getIcon(),
                    crop.getColoredName(),
                    unlocked ? "Culture deverrouillee" : "&cCulture verrouillee",
                    detailsArr,
                    null);
            inv.setItem(cropSlots[i], cropItem);
        }

        // Row 4: Seed shop button (slot 40)
        ItemStack shopButton = GuiHelper.fluidItem(Material.GOLD_INGOT,
                "&6&lBoutique de Graines",
                "Achetez des graines speciales",
                new String[]{"&7Ouvrez la boutique pour acheter", "&7des graines de cultures custom."},
                "Clic pour ouvrir");
        inv.setItem(40, shopButton);

        // Sell button (slot 42)
        ItemStack sellButton = GuiHelper.fluidItem(Material.EMERALD,
                "&a&lVendre vos Recoltes",
                "Vendez toutes vos recoltes custom",
                new String[]{"&7Vend toutes les recoltes custom", "&7dans votre inventaire."},
                "Clic pour vendre");
        inv.setItem(42, sellButton);

        // Close button (slot 49)
        inv.setItem(49, GuiHelper.closeButton());

        GuiHelper.decorate(inv, GuiHelper.Theme.SKILL);
        player.openInventory(inv);
        GuiHelper.playOpen(player);
    }

    @SuppressWarnings("deprecation")
    public void openSeedShop(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, SEED_SHOP_TITLE);

        UUID uuid = player.getUniqueId();
        int level = getFarmingLevel(uuid);

        CustomCrop[] allCrops = CustomCrop.values();
        int[] seedSlots = {10, 12, 14, 16, 28, 30};
        for (int i = 0; i < allCrops.length && i < seedSlots.length; i++) {
            CustomCrop crop = allCrops[i];
            boolean unlocked = level >= crop.getRequiredLevel();
            String status = unlocked ? "&aDeverrouille" : "&cNiveau " + crop.getRequiredLevel() + " requis";

            ItemStack seedItem = GuiHelper.fluidItem(crop.getBaseMaterial(),
                    crop.getSeedName(),
                    "Graine speciale",
                    new String[]{
                            "&7Prix: &6" + crop.getSeedPrice() + " pasteques",
                            status,
                            "&7Produit: " + crop.getColoredName()
                    },
                    unlocked ? "Clic pour acheter" : null);
            inv.setItem(seedSlots[i], seedItem);
        }

        // Back button (slot 48)
        inv.setItem(48, GuiHelper.backButton());

        // Close button (slot 49)
        inv.setItem(49, GuiHelper.closeButton());

        GuiHelper.decorate(inv, GuiHelper.Theme.SKILL);
        player.openInventory(inv);
        GuiHelper.playOpen(player);
    }

    // ── Sell System ─────────────────────────────────────────────────────

    public void sellCrops(Player player) {
        UUID uuid = player.getUniqueId();
        ItemStack[] contents = player.getInventory().getContents();
        double totalEarned = 0;
        int totalSold = 0;
        Map<CustomCrop, Integer> soldBreakdown = new HashMap<CustomCrop, Integer>();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
                continue;
            }
            List<String> lore = item.getItemMeta().getLore();
            for (String line : lore) {
                CustomCrop crop = CustomCrop.fromHarvestId(line);
                if (crop != null) {
                    int amount = item.getAmount();
                    double value = crop.getSellPrice() * amount;
                    totalEarned += value;
                    totalSold += amount;
                    Integer prev = soldBreakdown.get(crop);
                    soldBreakdown.put(crop, (prev != null ? prev : 0) + amount);
                    player.getInventory().setItem(i, null);
                    break;
                }
            }
        }

        if (totalSold == 0) {
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "&2&lPasteque &8\u00bb &cVous n'avez aucune recolte custom a vendre."));
            return;
        }

        plugin.getEconomyManager().add(uuid, totalEarned);

        player.sendMessage(PastequeSkyblockPlugin.color(
                "&2&lPasteque &8\u00bb &a&lRecoltes vendues!"));
        player.sendMessage(PastequeSkyblockPlugin.color(
                "  &7Total: &6" + (int) totalEarned + " pasteques &7(" + totalSold + " items)"));
        for (Map.Entry<CustomCrop, Integer> entry : soldBreakdown.entrySet()) {
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "  &8\u25B8 " + entry.getKey().getColoredName() + " &7x" + entry.getValue()
                            + " &8(&6" + (int) (entry.getKey().getSellPrice() * entry.getValue()) + "&8)"));
        }
        GuiHelper.playSuccess(player);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private String buildXpBar(int xp, int nextXp) {
        if (nextXp == -1) {
            return "&6&l|||||||||||||||||||| &aNIVEAU MAX";
        }
        int filled = (int) ((double) xp / nextXp * 20);
        if (filled > 20) {
            filled = 20;
        }
        StringBuilder bar = new StringBuilder("&a");
        for (int i = 0; i < 20; i++) {
            if (i == filled) {
                bar.append("&7");
            }
            bar.append("|");
        }
        bar.append(" &e").append(Math.round((double) xp / nextXp * 100)).append("%");
        return bar.toString();
    }

    public PastequeSkyblockPlugin getPlugin() {
        return plugin;
    }
}
