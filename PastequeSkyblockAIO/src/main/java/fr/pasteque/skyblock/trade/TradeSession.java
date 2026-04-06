package fr.pasteque.skyblock.trade;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class TradeSession {

    public enum TradeState {
        TRADING,
        LOCKED,
        COUNTDOWN,
        COMPLETED,
        CANCELLED
    }

    private final UUID player1;
    private final UUID player2;
    private final ItemStack[] player1Items;
    private final ItemStack[] player2Items;
    private boolean player1Confirmed;
    private boolean player2Confirmed;
    private boolean player1Locked;
    private boolean player2Locked;
    private long lockTimestamp;
    private TradeState state;
    private int countdownTaskId = -1;

    public TradeSession(UUID player1, UUID player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.player1Items = new ItemStack[12];
        this.player2Items = new ItemStack[12];
        this.player1Confirmed = false;
        this.player2Confirmed = false;
        this.player1Locked = false;
        this.player2Locked = false;
        this.lockTimestamp = 0;
        this.state = TradeState.TRADING;
    }

    public UUID getPlayer1() {
        return player1;
    }

    public UUID getPlayer2() {
        return player2;
    }

    public ItemStack[] getPlayer1Items() {
        return player1Items;
    }

    public ItemStack[] getPlayer2Items() {
        return player2Items;
    }

    public boolean isPlayer1Confirmed() {
        return player1Confirmed;
    }

    public boolean isPlayer2Confirmed() {
        return player2Confirmed;
    }

    public boolean isPlayer1Locked() {
        return player1Locked;
    }

    public boolean isPlayer2Locked() {
        return player2Locked;
    }

    public long getLockTimestamp() {
        return lockTimestamp;
    }

    public TradeState getState() {
        return state;
    }

    public void setState(TradeState state) {
        this.state = state;
    }

    public int getCountdownTaskId() {
        return countdownTaskId;
    }

    public void setCountdownTaskId(int countdownTaskId) {
        this.countdownTaskId = countdownTaskId;
    }

    public UUID getOther(UUID player) {
        if (player.equals(player1)) {
            return player2;
        }
        return player1;
    }

    public boolean isParticipant(UUID player) {
        return player.equals(player1) || player.equals(player2);
    }

    public ItemStack[] getItems(UUID player) {
        if (player.equals(player1)) {
            return player1Items;
        }
        return player2Items;
    }

    public ItemStack[] getOtherItems(UUID player) {
        if (player.equals(player1)) {
            return player2Items;
        }
        return player1Items;
    }

    public boolean isLocked(UUID player) {
        if (player.equals(player1)) {
            return player1Locked;
        }
        return player2Locked;
    }

    public boolean isConfirmed(UUID player) {
        if (player.equals(player1)) {
            return player1Confirmed;
        }
        return player2Confirmed;
    }

    public boolean areBothLocked() {
        return player1Locked && player2Locked;
    }

    public void addItem(UUID player, ItemStack item, int slot) {
        if (slot < 0 || slot >= 12 || item == null) {
            return;
        }
        ItemStack[] items = getItems(player);
        items[slot] = item.clone();
        resetLocks();
    }

    public ItemStack removeItem(UUID player, int slot) {
        if (slot < 0 || slot >= 12) {
            return null;
        }
        ItemStack[] items = getItems(player);
        ItemStack removed = items[slot];
        items[slot] = null;
        if (removed != null) {
            resetLocks();
        }
        return removed;
    }

    public void confirm(UUID player) {
        if (player.equals(player1)) {
            player1Confirmed = true;
        } else {
            player2Confirmed = true;
        }
    }

    public void unconfirm(UUID player) {
        if (player.equals(player1)) {
            player1Confirmed = false;
        } else {
            player2Confirmed = false;
        }
    }

    public void lock(UUID player) {
        if (player.equals(player1)) {
            player1Locked = true;
        } else {
            player2Locked = true;
        }
        if (player1Locked && player2Locked) {
            lockTimestamp = System.currentTimeMillis();
            state = TradeState.COUNTDOWN;
        } else {
            state = TradeState.LOCKED;
        }
    }

    public void unlock(UUID player) {
        if (player.equals(player1)) {
            player1Locked = false;
        } else {
            player2Locked = false;
        }
        if (state == TradeState.COUNTDOWN || state == TradeState.LOCKED) {
            state = TradeState.TRADING;
            lockTimestamp = 0;
        }
    }

    private void resetLocks() {
        player1Locked = false;
        player2Locked = false;
        player1Confirmed = false;
        player2Confirmed = false;
        if (state == TradeState.COUNTDOWN || state == TradeState.LOCKED) {
            state = TradeState.TRADING;
            lockTimestamp = 0;
        }
    }

    public int countItems(UUID player) {
        ItemStack[] items = getItems(player);
        int count = 0;
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                count++;
            }
        }
        return count;
    }

    public int countTotalItemStacks(UUID player) {
        ItemStack[] items = getItems(player);
        int count = 0;
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                count++;
            }
        }
        return count;
    }
}
