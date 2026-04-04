package fr.pasteque.skyblock.playershop.model;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class PendingShopCreation {
    private final String shopName;
    private final Inventory inventory;
    private final Location signLocation;
    private final Material signMaterial;
    private final BlockFace wallFacing;
    private boolean waitingPrice;
    private ItemStack template;
    private int stock;

    public PendingShopCreation(String shopName, Inventory inventory, Location signLocation, Material signMaterial, BlockFace wallFacing) {
        this.shopName = shopName;
        this.inventory = inventory;
        this.signLocation = signLocation;
        this.signMaterial = signMaterial;
        this.wallFacing = wallFacing;
    }

    public String getShopName() { return shopName; }
    public Inventory getInventory() { return inventory; }
    public Location getSignLocation() { return signLocation; }
    public Material getSignMaterial() { return signMaterial; }
    public BlockFace getWallFacing() { return wallFacing; }
    public boolean isWaitingPrice() { return waitingPrice; }
    public void setWaitingPrice(boolean waitingPrice) { this.waitingPrice = waitingPrice; }
    public ItemStack getTemplate() { return template == null ? null : template.clone(); }
    public void setTemplate(ItemStack template) { this.template = template == null ? null : template.clone(); }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
}
