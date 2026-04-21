package fr.pastequeworld.bedwars.shop;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.game.Arena;
import fr.pastequeworld.bedwars.team.Team;
import fr.pastequeworld.bedwars.team.TeamColor;
import fr.pastequeworld.bedwars.util.ColorUtil;
import fr.pastequeworld.bedwars.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

/**
 * Construit le GUI du shop villager "ITEMS" (7 categories).
 * Format inspire d'Hypixel : une rangee du haut pour les categories,
 * la grande zone pour les items de la categorie active.
 */
public class ShopGUI {

    public static final String TITLE_PREFIX = ColorUtil.color("&8Shop \u00bb ");

    private final BedWarsPlugin plugin;

    public ShopGUI(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Arena arena, String activeCategory) {
        ShopCategory active = plugin.getShopConfig().getCategory(activeCategory);
        if (active == null) {
            // prend la premiere
            active = plugin.getShopConfig().getCategories().values().iterator().next();
        }
        String title = TITLE_PREFIX + ColorUtil.color(active.getDisplayName());
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Bordure haute = categories
        int i = 0;
        for (ShopCategory cat : plugin.getShopConfig().getCategories().values()) {
            ItemBuilder b = new ItemBuilder(cat.getIcon())
                    .name(cat.getDisplayName())
                    .lore("&7Clic pour voir cette categorie.");
            if (cat.getKey().equals(active.getKey())) {
                b.glow();
                b.lore("&aVous consultez actuellement cette categorie.");
            }
            inv.setItem(i, b.build());
            i++;
        }
        // Separator (row 1)
        ItemStack sep = ItemBuilder.namedGlass(15, " ");
        for (int j = 9; j < 18; j++) inv.setItem(j, sep);

        // Slots centraux (row 2-5 = indexes 18..53)
        int slot = 19;
        Team team = arena == null ? null : plugin.getPlayerDataManager().get(player).getTeam();
        TeamColor color = team == null ? TeamColor.WHITE : team.getColor();

        for (ShopItem item : active.getItems()) {
            if (slot % 9 == 8) slot += 2; // eviter bordure droite
            if (slot >= 53) break;
            inv.setItem(slot, buildDisplay(item, color));
            slot++;
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    public ItemStack buildDisplay(ShopItem item, TeamColor teamColor) {
        ItemStack base;
        if (item.isUpgrade()) {
            // icones representatives
            Material icon = Material.CHAINMAIL_CHESTPLATE;
            if ("armor-upgrade".equals(item.getType())) {
                if (item.getLevel() == 1) icon = Material.CHAINMAIL_CHESTPLATE;
                else if (item.getLevel() == 2) icon = Material.IRON_CHESTPLATE;
                else icon = Material.DIAMOND_CHESTPLATE;
            } else if ("pickaxe-upgrade".equals(item.getType())) {
                icon = Material.IRON_PICKAXE;
            } else if ("axe-upgrade".equals(item.getType())) {
                icon = Material.IRON_AXE;
            }
            base = new ItemStack(icon);
        } else {
            Material mat = item.getMaterial() != null ? item.getMaterial() : Material.STONE;
            int data = item.getData();
            if (item.isTeamColored()) data = teamColor.getWoolData();
            base = new ItemStack(mat, item.getAmount(), (short) data);
        }

        List<String> lore = new ArrayList<String>();
        lore.add("");
        lore.add(ColorUtil.color("&7Cout: &f" + item.getPrice() + " " + costDisplay(item.getCost())));
        lore.add("");
        if (item.getLore() != null) {
            for (String l : item.getLore()) lore.add(ColorUtil.color(l));
            lore.add("");
        }
        lore.add(ColorUtil.color("&e\u25b6 Clic pour acheter"));

        ItemBuilder b = new ItemBuilder(base)
                .name(item.getDisplayName() == null ? item.getId() : item.getDisplayName())
                .lore(lore)
                .hideAttributes();

        for (String e : item.getEnchants()) {
            String[] parts = e.split(":");
            Enchantment ench = byName(parts[0]);
            int lvl = Integer.parseInt(parts[1]);
            if (ench != null) b.enchant(ench, lvl);
        }

        ItemStack finalStack = b.build();

        if (item.getPotionEffect() != null) {
            if (finalStack.getItemMeta() instanceof PotionMeta) {
                PotionMeta pm = (PotionMeta) finalStack.getItemMeta();
                String[] parts = item.getPotionEffect().split(":");
                PotionEffectType type = PotionEffectType.getByName(parts[0]);
                int amp = Integer.parseInt(parts[1]);
                int dur = Integer.parseInt(parts[2]);
                if (type != null) pm.addCustomEffect(new PotionEffect(type, dur, amp - 1), true);
                finalStack.setItemMeta(pm);
            }
        }
        if (finalStack.getItemMeta() instanceof LeatherArmorMeta) {
            LeatherArmorMeta lm = (LeatherArmorMeta) finalStack.getItemMeta();
            lm.setColor(org.bukkit.Color.fromRGB(
                    plugin.getConfigManager().getTeamAppearance(teamColor).armorR,
                    plugin.getConfigManager().getTeamAppearance(teamColor).armorG,
                    plugin.getConfigManager().getTeamAppearance(teamColor).armorB));
            finalStack.setItemMeta(lm);
        }
        return finalStack;
    }

    private String costDisplay(String cost) {
        if (cost == null) return "";
        if ("IRON".equalsIgnoreCase(cost)) return "Fer";
        if ("GOLD".equalsIgnoreCase(cost)) return "Or";
        if ("DIAMOND".equalsIgnoreCase(cost)) return "Diamant";
        if ("EMERALD".equalsIgnoreCase(cost)) return "Emeraude";
        return cost;
    }

    private Enchantment byName(String name) {
        if (name == null) return null;
        return Enchantment.getByName(name.toUpperCase());
    }
}
