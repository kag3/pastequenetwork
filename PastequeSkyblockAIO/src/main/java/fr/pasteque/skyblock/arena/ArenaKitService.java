package fr.pasteque.skyblock.arena;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.arena.model.ArenaPlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ArenaKitService {

    public static final String GUI_TITLE = ChatColor.DARK_GREEN + "Boutique d'arene";
    private final PastequeSkyblockPlugin plugin;
    private final ArenaWorldService arenaWorldService;

    public ArenaKitService(PastequeSkyblockPlugin plugin, ArenaWorldService arenaWorldService) {
        this.plugin = plugin;
        this.arenaWorldService = arenaWorldService;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, GUI_TITLE);
        fillBorders(inventory);
        inventory.setItem(13, createKitItem());
        inventory.setItem(22, createInfoItem(player));
        player.openInventory(inventory);
    }

    private void fillBorders(Inventory inventory) {
        short[] colors = new short[]{5, 2};
        for (int i = 0; i < inventory.getSize(); i++) {
            if (i >= 10 && i <= 16 || i == 22) {
                continue;
            }
            ItemStack pane = new ItemStack(Material.STAINED_GLASS_PANE, 1, colors[i % colors.length]);
            ItemMeta meta = pane.getItemMeta();
            meta.setDisplayName(ChatColor.RESET.toString());
            pane.setItemMeta(meta);
            inventory.setItem(i, pane);
        }
    }

    private ItemStack createKitItem() {
        ItemStack item = new ItemStack(Material.IRON_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.color("&2&lKit d'arene"));
        List<String> lore = new ArrayList<String>();
        for (String line : plugin.getConfig().getStringList("kit.description")) {
            lore.add(plugin.color(line.replace("%price%", String.valueOf((int) plugin.getConfig().getDouble("kit.price", 250.0D)))));
        }
        lore.add(plugin.color("&fClic pour acheter et recevoir votre equipement."));
        meta.setLore(lore);
        item.setItemMeta(meta);
        item.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1);
        return item;
    }

    private ItemStack createInfoItem(Player player) {
        ItemStack item = new ItemStack(Material.MELON);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.color("&d&lVos " + plugin.getEconomyManager().getCurrencyName()));
        List<String> lore = new ArrayList<String>();
        lore.add(plugin.color("&fSolde actuel : &d" + plugin.getEconomyManager().format(plugin.getEconomyManager().getBalance(player.getUniqueId()))));
        lore.add(plugin.color("&fMonde : &5" + player.getWorld().getName()));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
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
