package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.collection.CollectionManager;
import fr.pasteque.skyblock.collection.model.CollectionCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CollectionCommand implements CommandExecutor {

    private final CollectionManager collectionManager;

    public CollectionCommand(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PastequeSkyblockPlugin.color("&cCommande joueur uniquement."));
            return true;
        }
        Player player = (Player) sender;

        if (args.length >= 1) {
            // Try to open specific category
            String catName = args[0].toUpperCase();
            for (CollectionCategory category : CollectionCategory.values()) {
                if (category.name().equalsIgnoreCase(catName)
                        || category.getDisplayName().equalsIgnoreCase(args[0])) {
                    collectionManager.openCategoryGui(player, category);
                    return true;
                }
            }
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "&cCategorie inconnue. Utilisez: farming, mining, combat, fishing, foraging"
            ));
            return true;
        }

        collectionManager.openCollectionGui(player);
        return true;
    }
}
