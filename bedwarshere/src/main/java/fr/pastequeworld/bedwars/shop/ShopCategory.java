package fr.pastequeworld.bedwars.shop;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Categorie du shop (Blocs, Armes, Armure, Outils, Arcs, Potions, Utilitaire).
 */
public class ShopCategory {

    private final String key;
    private final String displayName;
    private final Material icon;
    private final int slot;
    private final List<ShopItem> items = new ArrayList<ShopItem>();

    public ShopCategory(String key, String displayName, Material icon, int slot) {
        this.key = key;
        this.displayName = displayName;
        this.icon = icon;
        this.slot = slot;
    }

    public String getKey() { return key; }
    public String getDisplayName() { return displayName; }
    public Material getIcon() { return icon; }
    public int getSlot() { return slot; }
    public List<ShopItem> getItems() { return items; }

    public void addItem(ShopItem item) { items.add(item); }
}
