package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.gui.GuiHelper;
import fr.pasteque.skyblock.model.CoopIsland;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CoopCommand implements CommandExecutor {
    private final PastequeSkyblockPlugin plugin;

    public static final String MENU_TITLE = PastequeSkyblockPlugin.color("&2&lPasteque &5&lIle Cooperative");

    public CoopCommand(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PastequeSkyblockPlugin.color("&2&lPasteque &8\u00bb &cCommande joueur uniquement."));
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
                    MessageUtil.send(player, plugin.getPrefix(), "&cLe joueur &e" + args[i] + " &cdoit etre connecte pour rejoindre une coop.");
                    return true;
                }
                if (target.getUniqueId().equals(player.getUniqueId()) || seen.contains(target.getUniqueId())) {
                    continue;
                }
                seen.add(target.getUniqueId());
                targets.add(target);
            }
            if (targets.isEmpty()) {
                MessageUtil.send(player, plugin.getPrefix(), "&cTu dois inviter au moins 1 autre joueur.");
                return true;
            }
            if (targets.size() > 5) {
                MessageUtil.send(player, plugin.getPrefix(), "&cMaximum 6 joueurs au total sur une coop.");
                return true;
            }
            if (plugin.getCoopManager().beginCreation(player, targets)) {
                MessageUtil.send(player, plugin.getPrefix(), "&aDemandes envoyees. Attends les validations pour lancer l'aventure coop.");
            }
            return true;
        }

        if (sub.equals("accept")) {
            MessageUtil.send(player, plugin.getPrefix(), plugin.getCoopManager().acceptInvite(player) ? "&aInvitation coop acceptee." : "&cTu n'as aucune invitation coop valide.");
            return true;
        }
        if (sub.equals("deny")) {
            MessageUtil.send(player, plugin.getPrefix(), plugin.getCoopManager().denyInvite(player) ? "&7Invitation coop refusee." : "&cTu n'as aucune invitation coop valide.");
            return true;
        }
        if (sub.equals("pending")) {
            int remaining = plugin.getCoopManager().getRemainingApprovals(player.getUniqueId());
            if (remaining < 0) {
                MessageUtil.send(player, plugin.getPrefix(), "&7Aucune creation coop en attente.");
            } else {
                MessageUtil.send(player, plugin.getPrefix(), "&7Creation coop en attente : &e" + remaining + " &7validation(s) restante(s).");
            }
            return true;
        }
        if (sub.equals("list")) {
            Collection<CoopIsland> islands = plugin.getCoopManager().getPlayerIslands(player.getUniqueId());
            if (islands.isEmpty()) {
                MessageUtil.send(player, plugin.getPrefix(), "&cTu n'es membre d'aucune ile coop.");
                return true;
            }
            MessageUtil.send(player, plugin.getPrefix(), "&5Tes iles coop :");
            for (CoopIsland island : islands) {
                String ownerName = Bukkit.getOfflinePlayer(island.getOwner()).getName();
                player.sendMessage(MessageUtil.color("&8\u25B8 &7" + island.getName() + " &8[" + island.getId() + "] &7Owner: &d" + (ownerName == null ? island.getOwner().toString() : ownerName)));
            }
            return true;
        }
        if (sub.equals("home")) {
            CoopIsland island = args.length >= 2 ? plugin.getCoopManager().getIsland(args[1]) : plugin.getCoopManager().getFirstIsland(player.getUniqueId());
            if (island == null || !island.isMember(player.getUniqueId())) {
                MessageUtil.send(player, plugin.getPrefix(), "&cAucune ile coop trouvee.");
                return true;
            }
            player.teleport(plugin.getCoopManager().getSafeTeleport(island));
            MessageUtil.send(player, plugin.getPrefix(), "&aTeleportation vers l'ile coop.");
            return true;
        }
        if (sub.equals("mine") || sub.equals("mining") || sub.equals("minage")) {
            CoopIsland island = args.length >= 2 ? plugin.getCoopManager().getIsland(args[1]) : plugin.getCoopManager().getFirstIsland(player.getUniqueId());
            if (island == null || !island.isMember(player.getUniqueId())) {
                MessageUtil.send(player, plugin.getPrefix(), "&cAucune ile coop trouvee.");
                return true;
            }
            if (!island.isMiningUnlocked() || island.getMiningHome() == null) {
                MessageUtil.send(player, plugin.getPrefix(), "&cIle de minage coop verrouillee. Utilise &e/iscoop unlock mine&c.");
                return true;
            }
            player.teleport(island.getMiningHome());
            MessageUtil.send(player, plugin.getPrefix(), "&aTeleportation vers le secteur minage coop.");
            return true;
        }
        if (sub.equals("sethome")) {
            CoopIsland island = plugin.getCoopManager().getIslandAt(player.getLocation());
            if (island == null || !island.isMember(player.getUniqueId())) {
                MessageUtil.send(player, plugin.getPrefix(), "&cTu dois etre sur une ile coop dont tu es membre.");
                return true;
            }
            plugin.getCoopManager().setHome(island, player.getLocation());
            MessageUtil.send(player, plugin.getPrefix(), "&aHome coop defini.");
            return true;
        }
        if (sub.equals("rename")) {
            CoopIsland island = plugin.getCoopManager().getIslandAt(player.getLocation());
            if (island == null || !island.isMember(player.getUniqueId())) {
                MessageUtil.send(player, plugin.getPrefix(), "&cTu dois etre sur ton ile coop.");
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
                MessageUtil.send(player, plugin.getPrefix(), "&cLe nom doit contenir entre 3 et 24 caracteres.");
                return true;
            }
            plugin.getCoopManager().rename(island, name);
            MessageUtil.send(player, plugin.getPrefix(), "&aNom de l'ile coop mis a jour.");
            return true;
        }
        if (sub.equals("unlock") && args.length >= 2 && (args[1].equalsIgnoreCase("mine") || args[1].equalsIgnoreCase("mining") || args[1].equalsIgnoreCase("minage"))) {
            CoopIsland island = plugin.getCoopManager().getIslandAt(player.getLocation());
            if (island == null || !island.isMember(player.getUniqueId())) {
                MessageUtil.send(player, plugin.getPrefix(), "&cTu dois etre sur ton ile coop.");
                return true;
            }
            int price = plugin.getConfig().getInt("island-expansions.coop.mining-price", 50000);
            if (!plugin.getCoopManager().unlockMining(island)) {
                MessageUtil.send(player, plugin.getPrefix(), "&cDeblocage impossible. Prix : &e" + price + " Pasteque &cou deja debloquee.");
                return true;
            }
            MessageUtil.send(player, plugin.getPrefix(), "&aIle de minage coop debloquee pour &e" + price + " Pasteque&a.");
            return true;
        }

        MessageUtil.send(player, plugin.getPrefix(), "&7/iscoop create, accept, deny, pending, list, home, mine, sethome, rename, unlock mine");
        return true;
    }

    private void openMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, MENU_TITLE);

        GuiHelper.addTopBorder(inv);
        GuiHelper.addBottomBorder(inv);

        Collection<CoopIsland> myIslands = plugin.getCoopManager().getPlayerIslands(player.getUniqueId());
        CoopIsland first = plugin.getCoopManager().getFirstIsland(player.getUniqueId());
        int pending = plugin.getCoopManager().getRemainingApprovals(player.getUniqueId());
        int minePrice = plugin.getConfig().getInt("island-expansions.coop.mining-price", 50000);
        String mineState = first != null && first.isMiningUnlocked() ? "&aDebloquee" : "&cVerrouillee";

        // Layout parfaitement symetrique: 3+3+2 items repartis sur 3 rangees
        // Rangee 2 (18-26): 20, 22, 24 — Creer, Info, Home
        // Rangee 3 (27-35): 29, 31, 33 — Mine, Rename, Pending
        // Rangee 4 (36-44): 39, 41     — Lister, Home Mine (symetriques autour de 40)
        inv.setItem(20, GuiHelper.createItem(Material.SAPLING, "&a&lCreer une Coop",
                "",
                "&8\u25CE &7Creation d'ile cooperative",
                "&8\u25B8 &7Commande: &d/iscoop create <joueurs...>",
                "&8\u25B8 &72 a 6 joueurs, validation requise",
                "",
                "&e\u25B6 Clic pour voir les instructions"));

        inv.setItem(22, GuiHelper.createItem(Material.COMPASS, "&b&lMes Iles Coop",
                "",
                "&8\u25CE &7Informations",
                "&8\u25B8 &7Nombre actuel: &e" + myIslands.size(),
                "",
                "&e\u25B6 Clic pour lister"));

        inv.setItem(24, GuiHelper.createItem(Material.BED, "&d&lHome Coop",
                "",
                "&8\u25B8 &7Teleportation vers ton ile coop",
                "",
                "&a\u25B6 Clic pour se teleporter"));

        inv.setItem(29, GuiHelper.createItem(Material.IRON_PICKAXE, "&e&lSecteur Minage Coop",
                "",
                "&8\u25CE &7Minage cooperatif",
                "&8\u25B8 &7Etat: " + mineState,
                "&8\u25B8 &7Prix unlock: &e" + minePrice + " Pasteque",
                "",
                "&e\u25B6 Clic pour debloquer"));

        inv.setItem(31, GuiHelper.createItem(Material.NAME_TAG, "&7&lRenommer la Coop",
                "",
                "&8\u25B8 &7Commande: &d/iscoop rename <nom>",
                "",
                "&e\u25B6 Clic pour voir les instructions"));

        inv.setItem(33, GuiHelper.createItem(Material.PAPER, "&e&lCreation en Attente",
                "",
                "&8\u25B8 &7" + (pending < 0 ? "Aucune invitation en attente" : (pending + " validation(s) restante(s)")),
                "",
                "&e\u25B6 Clic pour voir le statut"));

        inv.setItem(39, GuiHelper.createItem(Material.BOOK, "&d&lLister mes Coops",
                "",
                "&8\u25B8 &7Commande: &d/iscoop list",
                "",
                "&e\u25B6 Clic pour lister"));

        inv.setItem(41, GuiHelper.createItem(Material.DIAMOND_PICKAXE, "&7&lHome Minage Coop",
                "",
                "&8\u25B8 &7Commande: &d/iscoop mine",
                "",
                "&a\u25B6 Clic pour se teleporter"));

        inv.setItem(49, GuiHelper.closeButton());

        GuiHelper.fillEmpty(inv);

        player.openInventory(inv);
    }
}
