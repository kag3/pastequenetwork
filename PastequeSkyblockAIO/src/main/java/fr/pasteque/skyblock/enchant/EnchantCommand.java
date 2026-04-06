package fr.pasteque.skyblock.enchant;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class EnchantCommand implements CommandExecutor {

    private static final String PREFIX = "&2&lPasteque &8\u00bb ";

    private final PastequeSkyblockPlugin plugin;
    private final EnchantManager manager;

    public EnchantCommand(PastequeSkyblockPlugin plugin, EnchantManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // /enchant — open GUI
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(PastequeSkyblockPlugin.color(PREFIX + "&cCommande joueur uniquement."));
                return true;
            }
            manager.openEnchantTable((Player) sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        // /enchant list
        if (sub.equals("list")) {
            sender.sendMessage(PastequeSkyblockPlugin.color("&8&m─────────────────────────────────"));
            sender.sendMessage(PastequeSkyblockPlugin.color("&2&lPasteque &5&lEnchantements"));
            sender.sendMessage(PastequeSkyblockPlugin.color("&8&m─────────────────────────────────"));
            for (CustomEnchant ce : CustomEnchant.values()) {
                StringBuilder sb = new StringBuilder();
                sb.append(ce.getColor()).append("&l").append(ce.getDisplayName());
                sb.append(" &8- &7Niveau max: &f").append(ce.getMaxLevel());
                sb.append(" &8| &7Cout: &e").append(ce.getBaseCost()).append(" Pasteques");
                sender.sendMessage(PastequeSkyblockPlugin.color(sb.toString()));
                for (String desc : ce.getDescription()) {
                    sender.sendMessage(PastequeSkyblockPlugin.color("  " + desc));
                }
            }
            sender.sendMessage(PastequeSkyblockPlugin.color("&8&m─────────────────────────────────"));
            return true;
        }

        // /enchant give <player> <enchant> <level>
        if (sub.equals("give")) {
            if (!sender.hasPermission("pastequeskyblock.admin")) {
                sender.sendMessage(PastequeSkyblockPlugin.color(PREFIX + "&cPermission insuffisante."));
                return true;
            }
            if (args.length < 4) {
                sender.sendMessage(PastequeSkyblockPlugin.color(PREFIX + "&7Usage: /enchant give <joueur> <enchant> <niveau>"));
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(PastequeSkyblockPlugin.color(PREFIX + "&cJoueur introuvable."));
                return true;
            }

            CustomEnchant enchant = CustomEnchant.fromName(args[2]);
            if (enchant == null) {
                sender.sendMessage(PastequeSkyblockPlugin.color(PREFIX + "&cEnchantement inconnu: &f" + args[2]));
                sender.sendMessage(PastequeSkyblockPlugin.color(PREFIX + "&7Utilisez &f/enchant list &7pour voir la liste."));
                return true;
            }

            int level;
            try {
                level = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(PastequeSkyblockPlugin.color(PREFIX + "&cNiveau invalide."));
                return true;
            }

            if (level < 1 || level > enchant.getMaxLevel()) {
                sender.sendMessage(PastequeSkyblockPlugin.color(PREFIX + "&cNiveau invalide (1-" + enchant.getMaxLevel() + ")."));
                return true;
            }

            ItemStack hand = target.getItemInHand();
            if (hand == null || hand.getType() == org.bukkit.Material.AIR) {
                sender.sendMessage(PastequeSkyblockPlugin.color(PREFIX + "&c" + target.getName() + " n'a rien en main."));
                return true;
            }

            enchant.apply(hand, level);
            // Also unlock for the target
            manager.unlockEnchant(target.getUniqueId(), enchant);
            manager.save();

            sender.sendMessage(PastequeSkyblockPlugin.color(PREFIX + "&aEnchantement " + enchant.getColor() + enchant.getDisplayName() + " " + CustomEnchant.toRoman(level) + " &aapplique sur l'item de &f" + target.getName() + "&a."));
            MessageUtil.send(target, PastequeSkyblockPlugin.color(PREFIX), "&aVous avez recu l'enchantement " + enchant.getColor() + enchant.getDisplayName() + " " + CustomEnchant.toRoman(level) + "&a!");
            return true;
        }

        // Unknown subcommand
        sender.sendMessage(PastequeSkyblockPlugin.color(PREFIX + "&7Commandes:"));
        sender.sendMessage(PastequeSkyblockPlugin.color("  &f/enchant &8- &7Ouvrir la table d'enchantement"));
        sender.sendMessage(PastequeSkyblockPlugin.color("  &f/enchant list &8- &7Lister les enchantements"));
        sender.sendMessage(PastequeSkyblockPlugin.color("  &f/enchant give <joueur> <enchant> <niveau> &8- &7Admin"));
        return true;
    }
}
