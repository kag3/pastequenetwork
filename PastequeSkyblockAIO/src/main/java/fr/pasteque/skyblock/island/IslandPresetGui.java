package fr.pasteque.skyblock.island;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.gui.GuiHelper;
import fr.pasteque.skyblock.island.model.IslandPreset;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class IslandPresetGui {

    public static final String GUI_TITLE = PastequeSkyblockPlugin.color("&2&lPasteque &5&lChoix d'Ile");

    private final PastequeSkyblockPlugin plugin;

    public IslandPresetGui(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 36, GUI_TITLE);

        // Row 0: decorative border
        GuiHelper.addTopBorder(gui);

        // Row 1-2: preset items
        IslandPreset[] presets = IslandPreset.values();
        int[] slots;
        if (presets.length <= 7) {
            slots = new int[]{10, 11, 12, 13, 14, 15, 16};
        } else {
            slots = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        }

        for (int i = 0; i < presets.length && i < slots.length; i++) {
            IslandPreset preset = presets[i];
            ItemStack item = parseIcon(preset.getIcon());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(PastequeSkyblockPlugin.color(preset.getColor() + "&l" + preset.getDisplayName()));

            List<String> lore = new ArrayList<String>();
            lore.add("");
            lore.add(PastequeSkyblockPlugin.color("&8\u258E &7Description"));
            lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7" + preset.getDescription()));
            lore.add("");
            lore.add(PastequeSkyblockPlugin.color("&e\u25B6 Clic pour choisir!"));
            meta.setLore(lore);
            item.setItemMeta(meta);

            gui.setItem(slots[i], item);
        }

        // Row 3: info book at center
        gui.setItem(31, GuiHelper.createItem(Material.BOOK,
                "&b&lInformation",
                "",
                "&7Choisis un type d'ile pour",
                "&7commencer ton aventure !",
                "",
                "&8\u25B8 &7Chaque ile est unique."));

        // Fill remaining with black glass
        GuiHelper.fillEmpty(gui);

        player.openInventory(gui);
    }

    private ItemStack parseIcon(String iconStr) {
        if (iconStr.contains(":")) {
            String[] parts = iconStr.split(":");
            Material mat = Material.getMaterial(parts[0]);
            if (mat == null) {
                mat = Material.STONE;
            }
            short data = 0;
            try {
                data = Short.parseShort(parts[1]);
            } catch (NumberFormatException ignored) {
            }
            return new ItemStack(mat, 1, data);
        }
        Material mat = Material.getMaterial(iconStr);
        if (mat == null) {
            mat = Material.STONE;
        }
        return new ItemStack(mat, 1);
    }
}
