package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.manager.ConfirmationManager;
import fr.pasteque.skyblock.model.Island;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IslandCommand implements CommandExecutor {
    private final PastequeSkyblockPlugin plugin;

    public IslandCommand(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Commande joueur uniquement.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            openMenuPremium(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("create")) {
            if (plugin.getIslandManager().hasIsland(player.getUniqueId())) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu as déjà une île.");
                return true;
            }
            Island island = plugin.getIslandManager().createIsland(player);
            MessageUtil.send(player, plugin.getPrefix(), "&aTon île vient d'être créée. &7Nom par défaut: &f" + island.getDisplayName(player.getName()));
            player.teleport(plugin.getIslandManager().getSafeTeleport(island));
            plugin.getBorderManager().renderFor(player);
            return true;
        }

        if (sub.equals("home")) {
            Island island = plugin.getIslandManager().getOwnedIsland(player.getUniqueId());
            if (island == null) {
                island = plugin.getIslandManager().getIslandByPlayer(player.getUniqueId());
            }
            if (island == null) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu n'as pas d'île.");
                return true;
            }
            player.teleport(plugin.getIslandManager().getSafeTeleport(island));
            MessageUtil.send(player, plugin.getPrefix(), "&aTéléportation vers ton île.");
            plugin.getBorderManager().renderFor(player);
            return true;
        }

        if (sub.equals("farm") || sub.equals("farming")) {
            Island island = plugin.getIslandManager().getIslandByPlayer(player.getUniqueId());
            if (island == null) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu n'as pas d'ile.");
                return true;
            }
            if (!island.isFarmingUnlocked() || island.getFarmingHome() == null) {
                MessageUtil.send(player, plugin.getPrefix(), "&fIle farming verrouillee. Utilise &e/is unlock farming&f.");
                return true;
            }
            player.teleport(island.getFarmingHome());
            MessageUtil.send(player, plugin.getPrefix(), "&aTeleportation vers ton ile farming.");
            return true;
        }

        if (sub.equals("mine") || sub.equals("mining") || sub.equals("minage")) {
            Island island = plugin.getIslandManager().getIslandByPlayer(player.getUniqueId());
            if (island == null) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu n'as pas d'ile.");
                return true;
            }
            if (!island.isMiningUnlocked() || island.getMiningHome() == null) {
                MessageUtil.send(player, plugin.getPrefix(), "&fIle minage verrouillee. Utilise &e/is unlock mining&f.");
                return true;
            }
            player.teleport(island.getMiningHome());
            MessageUtil.send(player, plugin.getPrefix(), "&aTeleportation vers ton ile de minage.");
            return true;
        }

        if (sub.equals("sethome")) {
            Island island = plugin.getIslandManager().getOwnedIsland(player.getUniqueId());
            if (island == null) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu n'es pas propriétaire d'une île.");
                return true;
            }
            if (!island.isInside(player.getLocation(), plugin.getConfig().getInt("island.size", 320))) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu dois être sur ton île.");
                return true;
            }
            plugin.getIslandManager().setHome(island, player.getLocation());
            MessageUtil.send(player, plugin.getPrefix(), "&aHome d'île défini.");
            return true;
        }

        if (sub.equals("rename")) {
            Island island = plugin.getIslandManager().getOwnedIsland(player.getUniqueId());
            if (island == null) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu dois être propriétaire de l'île.");
                return true;
            }
            if (args.length < 2) {
                MessageUtil.send(player, plugin.getPrefix(), "&7Usage: /is rename <nom>");
                return true;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                if (i > 1) sb.append(' ');
                sb.append(args[i]);
            }
            String newName = sb.toString().trim();
            if (newName.length() < 3 || newName.length() > 24) {
                MessageUtil.send(player, plugin.getPrefix(), "&fLe nom doit contenir entre 3 et 24 caractères.");
                return true;
            }
            plugin.getIslandManager().rename(island, newName);
            MessageUtil.send(player, plugin.getPrefix(), "&aNom d'île mis à jour : &f" + newName);
            return true;
        }

        if (sub.equals("members")) {
            Island island = plugin.getIslandManager().getOwnedIsland(player.getUniqueId());
            if (island == null) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu dois être propriétaire de l'île.");
                return true;
            }
            openMembers(player, island);
            return true;
        }

        if (sub.equals("unlock")) {
            Island island = plugin.getIslandManager().getOwnedIsland(player.getUniqueId());
            if (island == null) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu dois être propriétaire de l'île.");
                return true;
            }
            if (args.length < 2) {
                MessageUtil.send(player, plugin.getPrefix(), "&7Usage: /is unlock <farming|mining>");
                return true;
            }
            if (args[1].equalsIgnoreCase("farming")) {
                int price = plugin.getConfig().getInt("island-expansions.solo.farming-price", 10000);
                if (!plugin.getIslandManager().unlockFarmingIsland(island)) {
                    MessageUtil.send(player, plugin.getPrefix(), "&fDéblocage impossible. Prix : &e" + price + " Pasteque&f ou déjà débloquée.");
                    return true;
                }
                MessageUtil.send(player, plugin.getPrefix(), "&aÎle Farming débloquée pour &e" + price + " Pasteque&a.");
                return true;
            }
            if (args[1].equalsIgnoreCase("mining") || args[1].equalsIgnoreCase("mine")) {
                int price = plugin.getConfig().getInt("island-expansions.solo.mining-price", 10000);
                if (!plugin.getIslandManager().unlockMiningIsland(island)) {
                    MessageUtil.send(player, plugin.getPrefix(), "&fDéblocage impossible. Prix : &e" + price + " Pasteque&f ou déjà débloquée.");
                    return true;
                }
                MessageUtil.send(player, plugin.getPrefix(), "&aÎle de minage débloquée pour &e" + price + " Pasteque&a.");
                return true;
            }
            MessageUtil.send(player, plugin.getPrefix(), "&7Usage: /is unlock <farming|mining>");
            return true;
        }

        if (sub.equals("invite")) {
            Island island = plugin.getIslandManager().getOwnedIsland(player.getUniqueId());
            if (island == null) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu dois être propriétaire de l'île.");
                return true;
            }
            if (args.length < 2) {
                MessageUtil.send(player, plugin.getPrefix(), "&7Usage: /is invite <joueur>");
                MessageUtil.send(player, plugin.getPrefix(), "&7Tu dois d'abord débloquer une invitation via l'ambassade.");
                return true;
            }
            if (island.getInviteUnlocks() <= 0) {
                MessageUtil.send(player, plugin.getPrefix(), "&f/is invite est verrouillé tant qu'aucune ambassade n'a été activée.");
                return true;
            }
            if (island.getMembers().size() >= plugin.getIslandManager().getMaxMembers()) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu as atteint la limite de &e" + plugin.getIslandManager().getMaxMembers() + " &fmembres.");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                MessageUtil.send(player, plugin.getPrefix(), "&fJoueur introuvable.");
                return true;
            }
            if (plugin.getIslandManager().invite(player, target)) {
                MessageUtil.send(player, plugin.getPrefix(), "&aInvitation envoyée à &f" + target.getName() + "&a. Invitations restantes: &e" + island.getInviteUnlocks());
                MessageUtil.send(target, plugin.getPrefix(), "&a" + player.getName() + " &7t'a invité sur son île. &f/is accept &7ou &f/is deny");
            } else {
                MessageUtil.send(player, plugin.getPrefix(), "&fImpossible d'envoyer l'invitation.");
            }
            return true;
        }

        if (sub.equals("accept")) {
            Island island = plugin.getIslandManager().acceptInvite(player);
            if (island == null) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu n'as aucune invitation valide.");
                return true;
            }
            MessageUtil.send(player, plugin.getPrefix(), "&aInvitation acceptée.");
            player.teleport(plugin.getIslandManager().getSafeTeleport(island));
            return true;
        }

        if (sub.equals("deny")) {
            if (plugin.getIslandManager().denyInvite(player)) {
                MessageUtil.send(player, plugin.getPrefix(), "&aInvitation refusée.");
            } else {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu n'as aucune invitation.");
            }
            return true;
        }

        if (sub.equals("kick") || sub.equals("remove")) {
            if (args.length < 2) {
                MessageUtil.send(player, plugin.getPrefix(), "&7Usage: /is kick <joueur>");
                return true;
            }
            Island island = plugin.getIslandManager().getOwnedIsland(player.getUniqueId());
            if (island == null) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu dois être propriétaire de l'île.");
                return true;
            }
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[1]);
            if (plugin.getIslandManager().removeMember(island, offlinePlayer.getUniqueId())) {
                MessageUtil.send(player, plugin.getPrefix(), "&aJoueur retiré de l'île.");
            } else {
                MessageUtil.send(player, plugin.getPrefix(), "&fCe joueur n'est pas membre.");
            }
            return true;
        }

        if (sub.equals("trust") || sub.equals("coop")) {
            if (args.length < 2) {
                MessageUtil.send(player, plugin.getPrefix(), "&7Usage: /is trust <joueur>");
                return true;
            }
            Island island = plugin.getIslandManager().getOwnedIsland(player.getUniqueId());
            if (island == null) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu dois être propriétaire de l'île.");
                return true;
            }
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[1]);
            island.getTrusted().add(offlinePlayer.getUniqueId());
            island.getBanned().remove(offlinePlayer.getUniqueId());
            plugin.getIslandManager().save();
            MessageUtil.send(player, plugin.getPrefix(), "&aJoueur ajouté aux visiteurs de confiance.");
            return true;
        }

        if (sub.equals("untrust")) {
            if (args.length < 2) {
                MessageUtil.send(player, plugin.getPrefix(), "&7Usage: /is untrust <joueur>");
                return true;
            }
            Island island = plugin.getIslandManager().getOwnedIsland(player.getUniqueId());
            if (island == null) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu dois être propriétaire de l'île.");
                return true;
            }
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[1]);
            island.getTrusted().remove(offlinePlayer.getUniqueId());
            plugin.getIslandManager().save();
            MessageUtil.send(player, plugin.getPrefix(), "&aJoueur retiré des visiteurs de confiance.");
            return true;
        }

        if (sub.equals("ban") || sub.equals("unban")) {
            if (args.length < 2) {
                MessageUtil.send(player, plugin.getPrefix(), "&7Usage: /is " + sub + " <joueur>");
                return true;
            }
            Island island = plugin.getIslandManager().getOwnedIsland(player.getUniqueId());
            if (island == null) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu dois être propriétaire de l'île.");
                return true;
            }
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[1]);
            if (sub.equals("ban")) {
                island.getBanned().add(offlinePlayer.getUniqueId());
                island.getTrusted().remove(offlinePlayer.getUniqueId());
                island.getMembers().remove(offlinePlayer.getUniqueId());
                MessageUtil.send(player, plugin.getPrefix(), "&aJoueur banni de l'île.");
            } else {
                island.getBanned().remove(offlinePlayer.getUniqueId());
                MessageUtil.send(player, plugin.getPrefix(), "&aJoueur débanni de l'île.");
            }
            plugin.getIslandManager().save();
            return true;
        }

        if (sub.equals("pvp")) {
            Island island = plugin.getIslandManager().getOwnedIsland(player.getUniqueId());
            if (island == null) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu dois être propriétaire de l'île.");
                return true;
            }
            if (args.length == 1) {
                plugin.getIslandManager().setPvp(island, !island.isPvpEnabled());
                MessageUtil.send(player, plugin.getPrefix(), "&7PvP de l'île: " + MessageUtil.bool(island.isPvpEnabled()));
                return true;
            }
            boolean enable = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("enable");
            plugin.getIslandManager().setPvp(island, enable);
            MessageUtil.send(player, plugin.getPrefix(), "&7PvP de l'île: " + MessageUtil.bool(island.isPvpEnabled()));
            return true;
        }

        if (sub.equals("pvpwarp")) {
            if (plugin.getPvpManager().getHardcoreWarp() == null) {
                MessageUtil.send(player, plugin.getPrefix(), "&fAucun warp PvP hardcore n'est configuré.");
                return true;
            }
            player.teleport(plugin.getPvpManager().getHardcoreWarp());
            MessageUtil.send(player, plugin.getPrefix(), "&aTéléportation au warp PvP hardcore.");
            return true;
        }

        if (sub.equals("visit")) {
            if (args.length < 2) {
                MessageUtil.send(player, plugin.getPrefix(), "&7Usage: /is visit <joueur>");
                return true;
            }
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[1]);
            Island island = plugin.getIslandManager().getOwnedIsland(offlinePlayer.getUniqueId());
            if (island == null) {
                MessageUtil.send(player, plugin.getPrefix(), "&fCette île n'existe pas.");
                return true;
            }
            if (!plugin.getIslandManager().canVisit(player, island) && !player.hasPermission("pastequeskyblock.bypass")) {
                MessageUtil.send(player, plugin.getPrefix(), "&fCette île est privée.");
                return true;
            }
            player.teleport(plugin.getIslandManager().getSafeTeleport(island));
            MessageUtil.send(player, plugin.getPrefix(), "&aTéléportation vers l'île de &f" + args[1]);
            plugin.getBorderManager().renderFor(player);
            return true;
        }

        if (sub.equals("public") || sub.equals("private")) {
            Island island = plugin.getIslandManager().getOwnedIsland(player.getUniqueId());
            if (island == null) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu dois être propriétaire de l'île.");
                return true;
            }
            island.setPublicVisit(sub.equals("public"));
            plugin.getIslandManager().save();
            MessageUtil.send(player, plugin.getPrefix(), sub.equals("public") ? "&aTon île est maintenant publique." : "&aTon île est maintenant privée.");
            return true;
        }

        if (sub.equals("level")) {
            Island island = plugin.getIslandManager().getIslandByPlayer(player.getUniqueId());
            if (island == null) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu n'as pas d'île.");
                return true;
            }
            int level = plugin.getIslandManager().getLevel(island);
            MessageUtil.send(player, plugin.getPrefix(), "&7Niveau de l'île : &e" + level);
            return true;
        }

        if (sub.equals("stats")) {
            Island island = plugin.getIslandManager().getIslandByPlayer(player.getUniqueId());
            if (island == null) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu n'as pas d'ile.");
                return true;
            }
            int level = plugin.getIslandManager().getLevel(island);
            String ownerName = Bukkit.getOfflinePlayer(island.getOwner()).getName();
            MessageUtil.send(player, plugin.getPrefix(), "&d== Profil ile ==");
            MessageUtil.send(player, plugin.getPrefix(), "&7Nom: &f" + island.getDisplayName(ownerName));
            MessageUtil.send(player, plugin.getPrefix(), "&7Owner: &f" + (ownerName == null ? island.getOwner().toString() : ownerName));
            MessageUtil.send(player, plugin.getPrefix(), "&7Membres: &e" + (1 + island.getMembers().size()) + "&7/&e" + plugin.getIslandManager().getMaxMembers());
            MessageUtil.send(player, plugin.getPrefix(), "&7Niveau: &e" + level);
            MessageUtil.send(player, plugin.getPrefix(), "&7Banque: &e" + island.getBankBalance() + " " + plugin.getEconomyManager().getCurrencyName());
            MessageUtil.send(player, plugin.getPrefix(), "&7Farming: " + (island.isFarmingUnlocked() ? "&aDebloquee" : "&cVerrouillee") + " &8| &7Minage: " + (island.isMiningUnlocked() ? "&aDebloquee" : "&cVerrouillee"));
            return true;
        }

        if (sub.equals("top")) {
            List<Island> islands = new ArrayList<Island>(plugin.getIslandManager().getIslands());
            final Map<UUID, Integer> levels = new HashMap<UUID, Integer>();
            for (Island island : islands) {
                levels.put(island.getOwner(), plugin.getIslandManager().getLevel(island));
            }
            Collections.sort(islands, new java.util.Comparator<Island>() {
                @Override public int compare(Island a, Island b) {
                    return Integer.compare(levels.get(b.getOwner()), levels.get(a.getOwner()));
                }
            });
            MessageUtil.send(player, plugin.getPrefix(), "&6Top iles Skyblock");
            int max = Math.min(10, islands.size());
            for (int i = 0; i < max; i++) {
                Island island = islands.get(i);
                String ownerName = Bukkit.getOfflinePlayer(island.getOwner()).getName();
                int level = levels.get(island.getOwner());
                player.sendMessage(MessageUtil.color("&e#" + (i + 1) + " &f" + island.getDisplayName(ownerName) + " &7(" + (ownerName == null ? island.getOwner().toString() : ownerName) + ") &8- &dLvl " + level));
            }
            if (max == 0) {
                MessageUtil.send(player, plugin.getPrefix(), "&fAucune ile enregistree.");
            }
            return true;
        }

        if (sub.equals("bank")) {
            Island island = plugin.getIslandManager().getOwnedIsland(player.getUniqueId());
            if (island == null) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu dois être propriétaire de l'île.");
                return true;
            }
            if (args.length == 1) {
                MessageUtil.send(player, plugin.getPrefix(), "&7Banque d'île : &e" + island.getBankBalance() + " " + plugin.getEconomyManager().getCurrencyName());
                return true;
            }
            if (args.length < 3) {
                MessageUtil.send(player, plugin.getPrefix(), "&7Usage: /is bank <deposit|withdraw> <montant>");
                return true;
            }
            double amount;
            try {
                amount = Double.parseDouble(args[2]);
            } catch (Exception e) {
                MessageUtil.send(player, plugin.getPrefix(), "&fMontant invalide.");
                return true;
            }
            if (args[1].equalsIgnoreCase("deposit")) {
                if (!plugin.getEconomyManager().take(player.getUniqueId(), amount)) {
                    MessageUtil.send(player, plugin.getPrefix(), "&fTu n'as pas assez d'argent.");
                    return true;
                }
                island.setBankBalance(island.getBankBalance() + amount);
                plugin.getIslandManager().save();
                MessageUtil.send(player, plugin.getPrefix(), "&aDépôt effectué.");
                return true;
            } else if (args[1].equalsIgnoreCase("withdraw")) {
                if (island.getBankBalance() < amount) {
                    MessageUtil.send(player, plugin.getPrefix(), "&fBanque insuffisante.");
                    return true;
                }
                island.setBankBalance(island.getBankBalance() - amount);
                plugin.getEconomyManager().add(player.getUniqueId(), amount);
                plugin.getIslandManager().save();
                MessageUtil.send(player, plugin.getPrefix(), "&aRetrait effectué.");
                return true;
            }
            MessageUtil.send(player, plugin.getPrefix(), "&7Usage: /is bank <deposit|withdraw> <montant>");
            return true;
        }

        if (sub.equals("reset") || sub.equals("delete")) {
            Island island = plugin.getIslandManager().getOwnedIsland(player.getUniqueId());
            if (island == null) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTu n'as pas d'île.");
                return true;
            }
            ConfirmationManager.ActionType type = sub.equals("reset") ? ConfirmationManager.ActionType.RESET : ConfirmationManager.ActionType.DELETE;
            if (args.length >= 2 && args[1].equalsIgnoreCase("confirm")) {
                if (!plugin.getConfirmationManager().consume(player.getUniqueId(), type)) {
                    MessageUtil.send(player, plugin.getPrefix(), "&fAucune confirmation valide en attente.");
                    return true;
                }
                if (type == ConfirmationManager.ActionType.RESET) {
                    plugin.getIslandManager().resetIsland(island);
                    player.teleport(plugin.getIslandManager().getSafeTeleport(island));
                    MessageUtil.send(player, plugin.getPrefix(), "&aÎle réinitialisée. &7Tu peux utiliser &f/is rollback &7si besoin.");
                } else {
                    plugin.getIslandManager().deleteIsland(island);
                    player.teleport(plugin.getWorldManager().getServerSpawn());
                    MessageUtil.send(player, plugin.getPrefix(), "&aÎle supprimée. &7Tu peux utiliser &f/is rollback &7si besoin.");
                }
                return true;
            }
            plugin.getConfirmationManager().request(player.getUniqueId(), type);
            MessageUtil.send(player, plugin.getPrefix(), type == ConfirmationManager.ActionType.RESET ? "&6Action sensible. Refais &f/is reset confirm &6dans les 20 secondes pour confirmer." : "&5Suppression définitive demandée. Refais &f/is delete confirm &5dans les 20 secondes.");
            return true;
        }

        if (sub.equals("rollback")) {
            if (plugin.getIslandManager().rollbackLastIsland(player.getUniqueId())) {
                MessageUtil.send(player, plugin.getPrefix(), "&aRollback effectué avec succès.");
            } else {
                MessageUtil.send(player, plugin.getPrefix(), "&fAucun rollback disponible.");
            }
            return true;
        }

        if (sub.equals("chat")) {
            plugin.toggleIslandChat(player.getUniqueId());
            MessageUtil.send(player, plugin.getPrefix(), "&7Chat d'île : " + (plugin.isIslandChatEnabled(player.getUniqueId()) ? "&aactivé" : "&5désactivé"));
            return true;
        }

        if (sub.equals("challenges")) {
            openChallenges(player);
            return true;
        }

        sendHelp(player);
        return true;
    }

    private void sendHelp(Player player) {
        MessageUtil.send(player, plugin.getPrefix(), "&7/is create, home, farm, mine, sethome, rename, members, invite, accept, deny, visit");
        MessageUtil.send(player, plugin.getPrefix(), "&7/is public, private, level, stats, top, bank, trust, untrust, ban, unban, pvp, pvpwarp");
        MessageUtil.send(player, plugin.getPrefix(), "&7/is reset, delete, rollback, chat, challenges, unlock");
    }

    private void openMenuPremium(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.DARK_GREEN + "Skyblock Premium");
        Island island = plugin.getIslandManager().getIslandByPlayer(player.getUniqueId());
        int level = island == null ? 0 : plugin.getIslandManager().getLevel(island);
        int farmPrice = plugin.getConfig().getInt("island-expansions.solo.farming-price", 10000);
        int minePrice = plugin.getConfig().getInt("island-expansions.solo.mining-price", 10000);
        String farmState = island != null && island.isFarmingUnlocked() ? ChatColor.GREEN + "Debloquee" : ChatColor.RED + "Verrouillee";
        String mineState = island != null && island.isMiningUnlocked() ? ChatColor.GREEN + "Debloquee" : ChatColor.RED + "Verrouillee";
        String pvpState = island != null && island.isPvpEnabled() ? ChatColor.GREEN + "Actif" : ChatColor.RED + "Inactif";
        String bank = island == null ? "0" : String.valueOf((int) island.getBankBalance());
        ItemStack green = pane((short) 5);
        ItemStack magenta = pane((short) 2);
        for (int slot = 0; slot < 54; slot++) {
            if (slot < 9 || slot >= 45 || slot % 9 == 0 || slot % 9 == 8) {
                inventory.setItem(slot, (slot % 2 == 0) ? green : magenta);
            }
        }
        if (island == null) {
            inventory.setItem(20, item(Material.SAPLING, ChatColor.GREEN + "Creer mon ile", ChatColor.GRAY + "Commande : /is create"));
        } else {
            inventory.setItem(20, item(Material.GRASS, ChatColor.GREEN + "Retour sur mon ile", ChatColor.GRAY + "Teleportation vers ton ile principale"));
        }
        inventory.setItem(21, item(Material.BED, ChatColor.LIGHT_PURPLE + "Definir le home", ChatColor.GRAY + "Commande : /is sethome"));
        inventory.setItem(22, item(Material.BOOK, ChatColor.AQUA + "Defis Skyblock", ChatColor.GRAY + "Voir et valider tes defis"));
        inventory.setItem(23, item(Material.PAPER, ChatColor.YELLOW + "Niveau d'ile", ChatColor.GRAY + "Niveau actuel: " + ChatColor.GOLD + level));
        inventory.setItem(24, item(Material.DIAMOND_SWORD, ChatColor.DARK_PURPLE + "PvP d'ile", ChatColor.GRAY + "Etat: " + pvpState, ChatColor.GRAY + "Commande : /is pvp"));
        inventory.setItem(29, item(Material.SKULL_ITEM, ChatColor.GOLD + "Membres", ChatColor.GRAY + "Gerer les membres et permissions"));
        inventory.setItem(30, item(Material.CARROT_ITEM, ChatColor.GREEN + "Farming", ChatColor.GRAY + "Etat: " + farmState, ChatColor.GRAY + "Prix unlock: " + ChatColor.GOLD + farmPrice, ChatColor.GRAY + "Commande : /is unlock farming"));
        inventory.setItem(31, item(Material.IRON_PICKAXE, ChatColor.GRAY + "Minage", ChatColor.GRAY + "Etat: " + mineState, ChatColor.GRAY + "Prix unlock: " + ChatColor.GOLD + minePrice, ChatColor.GRAY + "Commande : /is unlock mining"));
        inventory.setItem(32, item(Material.NAME_TAG, ChatColor.LIGHT_PURPLE + "Renommer l'ile", ChatColor.GRAY + "Commande : /is rename <nom>"));
        inventory.setItem(33, item(Material.CHEST, ChatColor.GOLD + "Banque d'ile", ChatColor.GRAY + "Solde: " + ChatColor.YELLOW + bank + " " + plugin.getEconomyManager().getCurrencyName(), ChatColor.GRAY + "Commande : /is bank"));
        inventory.setItem(34, item(Material.GOLD_INGOT, ChatColor.GOLD + "Top mondial iles", ChatColor.GRAY + "Commande : /is top"));
        inventory.setItem(39, item(Material.COMPASS, ChatColor.GREEN + "Home Farming", ChatColor.GRAY + "Commande : /is farm"));
        inventory.setItem(40, item(Material.SIGN, ChatColor.WHITE + "Iles publiques / privees", ChatColor.GRAY + "Commandes : /is public ou /is private"));
        inventory.setItem(41, item(Material.DIAMOND_PICKAXE, ChatColor.GRAY + "Home Minage", ChatColor.GRAY + "Commande : /is mine"));
        inventory.setItem(42, item(Material.BOOK_AND_QUILL, ChatColor.LIGHT_PURPLE + "Profil ile", ChatColor.GRAY + "Commande : /is stats"));
        player.openInventory(inventory);
    }

    @SuppressWarnings("unused")
    private void openMenuLegacyUnused(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, "§2Skyblock Premium");
        ItemStack green = pane((short) 5);
        ItemStack magenta = pane((short) 2);
        for (int slot = 0; slot < 54; slot++) {
            if (slot < 9 || slot >= 45 || slot % 9 == 0 || slot % 9 == 8) {
                inventory.setItem(slot, (slot % 2 == 0) ? green : magenta);
            }
        }
        inventory.setItem(20, item(Material.GRASS, "§aRetour sur mon île", "§7Téléportation vers ton île principale"));
        inventory.setItem(21, item(Material.BED, "§dDéfinir le home", "§7Définit le point de retour de l'île"));
        inventory.setItem(22, item(Material.BOOK, "§bDéfis", "§7Voir les défis et leurs récompenses"));
        inventory.setItem(23, item(Material.PAPER, "§eNiveau d'île", "§7Consulter la valeur et la progression"));
        inventory.setItem(24, item(Material.DIAMOND_SWORD, "§5PvP d'île", "§7Activer ou désactiver le PvP en survie"));
        inventory.setItem(29, item(Material.SKULL_ITEM, "§6Membres", "§7Gérer les membres, trusts et visiteurs"));
        inventory.setItem(30, item(Material.CARROT_ITEM, "§aDébloquer l'île Farming", "§710000 Pasteque", "§7Pont + île agricole détaillée"));
        inventory.setItem(31, item(Material.IRON_PICKAXE, "§7Débloquer l'île Minage", "§710000 Pasteque", "§7Pont + île minérale"));
        inventory.setItem(32, item(Material.NAME_TAG, "§dRenommer l'île", "§7Commande : /is rename <nom>"));
        inventory.setItem(33, item(Material.CHEST, "§6Banque d'île", "§7Commande : /is bank"));
        inventory.setItem(40, item(Material.SIGN, "§fÎles publiques / privées", "§7Commandes : /is public ou /is private"));
        inventory.setItem(41, item(Material.COMPASS, "§bVisiter / Home Farming/Minage", "§7Utilise /is visit ou /is home"));
        inventory.setItem(20, item(Material.GRASS, "Â§aRetour sur mon ile", "Â§7Teleportation vers ton ile principale"));
        inventory.setItem(21, item(Material.BED, "Â§dDefinir le home", "Â§7Definit le point de retour de l'ile"));
        inventory.setItem(22, item(Material.BOOK, "Â§bDefis", "Â§7Voir les defis et recompenses"));
        inventory.setItem(23, item(Material.PAPER, "Â§eNiveau d'ile", "Â§7Commande : /is level"));
        inventory.setItem(24, item(Material.DIAMOND_SWORD, "Â§5PvP d'ile", "Â§7Commande : /is pvp"));
        inventory.setItem(30, item(Material.CARROT_ITEM, "Â§aDebloquer Farming", "Â§710000 Pasteque", "Â§7Commande : /is unlock farming"));
        inventory.setItem(31, item(Material.IRON_PICKAXE, "Â§7Debloquer Minage", "Â§710000 Pasteque", "Â§7Commande : /is unlock mining"));
        inventory.setItem(34, item(Material.GOLD_INGOT, "Â§6Top iles", "Â§7Commande : /is top"));
        inventory.setItem(39, item(Material.COMPASS, "Â§aHome Farming", "Â§7Commande : /is farm"));
        inventory.setItem(41, item(Material.DIAMOND_PICKAXE, "Â§7Home Minage", "Â§7Commande : /is mine"));
        inventory.setItem(42, item(Material.BOOK_AND_QUILL, "Â§dProfil ile", "Â§7Commande : /is stats"));
        player.openInventory(inventory);
    }

    private void openMembers(Player player, Island island) {
        Inventory inventory = Bukkit.createInventory(null, 54, "§dMembres d'île");
        ItemStack green = pane((short) 5);
        ItemStack magenta = pane((short) 2);
        for (int slot = 0; slot < 54; slot++) {
            if (slot < 9 || slot >= 45 || slot % 9 == 0 || slot % 9 == 8) {
                inventory.setItem(slot, (slot % 2 == 0) ? green : magenta);
            }
        }
        int slot = 10;
        for (UUID uuid : island.getMembers()) {
            OfflinePlayer member = Bukkit.getOfflinePlayer(uuid);
            ItemStack skull = item(Material.SKULL_ITEM, "§e" + (member.getName() == null ? uuid.toString() : member.getName()), "§7Clique pour expulser ce membre", "§8UUID:" + uuid.toString());
            inventory.setItem(slot++, skull);
            if (slot % 9 == 8) slot += 2;
            if (slot >= 44) break;
        }
        player.openInventory(inventory);
    }

    private ItemStack pane(short data) {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, data);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack item(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lore = new ArrayList<String>();
            for (String line : loreLines) {
                lore.add(line);
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void openChallenges(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, "§bDéfis Skyblock");
        ItemStack green = pane((short) 5);
        ItemStack magenta = pane((short) 2);
        for (int slot = 0; slot < 54; slot++) {
            if (slot < 9 || slot >= 45 || slot % 9 == 0 || slot % 9 == 8) {
                inventory.setItem(slot, (slot % 2 == 0) ? green : magenta);
            }
        }
        int slot = 10;
        for (String challengeId : plugin.getChallengeManager().getChallengeIds()) {
            org.bukkit.configuration.ConfigurationSection section =
                    plugin.getConfig().getConfigurationSection("challenges." + challengeId);
            if (section == null) {
                continue;
            }

            Material material = Material.matchMaterial(section.getString("icon", section.getString("material", "BOOK")));
            if (material == null) {
                material = Material.BOOK;
            }

            String name = section.getString("name", challengeId);
            int amount = section.getInt("amount", 1);
            double reward = section.getDouble("reward", 0.0D);

            List<String> lore = new ArrayList<String>();
            lore.add("§7Objectif : " + amount + "x " + section.getString("material", "ITEM"));
            lore.add("§7Récompense : §a" + reward + " Pasteque");
            if (plugin.getChallengeManager().isCompleted(challengeId, player.getUniqueId())) {
                lore.add("§aDéjà complété");
            } else {
                lore.add("§eClique pour tenter le défi");
            }
            lore.add("§8ID:" + challengeId);

            ItemStack item = new ItemStack(material, 1);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§b" + name);
                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            inventory.setItem(slot++, item);
            if (slot % 9 == 8) slot += 2;
            if (slot >= 44) break;
        }

        player.openInventory(inventory);
    }

}
