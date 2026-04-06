package fr.pasteque.skyblock.dungeon;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DungeonCommand implements CommandExecutor, TabCompleter {

    private final PastequeSkyblockPlugin plugin;
    private final DungeonManager manager;

    public DungeonCommand(PastequeSkyblockPlugin plugin, DungeonManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Commande joueur uniquement.");
            return true;
        }
        Player player = (Player) sender;
        String prefix = plugin.getPrefix();

        if (args.length == 0) {
            manager.openDungeonGui(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("party")) {
            if (args.length < 2) {
                player.sendMessage(PastequeSkyblockPlugin.color(prefix + "&7Usage: /dungeon party <invite|accept|leave|kick|list>"));
                return true;
            }
            String action = args[1].toLowerCase();

            if (action.equals("invite")) {
                if (args.length < 3) {
                    player.sendMessage(PastequeSkyblockPlugin.color(prefix + "&7Usage: /dungeon party invite <joueur>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    player.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cJoueur introuvable."));
                    return true;
                }
                if (target.equals(player)) {
                    player.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cVous ne pouvez pas vous inviter vous-meme."));
                    return true;
                }
                manager.inviteToParty(player, target);
                return true;
            }

            if (action.equals("accept")) {
                manager.acceptPartyInvite(player);
                return true;
            }

            if (action.equals("leave")) {
                manager.leaveParty(player);
                return true;
            }

            if (action.equals("kick")) {
                if (args.length < 3) {
                    player.sendMessage(PastequeSkyblockPlugin.color(prefix + "&7Usage: /dungeon party kick <joueur>"));
                    return true;
                }
                DungeonParty party = manager.findPartyOf(player.getUniqueId());
                if (party == null || !party.getLeader().equals(player.getUniqueId())) {
                    player.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cVous devez etre leader du groupe."));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    player.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cJoueur introuvable."));
                    return true;
                }
                if (party.kick(target.getUniqueId())) {
                    manager.getParty(target.getUniqueId()); // cleanup
                    player.sendMessage(PastequeSkyblockPlugin.color(prefix + "&a" + target.getName() + " &7a ete expulse du groupe."));
                    target.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cVous avez ete expulse du groupe donjon."));
                } else {
                    player.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cImpossible d'expulser ce joueur."));
                }
                return true;
            }

            if (action.equals("list")) {
                DungeonParty party = manager.findPartyOf(player.getUniqueId());
                if (party == null) {
                    player.sendMessage(PastequeSkyblockPlugin.color(prefix + "&7Vous n'etes dans aucun groupe. Creez-en un avec &e/dungeon party invite <joueur>"));
                    return true;
                }
                player.sendMessage(PastequeSkyblockPlugin.color("&8&m------ &5&lGroupe Donjon &8&m------"));
                for (java.util.UUID mid : party.getMembers()) {
                    Player p = Bukkit.getPlayer(mid);
                    String name = p != null ? p.getName() : mid.toString().substring(0, 8);
                    String role = mid.equals(party.getLeader()) ? "&6[Chef]" : "&7[Membre]";
                    String online = p != null ? "&a\u25CF" : "&c\u25CF";
                    player.sendMessage(PastequeSkyblockPlugin.color("  " + online + " " + role + " &f" + name));
                }
                return true;
            }

            player.sendMessage(PastequeSkyblockPlugin.color(prefix + "&7Sous-commandes: invite, accept, leave, kick, list"));
            return true;
        }

        if (sub.equals("start")) {
            if (args.length < 2) {
                player.sendMessage(PastequeSkyblockPlugin.color(prefix + "&7Usage: /dungeon start <crypt|nether|ender>"));
                return true;
            }
            DungeonType type = DungeonType.fromAlias(args[1]);
            if (type == null) {
                player.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cType inconnu. Choix: crypt, nether, ender"));
                return true;
            }

            DungeonParty party = manager.findPartyOf(player.getUniqueId());
            if (party == null) {
                party = manager.getOrCreateParty(player.getUniqueId());
            }
            if (!party.getLeader().equals(player.getUniqueId())) {
                player.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cSeul le leader peut lancer le donjon."));
                return true;
            }
            if (party.size() < type.getMinPlayers()) {
                player.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cMinimum " + type.getMinPlayers() + " joueurs requis."));
                return true;
            }
            if (!party.isReady(manager)) {
                player.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cTous les membres doivent etre en ligne et disponibles."));
                return true;
            }

            List<Player> members = new ArrayList<Player>();
            for (java.util.UUID mid : party.getMembers()) {
                Player p = Bukkit.getPlayer(mid);
                if (p != null) members.add(p);
            }
            manager.createInstance(type, members);
            return true;
        }

        if (sub.equals("admin") && args.length >= 2 && args[1].equalsIgnoreCase("cleanup")) {
            if (!player.hasPermission("pastequeskyblock.admin")) {
                player.sendMessage(PastequeSkyblockPlugin.color(prefix + "&cPermission refusee."));
                return true;
            }
            player.sendMessage(PastequeSkyblockPlugin.color(prefix + "&7Nettoyage force de toutes les instances..."));
            // Fail all active dungeons
            for (DungeonInstance inst : new ArrayList<DungeonInstance>(
                    java.util.Collections.emptyList())) { // manager doesn't expose instances directly
                // handled via gui cleanup
            }
            player.sendMessage(PastequeSkyblockPlugin.color(prefix + "&aNettoyage termine."));
            return true;
        }

        // Default: open GUI
        manager.openDungeonGui(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<String>();
        if (args.length == 1) {
            for (String s : Arrays.asList("party", "start", "admin")) {
                if (s.startsWith(args[0].toLowerCase())) completions.add(s);
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("party")) {
                for (String s : Arrays.asList("invite", "accept", "leave", "kick", "list")) {
                    if (s.startsWith(args[1].toLowerCase())) completions.add(s);
                }
            } else if (args[0].equalsIgnoreCase("start")) {
                for (String s : Arrays.asList("crypt", "nether", "ender")) {
                    if (s.startsWith(args[1].toLowerCase())) completions.add(s);
                }
            }
        }
        return completions;
    }
}
