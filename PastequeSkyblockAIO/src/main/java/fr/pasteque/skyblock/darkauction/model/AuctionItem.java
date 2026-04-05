package fr.pasteque.skyblock.darkauction.model;

import java.util.ArrayList;
import java.util.List;

public class AuctionItem {

    private final String id;
    private final String displayName;
    private final String material;
    private final int amount;
    private final double startingBid;
    private final String lore;

    public AuctionItem(String id, String displayName, String material, int amount, double startingBid, String lore) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.amount = amount;
        this.startingBid = startingBid;
        this.lore = lore;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMaterial() {
        return material;
    }

    public int getAmount() {
        return amount;
    }

    public double getStartingBid() {
        return startingBid;
    }

    public String getLore() {
        return lore;
    }

    public static List<AuctionItem> createPool() {
        List<AuctionItem> pool = new ArrayList<AuctionItem>();
        pool.add(new AuctionItem("recombobulator", "Recombobulateur 3000", "NETHER_STAR", 1, 500000, "&7Ameliore la rarete d'un item"));
        pool.add(new AuctionItem("spirit_sceptre", "Sceptre Spectral", "BLAZE_ROD", 1, 250000, "&5Tire des boules de feu"));
        pool.add(new AuctionItem("soul_whip", "Fouet des Ames", "LEAD", 1, 150000, "&cVole la vie des ennemis"));
        pool.add(new AuctionItem("dark_claymore", "Claymore Sombre", "DIAMOND_SWORD", 1, 300000, "&4+150% degats de zone"));
        pool.add(new AuctionItem("necron_blade", "Lame de Necron", "GOLD_SWORD", 1, 1000000, "&6Epee legendaire"));
        pool.add(new AuctionItem("hyperion", "Hyperion", "IRON_SWORD", 1, 2000000, "&dArme ultime"));
        pool.add(new AuctionItem("auto_recombobulator", "Auto-Recombobulateur", "HOPPER", 1, 750000, "&7Recombobule automatiquement"));
        pool.add(new AuctionItem("enrichment", "Enrichissement", "EMERALD", 1, 100000, "&aBonus permanent +5 stats"));
        pool.add(new AuctionItem("dark_orb", "Orbe Sombre", "ENDER_PEARL", 1, 200000, "&8Invoque un familier"));
        pool.add(new AuctionItem("legendary_dye", "Teinture Legendaire", "INK_SACK", 1, 500000, "&6Colore ton armure en or"));
        pool.add(new AuctionItem("midas_staff", "Baton de Midas", "GOLD_INGOT", 1, 1500000, "&eTransforme tout en or"));
        pool.add(new AuctionItem("wither_relic", "Relique du Wither", "SKULL_ITEM", 1, 800000, "&8Item de collection rare"));
        pool.add(new AuctionItem("dragon_horn", "Corne de Dragon", "BONE", 1, 600000, "&5Invoque un dragon miniature"));
        pool.add(new AuctionItem("ancient_scroll", "Parchemin Ancien", "PAPER", 1, 350000, "&7Enchantement secret"));
        pool.add(new AuctionItem("flux_capacitor", "Condensateur de Flux", "REDSTONE_BLOCK", 1, 450000, "&bVoyage dans le temps (lag fix)"));
        return pool;
    }
}
