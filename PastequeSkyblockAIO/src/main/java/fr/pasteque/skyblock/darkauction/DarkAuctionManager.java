package fr.pasteque.skyblock.darkauction;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.darkauction.model.AuctionItem;
import fr.pasteque.skyblock.gui.GuiHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class DarkAuctionManager {

    public static final String GUI_TITLE = PastequeSkyblockPlugin.color("&2&lPasteque &5&lVente Sombre");

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
        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);

        // Row 0: decorative border
        GuiHelper.addTopBorder(inv);

        // Row 2: decorative border
        GuiHelper.addBottomBorder(inv);

        if (currentItem != null) {
            // Center slot (13): auction item with enchant glow
            ItemStack display = createRewardItem(currentItem);
            ItemMeta displayMeta = display.getItemMeta();
            List<String> displayLore = new ArrayList<String>();
            displayLore.add("");
            displayLore.add(PastequeSkyblockPlugin.color("&8\u258E &7Description"));
            displayLore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7" + currentItem.getLore()));
            displayLore.add("");
            displayLore.add(PastequeSkyblockPlugin.color("&8\u258E &7Enchere"));
            displayLore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Enchere actuelle: &6" + formatMoney(highestBid)));
            if (highestBidder != null) {
                Player bidder = Bukkit.getPlayer(highestBidder);
                String name = bidder != null ? bidder.getName() : "???";
                displayLore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Encherisseur: &e" + name));
            } else {
                displayLore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Aucun encherisseur"));
            }
            displayLore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Temps restant: &e" + timeLeft + "s"));
            displayLore.add("");
            displayLore.add(PastequeSkyblockPlugin.color("&5&lENCHERE SOMBRE"));
            displayMeta.setLore(displayLore);
            displayMeta.addEnchant(Enchantment.DURABILITY, 1, true);
            // Note: ItemFlag not available in 1.9.4
            display.setItemMeta(displayMeta);
            inv.setItem(13, display);

            // Slot 11: CLOCK - time remaining
            ItemStack clockItem = GuiHelper.createItem(Material.WATCH,
                    "&7&lTemps Restant",
                    "",
                    "&8\u25B8 &e" + timeLeft + " &7secondes");
            inv.setItem(11, clockItem);

            // Slot 15: SKULL_ITEM - best bidder
            ItemStack skullItem = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
            SkullMeta skullMeta = (SkullMeta) skullItem.getItemMeta();
            skullMeta.setDisplayName(PastequeSkyblockPlugin.color("&d&lMeilleur Encherisseur"));
            List<String> skullLore = new ArrayList<String>();
            skullLore.add("");
            if (highestBidder != null) {
                Player bidder = Bukkit.getPlayer(highestBidder);
                String name = bidder != null ? bidder.getName() : "???";
                skullLore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Joueur: &e" + name));
                skullLore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Montant: &6" + formatMoney(highestBid)));
                if (bidder != null) {
                    skullMeta.setOwner(bidder.getName());
                }
            } else {
                skullLore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Aucun encherisseur"));
            }
            skullMeta.setLore(skullLore);
            skullItem.setItemMeta(skullMeta);
            inv.setItem(15, skullItem);

            // Slot 22: GOLD_BLOCK - bid button
            ItemStack bidButton = GuiHelper.createItem(Material.GOLD_BLOCK,
                    "&e&lEncherir",
                    "",
                    "&7Clic pour placer une enchere",
                    "&7Minimum: &6" + formatMoney(highestBid * 1.10),
                    "",
                    "&e\u25B6 Clic pour encherir!");
            inv.setItem(22, bidButton);
        } else {
            // No auction active
            inv.setItem(13, GuiHelper.createItem(Material.STAINED_GLASS_PANE, 14,
                    "&c&lAucune enchere en cours",
                    "",
                    "&8\u25B8 &7Revenez plus tard !"));
        }

        // Close button at bottom-right
        inv.setItem(26, GuiHelper.closeButton());

        // Fill remaining with black glass
        GuiHelper.fillEmpty(inv);

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
        item.setItemMeta(meta);
        return item;
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------

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
