package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class SocialCommand implements CommandExecutor {
    private final PastequeSkyblockPlugin plugin;
    public SocialCommand(PastequeSkyblockPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Commande joueur uniquement."); return true; }
        Player player = (Player) sender;
        String cmd = command.getName().toLowerCase();
        if (cmd.equals("friends")) {
            if (args.length == 0 || args[0].equalsIgnoreCase("list")) return listNames(player, plugin.getSocialManager().getFriends(player.getUniqueId()), "Amis");
            if (args.length < 2) { MessageUtil.send(player, plugin.getPrefix(), "&7Usage: /friends <add|remove|list> <joueur>"); return true; }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (args[0].equalsIgnoreCase("add")) MessageUtil.send(player, plugin.getPrefix(), plugin.getSocialManager().addFriend(player.getUniqueId(), target.getUniqueId()) ? "&aAmi ajouté." : "&fImpossible d'ajouter cet ami.");
            else if (args[0].equalsIgnoreCase("remove")) MessageUtil.send(player, plugin.getPrefix(), plugin.getSocialManager().removeFriend(player.getUniqueId(), target.getUniqueId()) ? "&aAmi retiré." : "&fAucun changement.");
            else MessageUtil.send(player, plugin.getPrefix(), "&7Usage: /friends <add|remove|list> <joueur>");
            return true;
        }
        if (cmd.equals("enemy")) {
            if (args.length == 0 || args[0].equalsIgnoreCase("list")) return listNames(player, plugin.getSocialManager().getEnemies(player.getUniqueId()), "Ennemis");
            if (args.length < 2) { MessageUtil.send(player, plugin.getPrefix(), "&7Usage: /enemy <add|remove|list> <joueur>"); return true; }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (args[0].equalsIgnoreCase("add")) MessageUtil.send(player, plugin.getPrefix(), plugin.getSocialManager().addEnemy(player.getUniqueId(), target.getUniqueId()) ? "&aEnnemi ajouté." : "&fImpossible d'ajouter cet ennemi.");
            else if (args[0].equalsIgnoreCase("remove")) MessageUtil.send(player, plugin.getPrefix(), plugin.getSocialManager().removeEnemy(player.getUniqueId(), target.getUniqueId()) ? "&aEnnemi retiré." : "&fAucun changement.");
            else MessageUtil.send(player, plugin.getPrefix(), "&7Usage: /enemy <add|remove|list> <joueur>");
            return true;
        }
        if (cmd.equals("alliance") || cmd.equals("ally")) {
            if (args.length == 0) { MessageUtil.send(player, plugin.getPrefix(), "&7Usage: /alliance <create|invite|accept|deny|leave|list>"); return true; }
            if (args[0].equalsIgnoreCase("create") && args.length >= 2) { MessageUtil.send(player, plugin.getPrefix(), plugin.getSocialManager().createAlliance(player.getUniqueId(), args[1]) ? "&aAlliance créée." : "&fImpossible de créer cette alliance."); return true; }
            if (args[0].equalsIgnoreCase("invite") && args.length >= 2) {
                String ally = plugin.getSocialManager().getAlliance(player.getUniqueId()); if (ally == null) { MessageUtil.send(player, plugin.getPrefix(), "&fCrée d'abord une alliance."); return true; }
                Player target = Bukkit.getPlayerExact(args[1]); if (target == null) { MessageUtil.send(player, plugin.getPrefix(), "&fJoueur introuvable."); return true; }
                if (plugin.getSocialManager().inviteAlliance(target.getUniqueId(), ally)) { MessageUtil.send(player, plugin.getPrefix(), "&aInvitation d'alliance envoyée."); MessageUtil.send(target, plugin.getPrefix(), "&fInvitation reçue pour l'alliance &d" + ally + "&f. /alliance accept"); }
                else MessageUtil.send(player, plugin.getPrefix(), "&fImpossible d'inviter ce joueur.");
                return true;
            }
            if (args[0].equalsIgnoreCase("accept")) { MessageUtil.send(player, plugin.getPrefix(), plugin.getSocialManager().acceptAlliance(player.getUniqueId()) ? "&aAlliance rejointe." : "&fAucune invitation d'alliance."); return true; }
            if (args[0].equalsIgnoreCase("deny")) { plugin.getSocialManager().denyAlliance(player.getUniqueId()); MessageUtil.send(player, plugin.getPrefix(), "&aInvitation refusée."); return true; }
            if (args[0].equalsIgnoreCase("leave")) { MessageUtil.send(player, plugin.getPrefix(), plugin.getSocialManager().leaveAlliance(player.getUniqueId()) ? "&aAlliance quittée." : "&fTu n'as pas d'alliance."); return true; }
            if (args[0].equalsIgnoreCase("list")) {
                String ally = plugin.getSocialManager().getAlliance(player.getUniqueId());
                if (ally == null) { MessageUtil.send(player, plugin.getPrefix(), "&fTu n'as pas d'alliance."); return true; }
                return listNames(player, plugin.getSocialManager().getAllianceMembers(ally), "Alliance " + ally);
            }
            MessageUtil.send(player, plugin.getPrefix(), "&7Usage: /alliance <create|invite|accept|deny|leave|list>");
            return true;
        }
        return true;
    }

    private boolean listNames(Player player, Set<UUID> ids, String title) {
        StringBuilder sb = new StringBuilder();
        for (UUID id : ids) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(id);
            if (sb.length() > 0) sb.append("&7, ");
            sb.append("&f").append(op.getName() == null ? id.toString() : op.getName());
        }
        MessageUtil.send(player, plugin.getPrefix(), "&d" + title + "&7: " + (sb.length() == 0 ? "&faucun" : sb.toString()));
        return true;
    }
}
