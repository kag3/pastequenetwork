package fr.pasteque.skyblock.collection;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.collection.model.CollectionCategory;
import fr.pasteque.skyblock.collection.model.CollectionEntry;
import fr.pasteque.skyblock.collection.model.PlayerCollections;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class CollectionManager {

    public static final String MAIN_TITLE = PastequeSkyblockPlugin.color("&2&lPasteque &5&lCollections");
    public static final String CATEGORY_TITLE_PREFIX = PastequeSkyblockPlugin.color("&2&lPasteque &5&l");

    private final PastequeSkyblockPlugin plugin;
    private final File file;
    private final HashMap<UUID, PlayerCollections> cache = new HashMap<UUID, PlayerCollections>();
    private final List<CollectionEntry> entries;

    public CollectionManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "collections.yml");
        this.entries = CollectionEntry.createDefaults();
    }

    // -- Persistence ----------------------------------------------------------

    public void load() {
        cache.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = cfg.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String uuidStr : players.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                continue;
            }
            PlayerCollections pc = new PlayerCollections();
            ConfigurationSection section = players.getConfigurationSection(uuidStr);
            if (section == null) {
                continue;
            }
            ConfigurationSection countsSec = section.getConfigurationSection("counts");
            if (countsSec != null) {
                for (String mat : countsSec.getKeys(false)) {
                    pc.getCounts().put(mat, countsSec.getInt(mat));
                }
            }
            ConfigurationSection claimedSec = section.getConfigurationSection("claimed");
            if (claimedSec != null) {
                for (String entryId : claimedSec.getKeys(false)) {
                    pc.getClaimedTiers().put(entryId, claimedSec.getInt(entryId));
                }
            }
            cache.put(uuid, pc);
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerCollections> entry : cache.entrySet()) {
            String base = "players." + entry.getKey().toString();
            PlayerCollections pc = entry.getValue();
            for (Map.Entry<String, Integer> count : pc.getCounts().entrySet()) {
                cfg.set(base + ".counts." + count.getKey(), count.getValue());
            }
            for (Map.Entry<String, Integer> claimed : pc.getClaimedTiers().entrySet()) {
                cfg.set(base + ".claimed." + claimed.getKey(), claimed.getValue());
            }
        }
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder collections.yml: " + e.getMessage());
        }
    }

    // -- Data access ----------------------------------------------------------

    public PlayerCollections getPlayerCollections(UUID uuid) {
        PlayerCollections pc = cache.get(uuid);
        if (pc == null) {
            pc = new PlayerCollections();
            cache.put(uuid, pc);
        }
        return pc;
    }

    public List<CollectionEntry> getEntries() {
        return entries;
    }

    public CollectionEntry getEntryByMaterial(String material) {
        for (CollectionEntry entry : entries) {
            if (entry.getMaterial().equals(material)) {
                return entry;
            }
        }
        return null;
    }

    public CollectionEntry getEntryById(String id) {
        for (CollectionEntry entry : entries) {
            if (entry.getId().equals(id)) {
                return entry;
            }
        }
        return null;
    }

    // -- Collection tracking --------------------------------------------------

    public void addCollection(Player player, String material, int amount) {
        PlayerCollections pc = getPlayerCollections(player.getUniqueId());
        int oldCount = pc.getCount(material);
        pc.addCount(material, amount);
        int newCount = pc.getCount(material);

        for (CollectionEntry entry : entries) {
            if (!entry.getMaterial().equals(material)) {
                continue;
            }
            int[] tiers = entry.getTiers();
            int claimed = pc.getClaimedTier(entry.getId());
            for (int i = 0; i < tiers.length; i++) {
                int tierNum = i + 1;
                if (tierNum > claimed && oldCount < tiers[i] && newCount >= tiers[i]) {
                    player.sendMessage(PastequeSkyblockPlugin.color(
                            "&6&l>> &eCollection " + entry.getDisplayName()
                                    + " &7palier &a" + tierNum + " &7debloque ! &7(&e"
                                    + newCount + "/" + tiers[i] + "&7)"
                    ));
                }
            }
        }
    }

    // -- Tier rewards ---------------------------------------------------------

    public double getTierReward(CollectionEntry entry, int tier) {
        switch (tier) {
            case 1: return 100;
            case 2: return 300;
            case 3: return 700;
            case 4: return 1500;
            case 5: return 5000;
            default: return 0;
        }
    }

    // -- GUIs -----------------------------------------------------------------

    public void openCollectionGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, MAIN_TITLE);

        // Row 0: decorative border
        GuiHelper.addTopBorder(inv);

        // Row 1: category icons (centered)
        CollectionCategory[] categories = CollectionCategory.values();
        int[] slots = {11, 12, 13, 14, 15};
        for (int i = 0; i < categories.length && i < slots.length; i++) {
            CollectionCategory cat = categories[i];
            Material iconMat = Material.matchMaterial(cat.getIcon());
            if (iconMat == null) {
                iconMat = Material.BEDROCK;
            }
            ItemStack item = new ItemStack(iconMat);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(PastequeSkyblockPlugin.color(cat.getColor() + "&l" + cat.getDisplayName()));

            List<String> lore = new ArrayList<String>();
            lore.add("");
            int total = 0;
            int unlocked = 0;
            PlayerCollections pc = getPlayerCollections(player.getUniqueId());
            for (CollectionEntry entry : entries) {
                if (entry.getCategory() == cat) {
                    total++;
                    if (pc.getCount(entry.getMaterial()) > 0) {
                        unlocked++;
                    }
                }
            }
            lore.add(PastequeSkyblockPlugin.color("&8\u258E &7Progression"));
            lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Decouvertes: &a" + unlocked + "&8/&f" + total));
            lore.add("");
            lore.add(PastequeSkyblockPlugin.color("&e\u25B6 Clic pour ouvrir!"));
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slots[i], item);
        }

        // Row 3: back button
        inv.setItem(27, GuiHelper.backButton());

        // Fill remaining with black glass
        GuiHelper.fillEmpty(inv);

        player.openInventory(inv);
    }

    public void openCategoryGui(Player player, CollectionCategory category) {
        List<CollectionEntry> catEntries = new ArrayList<CollectionEntry>();
        for (CollectionEntry entry : entries) {
            if (entry.getCategory() == category) {
                catEntries.add(entry);
            }
        }

        int size = Math.max(36, (int) Math.ceil((catEntries.size() + 9) / 9.0) * 9 + 18);
        if (size > 54) {
            size = 54;
        }
        String title = CATEGORY_TITLE_PREFIX + PastequeSkyblockPlugin.color(category.getColor() + category.getDisplayName());
        Inventory inv = Bukkit.createInventory(null, size, title);

        // Row 0: decorative border
        GuiHelper.addTopBorder(inv);

        // Bottom border
        GuiHelper.addBottomBorder(inv);

        // Back button at bottom-left, close button at bottom-right
        inv.setItem(size - 9, GuiHelper.backButton());
        inv.setItem(size - 1, GuiHelper.closeButton());

        PlayerCollections pc = getPlayerCollections(player.getUniqueId());
        int slot = 10;
        int rowEnd = 16;
        for (CollectionEntry entry : catEntries) {
            if (slot >= size - 9) {
                break;
            }
            // Skip border columns for symmetry
            if (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
                if (slot > rowEnd) {
                    slot = ((slot / 9) + 1) * 9 + 1;
                    rowEnd = slot + 6;
                }
                continue;
            }

            Material iconMat = Material.matchMaterial(entry.getMaterial());
            if (iconMat == null) {
                iconMat = Material.BEDROCK;
            }
            ItemStack item = new ItemStack(iconMat);
            ItemMeta meta = item.getItemMeta();

            int count = pc.getCount(entry.getMaterial());
            int[] tiers = entry.getTiers();
            int currentTier = 0;
            for (int i = 0; i < tiers.length; i++) {
                if (count >= tiers[i]) {
                    currentTier = i + 1;
                }
            }
            int nextTier = currentTier < tiers.length ? tiers[currentTier] : tiers[tiers.length - 1];

            meta.setDisplayName(PastequeSkyblockPlugin.color(
                    category.getColor() + "&l" + entry.getDisplayName() + " &7(" + count + ")"
            ));

            List<String> lore = new ArrayList<String>();
            lore.add("");
            lore.add(PastequeSkyblockPlugin.color("&8\u258E &7Paliers"));
            for (int i = 0; i < tiers.length; i++) {
                int claimed = pc.getClaimedTier(entry.getId());
                String prefix;
                if (i + 1 <= claimed) {
                    prefix = "&a\u2714 ";
                } else if (count >= tiers[i]) {
                    prefix = "&e\u25B6 ";
                } else {
                    prefix = "&c\u2716 ";
                }
                lore.add(PastequeSkyblockPlugin.color(
                        "&8\u25B8 " + prefix + "&7Palier " + (i + 1) + ": &f" + tiers[i]
                                + " &8- &7" + entry.getTierRewards()[i]
                ));
            }
            lore.add("");
            // Progress bar
            double progress = currentTier >= tiers.length ? 1.0 : (double) count / nextTier;
            lore.add(buildProgressBar(progress));

            // If there is an unclaimed tier, show click hint
            int claimed = pc.getClaimedTier(entry.getId());
            if (currentTier > claimed) {
                lore.add("");
                lore.add(PastequeSkyblockPlugin.color("&a\u25B6 Clic pour recuperer!"));
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot, item);
            slot++;
            if (slot > rowEnd) {
                slot = ((slot / 9) + 1) * 9 + 1;
                rowEnd = slot + 6;
            }
        }

        // Fill remaining with black glass
        GuiHelper.fillEmpty(inv);

        player.openInventory(inv);
    }

    private String buildProgressBar(double progress) {
        int totalBars = 20;
        int filled = (int) (progress * totalBars);
        if (filled > totalBars) {
            filled = totalBars;
        }
        StringBuilder bar = new StringBuilder();
        bar.append("&a");
        for (int i = 0; i < filled; i++) {
            bar.append("|");
        }
        bar.append("&7");
        for (int i = filled; i < totalBars; i++) {
            bar.append("|");
        }
        return PastequeSkyblockPlugin.color(bar.toString());
    }

    // -- Getter ---------------------------------------------------------------

    public PastequeSkyblockPlugin getPlugin() {
        return plugin;
    }
}
