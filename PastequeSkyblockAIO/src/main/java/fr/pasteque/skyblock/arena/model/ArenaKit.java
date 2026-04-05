package fr.pasteque.skyblock.arena.model;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;

public class ArenaKit {

    private final String name;
    private final String displayName;
    private final String icon;
    private final int requiredLevel;
    private final int price;
    private final ItemStack[] armor;
    private final ItemStack[] items;

    public ArenaKit(String name, String displayName, String icon, int requiredLevel, int price, ItemStack[] armor, ItemStack[] items) {
        this.name = name;
        this.displayName = displayName;
        this.icon = icon;
        this.requiredLevel = requiredLevel;
        this.price = price;
        this.armor = armor;
        this.items = items;
    }

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public String getIcon() { return icon; }
    public int getRequiredLevel() { return requiredLevel; }
    public int getPrice() { return price; }
    public ItemStack[] getArmor() { return armor; }
    public ItemStack[] getItems() { return items; }

    // =========================================================================
    //  Default kits
    // =========================================================================

    public static List<ArenaKit> createDefaultKits() {
        List<ArenaKit> kits = new ArrayList<ArenaKit>();
        kits.add(createGuerrier());
        kits.add(createArcher());
        kits.add(createTank());
        kits.add(createAssassin());
        kits.add(createMage());
        kits.add(createBerserker());
        kits.add(createPaladin());
        return kits;
    }

    // ── Guerrier (Warrior) ──────────────────────────────────────────────────

    private static ArenaKit createGuerrier() {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = enchant(new ItemStack(Material.IRON_HELMET), Enchantment.PROTECTION_ENVIRONMENTAL, 1);
        armor[2] = enchant(new ItemStack(Material.IRON_CHESTPLATE), Enchantment.PROTECTION_ENVIRONMENTAL, 1);
        armor[1] = enchant(new ItemStack(Material.IRON_LEGGINGS), Enchantment.PROTECTION_ENVIRONMENTAL, 1);
        armor[0] = enchant(new ItemStack(Material.IRON_BOOTS), Enchantment.PROTECTION_ENVIRONMENTAL, 1);

        ItemStack[] items = new ItemStack[]{
                enchant(new ItemStack(Material.IRON_SWORD), Enchantment.DAMAGE_ALL, 1),
                new ItemStack(Material.GOLDEN_APPLE, 16)
        };
        return new ArenaKit("guerrier", "Guerrier", "IRON_SWORD", 0, 0, armor, items);
    }

    // ── Archer ──────────────────────────────────────────────────────────────

    private static ArenaKit createArcher() {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = enchant(new ItemStack(Material.LEATHER_HELMET), Enchantment.PROTECTION_ENVIRONMENTAL, 1);
        armor[2] = enchant(new ItemStack(Material.LEATHER_CHESTPLATE), Enchantment.PROTECTION_ENVIRONMENTAL, 1);
        armor[1] = enchant(new ItemStack(Material.LEATHER_LEGGINGS), Enchantment.PROTECTION_ENVIRONMENTAL, 1);
        armor[0] = enchant(new ItemStack(Material.LEATHER_BOOTS), Enchantment.PROTECTION_ENVIRONMENTAL, 1);

        ItemStack[] items = new ItemStack[]{
                enchant(new ItemStack(Material.BOW), Enchantment.ARROW_DAMAGE, 1),
                new ItemStack(Material.ARROW, 64),
                enchant(new ItemStack(Material.STONE_SWORD), Enchantment.DAMAGE_ALL, 1)
        };
        return new ArenaKit("archer", "Archer", "BOW", 3, 500, armor, items);
    }

    // ── Tank ────────────────────────────────────────────────────────────────

    private static ArenaKit createTank() {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = enchant(new ItemStack(Material.IRON_HELMET), Enchantment.PROTECTION_ENVIRONMENTAL, 2);
        armor[2] = enchant(new ItemStack(Material.DIAMOND_CHESTPLATE), Enchantment.PROTECTION_ENVIRONMENTAL, 2);
        armor[1] = enchant(new ItemStack(Material.IRON_LEGGINGS), Enchantment.PROTECTION_ENVIRONMENTAL, 2);
        armor[0] = enchant(new ItemStack(Material.IRON_BOOTS), Enchantment.PROTECTION_ENVIRONMENTAL, 2);

        ItemStack[] items = new ItemStack[]{
                enchant(new ItemStack(Material.STONE_SWORD), Enchantment.DAMAGE_ALL, 1),
                new ItemStack(Material.GOLDEN_APPLE, 32)
        };
        return new ArenaKit("tank", "Tank", "DIAMOND_CHESTPLATE", 5, 1000, armor, items);
    }

    // ── Assassin ────────────────────────────────────────────────────────────

    private static ArenaKit createAssassin() {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = enchant(new ItemStack(Material.CHAINMAIL_HELMET), Enchantment.PROTECTION_ENVIRONMENTAL, 1);
        armor[2] = enchant(new ItemStack(Material.CHAINMAIL_CHESTPLATE), Enchantment.PROTECTION_ENVIRONMENTAL, 1);
        armor[1] = enchant(new ItemStack(Material.CHAINMAIL_LEGGINGS), Enchantment.PROTECTION_ENVIRONMENTAL, 1);
        armor[0] = enchant(new ItemStack(Material.CHAINMAIL_BOOTS), Enchantment.PROTECTION_ENVIRONMENTAL, 1);

        ItemStack speedPotion = new ItemStack(Material.POTION);
        PotionMeta speedMeta = (PotionMeta) speedPotion.getItemMeta();
        speedMeta.setBasePotionData(new PotionData(PotionType.SPEED, false, true));
        speedPotion.setItemMeta(speedMeta);

        ItemStack[] items = new ItemStack[]{
                enchant(new ItemStack(Material.DIAMOND_SWORD), Enchantment.DAMAGE_ALL, 1),
                new ItemStack(Material.GOLDEN_APPLE, 8),
                speedPotion
        };
        return new ArenaKit("assassin", "Assassin", "DIAMOND_SWORD", 8, 2000, armor, items);
    }

    // ── Mage ────────────────────────────────────────────────────────────────

    private static ArenaKit createMage() {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = enchant(new ItemStack(Material.GOLD_HELMET), Enchantment.PROTECTION_ENVIRONMENTAL, 1);
        armor[2] = enchant(new ItemStack(Material.GOLD_CHESTPLATE), Enchantment.PROTECTION_ENVIRONMENTAL, 1);
        armor[1] = enchant(new ItemStack(Material.GOLD_LEGGINGS), Enchantment.PROTECTION_ENVIRONMENTAL, 1);
        armor[0] = enchant(new ItemStack(Material.GOLD_BOOTS), Enchantment.PROTECTION_ENVIRONMENTAL, 1);

        ItemStack harmPotion = new ItemStack(Material.SPLASH_POTION, 5);
        PotionMeta harmMeta = (PotionMeta) harmPotion.getItemMeta();
        harmMeta.setBasePotionData(new PotionData(PotionType.INSTANT_DAMAGE, false, false));
        harmPotion.setItemMeta(harmMeta);

        ItemStack healPotion = new ItemStack(Material.SPLASH_POTION, 5);
        PotionMeta healMeta = (PotionMeta) healPotion.getItemMeta();
        healMeta.setBasePotionData(new PotionData(PotionType.INSTANT_HEAL, false, false));
        healPotion.setItemMeta(healMeta);

        ItemStack[] items = new ItemStack[]{
                new ItemStack(Material.WOOD_SWORD),
                harmPotion,
                healPotion
        };
        return new ArenaKit("mage", "Mage", "POTION", 10, 3000, armor, items);
    }

    // ── Berserker ───────────────────────────────────────────────────────────

    private static ArenaKit createBerserker() {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = null; // no helmet
        armor[2] = enchant(new ItemStack(Material.IRON_CHESTPLATE), Enchantment.PROTECTION_ENVIRONMENTAL, 2);
        armor[1] = enchant(new ItemStack(Material.IRON_LEGGINGS), Enchantment.PROTECTION_ENVIRONMENTAL, 2);
        armor[0] = enchant(new ItemStack(Material.IRON_BOOTS), Enchantment.PROTECTION_ENVIRONMENTAL, 2);

        ItemStack[] items = new ItemStack[]{
                enchant(new ItemStack(Material.DIAMOND_AXE), Enchantment.DAMAGE_ALL, 2),
                new ItemStack(Material.GOLDEN_APPLE, 16)
        };
        return new ArenaKit("berserker", "Berserker", "DIAMOND_AXE", 15, 5000, armor, items);
    }

    // ── Paladin ─────────────────────────────────────────────────────────────

    private static ArenaKit createPaladin() {
        ItemStack[] armor = new ItemStack[4];
        armor[3] = enchant(new ItemStack(Material.DIAMOND_HELMET), Enchantment.PROTECTION_ENVIRONMENTAL, 2);
        armor[2] = enchant(new ItemStack(Material.DIAMOND_CHESTPLATE), Enchantment.PROTECTION_ENVIRONMENTAL, 2);
        armor[1] = enchant(new ItemStack(Material.DIAMOND_LEGGINGS), Enchantment.PROTECTION_ENVIRONMENTAL, 2);
        armor[0] = enchant(new ItemStack(Material.DIAMOND_BOOTS), Enchantment.PROTECTION_ENVIRONMENTAL, 2);

        ItemStack healPotion = new ItemStack(Material.SPLASH_POTION, 5);
        PotionMeta healMeta = (PotionMeta) healPotion.getItemMeta();
        healMeta.setBasePotionData(new PotionData(PotionType.INSTANT_HEAL, false, true));
        healPotion.setItemMeta(healMeta);

        // SHIELD was added in 1.9, use it if available, else fallback to IRON_BLOCK
        ItemStack offhand;
        try {
            offhand = new ItemStack(Material.valueOf("SHIELD"));
        } catch (IllegalArgumentException e) {
            offhand = new ItemStack(Material.IRON_BLOCK);
        }

        ItemStack[] items = new ItemStack[]{
                enchant(new ItemStack(Material.IRON_SWORD), Enchantment.DAMAGE_ALL, 2),
                healPotion,
                offhand
        };
        return new ArenaKit("paladin", "Paladin", "GOLD_CHESTPLATE", 20, 8000, armor, items);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static ItemStack enchant(ItemStack item, Enchantment enchantment, int level) {
        item.addUnsafeEnchantment(enchantment, level);
        return item;
    }
}
