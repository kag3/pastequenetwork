package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.model.CoopIsland;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CoopCommand implements CommandExecutor {
    private final PastequeSkyblockPlugin plugin;

    public CoopCommand(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Commande joueur uniquement.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            openMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("create")) {
            if (args.length < 2) {
                MessageUtil.send(player, plugin.getPrefix(), "&7Usage: /iscoop create <joueur1> [joueur2] [joueur3] [joueur4] [joueur5]");
                return true;
            }
            List<Player> targets = new ArrayList<Player>();
            Set<UUID> seen = new LinkedHashSet<UUID>();
            for (int i = 1; i < args.length; i++) {
                Player target = Bukkit.getPlayerExact(args[i]);
                if (target == null) {
                    MessageUtil.send(player, plugin.getPrefix(), "&fLe joueur &e" + args[i] + " &fdoit etre connecte pour rejoindre une coop.");
                    return true;
                }
                if (target.getUniqueId().equals(player.getUniqueId()) || seen.contains(target.getUniqueId())) {
                    continue;
                }
                seen.add(target.getUniqueId());
                targets.add(target);
            }
            if (targets.isEmpty()) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu dois inviter au moins 1 autre joueur.");
                return true;
            }
            if (targets.size() > 5) {
                MessageUtil.send(player, plugin.getPrefix(), "&fMaximum 6 joueurs au total sur une coop.");
                return true;
            }
            if (plugin.getCoopManager().beginCreation(player, targets)) {
                MessageUtil.send(player, plugin.getPrefix(), "&aDemandes envoyees. Attends les validations pour lancer l'aventure coop.");
            }
            return true;
        }

        if (sub.equals("accept")) {
            MessageUtil.send(player, plugin.getPrefix(), plugin.getCoopManager().acceptInvite(player) ? "&aInvitation coop acceptee." : "&fTu n'as aucune invitation coop valide.");
            return true;
        }
        if (sub.equals("deny")) {
            MessageUtil.send(player, plugin.getPrefix(), plugin.getCoopManager().denyInvite(player) ? "&fInvitation coop refusee." : "&fTu n'as aucune invitation coop valide.");
            return true;
        }
        if (sub.equals("pending")) {
            int remaining = plugin.getCoopManager().getRemainingApprovals(player.getUniqueId());
            if (remaining < 0) MessageUtil.send(player, plugin.getPrefix(), "&fAucune creation coop en attente.");
            else MessageUtil.send(player, plugin.getPrefix(), "&fCreation coop en attente : &e" + remaining + " &fvalidation(s) restante(s).");
            return true;
        }
        if (sub.equals("list")) {
            Collection<CoopIsland> islands = plugin.getCoopManager().getPlayerIslands(player.getUniqueId());
            if (islands.isEmpty()) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu n'es membre d'aucune ile coop.");
                return true;
            }
            MessageUtil.send(player, plugin.getPrefix(), "&dTes iles coop :");
            for (CoopIsland island : islands) {
                String ownerName = Bukkit.getOfflinePlayer(island.getOwner()).getName();
                player.sendMessage(MessageUtil.color("&7- &f" + island.getName() + " &8[" + island.getId() + "] &7Owner: &f" + (ownerName == null ? island.getOwner().toString() : ownerName)));
            }
            return true;
        }
        if (sub.equals("home")) {
            CoopIsland island = args.length >= 2 ? plugin.getCoopManager().getIsland(args[1]) : plugin.getCoopManager().getFirstIsland(player.getUniqueId());
            if (island == null || !island.isMember(player.getUniqueId())) {
                MessageUtil.send(player, plugin.getPrefix(), "&fAucune ile coop trouvee.");
                return true;
            }
            player.teleport(plugin.getCoopManager().getSafeTeleport(island));
            MessageUtil.send(player, plugin.getPrefix(), "&aTeleportation vers l'ile coop.");
            return true;
        }
        if (sub.equals("mine") || sub.equals("mining") || sub.equals("minage")) {
            CoopIsland island = args.length >= 2 ? plugin.getCoopManager().getIsland(args[1]) : plugin.getCoopManager().getFirstIsland(player.getUniqueId());
            if (island == null || !island.isMember(player.getUniqueId())) {
                MessageUtil.send(player, plugin.getPrefix(), "&fAucune ile coop trouvee.");
                return true;
            }
            if (!island.isMiningUnlocked() || island.getMiningHome() == null) {
                MessageUtil.send(player, plugin.getPrefix(), "&fIle de minage coop verrouillee. Utilise &e/iscoop unlock mine&f.");
                return true;
            }
            player.teleport(island.getMiningHome());
            MessageUtil.send(player, plugin.getPrefix(), "&aTeleportation vers le secteur minage coop.");
            return true;
        }
        if (sub.equals("sethome")) {
            CoopIsland island = plugin.getCoopManager().getIslandAt(player.getLocation());
            if (island == null || !island.isMember(player.getUniqueId())) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu dois etre sur une ile coop dont tu es membre.");
                return true;
            }
            plugin.getCoopManager().setHome(island, player.getLocation());
            MessageUtil.send(player, plugin.getPrefix(), "&aHome coop defini.");
            return true;
        }
        if (sub.equals("rename")) {
            CoopIsland island = plugin.getCoopManager().getIslandAt(player.getLocation());
            if (island == null || !island.isMember(player.getUniqueId())) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu dois etre sur ton ile coop.");
                return true;
            }
            if (args.length < 2) {
                MessageUtil.send(player, plugin.getPrefix(), "&7Usage: /iscoop rename <nom>");
                return true;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                if (i > 1) sb.append(' ');
                sb.append(args[i]);
            }
            String name = sb.toString().trim();
            if (name.length() < 3 || name.length() > 24) {
                MessageUtil.send(player, plugin.getPrefix(), "&fLe nom doit contenir entre 3 et 24 caracteres.");
                return true;
            }
            plugin.getCoopManager().rename(island, name);
            MessageUtil.send(player, plugin.getPrefix(), "&aNom de l'ile coop mis a jour.");
            return true;
        }
        if (sub.equals("unlock") && args.length >= 2 && (args[1].equalsIgnoreCase("mine") || args[1].equalsIgnoreCase("mining") || args[1].equalsIgnoreCase("minage"))) {
            CoopIsland island = plugin.getCoopManager().getIslandAt(player.getLocation());
            if (island == null || !island.isMember(player.getUniqueId())) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu dois etre sur ton ile coop.");
                return true;
            }
            int price = plugin.getConfig().getInt("island-expansions.coop.mining-price", 50000);
            if (!plugin.getCoopManager().unlockMining(island)) {
                MessageUtil.send(player, plugin.getPrefix(), "&fDeblocage impossible. Prix : &e" + price + " Pasteque&f ou deja debloquee.");
                return true;
            }
            MessageUtil.send(player, plugin.getPrefix(), "&aIle de minage coop debloquee pour &e" + price + " Pasteque&a.");
            return true;
        }

        MessageUtil.send(player, plugin.getPrefix(), "&7/iscoop create, accept, deny, pending, list, home, mine, sethome, rename, unlock mine");
        return true;
    }

    private void openMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_PURPLE + "Iles Coop Premium");
        Collection<CoopIsland> myIslands = plugin.getCoopManager().getPlayerIslands(player.getUniqueId());
        CoopIsland first = plugin.getCoopManager().getFirstIsland(player.getUniqueId());
        int pending = plugin.getCoopManager().getRemainingApprovals(player.getUniqueId());
        int minePrice = plugin.getConfig().getInt("island-expansions.coop.mining-price", 50000);
        String mineState = first != null && first.isMiningUnlocked() ? ChatColor.GREEN + "Debloquee" : ChatColor.RED + "Verrouillee";
        ItemStack green = pane((short) 5);
        ItemStack magenta = pane((short) 2);
        for (int slot = 0; slot < 54; slot++) {
            if (slot < 9 || slot >= 45 || slot % 9 == 0 || slot % 9 == 8) {
                inv.setItem(slot, (slot % 2 == 0) ? green : magenta);
            }
        }
        inv.setItem(20, item(Material.SAPLING, ChatColor.GREEN + "Creer une coop elite", ChatColor.GRAY + "Commande : /iscoop create <joueurs...>", ChatColor.GRAY + "2 a 6 joueurs, validation de toute l'equipe"));
        inv.setItem(22, item(Material.COMPASS, ChatColor.AQUA + "Mes iles coop", ChatColor.GRAY + "Nombre actuel: " + ChatColor.GOLD + myIslands.size(), ChatColor.GRAY + "Commande : /iscoop list"));
        inv.setItem(24, item(Material.BED, ChatColor.LIGHT_PURPLE + "Home coop", ChatColor.GRAY + "Commande : /iscoop home"));
        inv.setItem(30, item(Material.IRON_PICKAXE, ChatColor.GOLD + "Secteur minage coop", ChatColor.GRAY + "Etat: " + mineState, ChatColor.GRAY + "Prix unlock: " + ChatColor.YELLOW + minePrice, ChatColor.GRAY + "Commande : /iscoop unlock mine"));
        inv.setItem(31, item(Material.NAME_TAG, ChatColor.WHITE + "Renommer la coop", ChatColor.GRAY + "Commande : /iscoop rename <nom>"));
        inv.setItem(32, item(Material.PAPER, ChatColor.YELLOW + "Creation en attente", ChatColor.GRAY + (pending < 0 ? "Aucune invitation en attente" : (pending + " validation(s) restante(s)")), ChatColor.GRAY + "Commande : /iscoop pending"));
        inv.setItem(40, item(Material.BOOK, ChatColor.LIGHT_PURPLE + "Lister mes coops", ChatColor.GRAY + "Commande : /iscoop list"));
        inv.setItem(41, item(Material.DIAMOND_PICKAXE, ChatColor.GRAY + "Home minage coop", ChatColor.GRAY + "Commande : /iscoop mine"));
        player.openInventory(inv);
    }

    private ItemStack pane(short data) {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, data);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack item(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lore = new ArrayList<String>();
            for (String line : loreLines) lore.add(line);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
