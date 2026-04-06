package fr.pasteque.skyblock.listener;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.command.HdvCommand;
import fr.pasteque.skyblock.model.AuctionListing;
import fr.pasteque.skyblock.model.Island;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class GuiListener implements Listener {
    private final PastequeSkyblockPlugin plugin;

    public GuiListener(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory() == null || event.getView() == null || event.getCurrentItem() == null) return;
        HumanEntity clicker = event.getWhoClicked();
        if (!(clicker instanceof Player)) return;
        Player player = (Player) clicker;
        String title = event.getInventory().getTitle();
        String plainTitle = title == null ? "" : ChatColor.stripColor(title);

        // Signature sonore universelle: tout clic sur un GUI du plugin joue
        // un bruitage discret. On ignore les clics sur les vitres decoratives
        // (sert uniquement de filler visuel) et sur les slots vides.
        Material clickedMat = event.getCurrentItem().getType();
        if (clickedMat != Material.AIR && clickedMat != Material.STAINED_GLASS_PANE && isPluginGui(plainTitle)) {
            if (clickedMat == Material.REDSTONE_BLOCK) {
                fr.pasteque.skyblock.gui.GuiHelper.playClose(player);
            } else {
                fr.pasteque.skyblock.gui.GuiHelper.playClick(player);
            }
        }

        // Universal close button handler for all Pasteque GUIs
        if (plainTitle.startsWith("Pasteque")) {
            Material clickedType = event.getCurrentItem().getType();
            // Close button (REDSTONE_BLOCK at slot 49 or anywhere)
            if (clickedType == Material.REDSTONE_BLOCK) {
                event.setCancelled(true);
                player.closeInventory();
                return;
            }
            // Back button (ARROW) - close current GUI
            if (clickedType == Material.ARROW && event.getCurrentItem().hasItemMeta()
                    && event.getCurrentItem().getItemMeta().getDisplayName() != null
                    && ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).contains("Retour")) {
                event.setCancelled(true);
                player.closeInventory();
                return;
            }
        }

        if (plainTitle.contains("Pasteque Skyblock")) {
            event.setCancelled(true);
            Material type = event.getCurrentItem().getType();
            if (type == Material.SAPLING) player.performCommand("is create");
            else if (type == Material.GRASS) player.performCommand("is home");
            else if (type == Material.BED) player.performCommand("is sethome");
            else if (type == Material.BOOK) player.performCommand("is challenges");
            else if (type == Material.PAPER) player.performCommand("is level");
            else if (type == Material.DIAMOND_SWORD) player.performCommand("is pvp");
            else if (type == Material.SKULL_ITEM) player.performCommand("is members");
            else if (type == Material.CARROT_ITEM) player.performCommand("is unlock farming");
            else if (type == Material.IRON_PICKAXE) player.performCommand("is unlock mining");
            else if (type == Material.COMPASS) player.performCommand("is farm");
            else if (type == Material.DIAMOND_PICKAXE) player.performCommand("is mine");
            else if (type == Material.BOOK_AND_QUILL) player.performCommand("is stats");
            else if (type == Material.GOLD_INGOT) player.performCommand("is top");
            else if (type == Material.CHEST) player.performCommand("is bank");
            else if (type == Material.NAME_TAG) {
                player.closeInventory();
                MessageUtil.send(player, plugin.getPrefix(), "&7Rename ile: &f/is rename <nom>");
            }
            else if (type == Material.SIGN) {
                player.closeInventory();
                MessageUtil.send(player, plugin.getPrefix(), "&7Visibilite ile: &f/is public &7ou &f/is private");
            }
            return;
        }

        if (plainTitle.contains("Pasteque Ile Cooperative")) {
            event.setCancelled(true);
            Material type = event.getCurrentItem().getType();
            if (type == Material.SAPLING) {
                player.closeInventory();
                MessageUtil.send(player, plugin.getPrefix(), "&7Creation coop: &f/iscoop create <joueur1> [joueur2...]");
            }
            else if (type == Material.BED) player.performCommand("iscoop home");
            else if (type == Material.IRON_PICKAXE) player.performCommand("iscoop unlock mine");
            else if (type == Material.COMPASS || type == Material.BOOK) player.performCommand("iscoop list");
            else if (type == Material.DIAMOND_PICKAXE) player.performCommand("iscoop mine");
            else if (type == Material.PAPER) player.performCommand("iscoop pending");
            else if (type == Material.NAME_TAG) {
                player.closeInventory();
                MessageUtil.send(player, plugin.getPrefix(), "&7Rename coop: &f/iscoop rename <nom> &7(dans ta coop)");
            }
            return;
        }

        if (plainTitle.contains("Pasteque Admin")) {
            event.setCancelled(true);
            Material type = event.getCurrentItem().getType();
            if (type == Material.COMPASS) player.performCommand("psky islands");
            else if (type == Material.CHEST) player.performCommand("psky hdv list");
            else if (type == Material.IRON_SWORD) player.sendMessage(PastequeSkyblockPlugin.color("&2&lPasteque &8\u00bb &7PvP: &f/psky pvp pos1, pos2, create, delete"));
            else if (type == Material.NETHER_STAR) player.sendMessage(PastequeSkyblockPlugin.color("&2&lPasteque &8\u00bb &7Commande: &f/psky setpvpwarp"));
            else if (type == Material.MONSTER_EGG) player.performCommand("psky events invasion");
            return;
        }

        if (plainTitle.contains("Pasteque HDV")) {
            event.setCancelled(true);
            int currentPage = 1;
            try {
                String inside = plainTitle.substring(plainTitle.indexOf('[') + 1, plainTitle.indexOf(']'));
                currentPage = Integer.parseInt(inside.split("/")[0]);
            } catch (Exception ignored) {}

            if (event.getSlot() == 45) {
                new HdvCommand(plugin).openAuction(player, currentPage - 1);
                return;
            }
            if (event.getSlot() == 49) {
                player.performCommand("hdv recup");
                return;
            }
            if (event.getSlot() == 53) {
                new HdvCommand(plugin).openAuction(player, currentPage + 1);
                return;
            }
            if (event.getSlot() >= 45) return;

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasLore()) return;
            String id = null;
            for (String line : clicked.getItemMeta().getLore()) {
                String plain = ChatColor.stripColor(line);
                if (plain != null && plain.startsWith("ID:")) {
                    id = plain.substring("ID:".length()).trim();
                    break;
                }
            }
            if (id == null) return;

            AuctionListing listing = plugin.getAuctionManager().get(id);
            if (listing == null) {
                MessageUtil.send(player, plugin.getPrefix(), "&fCette annonce n'existe plus.");
                player.closeInventory();
                return;
            }
            if (!plugin.getEconomyManager().take(player.getUniqueId(), listing.getPrice())) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu n'as pas assez d'argent.");
                return;
            }
            plugin.getAuctionManager().buy(id, player.getUniqueId());
            player.getInventory().addItem(listing.getItem().clone());
            MessageUtil.send(player, plugin.getPrefix(), "&aAchat effectue.");
            new HdvCommand(plugin).openAuction(player, currentPage);
            return;
        }

        if (plainTitle.contains("Pasteque Retours HDV")) {
            event.setCancelled(true);
            if (event.getSlot() == 49) {
                player.performCommand("hdv recup all");
                player.closeInventory();
                return;
            }
            // Click individual item to retrieve it
            int slot = event.getRawSlot();
            if (slot >= 10 && slot < 44 && slot % 9 != 0 && slot % 9 != 8) {
                ItemStack clicked = event.getCurrentItem();
                if (clicked != null && clicked.getType() != Material.AIR
                        && clicked.getType() != Material.STAINED_GLASS_PANE) {
                    if (player.getInventory().firstEmpty() == -1) {
                        MessageUtil.send(player, plugin.getPrefix(), "&cTon inventaire est plein.");
                        return;
                    }
                    player.getInventory().addItem(clicked.clone());
                    event.getInventory().setItem(slot, new ItemStack(Material.AIR));
                    // Remove from claimable list
                    plugin.getAuctionManager().removeClaimable(player.getUniqueId(), clicked);
                    fr.pasteque.skyblock.gui.GuiHelper.playClick(player);
                    MessageUtil.send(player, plugin.getPrefix(), "&aObjet recupere !");
                }
            }
            return;
        }

        if (plainTitle.contains("Pasteque Defis")) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || !item.hasItemMeta()) return;
            List<String> lore = item.getItemMeta().getLore();
            if (lore == null || lore.isEmpty()) return;
            String plain = ChatColor.stripColor(lore.get(lore.size() - 1));
            if (plain == null || !plain.startsWith("ID:")) return;
            String challengeId = plain.substring("ID:".length()).trim();
            if (plugin.getChallengeManager().complete(player, challengeId)) MessageUtil.send(player, plugin.getPrefix(), "&aDefi termine avec succes.");
            else MessageUtil.send(player, plugin.getPrefix(), "&fImpossible de terminer ce defi.");
            player.closeInventory();
            return;
        }

        if (plainTitle.contains("Pasteque Membres")) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return;
            Island island = plugin.getIslandManager().getOwnedIsland(player.getUniqueId());
            if (island == null) {
                player.closeInventory();
                return;
            }
            for (String line : item.getItemMeta().getLore()) {
                String plain = ChatColor.stripColor(line);
                if (plain == null || !plain.startsWith("UUID:")) continue;
                try {
                    UUID uuid = UUID.fromString(plain.substring("UUID:".length()).trim());
                    if (plugin.getIslandManager().removeMember(island, uuid)) {
                        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                        MessageUtil.send(player, plugin.getPrefix(), "&aMembre expulse : &f" + (op.getName() == null ? uuid.toString() : op.getName()));
                        player.performCommand("is members");
                    }
                } catch (Exception ignored) {}
                return;
            }
        }
    }

    /**
     * Heuristique pour detecter si un inventaire ouvert est un GUI du plugin
     * (afin de jouer les sons uniquement dans nos menus). On s'appuie sur les
     * prefixes et mots-cles utilises par les titres de GUIs declares dans le
     * codebase — evite les faux-positifs dans les coffres vanilla.
     */
    private boolean isPluginGui(String plainTitle) {
        if (plainTitle == null || plainTitle.length() == 0) return false;
        if (plainTitle.startsWith("Pasteque")) return true;
        // Autres titres GUI du plugin (non prefixes Pasteque)
        return plainTitle.contains("Boutique")
                || plainTitle.contains("Classements")
                || plainTitle.contains("Recompense")
                || plainTitle.contains("Top")
                || plainTitle.contains("Admin")
                || plainTitle.contains("HDV")
                || plainTitle.contains("Minion")
                || plainTitle.contains("Pet")
                || plainTitle.contains("Skill")
                || plainTitle.contains("Slayer")
                || plainTitle.contains("Collection")
                || plainTitle.contains("Arene")
                || plainTitle.contains("Kit")
                || plainTitle.contains("Coop")
                || plainTitle.contains("Ile")
                || plainTitle.contains("Pass")
                || plainTitle.contains("Warp")
                || plainTitle.contains("Upgrade")
                || plainTitle.contains("Amelior")
                || plainTitle.contains("Enchere")
                || plainTitle.contains("Sombre")
                || plainTitle.contains("Signal")
                || plainTitle.contains("Shop");
    }
}
