package fr.pasteque.skyblock.arena;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.arena.model.ArenaPlayerData;
import fr.pasteque.skyblock.gui.GuiHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class ArenaKitService {

    public static final String GUI_TITLE = PastequeSkyblockPlugin.color("&2&lPasteque &5&lKit Arene");
    private final PastequeSkyblockPlugin plugin;
    private final ArenaWorldService arenaWorldService;

    public ArenaKitService(PastequeSkyblockPlugin plugin, ArenaWorldService arenaWorldService) {
        this.plugin = plugin;
        this.arenaWorldService = arenaWorldService;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);

        GuiHelper.addTopBorder(inv);
        GuiHelper.addBottomBorder(inv);

        // Kit item at center
        inv.setItem(13, createKitItem());

        // Player balance info
        inv.setItem(22, createInfoItem(player));

        // Close button
        inv.setItem(18, GuiHelper.closeButton());

        GuiHelper.fillEmpty(inv);

        player.openInventory(inv);
    }

    private ItemStack createKitItem() {
        ItemStack item = new ItemStack(Material.IRON_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(PastequeSkyblockPlugin.color("&a&lKit d'Arene"));
        List<String> lore = new ArrayList<String>();
        lore.add(PastequeSkyblockPlugin.color(""));
        lore.add(PastequeSkyblockPlugin.color("&8\u25CE &7Contenu du Kit"));
        lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Armure Protection I"));
        lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Epee Tranchant I"));
        lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Arc + 64 fleches"));
        lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &716 Pommes dorees"));
        lore.add(PastequeSkyblockPlugin.color(""));
        lore.add(PastequeSkyblockPlugin.color("&8\u25CE &7Prix"));
        lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &e" + (int) plugin.getConfig().getDouble("kit.price", 250.0D) + " Pasteque"));
        lore.add(PastequeSkyblockPlugin.color(""));
        lore.add(PastequeSkyblockPlugin.color("&a\u25B6 Clic pour acheter!"));
        meta.setLore(lore);
        meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
        // Note: ItemFlag not available in 1.9.4
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInfoItem(Player player) {
        double balance = plugin.getEconomyManager().getBalance(player.getUniqueId());
        return GuiHelper.createItem(Material.MELON, "&d&lVotre Solde",
                "",
                "&8\u25CE &7Informations",
                "&8\u25B8 &7Solde: &e" + plugin.getEconomyManager().format(balance) + " Pasteque",
                "&8\u25B8 &7Monde: &d" + player.getWorld().getName());
    }

    public boolean tryPurchase(Player player) {
        if (!arenaWorldService.isArenaWorld(player.getWorld())) {
            player.sendMessage(plugin.color(plugin.getPrefix() + plugin.getConfig().getString("messages.arena-only", "&fCette action doit etre utilisee depuis l'arene.")));
            return false;
        }
        double price = plugin.getConfig().getDouble("kit.price", 250.0D);
        if (!plugin.getEconomyManager().take(player.getUniqueId(), price)) {
            player.sendMessage(plugin.color(plugin.getPrefix() + plugin.getConfig().getString("messages.not-enough-pasteque", "&fIl vous manque des &dPasteque&f pour cet achat.")));
            return false;
        }
        giveKit(player);
        ArenaPlayerData data = plugin.getPlayerDataService().get(player);
        data.getPurchasedKits().add("default");
        player.sendMessage(plugin.color(plugin.getPrefix() + plugin.getConfig().getString("messages.kit-purchased", "&fVous avez achete le kit d'arene pour &d%price% Pasteque&f.").replace("%price%", String.valueOf((int) price))));
        return true;
    }

    private void giveKit(Player player) {
        player.getInventory().addItem(enchanted(new ItemStack(Material.IRON_HELMET), Enchantment.PROTECTION_ENVIRONMENTAL, 1));
        player.getInventory().addItem(enchanted(new ItemStack(Material.IRON_CHESTPLATE), Enchantment.PROTECTION_ENVIRONMENTAL, 1));
        player.getInventory().addItem(enchanted(new ItemStack(Material.IRON_LEGGINGS), Enchantment.PROTECTION_ENVIRONMENTAL, 1));
        player.getInventory().addItem(enchanted(new ItemStack(Material.IRON_BOOTS), Enchantment.PROTECTION_ENVIRONMENTAL, 1));
        player.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
        player.getInventory().addItem(enchanted(new ItemStack(Material.BOW), Enchantment.ARROW_KNOCKBACK, 1));
        player.getInventory().addItem(new ItemStack(Material.ARROW, 16));
        player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 3));
        player.getInventory().addItem(new ItemStack(Material.GRILLED_PORK, 16));
    }

    private ItemStack enchanted(ItemStack item, Enchantment enchantment, int level) {
        item.addUnsafeEnchantment(enchantment, level);
        return item;
    }
}
