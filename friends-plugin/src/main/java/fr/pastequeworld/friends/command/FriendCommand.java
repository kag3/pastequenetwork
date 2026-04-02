package fr.pastequeworld.friends.command;

import fr.pastequeworld.friends.PastequeFriends;
import fr.pastequeworld.friends.data.FriendData;
import fr.pastequeworld.friends.data.FriendManager;
import fr.pastequeworld.friends.util.Msg;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.*;

public class FriendCommand extends Command implements TabExecutor {

    private final PastequeFriends plugin;

    public FriendCommand(PastequeFriends plugin) {
        super("friend", null, "f", "ami", "amis", "friends");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(TextComponent.fromLegacyText(ChatColor.RED + "Commande joueur uniquement."));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;

        if (args.length == 0) {
            showHelp(player);
            return;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "add":
            case "ajouter":
                handleAdd(player, args);
                break;
            case "accept":
            case "accepter":
                handleAccept(player, args);
                break;
            case "deny":
            case "refuser":
                handleDeny(player, args);
                break;
            case "remove":
            case "supprimer":
            case "retirer":
                handleRemove(player, args);
                break;
            case "list":
            case "liste":
                handleList(player);
                break;
            case "requests":
            case "demandes":
                handleRequests(player);
                break;
            case "join":
            case "rejoindre":
                handleJoin(player, args);
                break;
            default:
                showHelp(player);
                break;
        }
    }

    // ==================== ADD ====================

    private void handleAdd(ProxiedPlayer player, String[] args) {
        if (args.length < 2) {
            Msg.send(player, "&cUtilisation: &e/friend add <joueur>");
            return;
        }

        String targetName = args[1];
        FriendManager fm = plugin.getFriendManager();
        String playerUuid = player.getUniqueId().toString();

        if (targetName.equalsIgnoreCase(player.getName())) {
            Msg.send(player, "&cVous ne pouvez pas vous ajouter vous-même !");
            return;
        }

        // Check max friends
        if (fm.getFriendCount(playerUuid) >= FriendManager.MAX_FRIENDS) {
            Msg.send(player, "&cVous avez atteint la limite de &e" + FriendManager.MAX_FRIENDS + " &camis !");
            return;
        }

        // Resolve target UUID
        String targetUuid = fm.getUuidByName(targetName);

        // Also check if target is online (more reliable for first encounters)
        ProxiedPlayer target = plugin.getProxy().getPlayer(targetName);
        if (target != null) {
            targetUuid = target.getUniqueId().toString();
            targetName = target.getName(); // Correct case
        }

        if (targetUuid == null) {
            Msg.send(player, "&cJoueur &e" + targetName + " &cinconnu. Il doit s'être connecté au moins une fois.");
            return;
        }

        int result = fm.sendRequest(playerUuid, targetUuid);

        switch (result) {
            case 0:
                Msg.send(player, "&aDemande d'ami envoyée à &e" + targetName + " &a!");
                // Notify target if online
                if (target != null) {
                    Msg.sendFriendRequest(target, player.getName());
                }
                break;
            case 1:
                Msg.send(player, "&e" + targetName + " &7est déjà votre ami !");
                break;
            case 2:
                Msg.send(player, "&7Vous avez déjà envoyé une demande à &e" + targetName + "&7.");
                break;
            case 3:
                // Auto-accept: they sent us a request already
                fm.acceptRequest(playerUuid, targetUuid);
                Msg.send(player, "&a\u2714 Vous êtes maintenant ami avec &e" + targetName + " &a!");
                if (target != null) {
                    Msg.send(target, "&a\u2714 &e" + player.getName() + " &aa accepté votre demande d'ami !");
                }
                break;
        }
    }

    // ==================== ACCEPT ====================

    private void handleAccept(ProxiedPlayer player, String[] args) {
        if (args.length < 2) {
            Msg.send(player, "&cUtilisation: &e/friend accept <joueur>");
            return;
        }

        String fromName = args[1];
        FriendManager fm = plugin.getFriendManager();
        String playerUuid = player.getUniqueId().toString();

        String fromUuid = fm.getUuidByName(fromName);
        if (fromUuid == null) {
            Msg.send(player, "&cJoueur &e" + fromName + " &cinconnu.");
            return;
        }

        boolean success = fm.acceptRequest(playerUuid, fromUuid);
        if (success) {
            String resolvedName = fm.getNameByUuid(fromUuid);
            if (resolvedName == null) resolvedName = fromName;

            Msg.send(player, "&a\u2714 Vous êtes maintenant ami avec &e" + resolvedName + " &a!");

            // Notify the other player if online
            ProxiedPlayer from = plugin.getProxy().getPlayer(UUID.fromString(fromUuid));
            if (from != null) {
                Msg.send(from, "&a\u2714 &e" + player.getName() + " &aa accepté votre demande d'ami !");
            }
        } else {
            Msg.send(player, "&cAucune demande d'ami de la part de &e" + fromName + "&c.");
        }
    }

    // ==================== DENY ====================

    private void handleDeny(ProxiedPlayer player, String[] args) {
        if (args.length < 2) {
            Msg.send(player, "&cUtilisation: &e/friend deny <joueur>");
            return;
        }

        String fromName = args[1];
        FriendManager fm = plugin.getFriendManager();
        String playerUuid = player.getUniqueId().toString();

        String fromUuid = fm.getUuidByName(fromName);
        if (fromUuid == null) {
            Msg.send(player, "&cJoueur &e" + fromName + " &cinconnu.");
            return;
        }

        boolean success = fm.denyRequest(playerUuid, fromUuid);
        if (success) {
            String resolvedName = fm.getNameByUuid(fromUuid);
            if (resolvedName == null) resolvedName = fromName;
            Msg.send(player, "&c\u2716 Demande de &e" + resolvedName + " &crefusée.");
        } else {
            Msg.send(player, "&cAucune demande d'ami de la part de &e" + fromName + "&c.");
        }
    }

    // ==================== REMOVE ====================

    private void handleRemove(ProxiedPlayer player, String[] args) {
        if (args.length < 2) {
            Msg.send(player, "&cUtilisation: &e/friend remove <joueur>");
            return;
        }

        String friendName = args[1];
        FriendManager fm = plugin.getFriendManager();
        String playerUuid = player.getUniqueId().toString();

        String friendUuid = fm.getUuidByName(friendName);
        if (friendUuid == null) {
            Msg.send(player, "&cJoueur &e" + friendName + " &cinconnu.");
            return;
        }

        boolean success = fm.removeFriend(playerUuid, friendUuid);
        if (success) {
            String resolvedName = fm.getNameByUuid(friendUuid);
            if (resolvedName == null) resolvedName = friendName;
            Msg.send(player, "&c\u2716 &e" + resolvedName + " &ca été retiré de votre liste d'amis.");

            ProxiedPlayer friend = plugin.getProxy().getPlayer(UUID.fromString(friendUuid));
            if (friend != null) {
                Msg.send(friend, "&c\u2716 &e" + player.getName() + " &cvous a retiré de ses amis.");
            }
        } else {
            Msg.send(player, "&e" + friendName + " &7n'est pas dans votre liste d'amis.");
        }
    }

    // ==================== LIST ====================

    private void handleList(ProxiedPlayer player) {
        FriendManager fm = plugin.getFriendManager();
        String playerUuid = player.getUniqueId().toString();
        FriendData data = fm.getData(playerUuid);

        if (data == null || data.getFriends().isEmpty()) {
            Msg.send(player, "&7Vous n'avez aucun ami pour le moment.");
            Msg.send(player, "&7Ajoutez quelqu'un avec &e/friend add <joueur>");
            return;
        }

        // Separate online and offline friends
        List<String[]> onlineFriends = new ArrayList<String[]>();  // [name, server]
        List<String> offlineFriends = new ArrayList<String>();

        for (String friendUuid : data.getFriends()) {
            String name = fm.getNameByUuid(friendUuid);
            if (name == null) name = friendUuid.substring(0, 8) + "...";

            ProxiedPlayer friend = null;
            try {
                friend = plugin.getProxy().getPlayer(UUID.fromString(friendUuid));
            } catch (IllegalArgumentException ignored) {
            }

            if (friend != null && friend.isConnected()) {
                String server = friend.getServer() != null
                        ? getDisplayServerName(friend.getServer().getInfo().getName())
                        : "???";
                onlineFriends.add(new String[]{name, server, friendUuid});
            } else {
                offlineFriends.add(name);
            }
        }

        int total = data.getFriends().size();
        int online = onlineFriends.size();

        // Header
        Msg.sendRaw(player, "");
        Msg.sendRaw(player, " &6&l\u2726 &e&lListe d'amis &7(" + online + "&a en ligne&7/" + total + ") &6&l\u2726");
        Msg.sendRaw(player, "");

        // Online friends
        if (!onlineFriends.isEmpty()) {
            for (String[] entry : onlineFriends) {
                String name = entry[0];
                String server = entry[1];

                TextComponent line = new TextComponent(TextComponent.fromLegacyText(
                        Msg.color(" &a\u25CF &e" + name + " &7\u00bb &a" + server)));

                TextComponent joinBtn = Msg.createJoinButton(name, server);
                player.sendMessage(line, joinBtn);
            }
        }

        // Offline friends
        if (!offlineFriends.isEmpty()) {
            for (String name : offlineFriends) {
                Msg.sendRaw(player, " &c\u25CF &7" + name + " &8\u00bb &cHors-ligne");
            }
        }

        // Footer
        Msg.sendRaw(player, "");
        Msg.sendRaw(player, " &8&m                                              ");
        Msg.sendRaw(player, "");
    }

    // ==================== REQUESTS ====================

    private void handleRequests(ProxiedPlayer player) {
        FriendManager fm = plugin.getFriendManager();
        String playerUuid = player.getUniqueId().toString();
        FriendData data = fm.getData(playerUuid);

        if (data == null || data.getPendingRequests().isEmpty()) {
            Msg.send(player, "&7Aucune demande d'ami en attente.");
            return;
        }

        Msg.sendRaw(player, "");
        Msg.sendRaw(player, " &6&l\u2726 &e&lDemandes d'ami en attente &6&l\u2726");
        Msg.sendRaw(player, "");

        for (String fromUuid : data.getPendingRequests()) {
            String name = fm.getNameByUuid(fromUuid);
            if (name == null) name = fromUuid.substring(0, 8);

            Msg.sendFriendRequest(player, name);
        }

        Msg.sendRaw(player, "");
    }

    // ==================== JOIN ====================

    private void handleJoin(ProxiedPlayer player, String[] args) {
        if (args.length < 2) {
            Msg.send(player, "&cUtilisation: &e/friend join <joueur>");
            return;
        }

        String friendName = args[1];
        FriendManager fm = plugin.getFriendManager();
        String playerUuid = player.getUniqueId().toString();

        String friendUuid = fm.getUuidByName(friendName);
        if (friendUuid == null) {
            Msg.send(player, "&cJoueur &e" + friendName + " &cinconnu.");
            return;
        }

        if (!fm.areFriends(playerUuid, friendUuid)) {
            Msg.send(player, "&e" + friendName + " &cn'est pas votre ami.");
            return;
        }

        ProxiedPlayer friend = null;
        try {
            friend = plugin.getProxy().getPlayer(UUID.fromString(friendUuid));
        } catch (IllegalArgumentException ignored) {
        }

        if (friend == null || !friend.isConnected()) {
            Msg.send(player, "&e" + friendName + " &cn'est pas connecté.");
            return;
        }

        if (friend.getServer() == null) {
            Msg.send(player, "&cImpossible de déterminer le serveur de &e" + friendName + "&c.");
            return;
        }

        ServerInfo targetServer = friend.getServer().getInfo();
        String displayName = getDisplayServerName(targetServer.getName());

        if (player.getServer() != null && player.getServer().getInfo().equals(targetServer)) {
            Msg.send(player, "&7Vous êtes déjà sur le même serveur que &e" + friend.getName() + " &7!");
            return;
        }

        Msg.send(player, "&aConnexion au serveur &e" + displayName + " &ade &e" + friend.getName() + "&a...");
        player.connect(targetServer);
    }

    // ==================== HELP ====================

    private void showHelp(ProxiedPlayer player) {
        Msg.sendRaw(player, "");
        Msg.sendRaw(player, " &6&l\u2726 &e&lPastèque&6&l.&d&lWorld &e&l- Amis &6&l\u2726");
        Msg.sendRaw(player, "");
        Msg.sendRaw(player, " &e/friend add <joueur> &7- Envoyer une demande d'ami");
        Msg.sendRaw(player, " &e/friend accept <joueur> &7- Accepter une demande");
        Msg.sendRaw(player, " &e/friend deny <joueur> &7- Refuser une demande");
        Msg.sendRaw(player, " &e/friend remove <joueur> &7- Retirer un ami");
        Msg.sendRaw(player, " &e/friend list &7- Voir votre liste d'amis");
        Msg.sendRaw(player, " &e/friend requests &7- Voir les demandes en attente");
        Msg.sendRaw(player, " &e/friend join <joueur> &7- Rejoindre le serveur d'un ami");
        Msg.sendRaw(player, "");
        Msg.sendRaw(player, " &7Alias: &e/f&7, &e/ami&7, &e/amis&7, &e/friends");
        Msg.sendRaw(player, "");
    }

    // ==================== UTILS ====================

    private String getDisplayServerName(String serverName) {
        String lower = serverName.toLowerCase();
        if (lower.contains("hub") || lower.contains("lobby")) return "Hub";
        if (lower.contains("labyroyale") || lower.contains("laby")) return "LabyRoyale";
        if (lower.contains("skyblock")) return "SkyBlock";
        if (lower.contains("build")) return "Build";
        if (lower.contains("raft")) return "Raft";
        // Capitalize first letter
        return serverName.substring(0, 1).toUpperCase() + serverName.substring(1);
    }

    // ==================== TAB COMPLETE ====================

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) return Collections.emptyList();

        ProxiedPlayer player = (ProxiedPlayer) sender;
        List<String> completions = new ArrayList<String>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (String sub : new String[]{"add", "accept", "deny", "remove", "list", "requests", "join"}) {
                if (sub.startsWith(partial)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            String partial = args[1].toLowerCase();

            if (sub.equals("add") || sub.equals("ajouter")) {
                // Suggest online players
                for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
                    if (!p.equals(player) && p.getName().toLowerCase().startsWith(partial)) {
                        completions.add(p.getName());
                    }
                }
            } else if (sub.equals("accept") || sub.equals("accepter") || sub.equals("deny") || sub.equals("refuser")) {
                // Suggest pending requests
                FriendData data = plugin.getFriendManager().getData(player.getUniqueId().toString());
                if (data != null) {
                    for (String uuid : data.getPendingRequests()) {
                        String name = plugin.getFriendManager().getNameByUuid(uuid);
                        if (name != null && name.toLowerCase().startsWith(partial)) {
                            completions.add(name);
                        }
                    }
                }
            } else if (sub.equals("remove") || sub.equals("supprimer") || sub.equals("retirer")
                    || sub.equals("join") || sub.equals("rejoindre")) {
                // Suggest friends
                FriendData data = plugin.getFriendManager().getData(player.getUniqueId().toString());
                if (data != null) {
                    for (String uuid : data.getFriends()) {
                        String name = plugin.getFriendManager().getNameByUuid(uuid);
                        if (name != null && name.toLowerCase().startsWith(partial)) {
                            completions.add(name);
                        }
                    }
                }
            }
        }

        return completions;
    }
}
