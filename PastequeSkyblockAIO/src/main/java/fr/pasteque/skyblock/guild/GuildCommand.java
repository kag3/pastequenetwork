package fr.pasteque.skyblock.guild;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

public class GuildCommand implements CommandExecutor, TabCompleter {

    private static final String[] SUBCOMMANDS = {
            "create", "invite", "accept", "deny", "leave", "kick",
            "promote", "demote", "transfer", "disband", "motd",
            "islands", "info", "list"
    };

    private final PastequeSkyblockPlugin plugin;
    private final GuildManager guildManager;

    public GuildCommand(PastequeSkyblockPlugin plugin, GuildManager guildManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PastequeSkyblockPlugin.color("&2&lPasteque &8\u00bb &cCommande joueur uniquement."));
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            guildManager.openGuildMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("create")) {
            if (args.length < 2) {
                guildManager.msg(player, "&7Usage: /guild create <nom>");
                return true;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(args[i]);
            }
            guildManager.createGuild(player, sb.toString());
            return true;
        }

        if (sub.equals("invite")) {
            if (args.length < 2) {
                guildManager.msg(player, "&7Usage: /guild invite <joueur>");
                return true;
            }
            Guild guild = guildManager.getGuild(player.getUniqueId());
            if (guild == null) {
                guildManager.msg(player, "&cTu n'es dans aucune guilde.");
                return true;
            }
            if (!guild.isLeader(player.getUniqueId()) && !guild.isOfficer(player.getUniqueId())) {
                guildManager.msg(player, "&cSeuls le chef et les officiers peuvent inviter.");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                guildManager.msg(player, "&cJoueur introuvable ou hors ligne.");
                return true;
            }
            if (target.getUniqueId().equals(player.getUniqueId())) {
                guildManager.msg(player, "&cTu ne peux pas t'inviter toi-meme.");
                return true;
            }
            if (guildManager.invitePlayer(player.getUniqueId(), target.getUniqueId(), guild.getId())) {
                guildManager.msg(player, "&aInvitation envoyee a &d" + target.getName() + "&a.");
            } else {
                guildManager.msg(player, "&cImpossible d'inviter ce joueur. Il est peut-etre deja dans une guilde.");
            }
            return true;
        }

        if (sub.equals("accept")) {
            if (guildManager.acceptInvite(player.getUniqueId())) {
                guildManager.msg(player, "&aInvitation acceptee !");
            } else {
                guildManager.msg(player, "&cAucune invitation valide en attente.");
            }
            return true;
        }

        if (sub.equals("deny")) {
            if (guildManager.denyInvite(player.getUniqueId())) {
                guildManager.msg(player, "&cInvitation refusee.");
            } else {
                guildManager.msg(player, "&cAucune invitation en attente.");
            }
            return true;
        }

        if (sub.equals("leave")) {
            Guild guild = guildManager.getGuild(player.getUniqueId());
            if (guild == null) {
                guildManager.msg(player, "&cTu n'es dans aucune guilde.");
                return true;
            }
            if (guild.isLeader(player.getUniqueId())) {
                guildManager.msg(player, "&cLe chef ne peut pas quitter. Utilise /guild transfer ou /guild disband.");
                return true;
            }
            if (guildManager.leaveGuild(player.getUniqueId())) {
                guildManager.msg(player, "&aTu as quitte la guilde.");
            }
            return true;
        }

        if (sub.equals("kick")) {
            if (args.length < 2) {
                guildManager.msg(player, "&7Usage: /guild kick <joueur>");
                return true;
            }
            Guild guild = guildManager.getGuild(player.getUniqueId());
            if (guild == null) {
                guildManager.msg(player, "&cTu n'es dans aucune guilde.");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (guildManager.kickMember(player.getUniqueId(), target.getUniqueId())) {
                guildManager.msg(player, "&a" + args[1] + " a ete expulse.");
            } else {
                guildManager.msg(player, "&cImpossible d'expulser ce joueur.");
            }
            return true;
        }

        if (sub.equals("promote")) {
            if (args.length < 2) {
                guildManager.msg(player, "&7Usage: /guild promote <joueur>");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (guildManager.promotePlayer(player.getUniqueId(), target.getUniqueId())) {
                guildManager.msg(player, "&a" + args[1] + " a ete promu officier.");
            } else {
                guildManager.msg(player, "&cImpossible de promouvoir ce joueur.");
            }
            return true;
        }

        if (sub.equals("demote")) {
            if (args.length < 2) {
                guildManager.msg(player, "&7Usage: /guild demote <joueur>");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (guildManager.demotePlayer(player.getUniqueId(), target.getUniqueId())) {
                guildManager.msg(player, "&a" + args[1] + " a ete retrograde.");
            } else {
                guildManager.msg(player, "&cImpossible de retrograder ce joueur.");
            }
            return true;
        }

        if (sub.equals("transfer")) {
            if (args.length < 2) {
                guildManager.msg(player, "&7Usage: /guild transfer <joueur>");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (guildManager.transferLeadership(player.getUniqueId(), target.getUniqueId())) {
                guildManager.msg(player, "&aLeadership transfere a &d" + args[1] + "&a.");
            } else {
                guildManager.msg(player, "&cImpossible de transferer le leadership.");
            }
            return true;
        }

        if (sub.equals("disband")) {
            guildManager.disbandGuild(player);
            return true;
        }

        if (sub.equals("motd")) {
            if (args.length < 2) {
                guildManager.msg(player, "&7Usage: /guild motd <message>");
                return true;
            }
            Guild guild = guildManager.getGuild(player.getUniqueId());
            if (guild == null) {
                guildManager.msg(player, "&cTu n'es dans aucune guilde.");
                return true;
            }
            if (!guild.isLeader(player.getUniqueId()) && !guild.isOfficer(player.getUniqueId())) {
                guildManager.msg(player, "&cSeuls le chef et les officiers peuvent changer le MOTD.");
                return true;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(args[i]);
            }
            String motd = sb.toString();
            if (motd.length() > 100) {
                guildManager.msg(player, "&cLe MOTD ne peut pas depasser 100 caracteres.");
                return true;
            }
            guild.setMotd(motd);
            guildManager.save();
            guildManager.msg(player, "&aMOTD mis a jour: &f" + motd);
            return true;
        }

        if (sub.equals("islands")) {
            guildManager.openGuildIslands(player);
            return true;
        }

        if (sub.equals("info")) {
            Guild guild = guildManager.getGuild(player.getUniqueId());
            if (guild == null) {
                guildManager.msg(player, "&cTu n'es dans aucune guilde.");
                return true;
            }
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            guildManager.msg(player, "&d&l" + guild.getDisplayName());
            guildManager.msg(player, "&7Niveau: &b" + guild.getLevel() + " &8| &7XP: &b" + guild.getXp()
                    + "/" + (guild.getXpForNextLevel() > 0 ? guild.getXpForNextLevel() : "MAX"));
            guildManager.msg(player, "&7Membres: &f" + guild.getMembers().size() + "/" + guild.getMaxMembers()
                    + " &8| &7Chef: &d" + Bukkit.getOfflinePlayer(guild.getLeader()).getName());
            guildManager.msg(player, "&7Cree le: &f" + sdf.format(new Date(guild.getCreatedAt())));
            if (!guild.getMotd().isEmpty()) {
                guildManager.msg(player, "&7MOTD: &f" + guild.getMotd());
            }

            StringBuilder officers = new StringBuilder();
            for (UUID uuid : guild.getOfficers()) {
                if (officers.length() > 0) officers.append("&7, ");
                officers.append("&b").append(Bukkit.getOfflinePlayer(uuid).getName());
            }
            if (officers.length() > 0) {
                guildManager.msg(player, "&7Officiers: " + officers.toString());
            }
            return true;
        }

        if (sub.equals("list")) {
            Collection<Guild> allGuilds = guildManager.getAllGuilds();
            if (allGuilds.isEmpty()) {
                guildManager.msg(player, "&7Aucune guilde n'existe pour le moment.");
                return true;
            }
            List<Guild> sorted = new ArrayList<Guild>(allGuilds);
            Collections.sort(sorted, new Comparator<Guild>() {
                @Override
                public int compare(Guild a, Guild b) {
                    int levelCompare = Integer.compare(b.getLevel(), a.getLevel());
                    if (levelCompare != 0) return levelCompare;
                    return Integer.compare(b.getXp(), a.getXp());
                }
            });
            guildManager.msg(player, "&d&lListe des Guildes &7(" + sorted.size() + ")");
            int rank = 1;
            for (Guild g : sorted) {
                if (rank > 15) {
                    guildManager.msg(player, "&7... et " + (sorted.size() - 15) + " autres.");
                    break;
                }
                String leaderName = Bukkit.getOfflinePlayer(g.getLeader()).getName();
                guildManager.msg(player, "&e" + rank + ". &d" + g.getDisplayName()
                        + " &7Niv.&b" + g.getLevel()
                        + " &8| &7" + g.getMembers().size() + " membres"
                        + " &8| &7Chef: &f" + leaderName);
                rank++;
            }
            return true;
        }

        // Unknown subcommand
        guildManager.msg(player, "&7Commandes: /guild <create|invite|accept|deny|leave|kick|promote|demote|transfer|disband|motd|islands|info|list>");
        return true;
    }

    // =========================================================================
    //  Tab Completion
    // =========================================================================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<String>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
            return completions;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            String input = args[1].toLowerCase();

            if (sub.equals("invite")) {
                // Online players not in a guild
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(input) && guildManager.getGuild(p.getUniqueId()) == null) {
                        completions.add(p.getName());
                    }
                }
            } else if (sub.equals("kick") || sub.equals("promote") || sub.equals("demote") || sub.equals("transfer")) {
                // Guild members
                if (sender instanceof Player) {
                    Guild guild = guildManager.getGuild(((Player) sender).getUniqueId());
                    if (guild != null) {
                        for (UUID memberId : guild.getMembers()) {
                            if (guild.isLeader(memberId)) continue;
                            OfflinePlayer op = Bukkit.getOfflinePlayer(memberId);
                            String name = op.getName();
                            if (name != null && name.toLowerCase().startsWith(input)) {
                                completions.add(name);
                            }
                        }
                    }
                }
            }
            return completions;
        }

        return completions;
    }
}
