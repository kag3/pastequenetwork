package fr.pasteque.skyblock.manager;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.model.AuctionListing;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class AuctionManager {
    public static final int PAGE_SIZE = 45;

    private final PastequeSkyblockPlugin plugin;
    private final DataFile dataFile;
    private final Map<String, AuctionListing> listings = new LinkedHashMap<String, AuctionListing>();
    private final Map<UUID, List<ItemStack>> claimableItems = new HashMap<UUID, List<ItemStack>>();

    public AuctionManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new DataFile(plugin, "auction.yml");
        load();
    }

    public void load() {
        listings.clear();
        claimableItems.clear();
        ConfigurationSection section = dataFile.getConfig().getConfigurationSection("listings");
        if (section != null) {
            for (String id : section.getKeys(false)) listings.put(id, AuctionListing.load(id, section.getConfigurationSection(id)));
        }
        ConfigurationSection returns = dataFile.getConfig().getConfigurationSection("claimable");
        if (returns != null) {
            for (String key : returns.getKeys(false)) {
                List<ItemStack> items = new ArrayList<ItemStack>();
                for (Object raw : returns.getList(key, new ArrayList<Object>())) if (raw instanceof ItemStack) items.add(((ItemStack) raw).clone());
                claimableItems.put(UUID.fromString(key), items);
            }
        }
        purgeExpired();
    }

    public void save() {
        dataFile.getConfig().set("listings", null);
        dataFile.getConfig().set("claimable", null);
        for (AuctionListing listing : listings.values()) {
            ConfigurationSection section = dataFile.getConfig().createSection("listings." + listing.getId());
            listing.save(section);
        }
        for (Map.Entry<UUID, List<ItemStack>> entry : claimableItems.entrySet()) {
            List<ItemStack> clones = new ArrayList<ItemStack>();
            for (ItemStack item : entry.getValue()) clones.add(item == null ? null : item.clone());
            dataFile.getConfig().set("claimable." + entry.getKey().toString(), clones);
        }
        dataFile.save();
    }

    public void purgeExpired() {
        List<String> expired = new ArrayList<String>();
        for (AuctionListing listing : listings.values()) if (listing.isExpired()) expired.add(listing.getId());
        for (String id : expired) expireListing(id);
        if (!expired.isEmpty()) save();
    }

    public Collection<AuctionListing> getListings() { purgeExpired(); return listings.values(); }
    public List<AuctionListing> getOrderedListings() { purgeExpired(); return new ArrayList<AuctionListing>(listings.values()); }
    public List<AuctionListing> getListingsPage(int page) {
        List<AuctionListing> ordered = getOrderedListings();
        if (ordered.isEmpty()) return Collections.emptyList();
        int totalPages = getTotalPages();
        page = Math.max(1, Math.min(page, totalPages));
        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(ordered.size(), start + PAGE_SIZE);
        return ordered.subList(start, end);
    }
    public int getTotalPages() { int size = getOrderedListings().size(); return Math.max(1, (int)Math.ceil(size / (double)PAGE_SIZE)); }
    public int getActiveCount(UUID seller) { int count = 0; for (AuctionListing listing : getOrderedListings()) if (listing.getSeller().equals(seller)) count++; return count; }

    public AuctionListing create(UUID seller, double price, ItemStack item) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        long createdAt = System.currentTimeMillis();
        long days = plugin.getConfig().getLong("auction.expiration-days", 7L);
        long expiresAt = createdAt + (days * 24L * 60L * 60L * 1000L);
        AuctionListing listing = new AuctionListing(id, seller, price, item, createdAt, expiresAt);
        listings.put(id, listing);
        save();
        return listing;
    }

    public AuctionListing get(String id) { purgeExpired(); return listings.get(id); }

    public void remove(String id) { if (listings.remove(id) != null) save(); }

    public void expireListing(String id) {
        AuctionListing listing = listings.remove(id);
        if (listing != null) addClaimable(listing.getSeller(), listing.getItem());
    }

    public boolean buy(String id, UUID buyer) {
        AuctionListing listing = get(id);
        if (listing == null) return false;
        listings.remove(id);
        plugin.getEconomyManager().add(listing.getSeller(), listing.getPrice());
        save();
        return true;
    }

    public boolean adminRemove(String id, boolean returnToSeller) {
        AuctionListing listing = listings.remove(id);
        if (listing == null) return false;
        if (returnToSeller) addClaimable(listing.getSeller(), listing.getItem());
        save();
        return true;
    }

    public void addClaimable(UUID uuid, ItemStack item) {
        List<ItemStack> items = claimableItems.get(uuid);
        if (items == null) {
            items = new ArrayList<ItemStack>();
            claimableItems.put(uuid, items);
        }
        items.add(item == null ? null : item.clone());
    }

    public List<ItemStack> getClaimable(UUID uuid) {
        List<ItemStack> source = claimableItems.get(uuid);
        if (source == null) return Collections.emptyList();
        List<ItemStack> out = new ArrayList<ItemStack>();
        for (ItemStack item : source) out.add(item == null ? null : item.clone());
        return out;
    }

    public int claimAll(UUID uuid, org.bukkit.inventory.Inventory inventory) {
        List<ItemStack> items = claimableItems.remove(uuid);
        if (items == null || items.isEmpty()) return 0;
        int moved = 0;
        for (ItemStack item : items) {
            java.util.HashMap<Integer, ItemStack> leftover = inventory.addItem(item.clone());
            if (leftover.isEmpty()) {
                moved++;
            } else {
                List<ItemStack> keep = claimableItems.get(uuid);
                if (keep == null) { keep = new ArrayList<ItemStack>(); claimableItems.put(uuid, keep); }
                keep.addAll(leftover.values());
            }
        }
        save();
        return moved;
    }
}
