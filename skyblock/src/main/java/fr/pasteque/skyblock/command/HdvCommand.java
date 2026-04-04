package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
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
    public HdvCommand(PastequeSkyblockPlugin plugin) { this.plugin = plugin; }
    public static String getTitle(int page, int total) { return "§6HDV §7[" + page + "/" + total + "]"; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Commande joueur uniquement."); return true; }
        Player player = (Player) sender;
        if (args.length == 0) { openAuction(player, 1); return true; }
        if (args[0].equalsIgnoreCase("page")) {
            int page = 1;
            if (args.length >= 2) try { page = Integer.parseInt(args[1]); } catch (Exception ignored) {}
            openAuction(player, page);
            return true;
        }
        if (args[0].equalsIgnoreCase("recup")) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("all")) {
                int moved = plugin.getAuctionManager().claimAll(player.getUniqueId(), player.getInventory());
                MessageUtil.send(player, plugin.getPrefix(), moved <= 0 ? "&fAucun objet à récupérer." : "&a" + moved + " objet(s) récupéré(s) depuis l'HDV.");
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
            if (hand == null || hand.getType() == Material.AIR) { MessageUtil.send(player, plugin.getPrefix(), "&fTu dois tenir un objet en main."); return true; }
            int amount; double price;
            try { price = Double.parseDouble(args[1]); amount = Integer.parseInt(args[2]); } catch (Exception ex) { MessageUtil.send(player, plugin.getPrefix(), "&fPrix ou quantité invalide."); return true; }
            if (amount <= 0 || price <= 0) { MessageUtil.send(player, plugin.getPrefix(), "&fPrix ou quantité invalide."); return true; }
            if (hand.getAmount() < amount) { MessageUtil.send(player, plugin.getPrefix(), "&fTu n'as pas autant d'objets en main."); return true; }
            int max = plugin.getConfig().getInt("auction.max-active-listings-per-player", 10);
            if (plugin.getAuctionManager().getActiveCount(player.getUniqueId()) >= max) { MessageUtil.send(player, plugin.getPrefix(), "&fTu as atteint la limite d'annonces."); return true; }
            double fee = plugin.getConfig().getDouble("auction.listing-fee", 25.0D);
            if (!plugin.getEconomyManager().take(player.getUniqueId(), fee)) { MessageUtil.send(player, plugin.getPrefix(), "&fIl te faut &e" + fee + " " + plugin.getEconomyManager().getCurrencyName() + " &fpour publier une annonce."); return true; }
            ItemStack item = hand.clone(); item.setAmount(amount); hand.setAmount(hand.getAmount() - amount); player.setItemInHand(hand.getAmount() <= 0 ? new ItemStack(Material.AIR) : hand);
            AuctionListing listing = plugin.getAuctionManager().create(player.getUniqueId(), price, item);
            MessageUtil.send(player, plugin.getPrefix(), "&aAnnonce créée sous l'identifiant &f" + listing.getId() + "&a. Expiration sous 7 jours.");
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
        SimpleDateFormat df = new SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE);
        for (AuctionListing listing : plugin.getAuctionManager().getListingsPage(page)) {
            ItemStack item = listing.getItem().clone();
            ItemMeta meta = item.getItemMeta();
            List<String> lore = meta != null && meta.hasLore() ? new ArrayList<String>(meta.getLore()) : new ArrayList<String>();
            lore.add("§7Prix: §e" + listing.getPrice() + " " + plugin.getEconomyManager().getCurrencyName());
            lore.add("§7Vendeur: §f" + Bukkit.getOfflinePlayer(listing.getSeller()).getName());
            lore.add("§7Expire: §f" + df.format(new Date(listing.getExpiresAt())));
            lore.add("§8Clique pour acheter");
            lore.add("§8ID: " + listing.getId());
            if (meta != null) { meta.setLore(lore); item.setItemMeta(meta); }
            inventory.addItem(item);
        }
        inventory.setItem(45, item(Material.ARROW, "§ePage précédente", "§7Revenir à la page précédente"));
        inventory.setItem(49, item(Material.CHEST, "§6Objets à récupérer", "§7Objets non vendus / modérés / expirés", "§7Clique pour ouvrir tes retours"));
        inventory.setItem(53, item(Material.ARROW, "§ePage suivante", "§7Passer à la page suivante"));
        player.openInventory(inventory);
    }

    public void openReturns(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, "§eRetours HDV");
        List<ItemStack> items = plugin.getAuctionManager().getClaimable(player.getUniqueId());
        int slot = 0;
        for (ItemStack item : items) { if (slot >= 45) break; inventory.setItem(slot++, item); }
        inventory.setItem(49, item(Material.ENDER_CHEST, "§aTout récupérer", "§7Récupérer tous les objets possibles"));
        player.openInventory(inventory);
    }

    private void sendHelp(Player player) {
        MessageUtil.send(player, plugin.getPrefix(), "&7/hdv &8- &fOuvrir l'hôtel des ventes");
        MessageUtil.send(player, plugin.getPrefix(), "&7/hdv page <numéro> &8- &fChanger de page");
        MessageUtil.send(player, plugin.getPrefix(), "&7/hdv vendre <prix> <quantité> &8- &fMettre l'objet en main en vente");
        MessageUtil.send(player, plugin.getPrefix(), "&7/hdv recup &8- &fVoir tes objets retournés");
        MessageUtil.send(player, plugin.getPrefix(), "&7/hdv recup all &8- &fTout récupérer");
    }

    private ItemStack item(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); List<String> lore = new ArrayList<String>(); for (String line : loreLines) lore.add(line); meta.setLore(lore); item.setItemMeta(meta); }
        return item;
    }
}
