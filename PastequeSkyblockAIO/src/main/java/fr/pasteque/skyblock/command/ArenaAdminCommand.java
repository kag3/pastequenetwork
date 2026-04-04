package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.arena.ArenaWorldService;
import fr.pasteque.skyblock.arena.PlayerDataService;
import fr.pasteque.skyblock.arena.SafeZoneService;
import fr.pasteque.skyblock.arena.SelectionService;
import fr.pasteque.skyblock.arena.model.ArenaPlayerData;
import fr.pasteque.skyblock.arena.model.SafeZone;
import fr.pasteque.skyblock.arena.model.Selection;
import fr.pasteque.skyblock.manager.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ArenaAdminCommand implements CommandExecutor, TabCompleter {

    private final PastequeSkyblockPlugin plugin;
    private final ArenaWorldService arenaWorldService;
    private final SafeZoneService safeZoneService;
    private final SelectionService selectionService;
    private final PlayerDataService playerDataService;

    public ArenaAdminCommand(PastequeSkyblockPlugin plugin, ArenaWorldService arenaWorldService, SafeZoneService safeZoneService, SelectionService selectionService, PlayerDataService playerDataService) {
        this.plugin = plugin;
        this.arenaWorldService = arenaWorldService;
        this.safeZoneService = safeZoneService;
        this.selectionService = selectionService;
        this.playerDataService = playerDataService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pastequearena.admin")) {
            sender.sendMessage(plugin.color(plugin.prefix("arena") + "&fPermission insuffisante."));
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("setspawn")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.color(plugin.prefix("arena") + "&fCommande reservee aux joueurs."));
                return true;
            }
            Player player = (Player) sender;
            if (!arenaWorldService.isArenaWorld(player.getWorld())) {
                player.sendMessage(plugin.color(plugin.prefix("arena") + plugin.getConfig().getString("messages.arena-only", "&fCette action doit etre utilisee depuis l'arene.")));
                return true;
            }
            arenaWorldService.setConfiguredSpawn(player.getLocation());
            player.sendMessage(plugin.color(plugin.prefix("arena") + plugin.getConfig().getString("messages.spawn-set", "&fLe spawn d'arrivee de l'arene a ete mis a jour.")));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            safeZoneService.load();
            playerDataService.save();
            sender.sendMessage(plugin.color(plugin.prefix("arena") + plugin.getConfig().getString("messages.admin-reload", "&fLa configuration de l'arene a ete rechargee.")));
            return true;
        }
        if (args[0].equalsIgnoreCase("safe")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.color(plugin.prefix("arena") + "&fCommande reservee aux joueurs."));
                return true;
            }
            Player player = (Player) sender;
            Selection selection = selectionService.get(player.getUniqueId());
            if (args.length < 2) {
                sendHelp(sender);
                return true;
            }
            if (args[1].equalsIgnoreCase("pos1")) {
                selection.setPos1(player.getLocation());
                player.sendMessage(plugin.color(plugin.prefix("arena") + plugin.getConfig().getString("messages.safe-pos1", "&fPosition 1 de la zone safe enregistree.")));
                return true;
            }
            if (args[1].equalsIgnoreCase("pos2")) {
                selection.setPos2(player.getLocation());
                player.sendMessage(plugin.color(plugin.prefix("arena") + plugin.getConfig().getString("messages.safe-pos2", "&fPosition 2 de la zone safe enregistree.")));
                return true;
            }
            if (args[1].equalsIgnoreCase("create")) {
                if (args.length < 3) {
                    sendHelp(sender);
                    return true;
                }
                if (!selection.isComplete()) {
                    player.sendMessage(plugin.color(plugin.prefix("arena") + plugin.getConfig().getString("messages.no-safe-selection", "&fVeuillez definir &dpos1 &fet &dpos2 &favant de creer la zone.")));
                    return true;
                }
                safeZoneService.create(args[2], selection.getPos1(), selection.getPos2());
                player.sendMessage(plugin.color(plugin.prefix("arena") + plugin.getConfig().getString("messages.safe-created", "&fLa zone safe &d%name% &fa ete creee.").replace("%name%", args[2])));
                return true;
            }
            if (args[1].equalsIgnoreCase("delete")) {
                if (args.length < 3) {
                    sendHelp(sender);
                    return true;
                }
                safeZoneService.delete(args[2]);
                player.sendMessage(plugin.color(plugin.prefix("arena") + plugin.getConfig().getString("messages.safe-deleted", "&fLa zone safe &d%name% &fa ete supprimee.").replace("%name%", args[2])));
                return true;
            }
            if (args[1].equalsIgnoreCase("list")) {
                player.sendMessage(plugin.color(plugin.prefix("arena") + plugin.getConfig().getString("messages.safe-list-header", "&fZones safe connues :")));
                for (SafeZone zone : safeZoneService.getZones()) {
                    player.sendMessage(plugin.color(plugin.getConfig().getString("messages.safe-list-entry", "&8- &d%name%").replace("%name%", zone.getName())));
                }
                return true;
            }
        }
        if (args[0].equalsIgnoreCase("stats") && args.length >= 2) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(plugin.color(plugin.prefix("arena") + plugin.getConfig().getString("messages.unknown-player", "&fJoueur introuvable.")));
                return true;
            }
            ArenaPlayerData data = playerDataService.get(target);
            sender.sendMessage(plugin.color(plugin.prefix("arena") + "&f" + target.getName() + " &8| &fNiveau : &d" + data.getLevel() + " &8| &fKills : &d" + data.getKills() + " &8| &fMorts : &d" + data.getDeaths()));
            return true;
        }
        if (args[0].equalsIgnoreCase("pasteque") && args.length >= 4) {
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(plugin.color(plugin.prefix("arena") + plugin.getConfig().getString("messages.unknown-player", "&fJoueur introuvable.")));
                return true;
            }
            double amount;
            try {
                amount = Double.parseDouble(args[3]);
            } catch (NumberFormatException ex) {
                sendHelp(sender);
                return true;
            }
            EconomyManager eco = plugin.getEconomyManager();
            if (args[1].equalsIgnoreCase("give")) {
                eco.deposit(target.getUniqueId(), amount);
                sender.sendMessage(plugin.color(plugin.prefix("arena") + "&f" + (int) amount + " Pasteque ajoutees a &d" + target.getName()));
                return true;
            }
            if (args[1].equalsIgnoreCase("take")) {
                eco.withdraw(target.getUniqueId(), amount);
                sender.sendMessage(plugin.color(plugin.prefix("arena") + "&f" + (int) amount + " Pasteque retirees a &d" + target.getName()));
                return true;
            }
            if (args[1].equalsIgnoreCase("set")) {
                double current = eco.getBalance(target.getUniqueId());
                double diff = amount - current;
                if (diff > 0) {
                    eco.deposit(target.getUniqueId(), diff);
                } else if (diff < 0) {
                    eco.withdraw(target.getUniqueId(), -diff);
                }
                sender.sendMessage(plugin.color(plugin.prefix("arena") + "&fSolde fixe a &d" + (int) amount + " Pasteque &fpour &d" + target.getName()));
                return true;
            }
        }
        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.color(plugin.prefix("arena") + plugin.getConfig().getString("messages.command-help-header", "&fCommandes disponibles :")));
        sendLine(sender, "/arenaadmin setspawn", "Definit le spawn d'arrivee de l'arene");
        sendLine(sender, "/arenaadmin safe pos1", "Definit la premiere position d'une zone safe");
        sendLine(sender, "/arenaadmin safe pos2", "Definit la seconde position d'une zone safe");
        sendLine(sender, "/arenaadmin safe create <nom>", "Cree une zone safe");
        sendLine(sender, "/arenaadmin safe delete <nom>", "Supprime une zone safe");
        sendLine(sender, "/arenaadmin safe list", "Liste les zones safe");
        sendLine(sender, "/arenaadmin pasteque give|take|set <joueur> <montant>", "Gere la monnaie Pasteque");
        sendLine(sender, "/arenaadmin stats <joueur>", "Affiche les statistiques d'un joueur");
        sendLine(sender, "/arenaadmin reload", "Recharge la configuration");
    }

    private void sendLine(CommandSender sender, String usage, String desc) {
        sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.command-help-entry", "&8- &d%usage% &7: %desc%")
                .replace("%usage%", usage)
                .replace("%desc%", desc)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("setspawn", "safe", "pasteque", "stats", "reload");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("safe")) {
            return Arrays.asList("pos1", "pos2", "create", "delete", "list");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("pasteque")) {
            return Arrays.asList("give", "take", "set");
        }
        if ((args.length == 3 && args[0].equalsIgnoreCase("pasteque")) || (args.length == 2 && args[0].equalsIgnoreCase("stats"))) {
            List<String> names = new ArrayList<String>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return names;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("safe") && args[1].equalsIgnoreCase("delete")) {
            Collection<SafeZone> zones = safeZoneService.getZones();
            List<String> names = new ArrayList<String>();
            for (SafeZone zone : zones) {
                names.add(zone.getName());
            }
            return names;
        }
        return Collections.emptyList();
    }
}
