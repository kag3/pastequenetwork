package fr.pasteque.skyblock.darkauction;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.darkauction.model.AuctionItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class DarkAuctionManager {

    public static final String GUI_TITLE = "Enchere Sombre";

    private final PastequeSkyblockPlugin plugin;
    private final List<AuctionItem> pool;
    private final Random random = new Random();

    private boolean active;
    private AuctionItem currentItem;
    private UUID highestBidder;
    private double highestBid;
    private int timeLeft;
    private BukkitTask timerTask;

    public DarkAuctionManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.pool = AuctionItem.createPool();
    }

    // ------------------------------------------------------------------
    //  Scheduling
    // ------------------------------------------------------------------

    public void schedule() {
        long intervalTicks = plugin.getConfig().getLong("dark-auction.interval-minutes", 120) * 60L * 20L;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) {
                    startAuction();
                }
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }

    // ------------------------------------------------------------------
    //  Auction lifecycle
    // ------------------------------------------------------------------

    public void startAuction() {
        if (active) {
            return;
        }
        currentItem = pool.get(random.nextInt(pool.size()));
        highestBid = currentItem.getStartingBid();
        highestBidder = null;
        timeLeft = 60;
        active = true;

        Bukkit.broadcastMessage(PastequeSkyblockPlugin.color(
                "&8&l[&5&lEnchere Sombre&8&l] &dUn objet rare est en vente : &e" + currentItem.getDisplayName()
                        + " &d! Mise de depart: &6" + formatMoney(highestBid) + " &d! Tapez &e/da &dpour encherir !"));

        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                timeLeft--;
                if (timeLeft <= 0) {
                    endAuction();
                    cancel();
                    return;
                }
                if (timeLeft == 30 || timeLeft == 10 || timeLeft == 5) {
                    Bukkit.broadcastMessage(PastequeSkyblockPlugin.color(
                            "&8&l[&5&lEnchere Sombre&8&l] &dTemps restant: &e" + timeLeft + "s &d| Enchere actuelle: &6"
                                    + formatMoney(highestBid)));
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void placeBid(Player player, double amount) {
        if (!active) {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cAucune enchere en cours."));
            return;
        }

        double minimumBid = highestBid * 1.10;
        if (amount < minimumBid) {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                    + "&cVotre enchere doit etre superieure a &6" + formatMoney(minimumBid) + "&c (+10%)."));
            return;
        }

        if (!plugin.getEconomyManager().take(player.getUniqueId(), amount)) {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cVous n'avez pas assez d'argent."));
            return;
        }

        // Refund previous bidder
        if (highestBidder != null) {
            plugin.getEconomyManager().add(highestBidder, highestBid);
            Player previous = Bukkit.getPlayer(highestBidder);
            if (previous != null && previous.isOnline()) {
                previous.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                        + "&eVotre enchere a ete depassee ! Vous avez ete rembourse de &6" + formatMoney(highestBid) + "&e."));
            }
        }

        highestBidder = player.getUniqueId();
        highestBid = amount;

        Bukkit.broadcastMessage(PastequeSkyblockPlugin.color(
                "&8&l[&5&lEnchere Sombre&8&l] &e" + player.getName() + " &da encherit &6"
                        + formatMoney(amount) + " &dpour &e" + currentItem.getDisplayName() + "&d !"));

        // Reset timer if under 10s
        if (timeLeft < 10) {
            timeLeft = 10;
        }
    }

    public void endAuction() {
        active = false;
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }

        if (highestBidder == null) {
            Bukkit.broadcastMessage(PastequeSkyblockPlugin.color(
                    "&8&l[&5&lEnchere Sombre&8&l] &cPersonne n'a encherit. L'objet a ete retire."));
            currentItem = null;
            return;
        }

        Player winner = Bukkit.getPlayer(highestBidder);
        ItemStack reward = createRewardItem(currentItem);

        if (winner != null && winner.isOnline()) {
            if (winner.getInventory().firstEmpty() != -1) {
                winner.getInventory().addItem(reward);
            } else {
                winner.getWorld().dropItemNaturally(winner.getLocation(), reward);
                winner.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                        + "&eVotre inventaire est plein, l'objet a ete depose au sol."));
            }
            winner.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                    + "&aVous avez remporte &e" + currentItem.getDisplayName() + " &apour &6" + formatMoney(highestBid) + "&a !"));
        }
        // If offline, item is lost (or implement mailbox later)

        Bukkit.broadcastMessage(PastequeSkyblockPlugin.color(
                "&8&l[&5&lEnchere Sombre&8&l] &e" + (winner != null ? winner.getName() : "???")
                        + " &da remporte &e" + currentItem.getDisplayName()
                        + " &dpour &6" + formatMoney(highestBid) + " &d!"));

        currentItem = null;
        highestBidder = null;
    }

    // ------------------------------------------------------------------
    //  GUI
    // ------------------------------------------------------------------

    public void openAuctionGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, PastequeSkyblockPlugin.color(GUI_TITLE));

        // Fill border with dark glass
        ItemStack filler = createItem(Material.STAINED_GLASS_PANE, (short) 15, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        if (currentItem != null) {
            // Show current item at slot 13
            ItemStack display = createRewardItem(currentItem);
            inv.setItem(13, display);

            // Info item at slot 4
            ItemStack info = createItem(Material.PAPER, (short) 0, "&d&lEnchere Sombre");
            ItemMeta infoMeta = info.getItemMeta();
            List<String> infoLore = new ArrayList<String>();
            infoLore.add(PastequeSkyblockPlugin.color("&7Enchere actuelle: &6" + formatMoney(highestBid)));
            if (highestBidder != null) {
                Player bidder = Bukkit.getPlayer(highestBidder);
                String name = bidder != null ? bidder.getName() : "???";
                infoLore.add(PastequeSkyblockPlugin.color("&7Meilleur encherisseur: &e" + name));
            } else {
                infoLore.add(PastequeSkyblockPlugin.color("&7Aucun encherisseur"));
            }
            infoLore.add(PastequeSkyblockPlugin.color("&7Temps restant: &e" + timeLeft + "s"));
            infoMeta.setLore(infoLore);
            info.setItemMeta(infoMeta);
            inv.setItem(4, info);

            // Bid button at slot 22
            ItemStack bidButton = createItem(Material.GOLD_BLOCK, (short) 0, "&6&lEncherir");
            ItemMeta bidMeta = bidButton.getItemMeta();
            bidMeta.setLore(Arrays.asList(
                    PastequeSkyblockPlugin.color("&7Cliquez pour placer une enchere"),
                    PastequeSkyblockPlugin.color("&7Minimum: &6" + formatMoney(highestBid * 1.10))
            ));
            bidButton.setItemMeta(bidMeta);
            inv.setItem(22, bidButton);
        } else {
            ItemStack noAuction = createItem(Material.BARRIER, (short) 0, "&c&lAucune enchere en cours");
            inv.setItem(13, noAuction);
        }

        player.openInventory(inv);
    }

    // ------------------------------------------------------------------
    //  Item creation
    // ------------------------------------------------------------------

    @SuppressWarnings("deprecation")
    public ItemStack createRewardItem(AuctionItem auctionItem) {
        Material mat;
        try {
            mat = Material.valueOf(auctionItem.getMaterial());
        } catch (IllegalArgumentException e) {
            mat = Material.STONE;
        }
        ItemStack item = new ItemStack(mat, auctionItem.getAmount());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(PastequeSkyblockPlugin.color("&d" + auctionItem.getDisplayName()));
        List<String> lore = new ArrayList<String>();
        lore.add(PastequeSkyblockPlugin.color(auctionItem.getLore()));
        lore.add("");
        lore.add(PastequeSkyblockPlugin.color("&5&lENCHERE SOMBRE"));
        meta.setLore(lore);
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------

    private ItemStack createItem(Material material, short data, String name) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(PastequeSkyblockPlugin.color(name));
        item.setItemMeta(meta);
        return item;
    }

    private String formatMoney(double amount) {
        return plugin.getEconomyManager().format(amount);
    }

    public boolean isActive() {
        return active;
    }

    public AuctionItem getCurrentItem() {
        return currentItem;
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public double getHighestBid() {
        return highestBid;
    }

    public UUID getHighestBidder() {
        return highestBidder;
    }
}
