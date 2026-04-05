package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.gui.GuiHelper;
import fr.pasteque.skyblock.model.AuctionListing;
import fr.pasteque.skyblock.model.Island;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PSkyAdminCommand implements CommandExecutor {
    private final PastequeSkyblockPlugin plugin;
    private final Map<UUID, Location> pos1 = new HashMap<UUID, Location>();
    private final Map<UUID, Location> pos2 = new HashMap<UUID, Location>();

    public PSkyAdminCommand(PastequeSkyblockPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(PastequeSkyblockPlugin.color("&2&lPasteque &8\u00bb &cCommande joueur uniquement.")); return true; }
        Player player = (Player) sender;
        if (!player.hasPermission("pastequeskyblock.admin")) { MessageUtil.send(player, plugin.getPrefix(), "&fCommande admin."); return true; }
        if (args.length == 0) { openAdminMenu(player); return true; }
        String sub = args[0].toLowerCase();

        if (sub.equals("setspawn")) {
            plugin.getWorldManager().getSpawnWorld().setSpawnLocation(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());
            MessageUtil.send(player, plugin.getPrefix(), "&aSpawn général défini.");
            return true;
        }
        if (sub.equals("setpvpwarp")) {
            plugin.getPvpManager().setHardcoreWarp(player.getLocation());
            MessageUtil.send(player, plugin.getPrefix(), "&aWarp PvP hardcore défini.");
            return true;
        }
        if (sub.equals("createfor") && args.length >= 2) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            Island island = plugin.getIslandManager().createIslandFor(target.getUniqueId());
            if (island == null) MessageUtil.send(player, plugin.getPrefix(), "&fCe joueur possède déjà une île.");
            else MessageUtil.send(player, plugin.getPrefix(), "&aÎle créée pour &f" + target.getName());
            return true;
        }
        if (sub.equals("reset") && args.length >= 2) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            Island island = plugin.getIslandManager().getOwnedIsland(target.getUniqueId());
            if (island == null) MessageUtil.send(player, plugin.getPrefix(), "&fÎle introuvable.");
            else { plugin.getIslandManager().resetIsland(island); MessageUtil.send(player, plugin.getPrefix(), "&aÎle réinitialisée pour &f" + target.getName()); }
            return true;
        }
        if (sub.equals("tp") && args.length >= 2) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            Island island = plugin.getIslandManager().getOwnedIsland(target.getUniqueId());
            if (island == null) MessageUtil.send(player, plugin.getPrefix(), "&fÎle introuvable.");
            else { player.teleport(plugin.getIslandManager().getSafeTeleport(island)); MessageUtil.send(player, plugin.getPrefix(), "&aTéléportation vers l'île de &f" + target.getName()); }
            return true;
        }
        if (sub.equals("islands")) {
            int i = 0;
            MessageUtil.send(player, plugin.getPrefix(), "&6Liste complète des îles :");
            for (Island island : plugin.getIslandManager().getSortedIslands()) {
                String name = Bukkit.getOfflinePlayer(island.getOwner()).getName();
                player.sendMessage(MessageUtil.color("&7- &f" + (name == null ? island.getOwner().toString() : name) + " &8[" + island.getCenterX() + ", " + island.getCenterZ() + "] &7- &d" + island.getDisplayName(name)));
                if (++i >= 60) break;
            }
            return true;
        }
        if (sub.equals("reload")) {
            plugin.reloadConfig();
            plugin.getIslandManager().load();
            plugin.getEconomyManager().load();
            plugin.getAuctionManager().load();
            plugin.getChallengeManager().load();
            plugin.getPvpManager().load();
            plugin.getSocialManager().load();
            MessageUtil.send(player, plugin.getPrefix(), "&aConfiguration rechargée.");
            return true;
        }
        if (sub.equals("money") && args.length >= 4) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
            double amount;
            try { amount = Double.parseDouble(args[3]); } catch (Exception ex) { MessageUtil.send(player, plugin.getPrefix(), "&fMontant invalide."); return true; }
            if (args[1].equalsIgnoreCase("give")) plugin.getEconomyManager().add(target.getUniqueId(), amount);
            else if (args[1].equalsIgnoreCase("take")) plugin.getEconomyManager().take(target.getUniqueId(), amount);
            else if (args[1].equalsIgnoreCase("set")) plugin.getEconomyManager().setBalance(target.getUniqueId(), amount);
            else { MessageUtil.send(player, plugin.getPrefix(), "&7Usage: /psky money <give|take|set> <joueur> <montant>"); return true; }
            plugin.getEconomyManager().save();
            MessageUtil.send(player, plugin.getPrefix(), "&aÉconomie mise à jour pour &f" + target.getName());
            return true;
        }
        if (sub.equals("hdv")) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
                MessageUtil.send(player, plugin.getPrefix(), "&6Annonces HDV actives : &f" + plugin.getAuctionManager().getListings().size());
                for (AuctionListing listing : plugin.getAuctionManager().getOrderedListings()) player.sendMessage(MessageUtil.color("&7- &f" + listing.getId() + " &7/ vendeur: &f" + Bukkit.getOfflinePlayer(listing.getSeller()).getName() + " &7/ prix: &e" + listing.getPrice()));
                return true;
            }
            if (args.length >= 3 && args[1].equalsIgnoreCase("remove")) {
                boolean ok = plugin.getAuctionManager().adminRemove(args[2], true);
                MessageUtil.send(player, plugin.getPrefix(), ok ? "&aAnnonce supprimée et objet renvoyé au vendeur." : "&fAnnonce introuvable.");
                return true;
            }
            MessageUtil.send(player, plugin.getPrefix(), "&7Usage: /psky hdv <list|remove <id>>");
            return true;
        }
        if (sub.equals("events") && args.length >= 2 && args[1].equalsIgnoreCase("invasion")) {
            plugin.getInvasionManager().startInvasion();
            MessageUtil.send(player, plugin.getPrefix(), "&aInvasion lancée.");
            return true;
        }
        if (sub.equals("pvp")) {
            if (args.length < 2) { sendHelp(player); return true; }
            if (args[1].equalsIgnoreCase("pos1")) { pos1.put(player.getUniqueId(), player.getLocation()); MessageUtil.send(player, plugin.getPrefix(), "&aPosition 1 enregistrée."); return true; }
            if (args[1].equalsIgnoreCase("pos2")) { pos2.put(player.getUniqueId(), player.getLocation()); MessageUtil.send(player, plugin.getPrefix(), "&aPosition 2 enregistrée."); return true; }
            if (args[1].equalsIgnoreCase("create")) {
                if (args.length < 3) { MessageUtil.send(player, plugin.getPrefix(), "&7Usage: /psky pvp create <nom>"); return true; }
                if (!pos1.containsKey(player.getUniqueId()) || !pos2.containsKey(player.getUniqueId())) { MessageUtil.send(player, plugin.getPrefix(), "&fDéfinis d'abord pos1 et pos2."); return true; }
                plugin.getPvpManager().createZone(args[2], pos1.get(player.getUniqueId()), pos2.get(player.getUniqueId()));
                MessageUtil.send(player, plugin.getPrefix(), "&aZone PvP créée : &f" + args[2]);
                return true;
            }
            if (args[1].equalsIgnoreCase("delete") && args.length >= 3) {
                boolean ok = plugin.getPvpManager().deleteZone(args[2]);
                MessageUtil.send(player, plugin.getPrefix(), ok ? "&aZone PvP supprimée." : "&fZone introuvable.");
                return true;
            }
        }
        sendHelp(player);
        return true;
    }

    private void sendHelp(Player player) {
        MessageUtil.send(player, plugin.getPrefix(), "&7/psky setspawn &8- &fDéfinir le spawn du lobby");
        MessageUtil.send(player, plugin.getPrefix(), "&7/psky setpvpwarp &8- &fDéfinir le warp PvP hardcore");
        MessageUtil.send(player, plugin.getPrefix(), "&7/psky createfor <joueur>");
        MessageUtil.send(player, plugin.getPrefix(), "&7/psky reset <joueur>");
        MessageUtil.send(player, plugin.getPrefix(), "&7/psky tp <joueur>");
        MessageUtil.send(player, plugin.getPrefix(), "&7/psky islands");
        MessageUtil.send(player, plugin.getPrefix(), "&7/psky reload");
        MessageUtil.send(player, plugin.getPrefix(), "&7/psky money <give|take|set> <joueur> <montant>");
        MessageUtil.send(player, plugin.getPrefix(), "&7/psky hdv <list|remove <id>>");
        MessageUtil.send(player, plugin.getPrefix(), "&7/psky events invasion");
        MessageUtil.send(player, plugin.getPrefix(), "&7/psky pvp <pos1|pos2|create|delete>");
    }

    @SuppressWarnings("deprecation")
    private void openAdminMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54,
                PastequeSkyblockPlugin.color("&2&lPasteque &5&lAdmin"));

        // Borders
        GuiHelper.addTopBorder(inv);
        GuiHelper.addBottomBorder(inv);

        // Content items - symmetric placement in rows 2-4
        inv.setItem(20, GuiHelper.createItem(Material.COMPASS,
                "&2Liste des iles",
                "", "&7Voir toutes les iles connues.", "", "&8Clic pour afficher."));

        inv.setItem(21, GuiHelper.createItem(Material.NETHER_STAR,
                "&5Warp PvP Hardcore",
                "", "&7Definir le warp PvP hardcore.", "", "&8/psky setpvpwarp"));

        inv.setItem(22, GuiHelper.createItem(Material.CHEST,
                "&2Moderation HDV",
                "", "&7Voir / supprimer les annonces", "&7du marche aux encheres.", "", "&8/psky hdv list"));

        inv.setItem(23, GuiHelper.createItem(Material.MONSTER_EGG,
                "&5Invasion Hub",
                "", "&7Lancer une invasion de", "&7monstres sur le hub.", "", "&8Clic pour lancer."));

        inv.setItem(24, GuiHelper.createItem(Material.IRON_SWORD,
                "&2Zones PvP",
                "", "&7Creer / supprimer les zones", "&7PvP du monde world.", "", "&8/psky pvp ..."));

        inv.setItem(30, GuiHelper.createItem(Material.PAPER,
                "&7Reload",
                "", "&7Recharger la configuration", "&7complete du plugin.", "", "&8/psky reload"));

        inv.setItem(32, GuiHelper.createItem(Material.GOLD_INGOT,
                "&5Economie",
                "", "&7Gerer l'economie des joueurs.", "", "&8/psky money <give|take|set>"));

        // Close button - bottom center
        inv.setItem(49, GuiHelper.closeButton());

        // Fill remaining empty slots with black glass
        GuiHelper.fillEmpty(inv);

        player.openInventory(inv);
    }
}
