package fr.pasteque.skyblock.island;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.island.model.IslandPreset;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class IslandPresetGui {

    public static final String GUI_TITLE = PastequeSkyblockPlugin.color("&2&lPasteque &5&lChoix d'Ile");

    private final PastequeSkyblockPlugin plugin;

    public IslandPresetGui(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, GUI_TITLE);

        // Fill all slots with black stained glass
        ItemStack filler = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        for (int s = 0; s < 27; s++) {
            gui.setItem(s, filler);
        }

        // Top border: alternating green (13) and purple (10) glass
        for (int s = 0; s < 9; s++) {
            short borderColor = (s % 2 == 0) ? (short) 13 : (short) 10;
            ItemStack border = new ItemStack(Material.STAINED_GLASS_PANE, 1, borderColor);
            ItemMeta borderMeta = border.getItemMeta();
            borderMeta.setDisplayName(" ");
            border.setItemMeta(borderMeta);
            gui.setItem(s, border);
        }

        // Place preset items in middle row
        IslandPreset[] presets = IslandPreset.values();
        int[] slots = {10, 11, 12, 13, 14, 15};

        for (int i = 0; i < presets.length && i < slots.length; i++) {
            IslandPreset preset = presets[i];
            ItemStack item = parseIcon(preset.getIcon());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(PastequeSkyblockPlugin.color(preset.getColor() + "&l" + preset.getDisplayName()));

            List<String> lore = new ArrayList<String>();
            lore.add("");
            lore.add(PastequeSkyblockPlugin.color("&7" + preset.getDescription()));
            lore.add("");
            lore.add(PastequeSkyblockPlugin.color("&2> &aCliquez pour choisir"));
            meta.setLore(lore);
            item.setItemMeta(meta);

            gui.setItem(slots[i], item);
        }

        player.openInventory(gui);
    }

    @SuppressWarnings("deprecation")
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
