package fr.pastequeworld.bedwars.shop;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Item de shop. Peut representer :
 *   - Un item classique (material + amount)
 *   - Un upgrade permanent (type = armor-upgrade / pickaxe-upgrade / axe-upgrade)
 *   - Une potion (potion-effect = TYPE:amplifier:duration)
 */
public class ShopItem {

    private final String id;
    private String type;               // optionnel : armor-upgrade, pickaxe-upgrade, axe-upgrade
    private Material material;
    private int data;
    private int amount = 1;
    private String cost;               // IRON, GOLD, DIAMOND, EMERALD
    private int price;
    private String displayName;
    private boolean teamColored;
    private boolean permanent;
    private int level;                 // pour upgrades
    private List<String> enchants = new ArrayList<String>();
    private String potionEffect;       // TYPE:amp:duration
    private List<String> lore = new ArrayList<String>();

    public ShopItem(String id) {
        this.id = id;
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }
    public int getData() { return data; }
    public void setData(int data) { this.data = data; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
    public String getCost() { return cost; }
    public void setCost(String cost) { this.cost = cost; }
    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public boolean isTeamColored() { return teamColored; }
    public void setTeamColored(boolean teamColored) { this.teamColored = teamColored; }
    public boolean isPermanent() { return permanent; }
    public void setPermanent(boolean permanent) { this.permanent = permanent; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public List<String> getEnchants() { return enchants; }
    public void setEnchants(List<String> enchants) { this.enchants = enchants == null ? new ArrayList<String>() : enchants; }
    public String getPotionEffect() { return potionEffect; }
    public void setPotionEffect(String potionEffect) { this.potionEffect = potionEffect; }
    public List<String> getLore() { return lore; }
    public void setLore(List<String> lore) { this.lore = lore == null ? new ArrayList<String>() : lore; }

    public boolean isUpgrade() { return type != null && type.endsWith("-upgrade"); }
}
