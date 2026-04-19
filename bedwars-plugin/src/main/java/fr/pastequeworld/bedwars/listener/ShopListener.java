package fr.pastequeworld.bedwars.listener;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.game.Arena;
import fr.pastequeworld.bedwars.player.BedWarsPlayer;
import fr.pastequeworld.bedwars.shop.ShopCategory;
import fr.pastequeworld.bedwars.shop.ShopGUI;
import fr.pastequeworld.bedwars.shop.ShopItem;
import fr.pastequeworld.bedwars.shop.ShopVillager;
import fr.pastequeworld.bedwars.shop.UpgradeGUI;
import fr.pastequeworld.bedwars.team.Team;
import fr.pastequeworld.bedwars.team.TeamColor;
import fr.pastequeworld.bedwars.util.ColorUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Interactions avec les shops villageois et les GUIs de shop.
 */
public class ShopListener implements Listener {

    private final BedWarsPlugin plugin;
    private final ShopGUI shopGUI;
    private final UpgradeGUI upgradeGUI;

    public ShopListener(BedWarsPlugin plugin) {
        this.plugin = plugin;
        this.shopGUI = new ShopGUI(plugin);
        this.upgradeGUI = new UpgradeGUI(plugin);
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!event.getRightClicked().hasMetadata("bw_shop")) return;
        event.setCancelled(true);
        String type = event.getRightClicked().getMetadata("bw_shop").get(0).asString();
        if (ShopVillager.Type.ITEMS.name().equals(type)) {
            Arena arena = plugin.getArenaManager().getArenaOfPlayer(event.getPlayer());
            shopGUI.open(event.getPlayer(), arena, "blocks");
        } else {
            Arena arena = plugin.getArenaManager().getArenaOfPlayer(event.getPlayer());
            upgradeGUI.open(event.getPlayer(), arena);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        String title = event.getView().getTitle();
        if (title == null) return;

        if (title.startsWith(ShopGUI.TITLE_PREFIX)) {
            event.setCancelled(true);
            handleShopClick(event, title);
        } else if (UpgradeGUI.TITLE.equals(title)) {
            event.setCancelled(true);
            handleUpgradeClick(event);
        }
    }

    private void handleShopClick(InventoryClickEvent event, String title) {
        Player player = (Player) event.getWhoClicked();
        Arena arena = plugin.getArenaManager().getArenaOfPlayer(player);
        if (arena == null) return;

        int slot = event.getRawSlot();
        Inventory inv = event.getView().getTopInventory();
        if (slot < 0 || slot >= inv.getSize()) return;

        // Clic sur une categorie (row 0, slots 0..8)
        if (slot < 9) {
            int idx = 0;
            for (ShopCategory cat : plugin.getShopConfig().getCategories().values()) {
                if (idx == slot) {
                    shopGUI.open(player, arena, cat.getKey());
                    return;
                }
                idx++;
            }
            return;
        }

        // Clic sur un item : deduit la categorie active a partir du title
        String categoryLabel = ChatColor.stripColor(title.replace(ChatColor.stripColor(ShopGUI.TITLE_PREFIX), ""));
        ShopCategory active = null;
        for (ShopCategory cat : plugin.getShopConfig().getCategories().values()) {
            if (ChatColor.stripColor(ColorUtil.color(cat.getDisplayName())).equals(categoryLabel)) {
                active = cat;
                break;
            }
        }
        if (active == null) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        // Trouver l'item par nom
        String clickedName = clicked.hasItemMeta() ? ChatColor.stripColor(clicked.getItemMeta().getDisplayName()) : "";
        for (ShopItem item : active.getItems()) {
            String itemName = ChatColor.stripColor(ColorUtil.color(item.getDisplayName() == null ? item.getId() : item.getDisplayName()));
            if (itemName.equals(clickedName)) {
                processPurchase(player, arena, item);
                return;
            }
        }
    }

    private void processPurchase(Player player, Arena arena, ShopItem item) {
        Material costMaterial = costMaterial(item.getCost());
        int required = item.getPrice();
        if (!hasEnough(player, costMaterial, required)) {
            player.sendMessage(plugin.getMessageManager().get("shop.cant-afford"));
            String currency = currencyName(item.getCost());
            player.sendMessage(plugin.getMessageManager().get("shop.need-currency")
                    .replace("%amount%", String.valueOf(required))
                    .replace("%currency%", currency));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        removeItems(player, costMaterial, required);

        if (item.isUpgrade()) {
            applyUpgrade(player, arena, item);
        } else {
            ItemStack stack = shopGUI.buildDisplay(item, arena != null
                    ? (plugin.getPlayerDataManager().get(player).getTeam() != null
                    ? plugin.getPlayerDataManager().get(player).getTeam().getColor()
                    : TeamColor.WHITE) : TeamColor.WHITE);
            // clean display (pas de lore/glow pour l'item recu)
            ItemStack toGive = new ItemStack(stack.getType(), stack.getAmount(), stack.getDurability());
            toGive.setItemMeta(stack.getItemMeta());
            org.bukkit.inventory.meta.ItemMeta meta = toGive.getItemMeta();
            if (meta != null) {
                meta.setLore(null);
                toGive.setItemMeta(meta);
            }
            // Cas potion : preserve les effets
            if (item.getPotionEffect() != null && toGive.getItemMeta() instanceof PotionMeta) {
                PotionMeta pm = (PotionMeta) toGive.getItemMeta();
                String[] parts = item.getPotionEffect().split(":");
                PotionEffectType type = PotionEffectType.getByName(parts[0]);
                int amp = Integer.parseInt(parts[1]);
                int dur = Integer.parseInt(parts[2]);
                if (type != null) pm.addCustomEffect(new PotionEffect(type, dur, amp - 1), true);
                toGive.setItemMeta(pm);
            }
            player.getInventory().addItem(toGive);
        }
        player.sendMessage(plugin.getMessageManager().get("shop.purchased", "item", item.getDisplayName()));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
    }

    private void applyUpgrade(Player player, Arena arena, ShopItem item) {
        BedWarsPlayer bw = plugin.getPlayerDataManager().get(player);
        if (bw == null) return;
        if ("armor-upgrade".equals(item.getType())) {
            Team team = bw.getTeam();
            if (team == null) return;
            // Ameliore toute l'armure cuir en fer/diamant/chaine selon le niveau
            Material[] mats;
            switch (item.getLevel()) {
                case 1:
                    mats = new Material[]{Material.CHAINMAIL_BOOTS, Material.CHAINMAIL_LEGGINGS};
                    break;
                case 2:
                    mats = new Material[]{Material.IRON_BOOTS, Material.IRON_LEGGINGS};
                    break;
                default:
                    mats = new Material[]{Material.DIAMOND_BOOTS, Material.DIAMOND_LEGGINGS};
                    break;
            }
            player.getInventory().setBoots(new ItemStack(mats[0]));
            player.getInventory().setLeggings(new ItemStack(mats[1]));
            // Protection sur tout
            if (team.getProtectionLevel() > 0) {
                for (ItemStack it : player.getInventory().getArmorContents()) {
                    if (it != null) it.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, team.getProtectionLevel());
                }
            }
        } else if ("pickaxe-upgrade".equals(item.getType())) {
            int current = bw.getPickaxeLevel();
            int next = Math.min(current + 1, 4);
            bw.setPickaxeLevel(next);
            // Donne la pioche correspondante
            Material mat = pickMaterial(next);
            player.getInventory().addItem(new ItemStack(mat));
        } else if ("axe-upgrade".equals(item.getType())) {
            int current = bw.getAxeLevel();
            int next = Math.min(current + 1, 4);
            bw.setAxeLevel(next);
            Material mat = axeMaterial(next);
            player.getInventory().addItem(new ItemStack(mat));
        }
    }

    private Material pickMaterial(int level) {
        switch (level) {
            case 1: return Material.WOOD_PICKAXE;
            case 2: return Material.STONE_PICKAXE;
            case 3: return Material.IRON_PICKAXE;
            default: return Material.DIAMOND_PICKAXE;
        }
    }

    private Material axeMaterial(int level) {
        switch (level) {
            case 1: return Material.WOOD_AXE;
            case 2: return Material.STONE_AXE;
            case 3: return Material.IRON_AXE;
            default: return Material.DIAMOND_AXE;
        }
    }

    private void handleUpgradeClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Arena arena = plugin.getArenaManager().getArenaOfPlayer(player);
        if (arena == null) return;
        BedWarsPlayer bw = plugin.getPlayerDataManager().get(player);
        if (bw == null || bw.getTeam() == null) return;
        Team team = bw.getTeam();

        int slot = event.getRawSlot();

        if (slot == 10) { // Sharpened Swords
            if (team.getSharpnessLevel() >= 1) {
                player.sendMessage(plugin.getMessageManager().get("shop.upgrade-maxed"));
                return;
            }
            if (!consumeDiamonds(player, UpgradeGUI.SHARPNESS_COST[0])) return;
            team.setSharpnessLevel(1);
            applySharpnessToTeam(team);
            announce(team, "Epees affutees", 1);
        } else if (slot == 11) { // Reinforced Armor
            int lvl = team.getProtectionLevel();
            if (lvl >= UpgradeGUI.PROTECTION_COST.length) {
                player.sendMessage(plugin.getMessageManager().get("shop.upgrade-maxed"));
                return;
            }
            if (!consumeDiamonds(player, UpgradeGUI.PROTECTION_COST[lvl])) return;
            team.setProtectionLevel(lvl + 1);
            applyProtectionToTeam(team);
            announce(team, "Armure renforcee", lvl + 1);
        } else if (slot == 12) { // Haste
            int lvl = team.getHasteLevel();
            if (lvl >= UpgradeGUI.HASTE_COST.length) {
                player.sendMessage(plugin.getMessageManager().get("shop.upgrade-maxed"));
                return;
            }
            if (!consumeDiamonds(player, UpgradeGUI.HASTE_COST[lvl])) return;
            team.setHasteLevel(lvl + 1);
            applyHasteToTeam(team);
            announce(team, "Mineurs acharnes", lvl + 1);
        } else if (slot == 13) { // HealPool
            if (team.hasHealPool()) {
                player.sendMessage(plugin.getMessageManager().get("shop.upgrade-already-active"));
                return;
            }
            if (!consumeDiamonds(player, UpgradeGUI.HEAL_POOL_COST)) return;
            team.setHealPool(true);
            announce(team, "Soin d'equipe", 1);
        } else if (slot == 14) { // Dragon Buff
            if (team.hasDragonBuff()) {
                player.sendMessage(plugin.getMessageManager().get("shop.upgrade-already-active"));
                return;
            }
            if (!consumeDiamonds(player, UpgradeGUI.DRAGON_BUFF_COST)) return;
            team.setDragonBuff(true);
            announce(team, "Buff du Dragon", 1);
        }

        upgradeGUI.open(player, arena); // refresh
    }

    private void applySharpnessToTeam(Team team) {
        for (java.util.UUID uuid : team.getMembers()) {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p == null) continue;
            for (ItemStack it : p.getInventory().getContents()) {
                if (it != null && it.getType().name().endsWith("_SWORD")) {
                    it.addEnchantment(Enchantment.DAMAGE_ALL, team.getSharpnessLevel());
                }
            }
        }
    }

    private void applyProtectionToTeam(Team team) {
        for (java.util.UUID uuid : team.getMembers()) {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p == null) continue;
            for (ItemStack it : p.getInventory().getArmorContents()) {
                if (it != null) it.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, team.getProtectionLevel());
            }
        }
    }

    private void applyHasteToTeam(Team team) {
        for (java.util.UUID uuid : team.getMembers()) {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p == null) continue;
            p.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING,
                    Integer.MAX_VALUE, team.getHasteLevel() - 1, true, false), true);
        }
    }

    private void announce(Team team, String name, int level) {
        for (java.util.UUID uuid : team.getMembers()) {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p == null) continue;
            p.sendMessage(ColorUtil.color("&7&l[&aUPGRADE&7&l] &aVotre equipe a debloque &f" + name
                    + " " + roman(level) + "&a !"));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        }
    }

    private String roman(int n) {
        switch (n) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            default: return String.valueOf(n);
        }
    }

    private boolean consumeDiamonds(Player player, int amount) {
        if (!hasEnough(player, Material.DIAMOND, amount)) {
            player.sendMessage(plugin.getMessageManager().get("shop.cant-afford"));
            player.sendMessage(plugin.getMessageManager().get("shop.need-currency")
                    .replace("%amount%", String.valueOf(amount))
                    .replace("%currency%", "Diamants"));
            return false;
        }
        removeItems(player, Material.DIAMOND, amount);
        return true;
    }

    private Material costMaterial(String cost) {
        if (cost == null) return Material.IRON_INGOT;
        if ("IRON".equalsIgnoreCase(cost)) return Material.IRON_INGOT;
        if ("GOLD".equalsIgnoreCase(cost)) return Material.GOLD_INGOT;
        if ("DIAMOND".equalsIgnoreCase(cost)) return Material.DIAMOND;
        if ("EMERALD".equalsIgnoreCase(cost)) return Material.EMERALD;
        return Material.IRON_INGOT;
    }

    private String currencyName(String cost) {
        if ("IRON".equalsIgnoreCase(cost)) return "Fer";
        if ("GOLD".equalsIgnoreCase(cost)) return "Or";
        if ("DIAMOND".equalsIgnoreCase(cost)) return "Diamants";
        if ("EMERALD".equalsIgnoreCase(cost)) return "Emeraudes";
        return cost;
    }

    private boolean hasEnough(Player player, Material material, int amount) {
        int total = 0;
        for (ItemStack it : player.getInventory().getContents()) {
            if (it != null && it.getType() == material) total += it.getAmount();
        }
        return total >= amount;
    }

    private void removeItems(Player player, Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack it = contents[i];
            if (it == null || it.getType() != material) continue;
            int take = Math.min(it.getAmount(), remaining);
            remaining -= take;
            if (it.getAmount() - take <= 0) contents[i] = null;
            else it.setAmount(it.getAmount() - take);
        }
        player.getInventory().setContents(contents);
    }
}
