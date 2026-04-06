package fr.pasteque.skyblock.enchant;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.gui.GuiHelper;
import fr.pasteque.skyblock.manager.EconomyManager;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class EnchantTableListener implements Listener {

    private static final String PREFIX = "&2&lPasteque &8\u00bb ";

    private final PastequeSkyblockPlugin plugin;
    private final EnchantManager manager;

    public EnchantTableListener(PastequeSkyblockPlugin plugin, EnchantManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() == null) return;
        String title = event.getInventory().getTitle();
        if (title == null || !title.equals(EnchantManager.GUI_TITLE)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        // Close button
        if (slot == 49) {
            GuiHelper.playClick(player);
            player.closeInventory();
            return;
        }

        // Map slot to enchant index
        int[] slots = {10, 11, 12, 13, 19, 20, 21, 22};
        int enchantIndex = -1;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                enchantIndex = i;
                break;
            }
        }
        if (enchantIndex < 0) return;

        CustomEnchant[] enchants = CustomEnchant.values();
        if (enchantIndex >= enchants.length) return;

        CustomEnchant enchant = enchants[enchantIndex];
        UUID uuid = player.getUniqueId();
        EconomyManager eco = plugin.getEconomyManager();

        GuiHelper.playClick(player);

        // Check held item
        ItemStack hand = player.getItemInHand();
        if (hand == null || hand.getType() == Material.AIR) {
            GuiHelper.playDeny(player);
            MessageUtil.send(player, PastequeSkyblockPlugin.color(PREFIX), "&cVous devez tenir un item en main!");
            return;
        }

        int currentLevel = enchant.getLevel(hand);
        boolean alreadyUnlocked = manager.hasUnlocked(uuid, enchant);

        // Already maxed
        if (currentLevel >= enchant.getMaxLevel()) {
            GuiHelper.playDeny(player);
            MessageUtil.send(player, PastequeSkyblockPlugin.color(PREFIX), "&cCet enchantement est deja au niveau maximum!");
            return;
        }

        int nextLevel;
        int cost;

        if (!alreadyUnlocked) {
            // First time: unlock + apply level 1
            nextLevel = 1;
            cost = enchant.getCost(1);
        } else {
            // Upgrade to next level
            nextLevel = currentLevel + 1;
            cost = enchant.getCost(nextLevel);
        }

        // Check balance
        double balance = eco.getBalance(uuid);
        if (balance < cost) {
            GuiHelper.playDeny(player);
            MessageUtil.send(player, PastequeSkyblockPlugin.color(PREFIX),
                    "&cVous n'avez pas assez de Pasteques! &7(Requis: &e" + cost + "&7, Solde: &e" + (int) balance + "&7)");
            return;
        }

        // Deduct cost and apply
        eco.take(uuid, cost);
        enchant.apply(hand, nextLevel);

        if (!alreadyUnlocked) {
            manager.unlockEnchant(uuid, enchant);
        }
        manager.save();

        GuiHelper.playSuccess(player);
        MessageUtil.send(player, PastequeSkyblockPlugin.color(PREFIX),
                "&aEnchantement " + enchant.getColor() + enchant.getDisplayName() + " " + CustomEnchant.toRoman(nextLevel) + " &aapplique! &7(-" + cost + " Pasteques)");

        // Refresh GUI
        player.closeInventory();
        manager.openEnchantTable(player);
    }
}
