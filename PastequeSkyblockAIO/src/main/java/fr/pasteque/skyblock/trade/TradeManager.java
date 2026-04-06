package fr.pasteque.skyblock.trade;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.gui.GuiHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class TradeManager {

    public static final String GUI_TITLE = "&2&lPasteque &5&lEchange";
    public static final String GUI_TITLE_COLORED = PastequeSkyblockPlugin.color(GUI_TITLE);
    private static final long REQUEST_EXPIRY_MS = 30000L;

    private final PastequeSkyblockPlugin plugin;
    private final Map<UUID, TradeSession> activeTrades;
    private final Map<UUID, UUID> pendingRequests;
    private final Map<UUID, Long> pendingTimestamps;

    public TradeManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.activeTrades = new HashMap<UUID, TradeSession>();
        this.pendingRequests = new HashMap<UUID, UUID>();
        this.pendingTimestamps = new HashMap<UUID, Long>();
    }

    public PastequeSkyblockPlugin getPlugin() {
        return plugin;
    }

    public TradeSession getSession(UUID player) {
        return activeTrades.get(player);
    }

    public boolean hasActiveTrade(UUID player) {
        return activeTrades.containsKey(player);
    }

    public boolean hasPendingRequest(UUID target) {
        if (!pendingRequests.containsKey(target)) {
            return false;
        }
        long timestamp = pendingTimestamps.get(target);
        if (System.currentTimeMillis() - timestamp > REQUEST_EXPIRY_MS) {
            pendingRequests.remove(target);
            pendingTimestamps.remove(target);
            return false;
        }
        return true;
    }

    public void sendRequest(Player sender, Player target) {
        String prefix = plugin.getPrefix();

        if (sender.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cVous ne pouvez pas echanger avec vous-meme."));
            return;
        }

        if (hasActiveTrade(sender.getUniqueId())) {
            sender.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cVous avez deja un echange en cours."));
            return;
        }

        if (hasActiveTrade(target.getUniqueId())) {
            sender.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cCe joueur est deja en train d'echanger."));
            return;
        }

        // Check if sender already sent a request to this target
        if (hasPendingRequest(target.getUniqueId())) {
            UUID existingSender = pendingRequests.get(target.getUniqueId());
            if (existingSender.equals(sender.getUniqueId())) {
                sender.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cVous avez deja envoye une demande a ce joueur."));
                return;
            }
        }

        // Check if target has sent a request to sender (auto-accept)
        if (hasPendingRequest(sender.getUniqueId())) {
            UUID existingSender = pendingRequests.get(sender.getUniqueId());
            if (existingSender.equals(target.getUniqueId())) {
                acceptRequest(sender);
                return;
            }
        }

        pendingRequests.put(target.getUniqueId(), sender.getUniqueId());
        pendingTimestamps.put(target.getUniqueId(), System.currentTimeMillis());

        sender.sendMessage(PastequeSkyblockPlugin.color(prefix + "&fDemande d'echange envoyee a &d" + target.getName() + "&f."));

        target.sendMessage(PastequeSkyblockPlugin.color(prefix + "&d" + sender.getName() + " &fveut echanger avec vous."));
        target.sendMessage(PastequeSkyblockPlugin.color(prefix + "&a/trade accept &f- Accepter | &c/trade deny &f- Refuser"));

        // Schedule expiry
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                UUID targetId = target.getUniqueId();
                if (hasPendingRequest(targetId)) {
                    // Already expired by timestamp check, just clean up
                } else {
                    pendingRequests.remove(targetId);
                    pendingTimestamps.remove(targetId);
                }
            }
        }, 600L); // 30 seconds
    }

    public void acceptRequest(Player target) {
        String prefix = plugin.getPrefix();

        if (!hasPendingRequest(target.getUniqueId())) {
            target.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cVous n'avez aucune demande d'echange en attente."));
            return;
        }

        UUID senderId = pendingRequests.remove(target.getUniqueId());
        pendingTimestamps.remove(target.getUniqueId());

        Player sender = Bukkit.getPlayer(senderId);
        if (sender == null || !sender.isOnline()) {
            target.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cLe joueur n'est plus en ligne."));
            return;
        }

        if (hasActiveTrade(sender.getUniqueId())) {
            target.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cCe joueur est deja en train d'echanger."));
            return;
        }

        if (hasActiveTrade(target.getUniqueId())) {
            target.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cVous avez deja un echange en cours."));
            return;
        }

        TradeSession session = new TradeSession(sender.getUniqueId(), target.getUniqueId());
        activeTrades.put(sender.getUniqueId(), session);
        activeTrades.put(target.getUniqueId(), session);

        sender.sendMessage(PastequeSkyblockPlugin.color(prefix + "&aEchange demarre avec &d" + target.getName() + "&a !"));
        target.sendMessage(PastequeSkyblockPlugin.color(prefix + "&aEchange demarre avec &d" + sender.getName() + "&a !"));

        openTradeGui(sender);
        openTradeGui(target);
    }

    public void denyRequest(Player target) {
        String prefix = plugin.getPrefix();

        if (!hasPendingRequest(target.getUniqueId())) {
            target.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cVous n'avez aucune demande d'echange en attente."));
            return;
        }

        UUID senderId = pendingRequests.remove(target.getUniqueId());
        pendingTimestamps.remove(target.getUniqueId());

        target.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cDemande d'echange refusee."));

        Player sender = Bukkit.getPlayer(senderId);
        if (sender != null && sender.isOnline()) {
            sender.sendMessage(PastequeSkyblockPlugin.color(prefix + "&c" + target.getName() + " a refuse votre demande d'echange."));
        }
    }

    public void cancelTrade(UUID playerId) {
        TradeSession session = activeTrades.get(playerId);
        if (session == null) {
            return;
        }

        session.setState(TradeSession.TradeState.CANCELLED);

        // Cancel countdown task if running
        if (session.getCountdownTaskId() != -1) {
            Bukkit.getScheduler().cancelTask(session.getCountdownTaskId());
            session.setCountdownTaskId(-1);
        }

        UUID p1 = session.getPlayer1();
        UUID p2 = session.getPlayer2();

        returnItems(session, p1);
        returnItems(session, p2);

        activeTrades.remove(p1);
        activeTrades.remove(p2);

        String prefix = plugin.getPrefix();
        Player player1 = Bukkit.getPlayer(p1);
        Player player2 = Bukkit.getPlayer(p2);

        if (player1 != null && player1.isOnline()) {
            player1.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cL'echange a ete annule."));
            closeTradeInventory(player1);
        }
        if (player2 != null && player2.isOnline()) {
            player2.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cL'echange a ete annule."));
            closeTradeInventory(player2);
        }
    }

    private void closeTradeInventory(Player player) {
        if (player.getOpenInventory() != null
                && player.getOpenInventory().getTopInventory() != null
                && player.getOpenInventory().getTitle() != null
                && player.getOpenInventory().getTitle().startsWith(PastequeSkyblockPlugin.color("&2&lPasteque"))) {
            // Set flag to prevent InventoryCloseEvent from re-cancelling
            player.closeInventory();
        }
    }

    private void returnItems(TradeSession session, UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }

        ItemStack[] items = session.getItems(playerId);
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(items[i].clone());
                if (!overflow.isEmpty()) {
                    for (ItemStack drop : overflow.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                }
                items[i] = null;
            }
        }
    }

    public void openTradeGui(Player player) {
        TradeSession session = activeTrades.get(player.getUniqueId());
        if (session == null) {
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE_COLORED);
        populateTradeGui(inv, session, player.getUniqueId());
        player.openInventory(inv);
        GuiHelper.playOpen(player);
    }

    public void updateTradeGui(TradeSession session) {
        Player p1 = Bukkit.getPlayer(session.getPlayer1());
        Player p2 = Bukkit.getPlayer(session.getPlayer2());

        if (p1 != null && p1.isOnline()) {
            updatePlayerGui(p1, session);
        }
        if (p2 != null && p2.isOnline()) {
            updatePlayerGui(p2, session);
        }
    }

    private void updatePlayerGui(Player player, TradeSession session) {
        if (player.getOpenInventory() == null
                || player.getOpenInventory().getTopInventory() == null
                || player.getOpenInventory().getTitle() == null) {
            return;
        }
        String title = player.getOpenInventory().getTitle();
        // Check for both the base title and countdown titles
        if (!title.startsWith(PastequeSkyblockPlugin.color("&2&lPasteque"))) {
            return;
        }

        Inventory inv = player.getOpenInventory().getTopInventory();
        populateTradeGui(inv, session, player.getUniqueId());
    }

    @SuppressWarnings("deprecation")
    public void populateTradeGui(Inventory inv, TradeSession session, UUID viewerId) {
        boolean isP1 = viewerId.equals(session.getPlayer1());
        UUID myId = viewerId;
        UUID otherId = session.getOther(viewerId);

        Player myPlayer = Bukkit.getPlayer(myId);
        Player otherPlayer = Bukkit.getPlayer(otherId);
        String myName = myPlayer != null ? myPlayer.getName() : "???";
        String otherName = otherPlayer != null ? otherPlayer.getName() : "???";

        ItemStack[] myItems = session.getItems(myId);
        ItemStack[] otherItems = session.getOtherItems(myId);

        ItemStack separator = GuiHelper.glassPane(7, " ");

        // Anti-scam warning glass
        boolean otherHasNothing = session.countItems(otherId) == 0 && session.countItems(myId) > 0;
        ItemStack warningGlass = otherHasNothing
                ? GuiHelper.glassPane(14, "&c&l/!\\ ATTENTION /!\\")
                : GuiHelper.glassPane(3, " ");

        // Row 0: top border
        for (int i = 0; i < 9; i++) {
            if (i == 4) {
                inv.setItem(i, separator.clone());
            } else {
                inv.setItem(i, warningGlass.clone());
            }
        }

        // Rows 1-3: trade item slots
        // Left side = my items (columns 0-3), right side = other items (columns 5-8)
        for (int row = 1; row <= 3; row++) {
            for (int col = 0; col <= 3; col++) {
                int slot = row * 9 + col;
                int itemIndex = (row - 1) * 4 + col;
                ItemStack myItem = myItems[itemIndex];
                if (myItem != null) {
                    inv.setItem(slot, myItem.clone());
                } else {
                    inv.setItem(slot, null);
                }
            }

            // Separator column 4
            inv.setItem(row * 9 + 4, separator.clone());

            for (int col = 5; col <= 8; col++) {
                int slot = row * 9 + col;
                int itemIndex = (row - 1) * 4 + (col - 5);
                ItemStack otherItem = otherItems[itemIndex];
                if (otherItem != null) {
                    inv.setItem(slot, otherItem.clone());
                } else {
                    inv.setItem(slot, null);
                }
            }
        }

        // Row 4: status and lock buttons
        boolean myLocked = session.isLocked(myId);
        boolean otherLocked = session.isLocked(otherId);

        // My status (slot 37 = row4 col1)
        ItemStack myStatus;
        if (myLocked) {
            myStatus = GuiHelper.createItem(Material.WOOL, 5, "&a&l" + myName, "&7Statut: &aVerrouille");
        } else {
            myStatus = GuiHelper.createItem(Material.WOOL, 14, "&c&l" + myName, "&7Statut: &cNon verrouille");
        }

        // Other status (slot 43 = row4 col7)
        ItemStack otherStatus;
        if (otherLocked) {
            otherStatus = GuiHelper.createItem(Material.WOOL, 5, "&a&l" + otherName, "&7Statut: &aVerrouille");
        } else {
            otherStatus = GuiHelper.createItem(Material.WOOL, 14, "&c&l" + otherName, "&7Statut: &cNon verrouille");
        }

        // Lock buttons
        ItemStack myLockBtn;
        if (myLocked) {
            myLockBtn = GuiHelper.createItem(Material.STAINED_CLAY, 5,
                    "&a&lVERROUILLE",
                    "", "&7Votre offre est verrouillee.",
                    "&7En attente de l'autre joueur...",
                    "", "&eCliquez pour deverrouiller.");
        } else {
            myLockBtn = GuiHelper.createItem(Material.STAINED_CLAY, 14,
                    "&c&lVERROUILLER",
                    "", "&7Verrouillez votre offre pour",
                    "&7confirmer les items proposes.",
                    "", "&eCliquez pour verrouiller.");
        }

        ItemStack otherLockBtn;
        if (otherLocked) {
            otherLockBtn = GuiHelper.createItem(Material.STAINED_CLAY, 5,
                    "&a&lVERROUILLE",
                    "", "&7L'autre joueur a verrouille.");
        } else {
            otherLockBtn = GuiHelper.createItem(Material.STAINED_CLAY, 14,
                    "&e&lEN ATTENTE",
                    "", "&7L'autre joueur n'a pas encore",
                    "&7verrouille son offre.");
        }

        // Countdown display
        if (session.getState() == TradeSession.TradeState.COUNTDOWN) {
            long elapsed = System.currentTimeMillis() - session.getLockTimestamp();
            int remaining = 5 - (int) (elapsed / 1000);
            if (remaining < 0) remaining = 0;

            ItemStack countdown = GuiHelper.createItem(Material.WATCH, "&6&lEchange dans " + remaining + "s",
                    "", "&7Les deux joueurs ont verrouille.",
                    "&7L'echange se fera dans &e" + remaining + " secondes&7.",
                    "", "&c&lNe modifiez rien !");
            inv.setItem(39, countdown.clone());
            inv.setItem(41, countdown.clone());

            myStatus = GuiHelper.createItem(Material.WOOL, 5, "&a&l" + myName, "&7Statut: &aVerrouille");
            otherStatus = GuiHelper.createItem(Material.WOOL, 5, "&a&l" + otherName, "&7Statut: &aVerrouille");
        } else {
            inv.setItem(39, myLockBtn);
            inv.setItem(41, otherLockBtn);
        }

        // Fill row 4 glass
        inv.setItem(36, warningGlass.clone());
        inv.setItem(37, myStatus);
        inv.setItem(38, warningGlass.clone());
        // 39 = lock button (set above)
        inv.setItem(40, separator.clone());
        // 41 = other lock button (set above)
        inv.setItem(42, warningGlass.clone());
        inv.setItem(43, otherStatus);
        inv.setItem(44, warningGlass.clone());

        // Row 5: bottom row
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, warningGlass.clone());
        }
        inv.setItem(49, GuiHelper.createItem(Material.REDSTONE_BLOCK, "&c&lAnnuler l'echange",
                "", "&7Cliquez pour annuler l'echange.",
                "&7Vos items vous seront rendus."));
    }

    public void completeTrade(final TradeSession session) {
        if (session.getState() == TradeSession.TradeState.COMPLETED
                || session.getState() == TradeSession.TradeState.CANCELLED) {
            return;
        }

        Player p1 = Bukkit.getPlayer(session.getPlayer1());
        Player p2 = Bukkit.getPlayer(session.getPlayer2());
        String prefix = plugin.getPrefix();

        if (p1 == null || !p1.isOnline() || p2 == null || !p2.isOnline()) {
            cancelTrade(session.getPlayer1());
            return;
        }

        // Count items to receive
        int p1ReceiveCount = session.countTotalItemStacks(session.getPlayer2());
        int p2ReceiveCount = session.countTotalItemStacks(session.getPlayer1());

        // Check inventory space for both players
        // Items they offered will be removed, freeing space; items they receive fill space
        int p1EmptySlots = countEmptySlots(p1);
        int p2EmptySlots = countEmptySlots(p2);

        // Items currently in trade are already removed from their inventory,
        // so we only need space for what they receive
        // But since the trade GUI is open, the items are "virtual" -
        // we need to account for the items they put in trade being returned
        int p1Offered = session.countTotalItemStacks(session.getPlayer1());
        int p2Offered = session.countTotalItemStacks(session.getPlayer2());

        // After trade: player gets back freed slots from their offered items,
        // minus slots needed for received items
        // Net slots needed = items to receive - items offered (already out of inventory)
        // Actually items are already removed from inventory when placed in trade,
        // so empty slots already account for that. We just need space for received items.
        if (p1EmptySlots < p1ReceiveCount) {
            p1.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cVous n'avez pas assez de place dans votre inventaire !"));
            p2.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cL'autre joueur n'a pas assez de place. Echange annule."));
            cancelTrade(session.getPlayer1());
            return;
        }

        if (p2EmptySlots < p2ReceiveCount) {
            p2.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cVous n'avez pas assez de place dans votre inventaire !"));
            p1.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cL'autre joueur n'a pas assez de place. Echange annule."));
            cancelTrade(session.getPlayer1());
            return;
        }

        session.setState(TradeSession.TradeState.COMPLETED);

        // Cancel countdown task
        if (session.getCountdownTaskId() != -1) {
            Bukkit.getScheduler().cancelTask(session.getCountdownTaskId());
            session.setCountdownTaskId(-1);
        }

        // Give P1 items from P2
        ItemStack[] p2Items = session.getPlayer2Items();
        for (int i = 0; i < p2Items.length; i++) {
            if (p2Items[i] != null) {
                p1.getInventory().addItem(p2Items[i].clone());
                p2Items[i] = null;
            }
        }

        // Give P2 items from P1
        ItemStack[] p1Items = session.getPlayer1Items();
        for (int i = 0; i < p1Items.length; i++) {
            if (p1Items[i] != null) {
                p2.getInventory().addItem(p1Items[i].clone());
                p1Items[i] = null;
            }
        }

        activeTrades.remove(session.getPlayer1());
        activeTrades.remove(session.getPlayer2());

        p1.sendMessage(PastequeSkyblockPlugin.color(prefix + "&aEchange termine avec succes !"));
        p2.sendMessage(PastequeSkyblockPlugin.color(prefix + "&aEchange termine avec succes !"));

        GuiHelper.playSuccess(p1);
        GuiHelper.playSuccess(p2);

        p1.closeInventory();
        p2.closeInventory();
    }

    private int countEmptySlots(Player player) {
        int count = 0;
        ItemStack[] contents = player.getInventory().getContents();
        // Only count main inventory slots (0-35), not armor
        for (int i = 0; i < 36; i++) {
            if (i < contents.length && (contents[i] == null || contents[i].getType() == Material.AIR)) {
                count++;
            }
        }
        return count;
    }

    public void startCountdown(final TradeSession session) {
        if (session.getState() != TradeSession.TradeState.COUNTDOWN) {
            return;
        }

        final String prefix = plugin.getPrefix();
        final Player p1 = Bukkit.getPlayer(session.getPlayer1());
        final Player p2 = Bukkit.getPlayer(session.getPlayer2());

        if (p1 != null) {
            p1.sendMessage(PastequeSkyblockPlugin.color(prefix + "&eLes deux joueurs ont verrouille ! Echange dans 5 secondes..."));
        }
        if (p2 != null) {
            p2.sendMessage(PastequeSkyblockPlugin.color(prefix + "&eLes deux joueurs ont verrouille ! Echange dans 5 secondes..."));
        }

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (session.getState() != TradeSession.TradeState.COUNTDOWN) {
                    if (session.getCountdownTaskId() != -1) {
                        Bukkit.getScheduler().cancelTask(session.getCountdownTaskId());
                        session.setCountdownTaskId(-1);
                    }
                    return;
                }

                ticks++;
                // Update GUI every second (20 ticks)
                if (ticks % 20 == 0) {
                    updateTradeGui(session);

                    long elapsed = System.currentTimeMillis() - session.getLockTimestamp();
                    if (elapsed >= 5000) {
                        Bukkit.getScheduler().cancelTask(session.getCountdownTaskId());
                        session.setCountdownTaskId(-1);
                        completeTrade(session);
                    }
                }
            }
        }, 0L, 1L);

        session.setCountdownTaskId(taskId);
    }

    public void cleanupPendingRequests(UUID playerId) {
        pendingRequests.remove(playerId);
        pendingTimestamps.remove(playerId);

        // Also remove any requests sent BY this player
        Iterator<Map.Entry<UUID, UUID>> it = pendingRequests.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, UUID> entry = it.next();
            if (entry.getValue().equals(playerId)) {
                pendingTimestamps.remove(entry.getKey());
                it.remove();
            }
        }
    }
}
