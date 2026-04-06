package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.gui.GuiHelper;
import fr.pasteque.skyblock.model.AuctionListing;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HdvCommand implements CommandExecutor {
    private final PastequeSkyblockPlugin plugin;

    public HdvCommand(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public static final String TITLE_PREFIX = PastequeSkyblockPlugin.color("&2&lPasteque &5&lHDV");
    public static final String RETURNS_TITLE = PastequeSkyblockPlugin.color("&2&lPasteque &5&lRetours HDV");

    public static String getTitle(int page, int total) {
        return PastequeSkyblockPlugin.color("&2&lPasteque &5&lHDV &8[&7" + page + "&8/&7" + total + "&8]");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PastequeSkyblockPlugin.color("&2&lPasteque &8\u00bb &cCommande joueur uniquement."));
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            openAuction(player, 1);
            return true;
        }
        if (args[0].equalsIgnoreCase("page")) {
            int page = 1;
            if (args.length >= 2) {
                try {
                    page = Integer.parseInt(args[1]);
                } catch (Exception ignored) {
                }
            }
            openAuction(player, page);
            return true;
        }
        if (args[0].equalsIgnoreCase("recup")) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("all")) {
                int moved = plugin.getAuctionManager().claimAll(player.getUniqueId(), player.getInventory());
                MessageUtil.send(player, plugin.getPrefix(), moved <= 0 ? "&cAucun objet a recuperer." : "&a" + moved + " objet(s) recupere(s) depuis l'HDV.");
                return true;
            }
            openReturns(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("vendre")) {
            if (args.length < 3) {
                sendHelp(player);
                return true;
            }
            ItemStack hand = player.getItemInHand();
            if (hand == null || hand.getType() == Material.AIR) {
                MessageUtil.send(player, plugin.getPrefix(), "&cTu dois tenir un objet en main.");
                return true;
            }
            int amount;
            double price;
            try {
                price = Double.parseDouble(args[1]);
                amount = Integer.parseInt(args[2]);
            } catch (Exception ex) {
                MessageUtil.send(player, plugin.getPrefix(), "&cPrix ou quantite invalide.");
                return true;
            }
            if (amount <= 0 || price <= 0) {
                MessageUtil.send(player, plugin.getPrefix(), "&cPrix ou quantite invalide.");
                return true;
            }
            if (hand.getAmount() < amount) {
                MessageUtil.send(player, plugin.getPrefix(), "&cTu n'as pas autant d'objets en main.");
                return true;
            }
            int max = plugin.getConfig().getInt("auction.max-active-listings-per-player", 10);
            if (plugin.getAuctionManager().getActiveCount(player.getUniqueId()) >= max) {
                MessageUtil.send(player, plugin.getPrefix(), "&cTu as atteint la limite d'annonces.");
                return true;
            }
            double fee = plugin.getConfig().getDouble("auction.listing-fee", 25.0D);
            if (!plugin.getEconomyManager().take(player.getUniqueId(), fee)) {
                MessageUtil.send(player, plugin.getPrefix(), "&cIl te faut &e" + fee + " " + plugin.getEconomyManager().getCurrencyName() + " &cpour publier une annonce.");
                return true;
            }
            ItemStack item = hand.clone();
            item.setAmount(amount);
            hand.setAmount(hand.getAmount() - amount);
            player.setItemInHand(hand.getAmount() <= 0 ? new ItemStack(Material.AIR) : hand);
            AuctionListing listing = plugin.getAuctionManager().create(player.getUniqueId(), price, item);
            MessageUtil.send(player, plugin.getPrefix(), "&aAnnonce creee sous l'identifiant &f" + listing.getId() + "&a. Expiration sous 7 jours.");
            return true;
        }
        sendHelp(player);
        return true;
    }

    public void openAuction(Player player, int page) {
        plugin.getAuctionManager().purgeExpired();
        int totalPages = plugin.getAuctionManager().getTotalPages();
        page = Math.max(1, Math.min(page, totalPages));
        Inventory inventory = Bukkit.createInventory(null, 54, getTitle(page, totalPages));

        // Decorate FIRST so borders don't overwrite items and buttons
        GuiHelper.decorate(inventory, GuiHelper.Theme.ECO);

        SimpleDateFormat df = new SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE);
        int slot = 10;
        for (AuctionListing listing : plugin.getAuctionManager().getListingsPage(page)) {
            if (slot >= 44) {
                break;
            }
            if (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
                continue;
            }
            ItemStack item = listing.getItem().clone();
            ItemMeta meta = item.getItemMeta();
            List<String> lore = meta != null && meta.hasLore() ? new ArrayList<String>(meta.getLore()) : new ArrayList<String>();
            lore.add(PastequeSkyblockPlugin.color(""));
            lore.add(PastequeSkyblockPlugin.color("&8▎ &7Informations"));
            lore.add(PastequeSkyblockPlugin.color("&8▸ &7Prix: &e" + listing.getPrice() + " " + plugin.getEconomyManager().getCurrencyName()));
            lore.add(PastequeSkyblockPlugin.color("&8▸ &7Vendeur: &d" + Bukkit.getOfflinePlayer(listing.getSeller()).getName()));
            lore.add(PastequeSkyblockPlugin.color("&8▸ &7Expire: &b" + df.format(new Date(listing.getExpiresAt()))));
            lore.add(PastequeSkyblockPlugin.color(""));
            lore.add(PastequeSkyblockPlugin.color("&e▶ Clic pour acheter"));
            lore.add(PastequeSkyblockPlugin.color("&8ID: " + listing.getId()));
            if (meta != null) {
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(slot, item);
            slot++;
        }

        inventory.setItem(45, GuiHelper.createItem(Material.ARROW, "&7&lPage precedente",
                "",
                "&8▸ &7Revenir a la page precedente",
                "&e▶ Clic pour naviguer"));
        inventory.setItem(49, GuiHelper.createItem(Material.CHEST, "&e&lObjets a recuperer",
                "",
                "&8▸ &7Objets non vendus / moderes / expires",
                "&e▶ Clic pour ouvrir tes retours"));
        inventory.setItem(53, GuiHelper.createItem(Material.ARROW, "&7&lPage suivante",
                "",
                "&8▸ &7Passer a la page suivante",
                "&e▶ Clic pour naviguer"));

        player.openInventory(inventory);
        GuiHelper.playOpen(player);
    }

    public void openReturns(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, RETURNS_TITLE);

        // Decorate FIRST so borders don't overwrite items and buttons
        GuiHelper.decorate(inventory, GuiHelper.Theme.ECO);

        List<ItemStack> items = plugin.getAuctionManager().getClaimable(player.getUniqueId());
        int slot = 10;
        for (ItemStack item : items) {
            if (slot >= 44) {
                break;
            }
            if (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
                continue;
            }
            inventory.setItem(slot++, item);
        }

        inventory.setItem(49, GuiHelper.createItem(Material.ENDER_CHEST, "&a&lTout recuperer",
                "",
                "&8▸ &7Recuperer tous les objets possibles",
                "&a▶ Clic pour recuperer"));

        player.openInventory(inventory);
        GuiHelper.playOpen(player);
    }

    private void sendHelp(Player player) {
        MessageUtil.send(player, plugin.getPrefix(), "&7/hdv &8- &7Ouvrir l'hotel des ventes");
        MessageUtil.send(player, plugin.getPrefix(), "&7/hdv page <numero> &8- &7Changer de page");
        MessageUtil.send(player, plugin.getPrefix(), "&7/hdv vendre <prix> <quantite> &8- &7Mettre l'objet en main en vente");
        MessageUtil.send(player, plugin.getPrefix(), "&7/hdv recup &8- &7Voir tes objets retournes");
        MessageUtil.send(player, plugin.getPrefix(), "&7/hdv recup all &8- &7Tout recuperer");
    }
}
