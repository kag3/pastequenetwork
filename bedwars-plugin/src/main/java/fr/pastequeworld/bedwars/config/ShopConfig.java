package fr.pastequeworld.bedwars.config;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.shop.ShopCategory;
import fr.pastequeworld.bedwars.shop.ShopItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parse shop.yml en objets {@link ShopCategory} / {@link ShopItem}.
 * L'iteration garde l'ordre declaratif du YAML pour les slots du GUI.
 */
public class ShopConfig {

    private final BedWarsPlugin plugin;
    private final Map<String, ShopCategory> categories = new LinkedHashMap<String, ShopCategory>();

    public ShopConfig(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "shop.yml");
        if (!file.exists()) plugin.saveResource("shop.yml", false);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection cats = cfg.getConfigurationSection("categories");
        if (cats == null) return;

        for (String catKey : cats.getKeys(false)) {
            ConfigurationSection cs = cats.getConfigurationSection(catKey);
            ShopCategory cat = new ShopCategory(
                    catKey,
                    cs.getString("name", catKey),
                    parseMaterial(cs.getString("icon", "STONE")),
                    cs.getInt("slot", 1));
            List<Map<?, ?>> items = cs.getMapList("items");
            for (Map<?, ?> raw : items) {
                ShopItem item = parseItem(raw);
                if (item != null) cat.addItem(item);
            }
            categories.put(catKey, cat);
        }
    }

    @SuppressWarnings("unchecked")
    private ShopItem parseItem(Map<?, ?> raw) {
        String id = (String) raw.get("id");
        if (id == null) return null;
        ShopItem item = new ShopItem(id);

        Object type = raw.get("type");
        if (type != null) item.setType((String) type);

        Object mat = raw.get("material");
        if (mat != null) item.setMaterial(parseMaterial((String) mat));

        Object data = raw.get("data");
        if (data != null) item.setData(((Number) data).intValue());

        Object amount = raw.get("amount");
        item.setAmount(amount == null ? 1 : ((Number) amount).intValue());

        Object cost = raw.get("cost");
        if (cost != null) item.setCost((String) cost);

        Object price = raw.get("price");
        if (price != null) item.setPrice(((Number) price).intValue());

        Object name = raw.get("name");
        if (name != null) item.setDisplayName((String) name);

        Object teamColored = raw.get("team-colored");
        if (teamColored != null) item.setTeamColored(Boolean.TRUE.equals(teamColored));

        Object permanent = raw.get("permanent");
        if (permanent != null) item.setPermanent(Boolean.TRUE.equals(permanent));

        Object level = raw.get("level");
        if (level != null) item.setLevel(((Number) level).intValue());

        Object enchants = raw.get("enchants");
        if (enchants instanceof List) item.setEnchants(new ArrayList<String>((List<String>) enchants));

        Object potion = raw.get("potion-effect");
        if (potion != null) item.setPotionEffect((String) potion);

        Object lore = raw.get("lore");
        if (lore instanceof List) item.setLore(new ArrayList<String>((List<String>) lore));

        return item;
    }

    private Material parseMaterial(String name) {
        if (name == null) return Material.STONE;
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.STONE;
        }
    }

    public Map<String, ShopCategory> getCategories() { return categories; }

    public ShopCategory getCategory(String key) { return categories.get(key); }
}
