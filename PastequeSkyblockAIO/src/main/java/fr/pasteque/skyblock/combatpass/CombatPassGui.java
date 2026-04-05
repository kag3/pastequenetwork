package fr.pasteque.skyblock.combatpass;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.combatpass.model.PassTier;
import fr.pasteque.skyblock.combatpass.model.PlayerPassData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("deprecation")
public final class CombatPassGui {

    /** Title used to identify the pass inventory in click events. */
    public static final String TITLE_PREFIX = PastequeSkyblockPlugin.color("&d&lPasse de Combat &7Saison 1");

    private static final int TIERS_PER_PAGE = 9;

    private CombatPassGui() {
    }

    // =========================================================================
    //  Open
    // =========================================================================

    public static void open(CombatPassManager manager, Player player, int page) {
        PlayerPassData data = manager.getData(player.getUniqueId());
        int maxPage = (int) Math.ceil(30.0 / TIERS_PER_PAGE) - 1; // 0-based, 0..3
        if (page < 0) page = 0;
        if (page > maxPage) page = maxPage;

        String title = PastequeSkyblockPlugin.color("&d&lPasse de Combat &7S1 p." + (page + 1));
        Inventory inv = Bukkit.createInventory(null, 54, title);

        int startTier = page * TIERS_PER_PAGE + 1; // 1-based tier number

        // Row 0 (slots 0-8): decorative purple glass
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, glass((short) 10, "&d&lPasse de Combat", "&7Saison 1"));
        }

        // Row 1 (slots 9-17): FREE tier rewards
        for (int i = 0; i < TIERS_PER_PAGE; i++) {
            int tier = startTier + i;
            if (tier > 30) {
                inv.setItem(9 + i, glass((short) 7, "&8Vide", null));
                continue;
            }
            PassTier pt = manager.getFreeTiers().get(tier - 1);
            boolean unlocked = data.getXp() >= tier * 500;
            int freeClaimKey = tier;
            boolean claimed = data.hasClaimed(freeClaimKey);

            Material icon = Material.matchMaterial(pt.getMaterial());
            if (icon == null) icon = Material.BEDROCK;

            ItemStack item = new ItemStack(icon, Math.max(1, Math.min(pt.getAmount(), 64)));
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(PastequeSkyblockPlugin.color(
                    "&a&lPalier " + tier + " &7(Gratuit)"));
            List<String> lore = new ArrayList<String>();
            lore.add(PastequeSkyblockPlugin.color("&7Recompense: " + pt.getRewardDescription()));
            if (pt.getMoneyReward() > 0) {
                lore.add(PastequeSkyblockPlugin.color("&eMoney: &6" + (int) pt.getMoneyReward() + "$"));
            }
            lore.add("");
            if (claimed) {
                lore.add(PastequeSkyblockPlugin.color("&a✔ Recuperee"));
            } else if (unlocked) {
                lore.add(PastequeSkyblockPlugin.color("&e▶ Clic pour recuperer"));
            } else {
                lore.add(PastequeSkyblockPlugin.color("&c✖ Verrouillee &7(" + tier * 500 + " XP requis)"));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(9 + i, item);
        }

        // Row 2 (slots 18-26): progress bar
        for (int i = 0; i < TIERS_PER_PAGE; i++) {
            int tier = startTier + i;
            if (tier > 30) {
                inv.setItem(18 + i, glass((short) 7, "&8-", null));
                continue;
            }
            short color;
            String label;
            if (data.getXp() >= tier * 500) {
                color = 5; // green (lime)
                label = "&a&lPalier " + tier + " &a✔";
            } else if (data.getCurrentTier() == tier - 1) {
                color = 4; // yellow - current progress
                int needed = tier * 500;
                int progress = data.getXp() - (tier - 1) * 500;
                if (progress < 0) progress = 0;
                label = "&e&lPalier " + tier + " &7(" + data.getXp() + "/" + needed + " XP)";
            } else {
                color = 7; // gray
                label = "&7&lPalier " + tier + " &c✖";
            }
            inv.setItem(18 + i, glass(color, label, "&7" + tier * 500 + " XP requis"));
        }

        // Row 3 (slots 27-35): PREMIUM tier rewards
        for (int i = 0; i < TIERS_PER_PAGE; i++) {
            int tier = startTier + i;
            if (tier > 30) {
                inv.setItem(27 + i, glass((short) 7, "&8Vide", null));
                continue;
            }
            PassTier pt = manager.getPremiumTiers().get(tier - 1);
            boolean unlocked = data.getXp() >= tier * 500;
            int premiumClaimKey = -tier;
            boolean claimed = data.hasClaimed(premiumClaimKey);

            if (!data.isPremium()) {
                // Show locked gold glass
                ItemStack locked = glass((short) 1, "&6&lPalier " + tier + " &6(Premium)", "&c&lVerrouille &7- Achete le Passe Premium !");
                inv.setItem(27 + i, locked);
                continue;
            }

            Material icon = Material.matchMaterial(pt.getMaterial());
            if (icon == null) icon = Material.BEDROCK;

            ItemStack item = new ItemStack(icon, Math.max(1, Math.min(pt.getAmount(), 64)));
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(PastequeSkyblockPlugin.color(
                    "&6&lPalier " + tier + " &e(Premium)"));
            List<String> lore = new ArrayList<String>();
            lore.add(PastequeSkyblockPlugin.color("&7Recompense: " + pt.getRewardDescription()));
            if (pt.getMoneyReward() > 0) {
                lore.add(PastequeSkyblockPlugin.color("&eMoney: &6" + (int) pt.getMoneyReward() + "$"));
            }
            lore.add("");
            if (claimed) {
                lore.add(PastequeSkyblockPlugin.color("&a✔ Recuperee"));
            } else if (unlocked) {
                lore.add(PastequeSkyblockPlugin.color("&e▶ Clic pour recuperer"));
            } else {
                lore.add(PastequeSkyblockPlugin.color("&c✖ Verrouillee &7(" + tier * 500 + " XP requis)"));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(27 + i, item);
        }

        // Row 4 (slots 36-44): claim buttons
        for (int i = 0; i < TIERS_PER_PAGE; i++) {
            int tier = startTier + i;
            if (tier > 30) {
                inv.setItem(36 + i, glass((short) 7, "&8-", null));
                continue;
            }
            boolean unlocked = data.getXp() >= tier * 500;
            int freeKey = tier;
            int premiumKey = -tier;
            boolean freeClaimed = data.hasClaimed(freeKey);
            boolean premiumClaimed = data.hasClaimed(premiumKey) || !data.isPremium();

            if (freeClaimed && premiumClaimed) {
                inv.setItem(36 + i, glass((short) 14, "&c&lDeja recupere", "&7Palier " + tier));
            } else if (unlocked) {
                inv.setItem(36 + i, glass((short) 5, "&a&lRecuperer", "&ePalier " + tier + " &7- Clic !"));
            } else {
                inv.setItem(36 + i, glass((short) 7, "&7&lVerrouille", "&7Palier " + tier));
            }
        }

        // Row 5 (slots 45-53): navigation
        for (int i = 0; i < 9; i++) {
            inv.setItem(45 + i, glass((short) 10, "&d", null));
        }

        // Slot 45: previous page
        if (page > 0) {
            ItemStack prev = glass((short) 4, "&e&l← Page precedente", "&7Page " + page);
            inv.setItem(45, prev);
        }

        // Slot 49: XP info
        ItemStack info = new ItemStack(Material.EXP_BOTTLE, 1);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(PastequeSkyblockPlugin.color("&d&lTon Passe de Combat"));
        List<String> infoLore = new ArrayList<String>();
        infoLore.add(PastequeSkyblockPlugin.color("&7XP: &e" + data.getXp()));
        infoLore.add(PastequeSkyblockPlugin.color("&7Palier: &e" + data.getCurrentTier() + "&7/30"));
        if (data.getCurrentTier() < 30) {
            int nextXp = (data.getCurrentTier() + 1) * 500;
            infoLore.add(PastequeSkyblockPlugin.color("&7Prochain palier: &e" + nextXp + " XP"));
        }
        infoLore.add("");
        infoLore.add(PastequeSkyblockPlugin.color(data.isPremium() ? "&6✔ Passe Premium actif" : "&7Passe Gratuit"));
        infoMeta.setLore(infoLore);
        info.setItemMeta(infoMeta);
        inv.setItem(49, info);

        // Slot 53: next page
        if (page < maxPage) {
            ItemStack next = glass((short) 4, "&e&lPage suivante →", "&7Page " + (page + 2));
            inv.setItem(53, next);
        }

        // Slot 51: buy premium button (only if not premium)
        if (!data.isPremium()) {
            ItemStack buy = new ItemStack(Material.NETHER_STAR, 1);
            ItemMeta buyMeta = buy.getItemMeta();
            buyMeta.setDisplayName(PastequeSkyblockPlugin.color("&6&lAcheter le Passe Premium"));
            List<String> buyLore = new ArrayList<String>();
            buyLore.add(PastequeSkyblockPlugin.color("&7Debloque les recompenses &6Premium"));
            buyLore.add(PastequeSkyblockPlugin.color("&7pour toute la saison !"));
            buyLore.add("");
            buyLore.add(PastequeSkyblockPlugin.color("&ePrix: &c25000$"));
            buyLore.add("");
            buyLore.add(PastequeSkyblockPlugin.color("&e▶ Clic pour acheter"));
            buyMeta.setLore(buyLore);
            buy.setItemMeta(buyMeta);
            inv.setItem(51, buy);
        } else {
            ItemStack star = new ItemStack(Material.NETHER_STAR, 1);
            ItemMeta starMeta = star.getItemMeta();
            starMeta.setDisplayName(PastequeSkyblockPlugin.color("&6&l✔ Passe Premium"));
            starMeta.setLore(Arrays.asList(
                    PastequeSkyblockPlugin.color("&aTu possedes le Passe Premium !")));
            star.setItemMeta(starMeta);
            inv.setItem(51, star);
        }

        player.openInventory(inv);
    }

    // =========================================================================
    //  Helper: page extraction from title
    // =========================================================================

    /**
     * Extracts the 0-based page number from the inventory title.
     * Returns 0 if parsing fails.
     */
    public static int pageFromTitle(String title) {
        // Title format: colored "Passe de Combat S1 p.X"
        String stripped = org.bukkit.ChatColor.stripColor(title);
        int idx = stripped.lastIndexOf("p.");
        if (idx < 0) return 0;
        try {
            return Integer.parseInt(stripped.substring(idx + 2).trim()) - 1;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // =========================================================================
    //  Helper: stained glass pane
    // =========================================================================

    @SuppressWarnings("deprecation")
    private static ItemStack glass(short data, String name, String loreLine) {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(PastequeSkyblockPlugin.color(name));
        if (loreLine != null) {
            List<String> lore = new ArrayList<String>();
            lore.add(PastequeSkyblockPlugin.color(loreLine));
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }
}
