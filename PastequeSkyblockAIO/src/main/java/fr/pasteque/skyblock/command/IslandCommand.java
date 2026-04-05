package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.gui.GuiHelper;
import fr.pasteque.skyblock.manager.ConfirmationManager;
import fr.pasteque.skyblock.model.Island;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

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
            sender.sendMessage(PastequeSkyblockPlugin.color("&2&lPasteque &8\u00bb &cCommande joueur uniquement."));
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
                MessageUtil.send(player, plugin.getPrefix(), "&fTu as deja une ile.");
                return true;
            }
            if (plugin.getIslandUpgradeListener().isGenerating(player.getUniqueId())) {
                MessageUtil.send(player, plugin.getPrefix(), "&fTon ile est en cours de generation, patiente...");
                return true;
            }
            plugin.getIslandPresetGui().open(player);
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

    @SuppressWarnings("deprecation")
    private void openMenuPremium(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54,
                PastequeSkyblockPlugin.color("&2&lPasteque &5&lSkyblock"));
        Island island = plugin.getIslandManager().getIslandByPlayer(player.getUniqueId());
        int level = island == null ? 0 : plugin.getIslandManager().getLevel(island);
        int farmPrice = plugin.getConfig().getInt("island-expansions.solo.farming-price", 10000);
        int minePrice = plugin.getConfig().getInt("island-expansions.solo.mining-price", 10000);
        String farmState = island != null && island.isFarmingUnlocked() ? "&aDebloquee" : "&cVerrouillee";
        String mineState = island != null && island.isMiningUnlocked() ? "&aDebloquee" : "&cVerrouillee";
        String pvpState = island != null && island.isPvpEnabled() ? "&aActif" : "&cInactif";
        String bank = island == null ? "0" : String.valueOf((int) island.getBankBalance());

        GuiHelper.addTopBorder(inventory);
        GuiHelper.addBottomBorder(inventory);

        // Layout parfaitement symetrique aere — 15 items repartis en 4+4+4+3
        // Row 1 (9-17)  : 10, 12, 14, 16  — actions principales
        // Row 2 (18-26) : 19, 21, 23, 25  — management
        // Row 3 (27-35) : 28, 30, 32, 34  — unlocks / stats
        // Row 4 (36-44) : 38, 40, 42      — homes / profil (symetriques autour de 40)
        // Close         : 49
        if (island == null) {
            inventory.setItem(10, GuiHelper.createItem(Material.SAPLING,
                    "&2&lCreer mon ile", "&8&m                    ", "&7Commande : &f/is create", "&8&m                    "));
        } else {
            inventory.setItem(10, GuiHelper.createItem(Material.GRASS,
                    "&2&lRetour sur mon ile", "&8&m                    ", "&7Teleportation vers ton ile principale", "&8&m                    "));
        }
        inventory.setItem(12, GuiHelper.createItem(Material.BED,
                "&5&lDefinir le home", "&8&m                    ", "&7Commande : &f/is sethome", "&8&m                    "));
        inventory.setItem(14, GuiHelper.createItem(Material.BOOK,
                "&2&lDefis Skyblock", "&8&m                    ", "&7Voir et valider tes defis", "&8&m                    "));
        inventory.setItem(16, GuiHelper.createItem(Material.DIAMOND_SWORD,
                "&2&lPvP d'ile", "&8&m                    ", "&7Etat: " + pvpState, "&7Commande : &f/is pvp", "&8&m                    "));

        inventory.setItem(19, GuiHelper.createItem(Material.SKULL_ITEM,
                "&5&lMembres", "&8&m                    ", "&7Gerer les membres et permissions", "&8&m                    "));
        inventory.setItem(21, GuiHelper.createItem(Material.NAME_TAG,
                "&5&lRenommer l'ile", "&8&m                    ", "&7Commande : &f/is rename <nom>", "&8&m                    "));
        inventory.setItem(23, GuiHelper.createItem(Material.CHEST,
                "&2&lBanque d'ile", "&8&m                    ", "&7Solde: &e" + bank + " " + plugin.getEconomyManager().getCurrencyName(), "&7Commande : &f/is bank", "&8&m                    "));
        inventory.setItem(25, GuiHelper.createItem(Material.PAPER,
                "&5&lNiveau d'ile", "&8&m                    ", "&7Niveau actuel: &e" + level, "&8&m                    "));

        inventory.setItem(28, GuiHelper.createItem(Material.CARROT_ITEM,
                "&2&lFarming", "&8&m                    ", "&7Etat: " + farmState, "&7Prix unlock: &e" + farmPrice, "&7Commande : &f/is unlock farming", "&8&m                    "));
        inventory.setItem(30, GuiHelper.createItem(Material.IRON_PICKAXE,
                "&7&lMinage", "&8&m                    ", "&7Etat: " + mineState, "&7Prix unlock: &e" + minePrice, "&7Commande : &f/is unlock mining", "&8&m                    "));
        inventory.setItem(32, GuiHelper.createItem(Material.SIGN,
                "&7&lIles publiques / privees", "&8&m                    ", "&7Commandes : &f/is public &7ou &f/is private", "&8&m                    "));
        inventory.setItem(34, GuiHelper.createItem(Material.GOLD_INGOT,
                "&5&lTop mondial iles", "&8&m                    ", "&7Commande : &f/is top", "&8&m                    "));

        inventory.setItem(38, GuiHelper.createItem(Material.COMPASS,
                "&2&lHome Farming", "&8&m                    ", "&7Commande : &f/is farm", "&8&m                    "));
        inventory.setItem(40, GuiHelper.createItem(Material.BOOK_AND_QUILL,
                "&2&lProfil ile", "&8&m                    ", "&7Commande : &f/is stats", "&8&m                    "));
        inventory.setItem(42, GuiHelper.createItem(Material.DIAMOND_PICKAXE,
                "&5&lHome Minage", "&8&m                    ", "&7Commande : &f/is mine", "&8&m                    "));

        // Close button at bottom center
        inventory.setItem(49, GuiHelper.closeButton());

        GuiHelper.fillEmpty(inventory);
        player.openInventory(inventory);
    }

    @SuppressWarnings("deprecation")
    private void openMembers(Player player, Island island) {
        Inventory inventory = Bukkit.createInventory(null, 54,
                PastequeSkyblockPlugin.color("&2&lPasteque &5&lMembres"));

        GuiHelper.addTopBorder(inventory);
        GuiHelper.addBottomBorder(inventory);

        int slot = 10;
        for (UUID uuid : island.getMembers()) {
            OfflinePlayer member = Bukkit.getOfflinePlayer(uuid);
            String memberName = member.getName() == null ? uuid.toString() : member.getName();
            inventory.setItem(slot++, GuiHelper.createItem(Material.SKULL_ITEM,
                    "&5&l" + memberName,
                    "&8&m                    ",
                    "&7Clique pour expulser ce membre",
                    "&8UUID:" + uuid.toString(),
                    "&8&m                    "));
            if (slot % 9 == 8) slot += 2;
            if (slot >= 44) break;
        }

        // Back button at bottom-left area, close button at bottom center
        inventory.setItem(48, GuiHelper.backButton());
        inventory.setItem(49, GuiHelper.closeButton());

        GuiHelper.fillEmpty(inventory);
        player.openInventory(inventory);
    }

    @SuppressWarnings("deprecation")
    private void openChallenges(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54,
                PastequeSkyblockPlugin.color("&2&lPasteque &5&lDefis"));

        GuiHelper.addTopBorder(inventory);
        GuiHelper.addBottomBorder(inventory);

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

            String statusLine;
            if (plugin.getChallengeManager().isCompleted(challengeId, player.getUniqueId())) {
                statusLine = "&aComplete";
            } else {
                statusLine = "&eClique pour tenter le defi";
            }

            inventory.setItem(slot++, GuiHelper.createItem(material,
                    "&2&l" + name,
                    "&8&m                    ",
                    "&7Objectif : &f" + amount + "x " + section.getString("material", "ITEM"),
                    "&7Recompense : &a" + reward + " Pasteque",
                    statusLine,
                    "&8ID:" + challengeId,
                    "&8&m                    "));
            if (slot % 9 == 8) slot += 2;
            if (slot >= 44) break;
        }

        // Back button at bottom-left area, close button at bottom center
        inventory.setItem(48, GuiHelper.backButton());
        inventory.setItem(49, GuiHelper.closeButton());

        GuiHelper.fillEmpty(inventory);
        player.openInventory(inventory);
    }

}
