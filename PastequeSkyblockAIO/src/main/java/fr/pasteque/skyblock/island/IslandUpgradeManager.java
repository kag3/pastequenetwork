package fr.pasteque.skyblock.island;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.gui.GuiHelper;
import fr.pasteque.skyblock.island.model.IslandUpgrade;
import fr.pasteque.skyblock.manager.DataFile;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class IslandUpgradeManager {

    public static final String GUI_TITLE = PastequeSkyblockPlugin.color("&2&lPasteque &5&lAmeliorations");

    private final PastequeSkyblockPlugin plugin;
    private final DataFile dataFile;
    private final Map<UUID, Map<IslandUpgrade, Integer>> upgradeLevels = new HashMap<UUID, Map<IslandUpgrade, Integer>>();

    public IslandUpgradeManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new DataFile(plugin, "island-upgrades.yml");
        load();
    }

    public void load() {
        upgradeLevels.clear();
        ConfigurationSection section = dataFile.getConfig().getConfigurationSection("upgrades");
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
            Map<IslandUpgrade, Integer> levels = new HashMap<IslandUpgrade, Integer>();
            ConfigurationSection playerSection = section.getConfigurationSection(uuidStr);
            if (playerSection != null) {
                for (String upgradeKey : playerSection.getKeys(false)) {
                    try {
                        IslandUpgrade upgrade = IslandUpgrade.valueOf(upgradeKey);
                        levels.put(upgrade, playerSection.getInt(upgradeKey));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            upgradeLevels.put(owner, levels);
        }
    }

    public void save() {
        dataFile.getConfig().set("upgrades", null);
        for (Map.Entry<UUID, Map<IslandUpgrade, Integer>> entry : upgradeLevels.entrySet()) {
            String path = "upgrades." + entry.getKey().toString();
            for (Map.Entry<IslandUpgrade, Integer> upgradeEntry : entry.getValue().entrySet()) {
                dataFile.getConfig().set(path + "." + upgradeEntry.getKey().name(), upgradeEntry.getValue());
            }
        }
        dataFile.save();
    }

    public int getLevel(UUID owner, IslandUpgrade upgrade) {
        Map<IslandUpgrade, Integer> levels = upgradeLevels.get(owner);
        if (levels == null) {
            return 0;
        }
        Integer level = levels.get(upgrade);
        return level == null ? 0 : level;
    }

    public void upgrade(Player player, IslandUpgrade upgrade) {
        UUID owner = player.getUniqueId();
        int currentLevel = getLevel(owner, upgrade);

        if (currentLevel >= upgrade.getMaxLevel()) {
            MessageUtil.send(player, plugin.getPrefix(), "&cCette amelioration est deja au niveau maximum !");
            return;
        }

        int nextLevel = currentLevel + 1;
        double cost = upgrade.getCost(nextLevel);

        if (!plugin.getEconomyManager().take(owner, cost)) {
            MessageUtil.send(player, plugin.getPrefix(), "&cTu n'as pas assez d'argent ! Il te faut &e"
                    + plugin.getEconomyManager().format(cost) + "&c.");
            return;
        }

        Map<IslandUpgrade, Integer> levels = upgradeLevels.get(owner);
        if (levels == null) {
            levels = new HashMap<IslandUpgrade, Integer>();
            upgradeLevels.put(owner, levels);
        }
        levels.put(upgrade, nextLevel);
        save();

        MessageUtil.send(player, plugin.getPrefix(), "&a" + upgrade.getDisplayName()
                + " ameliore au niveau &e" + (nextLevel + 1) + " &a(&f"
                + upgrade.getTierValue(nextLevel) + "&a) !");
    }

    public int getEffectiveValue(UUID owner, IslandUpgrade upgrade) {
        return upgrade.getTierValue(getLevel(owner, upgrade));
    }

    public void openUpgradeGui(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, GUI_TITLE);
        UUID owner = player.getUniqueId();

        // Row 0: decorative border
        GuiHelper.addTopBorder(gui);

        // Row 2: decorative border
        GuiHelper.addBottomBorder(gui);

        IslandUpgrade[] upgrades = IslandUpgrade.values();
        int[] slots = {10, 11, 12, 13, 14};
        Material[] icons = {Material.GRASS, Material.SKULL_ITEM, Material.DIAMOND_PICKAXE, Material.MOB_SPAWNER, Material.IRON_FENCE};

        for (int i = 0; i < upgrades.length && i < slots.length; i++) {
            IslandUpgrade upgrade = upgrades[i];
            int level = getLevel(owner, upgrade);
            boolean maxed = level >= upgrade.getMaxLevel();

            ItemStack item = new ItemStack(icons[i], 1);
            ItemMeta meta = item.getItemMeta();

            meta.setDisplayName(PastequeSkyblockPlugin.color("&e&l" + upgrade.getDisplayName()
                    + " &7Niv. &f" + (level + 1)));

            List<String> lore = new ArrayList<String>();
            lore.add("");
            lore.add(PastequeSkyblockPlugin.color("&8\u258E &7Statistiques"));
            lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Valeur actuelle: &f" + upgrade.getTierValue(level)));

            if (maxed) {
                lore.add("");
                lore.add(PastequeSkyblockPlugin.color("&a&lNIVEAU MAXIMUM"));
            } else {
                int nextLevel = level + 1;
                lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Prochaine valeur: &f" + upgrade.getTierValue(nextLevel)));
                lore.add("");
                lore.add(PastequeSkyblockPlugin.color("&8\u258E &7Cout"));
                lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &e" + plugin.getEconomyManager().format(upgrade.getCost(nextLevel))));
                double bal = plugin.getEconomyManager().getBalance(owner);
                lore.add("");
                if (bal >= upgrade.getCost(nextLevel)) {
                    lore.add(PastequeSkyblockPlugin.color("&e\u25B6 Clic pour ameliorer!"));
                } else {
                    lore.add(PastequeSkyblockPlugin.color("&c\u2716 Pas assez d'argent"));
                }
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
            gui.setItem(slots[i], item);
        }

        // Back button at bottom-left
        gui.setItem(18, GuiHelper.backButton());

        // Close button at bottom-right
        gui.setItem(26, GuiHelper.closeButton());

        // Fill remaining slots with black glass
        GuiHelper.fillEmpty(gui);

        player.openInventory(gui);
    }
}
