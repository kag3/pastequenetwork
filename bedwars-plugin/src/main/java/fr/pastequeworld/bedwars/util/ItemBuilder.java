package fr.pastequeworld.bedwars.util;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builder fluide pour construire des ItemStack.
 * Supporte les data values (couleurs de laine, etc) via le constructeur deprecie de 1.9.4.
 */
public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    @SuppressWarnings("deprecation")
    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    @SuppressWarnings("deprecation")
    public ItemBuilder(Material material, int amount, short data) {
        this.item = new ItemStack(material, amount, data);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(ItemStack existing) {
        this.item = existing.clone();
        this.meta = item.getItemMeta();
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemBuilder name(String name) {
        if (meta != null) meta.setDisplayName(ColorUtil.color(name));
        return this;
    }

    public ItemBuilder lore(String... lines) {
        if (meta == null) return this;
        List<String> lore = new ArrayList<String>();
        for (String line : lines) lore.add(ColorUtil.color(line));
        meta.setLore(lore);
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        if (meta == null) return this;
        List<String> colored = new ArrayList<String>();
        for (String line : lines) colored.add(ColorUtil.color(line));
        meta.setLore(colored);
        return this;
    }

    public ItemBuilder enchant(Enchantment enchant, int level) {
        if (meta != null) meta.addEnchant(enchant, level, true);
        return this;
    }

    public ItemBuilder glow() {
        if (meta != null) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    public ItemBuilder hideAttributes() {
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        }
        return this;
    }

    public ItemBuilder unbreakable() {
        if (meta != null) meta.spigot().setUnbreakable(true);
        return this;
    }

    public ItemBuilder leatherColor(int r, int g, int b) {
        if (meta instanceof LeatherArmorMeta) {
            ((LeatherArmorMeta) meta).setColor(Color.fromRGB(r, g, b));
        }
        return this;
    }

    public ItemBuilder potionEffect(PotionEffectType type, int amplifier, int durationTicks) {
        if (meta instanceof PotionMeta) {
            ((PotionMeta) meta).addCustomEffect(new PotionEffect(type, durationTicks, amplifier), true);
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) item.setItemMeta(meta);
        return item;
    }

    public static ItemBuilder of(Material mat) {
        return new ItemBuilder(mat);
    }

    public static ItemStack namedGlass(int data, String name) {
        return new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (short) data).name(name).build();
    }
}
