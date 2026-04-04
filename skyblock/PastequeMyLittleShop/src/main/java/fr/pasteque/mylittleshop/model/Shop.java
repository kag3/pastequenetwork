package fr.pasteque.mylittleshop.model;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Shop {
    private final UUID owner;
    private final String name;
    private final Location signLocation;
    private final ItemStack template;
    private double price;
    private int stock;

    public Shop(UUID owner, String name, Location signLocation, ItemStack template, double price, int stock) {
        this.owner = owner;
        this.name = name;
        this.signLocation = signLocation;
        this.template = template;
        this.price = price;
        this.stock = stock;
    }

    public UUID getOwner() { return owner; }
    public String getName() { return name; }
    public Location getSignLocation() { return signLocation; }
    public ItemStack getTemplate() { return template.clone(); }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = Math.max(0, stock); }
    public void addStock(int amount) { this.stock += amount; }
    public boolean takeStock(int amount) {
        if (stock < amount) return false;
        stock -= amount;
        return true;
    }

    public String locationKey() {
        Location l = signLocation;
        return l.getWorld().getName() + ";" + l.getBlockX() + ";" + l.getBlockY() + ";" + l.getBlockZ();
    }
}
