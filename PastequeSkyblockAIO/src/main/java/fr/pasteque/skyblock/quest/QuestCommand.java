package fr.pasteque.skyblock.quest;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.gui.GuiHelper;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class QuestCommand implements CommandExecutor {

    private final PastequeSkyblockPlugin plugin;
    private final QuestManager manager;

    public QuestCommand(PastequeSkyblockPlugin plugin, QuestManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Admin sub-commands
        if (args.length >= 1 && args[0].equalsIgnoreCase("admin")) {
            if (!sender.hasPermission("pastequeskyblock.admin")) {
                sender.sendMessage(PastequeSkyblockPlugin.color(
                        "&2&lPasteque &5&lMissions &8\u00bb &cVous n'avez pas la permission."));
                return true;
            }

            if (args.length >= 2 && args[1].equalsIgnoreCase("reload")) {
                manager.reload();
                sender.sendMessage(PastequeSkyblockPlugin.color(
                        "&2&lPasteque &5&lMissions &8\u00bb &aQuetes rechargees avec succes ! &7("
                                + manager.getQuests().size() + " quete(s))"));
                return true;
            }

            if (args.length >= 4 && args[1].equalsIgnoreCase("give")) {
                String targetName = args[2];
                String questId = args[3];

                Player target = Bukkit.getPlayerExact(targetName);
                if (target == null) {
                    sender.sendMessage(PastequeSkyblockPlugin.color(
                            "&2&lPasteque &5&lMissions &8\u00bb &cJoueur introuvable: " + targetName));
                    return true;
                }

                Quest quest = manager.getQuest(questId);
                if (quest == null) {
                    sender.sendMessage(PastequeSkyblockPlugin.color(
                            "&2&lPasteque &5&lMissions &8\u00bb &cQuete introuvable: " + questId));
                    return true;
                }

                manager.assignQuest(target.getUniqueId(), questId);
                sender.sendMessage(PastequeSkyblockPlugin.color(
                        "&2&lPasteque &5&lMissions &8\u00bb &aQuete &e" + quest.getName()
                                + " &aassignee a &e" + target.getName() + "&a."));
                target.sendMessage(PastequeSkyblockPlugin.color(
                        "&2&lPasteque &5&lMissions &8\u00bb &aNouvelle mission: " + quest.getName()
                                + " &7- Utilisez &e/quest &7pour la consulter."));
                GuiHelper.playSuccess(target);
                return true;
            }

            // Admin usage
            sender.sendMessage(PastequeSkyblockPlugin.color(
                    "&2&lPasteque &5&lMissions &8\u00bb &7Utilisation:"));
            sender.sendMessage(PastequeSkyblockPlugin.color(
                    "&e/quest admin reload &7- Recharger les quetes"));
            sender.sendMessage(PastequeSkyblockPlugin.color(
                    "&e/quest admin give <joueur> <questId> &7- Assigner une quete"));
            return true;
        }

        // Player: open quest GUI
        if (!(sender instanceof Player)) {
            sender.sendMessage(PastequeSkyblockPlugin.color(
                    "&2&lPasteque &5&lMissions &8\u00bb &cCommande joueur uniquement."));
            return true;
        }

        Player player = (Player) sender;
        manager.openQuestGui(player);
        return true;
    }
}
