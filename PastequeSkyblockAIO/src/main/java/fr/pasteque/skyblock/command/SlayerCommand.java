package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.slayer.SlayerManager;
import fr.pasteque.skyblock.slayer.model.SlayerType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SlayerCommand implements CommandExecutor {

    private final PastequeSkyblockPlugin plugin;
    private final SlayerManager manager;

    public SlayerCommand(PastequeSkyblockPlugin plugin, SlayerManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PastequeSkyblockPlugin.color("&2&lPasteque &8\u00bb &cCommande joueur uniquement."));
            return true;
        }

        Player player = (Player) sender;

        if (args.length >= 1) {
            // Try to parse slayer type
            SlayerType type = SlayerType.fromName(args[0]);
            if (type != null) {
                manager.openTierGui(player, type);
                return true;
            }

            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                    + "&cSlayer inconnu. Types: &eZombie, Spider, Wolf, Enderman, Blaze"));
            return true;
        }

        manager.openSlayerGui(player);
        return true;
    }
}
