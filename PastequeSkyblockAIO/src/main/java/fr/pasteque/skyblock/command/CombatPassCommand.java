package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.combatpass.CombatPassManager;
import fr.pasteque.skyblock.combatpass.model.PlayerPassData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CombatPassCommand implements CommandExecutor {

    private final PastequeSkyblockPlugin plugin;
    private final CombatPassManager manager;

    public CombatPassCommand(PastequeSkyblockPlugin plugin, CombatPassManager manager) {
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

        if (args.length > 0 && args[0].equalsIgnoreCase("info")) {
            PlayerPassData data = manager.getData(player.getUniqueId());
            player.sendMessage(PastequeSkyblockPlugin.color("&d&lPasse de Combat &7» &7Ton XP: &e" + data.getXp()));
            player.sendMessage(PastequeSkyblockPlugin.color("&d&lPasse de Combat &7» &7Ton palier: &e" + data.getCurrentTier() + "&7/30"));
            if (data.getCurrentTier() < 30) {
                int nextXp = (data.getCurrentTier() + 1) * 500;
                player.sendMessage(PastequeSkyblockPlugin.color("&d&lPasse de Combat &7» &7Prochain palier: &e" + nextXp + " XP &7(&e" + (nextXp - data.getXp()) + " &7restants)"));
            } else {
                player.sendMessage(PastequeSkyblockPlugin.color("&d&lPasse de Combat &7» &a&lTu as atteint le palier maximum !"));
            }
            player.sendMessage(PastequeSkyblockPlugin.color("&d&lPasse de Combat &7» &7Statut: " + (data.isPremium() ? "&6Premium" : "&7Gratuit")));
            return true;
        }

        manager.openPassGui(player);
        return true;
    }
}
