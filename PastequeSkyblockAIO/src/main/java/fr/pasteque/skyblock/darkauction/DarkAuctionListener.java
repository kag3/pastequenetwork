package fr.pasteque.skyblock.darkauction;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DarkAuctionListener implements Listener {

    private final PastequeSkyblockPlugin plugin;
    private final DarkAuctionManager manager;
    private final Set<UUID> awaitingBid = new HashSet<UUID>();

    public DarkAuctionListener(PastequeSkyblockPlugin plugin, DarkAuctionManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() == null) {
            return;
        }
        String title = event.getInventory().getTitle();
        if (title == null || !title.equals(PastequeSkyblockPlugin.color(DarkAuctionManager.GUI_TITLE))) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        // Bid button at slot 22
        if (event.getRawSlot() == 22 && event.getCurrentItem().getType() == Material.GOLD_BLOCK) {
            if (!manager.isActive()) {
                player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cAucune enchere en cours."));
                player.closeInventory();
                return;
            }
            awaitingBid.add(player.getUniqueId());
            player.closeInventory();
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                    + "&dEntrez le montant de votre enchere dans le chat :"));
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                    + "&7Minimum: &6" + plugin.getEconomyManager().format(manager.getHighestBid() * 1.10)));
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                    + "&7Tapez &cannuler &7pour annuler."));
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        if (!awaitingBid.contains(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        awaitingBid.remove(player.getUniqueId());

        String message = event.getMessage().trim();
        if (message.equalsIgnoreCase("annuler") || message.equalsIgnoreCase("cancel")) {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cEnchere annulee."));
            return;
        }

        final double amount;
        try {
            amount = Double.parseDouble(message.replace(",", "."));
        } catch (NumberFormatException e) {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cMontant invalide."));
            return;
        }

        if (amount <= 0) {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cLe montant doit etre positif."));
            return;
        }

        // Run bid on main thread
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                manager.placeBid(player, amount);
            }
        });
    }

    public boolean isAwaitingBid(UUID uuid) {
        return awaitingBid.contains(uuid);
    }

    public void removeAwaiting(UUID uuid) {
        awaitingBid.remove(uuid);
    }
}
