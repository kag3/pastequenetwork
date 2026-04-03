package fr.pastequeworld.party.command;

import fr.pastequeworld.party.PastequeParty;
import fr.pastequeworld.party.data.Party;
import fr.pastequeworld.party.data.PartyManager;
import fr.pastequeworld.party.util.Msg;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.*;

public class PartyCommand extends Command implements TabExecutor {

    private final PastequeParty plugin;

    public PartyCommand(PastequeParty plugin) {
        super("party", null, "g", "groupe", "group", "p");
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
            case "create":
            case "creer":
            case "créer":
                handleCreate(player);
                break;
            case "invite":
            case "inviter":
            case "add":
                handleInvite(player, args);
                break;
            case "accept":
            case "accepter":
            case "join":
            case "rejoindre":
                handleAccept(player, args);
                break;
            case "deny":
            case "refuser":
                handleDeny(player, args);
                break;
            case "leave":
            case "quitter":
                handleLeave(player);
                break;
            case "kick":
            case "exclure":
                handleKick(player, args);
                break;
            case "leader":
            case "chef":
                handleLeader(player, args);
                break;
            case "list":
            case "liste":
            case "info":
                handleList(player);
                break;
            case "disband":
            case "dissoudre":
                handleDisband(player);
                break;
            case "chat":
            case "c":
                handleChat(player, args);
                break;
            default:
                // If it looks like a player name, treat as invite shortcut
                ProxiedPlayer target = plugin.getProxy().getPlayer(sub);
                if (target != null) {
                    handleInvite(player, new String[]{"invite", sub});
                } else {
                    showHelp(player);
                }
                break;
        }
    }

    // ==================== CREATE ====================

    private void handleCreate(ProxiedPlayer player) {
        PartyManager pm = plugin.getPartyManager();

        if (pm.isInParty(player.getUniqueId())) {
            Msg.send(player, "&cVous êtes déjà dans un groupe ! &7(/party leave)");
            return;
        }

        Party party = pm.createParty(player.getUniqueId());
        if (party != null) {
            Msg.send(player, "&aGroupe créé ! &7Invitez des joueurs avec &e/party invite <joueur>");
        }
    }

    // ==================== INVITE ====================

    private void handleInvite(ProxiedPlayer player, String[] args) {
        if (args.length < 2) {
            Msg.send(player, "&cUtilisation: &e/party invite <joueur>");
            return;
        }

        PartyManager pm = plugin.getPartyManager();
        Party party = pm.getParty(player.getUniqueId());

        // Auto-create party if not in one
        if (party == null) {
            party = pm.createParty(player.getUniqueId());
            Msg.send(player, "&aGroupe créé automatiquement !");
        }

        if (!party.isLeader(player.getUniqueId())) {
            Msg.send(player, "&cSeul le chef du groupe peut inviter des joueurs !");
            return;
        }

        if (party.getSize() >= PartyManager.MAX_PARTY_SIZE) {
            Msg.send(player, "&cLe groupe est plein ! (&e" + PartyManager.MAX_PARTY_SIZE + " max&c)");
            return;
        }

        String targetName = args[1];
        ProxiedPlayer target = plugin.getProxy().getPlayer(targetName);

        if (target == null) {
            Msg.send(player, "&cJoueur &e" + targetName + " &cnon trouvé ou hors-ligne.");
            return;
        }

        if (target.equals(player)) {
            Msg.send(player, "&cVous ne pouvez pas vous inviter vous-même !");
            return;
        }

        if (party.isMember(target.getUniqueId())) {
            Msg.send(player, "&e" + target.getName() + " &7est déjà dans votre groupe.");
            return;
        }

        if (pm.isInParty(target.getUniqueId())) {
            Msg.send(player, "&e" + target.getName() + " &cest déjà dans un autre groupe.");
            return;
        }

        if (party.hasInvite(target.getUniqueId())) {
            Msg.send(player, "&7Vous avez déjà invité &e" + target.getName() + "&7.");
            return;
        }

        party.addInvite(target.getUniqueId());

        Msg.send(player, "&aInvitation envoyée à &e" + target.getName() + " &a!");
        Msg.sendInvite(target, player.getName());

        // Notify other members
        for (UUID memberUuid : party.getMembers()) {
            if (memberUuid.equals(player.getUniqueId())) continue;
            ProxiedPlayer member = plugin.getProxy().getPlayer(memberUuid);
            if (member != null) {
                Msg.send(member, "&e" + player.getName() + " &7a invité &e" + target.getName() + " &7dans le groupe.");
            }
        }
    }

    // ==================== ACCEPT ====================

    private void handleAccept(ProxiedPlayer player, String[] args) {
        PartyManager pm = plugin.getPartyManager();

        if (pm.isInParty(player.getUniqueId())) {
            Msg.send(player, "&cVous êtes déjà dans un groupe ! &7(/party leave d'abord)");
            return;
        }

        Party party = null;

        if (args.length >= 2) {
            // Accept specific invite
            String leaderName = args[1];
            ProxiedPlayer leaderPlayer = plugin.getProxy().getPlayer(leaderName);
            if (leaderPlayer != null) {
                party = pm.findPartyWithInvite(player.getUniqueId(), leaderPlayer.getUniqueId());
            }
            if (party == null) {
                Msg.send(player, "&cAucune invitation de la part de &e" + leaderName + "&c.");
                return;
            }
        } else {
            // Accept most recent invite
            party = pm.findAnyPartyWithInvite(player.getUniqueId());
            if (party == null) {
                Msg.send(player, "&cAucune invitation en attente.");
                return;
            }
        }

        pm.joinParty(player.getUniqueId(), party);

        // Notify all members
        for (UUID memberUuid : party.getMembers()) {
            ProxiedPlayer member = plugin.getProxy().getPlayer(memberUuid);
            if (member != null) {
                if (memberUuid.equals(player.getUniqueId())) {
                    ProxiedPlayer leaderPlayer = plugin.getProxy().getPlayer(party.getLeader());
                    String leaderName = leaderPlayer != null ? leaderPlayer.getName() : "???";
                    Msg.send(member, "&aVous avez rejoint le groupe de &e" + leaderName + " &a!");
                } else {
                    Msg.send(member, "&a+ &e" + player.getName() + " &7a rejoint le groupe ! &8(" + party.getSize() + " joueurs)");
                }
            }
        }
    }

    // ==================== DENY ====================

    private void handleDeny(ProxiedPlayer player, String[] args) {
        PartyManager pm = plugin.getPartyManager();

        Party party = null;
        if (args.length >= 2) {
            String leaderName = args[1];
            ProxiedPlayer leaderPlayer = plugin.getProxy().getPlayer(leaderName);
            if (leaderPlayer != null) {
                party = pm.findPartyWithInvite(player.getUniqueId(), leaderPlayer.getUniqueId());
            }
        } else {
            party = pm.findAnyPartyWithInvite(player.getUniqueId());
        }

        if (party == null) {
            Msg.send(player, "&cAucune invitation en attente.");
            return;
        }

        party.removeInvite(player.getUniqueId());
        Msg.send(player, "&cInvitation refusée.");

        ProxiedPlayer leader = plugin.getProxy().getPlayer(party.getLeader());
        if (leader != null) {
            Msg.send(leader, "&c\u2716 &e" + player.getName() + " &7a refusé l'invitation.");
        }
    }

    // ==================== LEAVE ====================

    private void handleLeave(ProxiedPlayer player) {
        PartyManager pm = plugin.getPartyManager();
        Party party = pm.getParty(player.getUniqueId());

        if (party == null) {
            Msg.send(player, "&cVous n'êtes dans aucun groupe.");
            return;
        }

        boolean wasLeader = party.isLeader(player.getUniqueId());
        String playerName = player.getName();

        Party remaining = pm.leaveParty(player.getUniqueId());
        Msg.send(player, "&cVous avez quitté le groupe.");

        if (remaining != null) {
            // Notify remaining members
            for (UUID memberUuid : remaining.getMembers()) {
                ProxiedPlayer member = plugin.getProxy().getPlayer(memberUuid);
                if (member != null) {
                    Msg.send(member, "&c- &e" + playerName + " &7a quitté le groupe. &8(" + remaining.getSize() + " joueurs)");

                    if (wasLeader && remaining.isLeader(memberUuid)) {
                        Msg.send(member, "&6\u2605 &eVous êtes maintenant le chef du groupe !");
                    }
                }
            }
        }
    }

    // ==================== KICK ====================

    private void handleKick(ProxiedPlayer player, String[] args) {
        if (args.length < 2) {
            Msg.send(player, "&cUtilisation: &e/party kick <joueur>");
            return;
        }

        PartyManager pm = plugin.getPartyManager();
        Party party = pm.getParty(player.getUniqueId());

        if (party == null) {
            Msg.send(player, "&cVous n'êtes dans aucun groupe.");
            return;
        }

        if (!party.isLeader(player.getUniqueId())) {
            Msg.send(player, "&cSeul le chef peut exclure des joueurs !");
            return;
        }

        String targetName = args[1];
        ProxiedPlayer target = plugin.getProxy().getPlayer(targetName);
        if (target == null || !party.isMember(target.getUniqueId())) {
            Msg.send(player, "&e" + targetName + " &cn'est pas dans votre groupe.");
            return;
        }

        if (target.equals(player)) {
            Msg.send(player, "&cVous ne pouvez pas vous exclure vous-même ! &7(/party leave)");
            return;
        }

        pm.leaveParty(target.getUniqueId());
        // Re-add leader to party if it was disbanded
        if (pm.getParty(player.getUniqueId()) == null) {
            // Party was disbanded because only 1 left
            Msg.send(player, "&e" + target.getName() + " &ca été exclu. Le groupe a été dissous.");
        } else {
            Msg.send(player, "&e" + target.getName() + " &ca été exclu du groupe.");
        }
        Msg.send(target, "&cVous avez été exclu du groupe par &e" + player.getName() + "&c.");

        // Notify other members
        Party current = pm.getParty(player.getUniqueId());
        if (current != null) {
            for (UUID memberUuid : current.getMembers()) {
                if (memberUuid.equals(player.getUniqueId())) continue;
                ProxiedPlayer member = plugin.getProxy().getPlayer(memberUuid);
                if (member != null) {
                    Msg.send(member, "&c- &e" + target.getName() + " &7a été exclu du groupe.");
                }
            }
        }
    }

    // ==================== LEADER ====================

    private void handleLeader(ProxiedPlayer player, String[] args) {
        if (args.length < 2) {
            Msg.send(player, "&cUtilisation: &e/party leader <joueur>");
            return;
        }

        PartyManager pm = plugin.getPartyManager();
        Party party = pm.getParty(player.getUniqueId());

        if (party == null) {
            Msg.send(player, "&cVous n'êtes dans aucun groupe.");
            return;
        }

        if (!party.isLeader(player.getUniqueId())) {
            Msg.send(player, "&cSeul le chef actuel peut transférer le rôle !");
            return;
        }

        String targetName = args[1];
        ProxiedPlayer target = plugin.getProxy().getPlayer(targetName);
        if (target == null || !party.isMember(target.getUniqueId())) {
            Msg.send(player, "&e" + targetName + " &cn'est pas dans votre groupe.");
            return;
        }

        if (target.equals(player)) {
            Msg.send(player, "&7Vous êtes déjà le chef !");
            return;
        }

        pm.transferLeadership(party, target.getUniqueId());

        // Notify all
        for (UUID memberUuid : party.getMembers()) {
            ProxiedPlayer member = plugin.getProxy().getPlayer(memberUuid);
            if (member != null) {
                Msg.send(member, "&6\u2605 &e" + target.getName() + " &7est maintenant le chef du groupe !");
            }
        }
    }

    // ==================== LIST ====================

    private void handleList(ProxiedPlayer player) {
        PartyManager pm = plugin.getPartyManager();
        Party party = pm.getParty(player.getUniqueId());

        if (party == null) {
            Msg.send(player, "&7Vous n'êtes dans aucun groupe.");
            Msg.send(player, "&7Créez-en un avec &e/party create &7ou &e/party invite <joueur>");
            return;
        }

        Msg.sendRaw(player, "");
        Msg.sendRaw(player, " &6&l\u2726 &d&lGroupe &7(" + party.getSize() + " joueurs) &6&l\u2726");
        Msg.sendRaw(player, "");

        for (UUID memberUuid : party.getMembers()) {
            ProxiedPlayer member = plugin.getProxy().getPlayer(memberUuid);
            boolean isLeader = party.isLeader(memberUuid);

            if (member != null && member.isConnected()) {
                String server = member.getServer() != null
                        ? getDisplayServerName(member.getServer().getInfo().getName())
                        : "???";
                String prefix = isLeader ? " &6\u2605 " : " &d\u25CF ";
                String role = isLeader ? "&6[Chef] " : "";
                Msg.sendRaw(player, prefix + role + "&e" + member.getName() + " &7\u00bb &a" + server);
            } else {
                String name = memberUuid.toString().substring(0, 8);
                Msg.sendRaw(player, " &c\u25CF &7" + name + " &8\u00bb &cHors-ligne");
            }
        }

        Msg.sendRaw(player, "");
        Msg.sendRaw(player, " &8&m                                              ");
        Msg.sendRaw(player, "");
    }

    // ==================== DISBAND ====================

    private void handleDisband(ProxiedPlayer player) {
        PartyManager pm = plugin.getPartyManager();
        Party party = pm.getParty(player.getUniqueId());

        if (party == null) {
            Msg.send(player, "&cVous n'êtes dans aucun groupe.");
            return;
        }

        if (!party.isLeader(player.getUniqueId())) {
            Msg.send(player, "&cSeul le chef peut dissoudre le groupe !");
            return;
        }

        // Notify all members before disbanding
        for (UUID memberUuid : party.getMembers()) {
            ProxiedPlayer member = plugin.getProxy().getPlayer(memberUuid);
            if (member != null) {
                Msg.send(member, "&cLe groupe a été dissous par &e" + player.getName() + "&c.");
            }
        }

        pm.disbandParty(party);
    }

    // ==================== PARTY CHAT ====================

    private void handleChat(ProxiedPlayer player, String[] args) {
        PartyManager pm = plugin.getPartyManager();
        Party party = pm.getParty(player.getUniqueId());

        if (party == null) {
            Msg.send(player, "&cVous n'êtes dans aucun groupe.");
            return;
        }

        if (args.length < 2) {
            Msg.send(player, "&cUtilisation: &e/party chat <message>");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) sb.append(" ");
            sb.append(args[i]);
        }
        String message = sb.toString();

        boolean isLeader = party.isLeader(player.getUniqueId());
        String prefix = isLeader ? "&6\u2605" : "&d\u25CF";

        for (UUID memberUuid : party.getMembers()) {
            ProxiedPlayer member = plugin.getProxy().getPlayer(memberUuid);
            if (member != null) {
                Msg.sendRaw(member, " " + prefix + " &dGroupe &7| &e" + player.getName() + " &f\u00bb &7" + message);
            }
        }
    }

    // ==================== HELP ====================

    private void showHelp(ProxiedPlayer player) {
        Msg.sendRaw(player, "");
        Msg.sendRaw(player, " &6&l\u2726 &d&lPastèque&6&l.&d&lWorld &d&l- Groupe &6&l\u2726");
        Msg.sendRaw(player, "");
        Msg.sendRaw(player, " &e/party create &7- Créer un groupe");
        Msg.sendRaw(player, " &e/party invite <joueur> &7- Inviter un joueur");
        Msg.sendRaw(player, " &e/party accept [joueur] &7- Accepter une invitation");
        Msg.sendRaw(player, " &e/party deny [joueur] &7- Refuser une invitation");
        Msg.sendRaw(player, " &e/party leave &7- Quitter le groupe");
        Msg.sendRaw(player, " &e/party kick <joueur> &7- Exclure un joueur");
        Msg.sendRaw(player, " &e/party leader <joueur> &7- Transférer le rôle de chef");
        Msg.sendRaw(player, " &e/party list &7- Voir les membres du groupe");
        Msg.sendRaw(player, " &e/party chat <message> &7- Chat de groupe");
        Msg.sendRaw(player, " &e/party disband &7- Dissoudre le groupe");
        Msg.sendRaw(player, "");
        Msg.sendRaw(player, " &7Alias: &e/g&7, &e/groupe&7, &e/group&7, &e/p");
        Msg.sendRaw(player, " &d\u25B6 &7Le groupe suit le chef quand il change de serveur");
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
            for (String sub : new String[]{"create", "invite", "accept", "deny", "leave", "kick", "leader", "list", "chat", "disband"}) {
                if (sub.startsWith(partial)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            String partial = args[1].toLowerCase();

            if (sub.equals("invite") || sub.equals("inviter") || sub.equals("add")) {
                // Online players not in the party
                for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
                    if (!p.equals(player) && p.getName().toLowerCase().startsWith(partial)) {
                        completions.add(p.getName());
                    }
                }
            } else if (sub.equals("kick") || sub.equals("exclure") || sub.equals("leader") || sub.equals("chef")) {
                // Party members
                Party party = plugin.getPartyManager().getParty(player.getUniqueId());
                if (party != null) {
                    for (UUID uuid : party.getMembers()) {
                        if (uuid.equals(player.getUniqueId())) continue;
                        ProxiedPlayer member = plugin.getProxy().getPlayer(uuid);
                        if (member != null && member.getName().toLowerCase().startsWith(partial)) {
                            completions.add(member.getName());
                        }
                    }
                }
            }
        }

        return completions;
    }
}
