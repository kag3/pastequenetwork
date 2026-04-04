package fr.pasteque.skyblock.model;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class AuctionListing {
    private final String id;
    private final UUID seller;
    private final double price;
    private final ItemStack item;
    private final long createdAt;
    private final long expiresAt;

    public AuctionListing(String id, UUID seller, double price, ItemStack item, long createdAt, long expiresAt) {
        this.id = id;
        this.seller = seller;
        this.price = price;
        this.item = item;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getId() { return id; }
    public UUID getSeller() { return seller; }
    public double getPrice() { return price; }
    public ItemStack getItem() { return item; }
    public long getCreatedAt() { return createdAt; }
    public long getExpiresAt() { return expiresAt; }
    public boolean isExpired() { return System.currentTimeMillis() >= expiresAt; }

    public void save(ConfigurationSection section) {
        section.set("seller", seller.toString());
        section.set("price", price);
        section.set("item", item);
        section.set("createdAt", createdAt);
        section.set("expiresAt", expiresAt);
    }

    public static AuctionListing load(String id, ConfigurationSection section) {
        long createdAt = section.getLong("createdAt", System.currentTimeMillis());
        long expiresAt = section.getLong("expiresAt", createdAt + (7L * 24L * 60L * 60L * 1000L));
        return new AuctionListing(id, UUID.fromString(section.getString("seller")), section.getDouble("price"), section.getItemStack("item"), createdAt, expiresAt);
    }

    public String getDisplayName() {
        Material material = item == null ? Material.BEDROCK : item.getType();
        return material.name() + " x" + (item == null ? 1 : item.getAmount());
    }
}
