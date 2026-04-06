package fr.pasteque.skyblock.farming;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.gui.GuiHelper;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class FarmingCommand implements CommandExecutor {

    private final PastequeSkyblockPlugin plugin;
    private final FarmingManager manager;

    public FarmingCommand(PastequeSkyblockPlugin plugin, FarmingManager manager) {
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

        if (args.length == 0) {
            manager.openFarmingGui(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("sell")) {
            manager.sellCrops(player);
            return true;
        }

        if (sub.equals("level")) {
            showLevel(player);
            return true;
        }

        if (sub.equals("seeds")) {
            manager.openSeedShop(player);
            return true;
        }

        if (sub.equals("admin")) {
            handleAdmin(player, args);
            return true;
        }

        // Unknown subcommand
        sendHelp(player);
        return true;
    }

    private void showLevel(Player player) {
        int level = manager.getFarmingLevel(player.getUniqueId());
        int xp = manager.getFarmingXp(player.getUniqueId());
        int nextXp = manager.getXpForNextLevel(level);

        player.sendMessage(PastequeSkyblockPlugin.color("&2&lPasteque &5&lFarming &8\u00bb &aVotre profil:"));
        player.sendMessage(PastequeSkyblockPlugin.color("  &7Niveau: &a" + level + " / 10"));
        player.sendMessage(PastequeSkyblockPlugin.color("  &7XP: &e" + xp));
        if (nextXp == -1) {
            player.sendMessage(PastequeSkyblockPlugin.color("  &6Vous avez atteint le niveau maximum!"));
        } else {
            player.sendMessage(PastequeSkyblockPlugin.color("  &7Prochain niveau: &e" + nextXp + " XP"));
            int remaining = nextXp - xp;
            if (remaining > 0) {
                player.sendMessage(PastequeSkyblockPlugin.color("  &7Restant: &e" + remaining + " XP"));
            }
        }
    }

    private void handleAdmin(Player player, String[] args) {
        if (!player.hasPermission("pasteque.farming.admin")) {
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "&2&lPasteque &8\u00bb &cVous n'avez pas la permission."));
            return;
        }

        // /farming admin give <player> <crop> <amount>
        if (args.length < 5 || !args[1].equalsIgnoreCase("give")) {
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "&2&lPasteque &8\u00bb &cUsage: &e/farming admin give <joueur> <crop> <quantite>"));
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "&7Crops disponibles:"));
            for (CustomCrop crop : CustomCrop.values()) {
                player.sendMessage(PastequeSkyblockPlugin.color(
                        "  &8\u25B8 &e" + crop.name()));
            }
            return;
        }

        String targetName = args[2];
        String cropName = args[3].toUpperCase();
        int amount;

        try {
            amount = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "&2&lPasteque &8\u00bb &cQuantite invalide: &e" + args[4]));
            return;
        }

        if (amount <= 0 || amount > 64) {
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "&2&lPasteque &8\u00bb &cLa quantite doit etre entre 1 et 64."));
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "&2&lPasteque &8\u00bb &cJoueur &e" + targetName + " &cnon trouve."));
            return;
        }

        CustomCrop crop;
        try {
            crop = CustomCrop.valueOf(cropName);
        } catch (IllegalArgumentException e) {
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "&2&lPasteque &8\u00bb &cCrop inconnue: &e" + cropName));
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "&7Crops disponibles:"));
            for (CustomCrop c : CustomCrop.values()) {
                player.sendMessage(PastequeSkyblockPlugin.color(
                        "  &8\u25B8 &e" + c.name()));
            }
            return;
        }

        ItemStack harvest = manager.createHarvestItem(crop, amount);
        target.getInventory().addItem(harvest);

        player.sendMessage(PastequeSkyblockPlugin.color(
                "&2&lPasteque &8\u00bb &aDonne &6" + amount + "x " + crop.getColoredName()
                        + " &aa &e" + target.getName() + "&a."));
        target.sendMessage(PastequeSkyblockPlugin.color(
                "&2&lPasteque &8\u00bb &aVous avez recu &6" + amount + "x " + crop.getColoredName() + "&a!"));
        GuiHelper.playSuccess(target);
    }

    private void sendHelp(Player player) {
        player.sendMessage(PastequeSkyblockPlugin.color("&2&lPasteque &5&lFarming &8\u00bb &7Commandes:"));
        player.sendMessage(PastequeSkyblockPlugin.color("  &e/farming &8- &7Ouvre le menu farming"));
        player.sendMessage(PastequeSkyblockPlugin.color("  &e/farming sell &8- &7Vend vos recoltes"));
        player.sendMessage(PastequeSkyblockPlugin.color("  &e/farming level &8- &7Voir votre niveau"));
        player.sendMessage(PastequeSkyblockPlugin.color("  &e/farming seeds &8- &7Boutique de graines"));
        if (player.hasPermission("pasteque.farming.admin")) {
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "  &e/farming admin give <joueur> <crop> <qte> &8- &7Donne des recoltes"));
        }
    }
}
