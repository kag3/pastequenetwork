package fr.pasteque.skyblock.arena;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.arena.model.ArenaKit;
import fr.pasteque.skyblock.arena.model.ArenaPlayerData;
import fr.pasteque.skyblock.gui.GuiHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class MultiKitGui {

    public static final String GUI_TITLE = PastequeSkyblockPlugin.color("&2&lPasteque &5&lKits d'Arene");
    private static final int SIZE = 54;

    private final PastequeSkyblockPlugin plugin;
    private final List<ArenaKit> kits;

    public MultiKitGui(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.kits = ArenaKit.createDefaultKits();
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, GUI_TITLE);
        ArenaPlayerData data = plugin.getPlayerDataService().get(player);
        int playerLevel = data.getLevel();

        // Row 0: decorative border
        GuiHelper.addTopBorder(inv);

        // Place kits in rows 1-3 (slots 10-16, 19-25, 28-34)
        int[] slots = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        for (int i = 0; i < kits.size() && i < slots.length; i++) {
            ArenaKit kit = kits.get(i);
            int slot = slots[i];
            boolean canUse = playerLevel >= kit.getRequiredLevel();

            // Kit icon
            Material iconMat;
            try {
                iconMat = Material.valueOf(kit.getIcon());
            } catch (IllegalArgumentException e) {
                iconMat = Material.CHEST;
            }

            ItemStack icon = new ItemStack(iconMat);
            ItemMeta meta = icon.getItemMeta();

            String nameColor = canUse ? "&a" : "&c";
            meta.setDisplayName(PastequeSkyblockPlugin.color(nameColor + "&l" + kit.getDisplayName()));

            List<String> lore = new ArrayList<String>();
            lore.add("");
            lore.add(PastequeSkyblockPlugin.color("&8\u258E &7Contenu"));
            lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Armure: &f" + kit.getDisplayName()));
            lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Arme: &f" + kit.getIcon()));
            lore.add("");
            lore.add(PastequeSkyblockPlugin.color("&8\u258E &7Niveau requis: &d" + kit.getRequiredLevel()));
            if (kit.getPrice() > 0) {
                lore.add(PastequeSkyblockPlugin.color("&8\u258E &7Prix: &e" + kit.getPrice() + "$"));
            } else {
                lore.add(PastequeSkyblockPlugin.color("&8\u258E &7Prix: &aGratuit"));
            }
            lore.add("");

            if (canUse) {
                lore.add(PastequeSkyblockPlugin.color("&a\u25B6 Clic pour equiper!"));
            } else {
                lore.add(PastequeSkyblockPlugin.color("&c\u2716 Niveau " + kit.getRequiredLevel() + " requis"));
            }

            meta.setLore(lore);
            icon.setItemMeta(meta);
            inv.setItem(slot, icon);
        }

        // Row 5: decorative border + back button
        GuiHelper.addBottomBorder(inv);
        inv.setItem(45, GuiHelper.backButton());

        // Fill empty with black glass
        GuiHelper.fillEmpty(inv);
        player.openInventory(inv);
    }

    public ArenaKit getKitBySlot(int slot) {
        int[] slots = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        for (int i = 0; i < slots.length && i < kits.size(); i++) {
            if (slots[i] == slot) {
                return kits.get(i);
            }
        }
        return null;
    }

    public List<ArenaKit> getKits() {
        return kits;
    }

    public void equipKit(Player player, ArenaKit kit) {
        ArenaPlayerData data = plugin.getPlayerDataService().get(player);
        if (data.getLevel() < kit.getRequiredLevel()) {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cVous devez etre niveau &d" + kit.getRequiredLevel() + " &cpour utiliser ce kit."));
            return;
        }

        if (kit.getPrice() > 0 && !data.getPurchasedKits().contains(kit.getName())) {
            if (!plugin.getEconomyManager().take(player.getUniqueId(), kit.getPrice())) {
                player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cVous n'avez pas assez de " + plugin.getEconomyManager().getCurrencyName() + "."));
                return;
            }
            data.getPurchasedKits().add(kit.getName());
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&fKit &d" + kit.getDisplayName() + " &fachete pour &d" + kit.getPrice() + " " + plugin.getEconomyManager().getCurrencyName() + "&f."));
        }

        // Clear and equip
        player.getInventory().clear();
        ItemStack[] armor = kit.getArmor();
        if (armor[0] != null) player.getInventory().setBoots(armor[0].clone());
        if (armor[1] != null) player.getInventory().setLeggings(armor[1].clone());
        if (armor[2] != null) player.getInventory().setChestplate(armor[2].clone());
        if (armor[3] != null) player.getInventory().setHelmet(armor[3].clone());

        for (ItemStack item : kit.getItems()) {
            if (item != null) {
                player.getInventory().addItem(item.clone());
            }
        }

        player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&fVous etes equipe du kit &d" + kit.getDisplayName() + "&f."));
        player.closeInventory();
    }
}
