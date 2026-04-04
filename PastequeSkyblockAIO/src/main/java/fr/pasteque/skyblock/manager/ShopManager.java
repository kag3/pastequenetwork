package fr.pasteque.skyblock.manager;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Material;
import org.bukkit.block.Sign;

public class ShopManager {
    private final PastequeSkyblockPlugin plugin;

    public ShopManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isShopSign(Sign sign) {
        String tag = plugin.getConfig().getString("shop.sign-tag", "[PSHOP]");
        return sign.getLine(0) != null && sign.getLine(0).equalsIgnoreCase(tag);
    }

    public Material getMaterial(Sign sign) {
        return Material.matchMaterial(sign.getLine(1));
    }

    public int getAmount(Sign sign) {
        try {
            return Integer.parseInt(sign.getLine(2));
        } catch (Exception ex) {
            return 1;
        }
    }

    public double getBuyPrice(Sign sign) {
        String line = sign.getLine(3) == null ? "" : sign.getLine(3).toUpperCase();
        if (line.contains("B")) {
            String value = line.split(" ")[0].replace("B", "").replace(":", "").trim();
            try {
                return Double.parseDouble(value);
            } catch (Exception ignored) {
            }
        }
        return -1;
    }

    public double getSellPrice(Sign sign) {
        String line = sign.getLine(3) == null ? "" : sign.getLine(3).toUpperCase();
        if (line.contains("S")) {
            int sIndex = line.indexOf("S");
            String value = line.substring(sIndex + 1).replace(":", "").trim();
            try {
                return Double.parseDouble(value);
            } catch (Exception ignored) {
            }
        }
        return -1;
    }
}
