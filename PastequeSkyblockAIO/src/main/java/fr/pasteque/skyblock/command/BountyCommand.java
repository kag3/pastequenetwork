package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.arena.BountyService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BountyCommand implements CommandExecutor {

    private final PastequeSkyblockPlugin plugin;
    private final BountyService bountyService;

    public BountyCommand(PastequeSkyblockPlugin plugin, BountyService bountyService) {
        this.plugin = plugin;
        this.bountyService = bountyService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&fCommande reservee aux joueurs."));
            return true;
        }
        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&fUtilisation :"));
            player.sendMessage(PastequeSkyblockPlugin.color("  &d/bounty <joueur> <montant> &f- Placer une prime"));
            player.sendMessage(PastequeSkyblockPlugin.color("  &d/bounty list &f- Voir les primes"));
            player.sendMessage(PastequeSkyblockPlugin.color("  &d/bounty check [joueur] &f- Verifier une prime"));
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            showTopBounties(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("check")) {
            if (args.length >= 2) {
                checkBounty(player, args[1]);
            } else {
                checkBounty(player, player.getName());
            }
            return true;
        }

        // /bounty <player> <amount>
        if (args.length < 2) {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&fUtilisation : &d/bounty <joueur> <montant>"));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cMontant invalide."));
            return true;
        }

        bountyService.placeBounty(player, args[0], amount);
        return true;
    }

    private void showTopBounties(Player player) {
        List<Map.Entry<UUID, Double>> top = bountyService.getTopBounties(10);
        player.sendMessage(PastequeSkyblockPlugin.color("&5&l&m--------&r &5&lPrimes les plus recherchees &5&l&m--------"));
        if (top.isEmpty()) {
            player.sendMessage(PastequeSkyblockPlugin.color("  &7Aucune prime active."));
        } else {
            int rank = 1;
            for (Map.Entry<UUID, Double> entry : top) {
                Player target = Bukkit.getPlayer(entry.getKey());
                String name = target != null ? target.getName() : entry.getKey().toString().substring(0, 8);
                player.sendMessage(PastequeSkyblockPlugin.color("  &d#" + rank + " &f" + name + " &7- &d" + (int) entry.getValue().doubleValue() + " " + plugin.getEconomyManager().getCurrencyName()));
                rank++;
            }
        }
        player.sendMessage(PastequeSkyblockPlugin.color("&5&l&m-----------------------------------------"));
    }

    @SuppressWarnings("deprecation")
    private void checkBounty(Player player, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cJoueur introuvable."));
            return;
        }
        double bounty = bountyService.getBounty(target.getUniqueId());
        if (bounty <= 0) {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&fAucune prime sur &d" + target.getName() + "&f."));
        } else {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&fPrime sur &d" + target.getName() + " &f: &c" + (int) bounty + " " + plugin.getEconomyManager().getCurrencyName()));
        }
    }
}
