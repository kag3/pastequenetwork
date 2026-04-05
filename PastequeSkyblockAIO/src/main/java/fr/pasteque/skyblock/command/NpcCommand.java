package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.npc.NpcManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * /npc create <id> <name> <action>
 * /npc remove <id>
 * /npc list
 *
 * action: "command:<cmd>" | "console:<cmd>" | "msg:<text>"
 * Exemple: /npc create banquier &6&lBanquier command:money
 */
public class NpcCommand implements CommandExecutor {

    private final PastequeSkyblockPlugin plugin;
    private final NpcManager npcManager;

    public NpcCommand(PastequeSkyblockPlugin plugin, NpcManager npcManager) {
        this.plugin = plugin;
        this.npcManager = npcManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pastequeskyblock.admin") && !sender.isOp()) {
            sender.sendMessage(PastequeSkyblockPlugin.color("&cPermission requise."));
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("create")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(PastequeSkyblockPlugin.color("&cCommande joueur uniquement."));
                return true;
            }
            if (args.length < 4) {
                sender.sendMessage(PastequeSkyblockPlugin.color("&cUsage: /npc create <id> <nom> <action>"));
                sender.sendMessage(PastequeSkyblockPlugin.color("&7Exemple: /npc create banquier &6Banquier command:money"));
                return true;
            }
            Player p = (Player) sender;
            String id = args[1].toLowerCase();
            // Name is args[2], action is the rest joined
            String name = args[2];
            StringBuilder actionB = new StringBuilder();
            for (int i = 3; i < args.length; i++) {
                if (actionB.length() > 0) actionB.append(' ');
                actionB.append(args[i]);
            }
            String action = actionB.toString();
            if (npcManager.createNpc(id, p.getLocation(), name, action)) {
                p.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                        + "&aNPC &e" + id + " &acree a votre position. &7Action: &f" + action));
            } else {
                p.sendMessage(PastequeSkyblockPlugin.color("&cUn NPC avec cet ID existe deja."));
            }
            return true;
        }
        if (sub.equals("remove") || sub.equals("delete")) {
            if (args.length < 2) {
                sender.sendMessage(PastequeSkyblockPlugin.color("&cUsage: /npc remove <id>"));
                return true;
            }
            if (npcManager.removeNpc(args[1].toLowerCase())) {
                sender.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&aNPC supprime."));
            } else {
                sender.sendMessage(PastequeSkyblockPlugin.color("&cNPC introuvable."));
            }
            return true;
        }
        if (sub.equals("list")) {
            sender.sendMessage(PastequeSkyblockPlugin.color("&6&l== NPCs &8(" + npcManager.getAll().size() + ") =="));
            for (Map.Entry<String, NpcManager.NpcData> e : npcManager.getAll().entrySet()) {
                NpcManager.NpcData d = e.getValue();
                sender.sendMessage(PastequeSkyblockPlugin.color(
                        "&8- &e" + d.id + " &7[&f" + d.location.getWorld().getName()
                                + " " + d.location.getBlockX() + "/" + d.location.getBlockY() + "/" + d.location.getBlockZ()
                                + "&7] &8-> &f" + d.action));
            }
            return true;
        }
        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage(PastequeSkyblockPlugin.color("&6&l== /npc =="));
        s.sendMessage(PastequeSkyblockPlugin.color("&e/npc create <id> <nom> <action> &8- &7Creer un NPC"));
        s.sendMessage(PastequeSkyblockPlugin.color("&e/npc remove <id> &8- &7Supprimer un NPC"));
        s.sendMessage(PastequeSkyblockPlugin.color("&e/npc list &8- &7Lister les NPCs"));
        s.sendMessage(PastequeSkyblockPlugin.color("&7Actions: &fcommand:<cmd>&7, &fconsole:<cmd>&7, &fmsg:<texte>"));
    }
}
