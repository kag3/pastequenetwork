package fr.pasteque.skyblock.combatpass;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.combatpass.model.PassTier;
import fr.pasteque.skyblock.combatpass.model.PlayerPassData;
import fr.pasteque.skyblock.gui.GuiHelper;
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
    public static final String TITLE_PREFIX = PastequeSkyblockPlugin.color("&2&lPasteque &5&lPasse de Combat");

    private static final int TIERS_PER_PAGE = 7;

    private CombatPassGui() {
    }

    // =========================================================================
    //  Open
    // =========================================================================

    public static void open(CombatPassManager manager, Player player, int page) {
        PlayerPassData data = manager.getData(player.getUniqueId());
        int maxPage = (int) Math.ceil(30.0 / TIERS_PER_PAGE) - 1;
        if (page < 0) page = 0;
        if (page > maxPage) page = maxPage;

        String title = PastequeSkyblockPlugin.color("&2&lPasteque &5&lPasse &7p." + (page + 1));
        Inventory inv = Bukkit.createInventory(null, 54, title);

        int startTier = page * TIERS_PER_PAGE + 1;

        // ── Row 0: decorative border with center NETHER_STAR ─────────────
        inv.setItem(4, GuiHelper.createItem(Material.NETHER_STAR,
                "&d&lSaison 1",
                "&7Passe de Combat",
                "&8\u25B8 30 paliers de recompenses",
                "",
                "&5Pasteque Skyblock"));

        // ── Row 1 (slots 10-16): Free tier rewards ──────────────────────
        for (int i = 0; i < TIERS_PER_PAGE; i++) {
            int tier = startTier + i;
            if (tier > 30) {
                inv.setItem(10 + i, GuiHelper.glassPane(15, " "));
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
            meta.setDisplayName(PastequeSkyblockPlugin.color("&a&lPalier " + tier + " &7(Gratuit)"));

            List<String> lore = new ArrayList<String>();
            lore.add("");
            lore.add(PastequeSkyblockPlugin.color("&8\u258E &7Recompense"));
            lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &f" + pt.getRewardDescription()));
            if (pt.getMoneyReward() > 0) {
                lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &eMoney: &6" + (int) pt.getMoneyReward() + "$"));
            }
            lore.add("");
            if (claimed) {
                lore.add(PastequeSkyblockPlugin.color("&a\u2714 Recuperee"));
            } else if (unlocked) {
                lore.add(PastequeSkyblockPlugin.color("&e\u25B6 Clic pour recuperer!"));
            } else {
                lore.add(PastequeSkyblockPlugin.color("&c\u2716 Verrouillee &7(" + tier * 500 + " XP requis)"));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(10 + i, item);
        }

        // ── Row 2 (slots 19-25): Progress bar (STAINED_CLAY) ────────────
        for (int i = 0; i < TIERS_PER_PAGE; i++) {
            int tier = startTier + i;
            if (tier > 30) {
                inv.setItem(19 + i, GuiHelper.glassPane(15, " "));
                continue;
            }
            short color;
            String label;
            if (data.getXp() >= tier * 500) {
                color = 5; // lime clay
                label = "&a&lPalier " + tier + " &a\u2714";
            } else if (data.getCurrentTier() == tier - 1) {
                color = 4; // yellow clay
                label = "&e&lPalier " + tier + " &7(" + data.getXp() + "/" + (tier * 500) + " XP)";
            } else {
                color = 7; // gray clay
                label = "&7&lPalier " + tier + " &c\u2716";
            }
            ItemStack clay = new ItemStack(Material.STAINED_CLAY, 1, color);
            ItemMeta clayMeta = clay.getItemMeta();
            clayMeta.setDisplayName(PastequeSkyblockPlugin.color(label));
            List<String> clayLore = new ArrayList<String>();
            clayLore.add(PastequeSkyblockPlugin.color("&7" + tier * 500 + " XP requis"));
            clayMeta.setLore(clayLore);
            clay.setItemMeta(clayMeta);
            inv.setItem(19 + i, clay);
        }

        // ── Row 3 (slots 28-34): Premium tier rewards ───────────────────
        for (int i = 0; i < TIERS_PER_PAGE; i++) {
            int tier = startTier + i;
            if (tier > 30) {
                inv.setItem(28 + i, GuiHelper.glassPane(15, " "));
                continue;
            }
            PassTier pt = manager.getPremiumTiers().get(tier - 1);
            boolean unlocked = data.getXp() >= tier * 500;
            int premiumClaimKey = -tier;
            boolean claimed = data.hasClaimed(premiumClaimKey);

            if (!data.isPremium()) {
                inv.setItem(28 + i, GuiHelper.createItem(Material.STAINED_GLASS_PANE, 1,
                        "&6&lPalier " + tier + " &6(Premium)",
                        "",
                        "&c&lVerrouille",
                        "&7Achete le Passe Premium !",
                        "",
                        "&e\u25B6 Clic sur &6Acheter Premium"));
                continue;
            }

            Material icon = Material.matchMaterial(pt.getMaterial());
            if (icon == null) icon = Material.BEDROCK;

            ItemStack item = new ItemStack(icon, Math.max(1, Math.min(pt.getAmount(), 64)));
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(PastequeSkyblockPlugin.color("&6&lPalier " + tier + " &e(Premium)"));

            List<String> lore = new ArrayList<String>();
            lore.add("");
            lore.add(PastequeSkyblockPlugin.color("&8\u258E &7Recompense"));
            lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &f" + pt.getRewardDescription()));
            if (pt.getMoneyReward() > 0) {
                lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &eMoney: &6" + (int) pt.getMoneyReward() + "$"));
            }
            lore.add("");
            if (claimed) {
                lore.add(PastequeSkyblockPlugin.color("&a\u2714 Recuperee"));
            } else if (unlocked) {
                lore.add(PastequeSkyblockPlugin.color("&e\u25B6 Clic pour recuperer!"));
            } else {
                lore.add(PastequeSkyblockPlugin.color("&c\u2716 Verrouillee &7(" + tier * 500 + " XP requis)"));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(28 + i, item);
        }

        // ── Row 4 (slots 37-43): Claim buttons (INK_SACK dyes) ──────────
        for (int i = 0; i < TIERS_PER_PAGE; i++) {
            int tier = startTier + i;
            if (tier > 30) {
                inv.setItem(37 + i, GuiHelper.glassPane(15, " "));
                continue;
            }
            boolean unlocked = data.getXp() >= tier * 500;
            int freeKey = tier;
            int premiumKey = -tier;
            boolean freeClaimed = data.hasClaimed(freeKey);
            boolean premiumClaimed = data.hasClaimed(premiumKey) || !data.isPremium();

            if (freeClaimed && premiumClaimed) {
                // Gray dye (INK_SACK:8) - already claimed
                inv.setItem(37 + i, GuiHelper.createItem(Material.INK_SACK, 8,
                        "&7&lDeja recupere",
                        "&8Palier " + tier));
            } else if (unlocked) {
                // Lime dye (INK_SACK:10) - claimable
                inv.setItem(37 + i, GuiHelper.createItem(Material.INK_SACK, 10,
                        "&a&lRecuperer!",
                        "&ePalier " + tier + " &7- Clic !"));
            } else {
                // Red dye (INK_SACK:1) - locked
                inv.setItem(37 + i, GuiHelper.createItem(Material.INK_SACK, 1,
                        "&c&lVerrouille",
                        "&7Palier " + tier));
            }
        }

        // ── Row 5: Navigation ────────────────────────────────────────────

        // Slot 45: previous page
        if (page > 0) {
            inv.setItem(45, GuiHelper.createItem(Material.ARROW,
                    "&e&l\u2190 Page precedente",
                    "&7Page " + page));
        }

        // Slot 47: buy premium / premium status
        if (!data.isPremium()) {
            inv.setItem(47, GuiHelper.createItem(Material.GOLD_BLOCK,
                    "&6&lAcheter Premium",
                    "",
                    "&7Debloque les recompenses &6Premium",
                    "&7pour toute la saison !",
                    "",
                    "&8\u258E &7Prix: &c25000$",
                    "",
                    "&a\u25B6 Clic pour acheter!"));
        } else {
            inv.setItem(47, GuiHelper.createItem(Material.GOLD_BLOCK,
                    "&6&l\u2714 Passe Premium",
                    "",
                    "&aTu possedes le Passe Premium !"));
        }

        // Slot 49: XP info book
        ItemStack info = new ItemStack(Material.BOOK, 1);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(PastequeSkyblockPlugin.color("&d&lTon Passe de Combat"));
        List<String> infoLore = new ArrayList<String>();
        infoLore.add("");
        infoLore.add(PastequeSkyblockPlugin.color("&8\u258E &7Statistiques"));
        infoLore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7XP: &e" + data.getXp()));
        infoLore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Palier: &e" + data.getCurrentTier() + "&7/&f30"));
        if (data.getCurrentTier() < 30) {
            int nextXp = (data.getCurrentTier() + 1) * 500;
            infoLore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Prochain: &e" + nextXp + " XP"));
        }
        infoLore.add("");
        infoLore.add(PastequeSkyblockPlugin.color(data.isPremium() ? "&6\u2714 Passe Premium actif" : "&7Passe Gratuit"));
        infoMeta.setLore(infoLore);
        info.setItemMeta(infoMeta);
        inv.setItem(49, info);

        // Slot 53: next page
        if (page < maxPage) {
            inv.setItem(53, GuiHelper.createItem(Material.ARROW,
                    "&e&lPage suivante \u2192",
                    "&7Page " + (page + 2)));
        }

        // Fill remaining empty slots with black glass
        GuiHelper.decorate(inv, GuiHelper.Theme.PVP);

        player.openInventory(inv);
        GuiHelper.playOpen(player);
    }

    // =========================================================================
    //  Helper: page extraction from title
    // =========================================================================

    public static int pageFromTitle(String title) {
        String stripped = org.bukkit.ChatColor.stripColor(title);
        int idx = stripped.lastIndexOf("p.");
        if (idx < 0) return 0;
        try {
            return Integer.parseInt(stripped.substring(idx + 2).trim()) - 1;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
