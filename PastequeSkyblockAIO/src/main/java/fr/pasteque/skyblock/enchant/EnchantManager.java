package fr.pasteque.skyblock.enchant;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.gui.GuiHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class EnchantManager {

    public static final String GUI_TITLE = "\u00a72\u00a7lPasteque \u00a75\u00a7lEnchantements";

    private final PastequeSkyblockPlugin plugin;
    private final File file;
    private YamlConfiguration config;
    private final Map<UUID, Set<CustomEnchant>> unlocked = new HashMap<UUID, Set<CustomEnchant>>();

    public EnchantManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "enchant-data.yml");
        load();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Persistence
    // ─────────────────────────────────────────────────────────────────────

    public void load() {
        unlocked.clear();
        if (!file.exists()) {
            config = new YamlConfiguration();
            return;
        }
        config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("unlocked");
        if (section == null) return;
        for (String uuidStr : section.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                continue;
            }
            List<String> names = section.getStringList(uuidStr);
            Set<CustomEnchant> set = new HashSet<CustomEnchant>();
            for (String name : names) {
                CustomEnchant ce = CustomEnchant.fromName(name);
                if (ce != null) set.add(ce);
            }
            unlocked.put(uuid, set);
        }
    }

    public void save() {
        config.set("unlocked", null);
        for (Map.Entry<UUID, Set<CustomEnchant>> entry : unlocked.entrySet()) {
            java.util.List<String> names = new java.util.ArrayList<String>();
            for (CustomEnchant ce : entry.getValue()) {
                names.add(ce.name());
            }
            config.set("unlocked." + entry.getKey().toString(), names);
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("[Enchant] Impossible de sauvegarder enchant-data.yml: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Unlock tracking
    // ─────────────────────────────────────────────────────────────────────

    public void unlockEnchant(UUID uuid, CustomEnchant enchant) {
        Set<CustomEnchant> set = unlocked.get(uuid);
        if (set == null) {
            set = new HashSet<CustomEnchant>();
            unlocked.put(uuid, set);
        }
        set.add(enchant);
    }

    public boolean hasUnlocked(UUID uuid, CustomEnchant enchant) {
        Set<CustomEnchant> set = unlocked.get(uuid);
        if (set == null) return false;
        return set.contains(enchant);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  GUI
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Opens the enchant table GUI for the given player.
     * Layout: 54 slots, enchants in rows 2-3 (slots 10-16, 19-25),
     * with coloured glass behind indicating unlock status.
     */
    public void openEnchantTable(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);

        GuiHelper.decorate(inv, GuiHelper.Theme.SKILL);

        UUID uuid = player.getUniqueId();
        CustomEnchant[] enchants = CustomEnchant.values();

        // Place enchants in the two middle rows: slots 10-13 and 19-22
        int[] slots = {10, 11, 12, 13, 19, 20, 21, 22};

        for (int i = 0; i < enchants.length && i < slots.length; i++) {
            CustomEnchant ce = enchants[i];
            boolean playerUnlocked = hasUnlocked(uuid, ce);
            int currentLevel = 0;

            // Check current level on held item
            ItemStack hand = player.getItemInHand();
            if (hand != null && hand.getType() != Material.AIR) {
                currentLevel = ce.getLevel(hand);
            }

            int nextLevel = currentLevel + 1;
            boolean maxed = currentLevel >= ce.getMaxLevel();

            // Build the GUI item
            String statusLine;
            String ctaLine;
            if (maxed) {
                statusLine = "&a&lMAX";
                ctaLine = "&7Niveau maximum atteint.";
            } else if (!playerUnlocked) {
                statusLine = "&c&lVerrouille";
                ctaLine = "&eClic pour debloquer (" + ce.getCost(1) + " Pasteques)";
            } else {
                statusLine = "&aNiveau " + currentLevel + " &8\u279C &aNiveau " + nextLevel;
                ctaLine = "&eClic pour appliquer (" + ce.getCost(nextLevel) + " Pasteques)";
            }

            String[] details = new String[ce.getDescription().length + 2];
            for (int d = 0; d < ce.getDescription().length; d++) {
                details[d] = ce.getDescription()[d];
            }
            details[ce.getDescription().length] = "";
            details[ce.getDescription().length + 1] = "&7Niveau max: &f" + ce.getMaxLevel()
                    + " &8| &7Statut: " + statusLine;

            ItemStack item = GuiHelper.fluidItem(
                    ce.getIcon(),
                    ce.getColor() + "&l" + ce.getDisplayName(),
                    "Enchantement personnalise",
                    details,
                    ctaLine
            );
            inv.setItem(slots[i], item);

            // Glass pane behind: green if unlocked, red if locked
            int bgSlot = slots[i] + 9;
            if (bgSlot < 54) {
                int glassData = playerUnlocked ? 5 : 14; // 5 = lime, 14 = red
                inv.setItem(bgSlot, GuiHelper.glassPane(glassData, " "));
            }
        }

        // Close button
        inv.setItem(49, GuiHelper.closeButton());

        GuiHelper.playOpen(player);
        player.openInventory(inv);
    }

    public PastequeSkyblockPlugin getPlugin() {
        return plugin;
    }
}
