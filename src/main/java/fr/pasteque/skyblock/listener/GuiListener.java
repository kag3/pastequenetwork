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

        if (plainTitle.contains("Menu Skyblock") || plainTitle.contains("Skyblock Premium")) {
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

        if (plainTitle.contains("Iles Coop") || plainTitle.contains("Îles Coop")) {
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

        if (plainTitle.contains("Admin Skyblock")) {
            event.setCancelled(true);
            Material type = event.getCurrentItem().getType();
            if (type == Material.COMPASS) player.performCommand("psky islands");
            else if (type == Material.CHEST) player.performCommand("psky hdv list");
            else if (type == Material.IRON_SWORD) player.sendMessage(ChatColor.GRAY + "PvP: /psky pvp pos1, /psky pvp pos2, /psky pvp create <nom>, /psky pvp delete <nom>");
            else if (type == Material.NETHER_STAR) player.sendMessage(ChatColor.GRAY + "Commande: /psky setpvpwarp");
            else if (type == Material.MONSTER_EGG) player.performCommand("psky events invasion");
            return;
        }

        if (plainTitle.startsWith("HDV [")) {
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

        if (plainTitle.equalsIgnoreCase("Retours HDV")) {
            event.setCancelled(true);
            if (event.getSlot() == 49) {
                player.performCommand("hdv recup all");
                player.closeInventory();
            }
            return;
        }

        if (plainTitle.equalsIgnoreCase("Defis Skyblock") || plainTitle.equalsIgnoreCase("Défis Skyblock")) {
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

        if (plainTitle.contains("Membres d")) {
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
}
