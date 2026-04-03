package fr.pastequeworld.labyroyal.command;

import fr.pastequeworld.labyroyal.LabyRoyalPlugin;
import fr.pastequeworld.labyroyal.game.Game;
import fr.pastequeworld.labyroyal.game.LabyGameMode;
import fr.pastequeworld.labyroyal.util.MessageUtil;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LabyRoyalCommand implements CommandExecutor, TabCompleter {

    private final LabyRoyalPlugin plugin;

    public LabyRoyalCommand(LabyRoyalPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande est r\u00e9serv\u00e9e aux joueurs.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "solo":
                joinGame(player, LabyGameMode.SOLO);
                break;
            case "duo":
                joinGame(player, LabyGameMode.DUO);
                break;
            case "duel":
                joinGame(player, LabyGameMode.DUEL);
                break;
            case "leave":
            case "quit":
            case "quitter":
                leaveGame(player);
                break;
            case "partywait":
                handlePartyWait(player, args);
                break;
            case "stats":
                showStats(player);
                break;
            case "admin":
                handleAdmin(player, args);
                break;
            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    /**
     * Called by BungeeCord PastequeParty via forced chat command.
     * Registers this player as waiting for the party leader's mode choice.
     * Usage: /lr partywait <leaderName>
     */
    private void handlePartyWait(Player player, String[] args) {
        if (args.length < 2) return;
        String leaderName = args[1];

        // Mark as chosen so the cabin doesn't open
        plugin.getModeSelectListener().markChosen(player.getUniqueId());

        // Register as waiting for leader's mode choice
        plugin.getModeSelectListener().addPartyWaiter(player.getUniqueId(), leaderName);

        // Freeze and show waiting message
        player.setWalkSpeed(0f);
        player.setFlySpeed(0f);
        player.closeInventory();
        MessageUtil.send(player, "&d\u25B6 &7En attente du choix de &e" + leaderName + "&7...");
    }

    private void joinGame(Player player, LabyGameMode mode) {
        MessageUtil.send(player, "&eRecherche d'une partie " + mode.getDisplayName() + "...");
        boolean joined = plugin.getGameManager().joinGame(player, mode);
        if (joined) {
            MessageUtil.send(player, "&aVous avez rejoint une partie " + mode.getDisplayName() + " !");
        }
    }

    private void leaveGame(Player player) {
        plugin.getGameManager().leaveGame(player);
        plugin.sendToHub(player);
    }

    private void showStats(Player player) {
        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());

        MessageUtil.sendRaw(player, MessageUtil.line());
        MessageUtil.sendRaw(player, "&6&l   \u2726 LabyRoyale - Informations \u2726");
        MessageUtil.sendRaw(player, "");

        if (game != null) {
            MessageUtil.sendRaw(player, "  &fPartie: &e#" + game.getId()
                    + " &7(" + game.getGameMode().getDisplayName() + ")");
            MessageUtil.sendRaw(player, "  &f\u00c9tat: &e" + game.getState().getDisplayName());
            MessageUtil.sendRaw(player, "  &fJoueurs: &a" + game.getPlayers().size()
                    + "&7/&a" + game.getMaxPlayers());
            MessageUtil.sendRaw(player, "  &fEn vie: &c" + game.getAliveCount());
        } else {
            MessageUtil.sendRaw(player, "  &7Vous n'\u00eates dans aucune partie.");
            MessageUtil.sendRaw(player, "  &7Utilisez &e/lr solo&7, &e/lr duo &7ou &e/lr duel &7pour jouer !");
        }

        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player, "  &7Parties actives: &f" + plugin.getGameManager().getActiveGameCount());
        MessageUtil.sendRaw(player, "  &7Joueurs total: &f" + plugin.getGameManager().getTotalPlayers());
        MessageUtil.sendRaw(player, MessageUtil.line());
    }

    private void handleAdmin(Player player, String[] args) {
        if (!player.hasPermission("labyroyale.admin")) {
            MessageUtil.send(player, "&cVous n'avez pas la permission.");
            return;
        }

        if (args.length < 2) {
            sendAdminHelp(player);
            return;
        }

        String adminSub = args[1].toLowerCase();
        switch (adminSub) {
            case "list":
                MessageUtil.send(player, "&6Parties actives:");
                for (Game game : plugin.getGameManager().getGames()) {
                    MessageUtil.sendRaw(player, "  &e#" + game.getId()
                            + " &7- " + game.getGameMode().getDisplayName()
                            + " &7- " + game.getState().getDisplayName()
                            + " &7- &f" + game.getPlayers().size() + " joueurs");
                }
                if (plugin.getGameManager().getActiveGameCount() == 0) {
                    MessageUtil.sendRaw(player, "  &7Aucune partie active.");
                }
                break;
            case "forcestart":
                Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());
                if (game == null) {
                    MessageUtil.send(player, "&cVous devez \u00eatre dans une partie.");
                    return;
                }
                if (game.getPlayers().size() < 2) {
                    MessageUtil.send(player, "&cIl faut au moins 2 joueurs.");
                    return;
                }
                MessageUtil.send(player, "&aForce start de la partie #" + game.getId());
                game.broadcast("&c&l[ADMIN] &eForce start par " + player.getName());
                break;
            case "stop":
                MessageUtil.send(player, "&cArr\u00eat de toutes les parties...");
                plugin.getGameManager().shutdownAll();
                MessageUtil.send(player, "&aToutes les parties ont \u00e9t\u00e9 arr\u00eat\u00e9es.");
                break;
            case "reload":
                plugin.reloadConfig();
                MessageUtil.send(player, "&aConfiguration recharg\u00e9e !");
                break;
            default:
                sendAdminHelp(player);
                break;
        }
    }

    private void sendHelp(Player player) {
        MessageUtil.sendRaw(player, MessageUtil.line());
        MessageUtil.sendRaw(player, "&6&l   \u2726 LabyRoyale - Commandes \u2726");
        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player, "  &e/lr solo &7- Rejoindre une partie solo");
        MessageUtil.sendRaw(player, "  &e/lr duo &7- Rejoindre une partie duo");
        MessageUtil.sendRaw(player, "  &e/lr duel &7- Rejoindre un duel 1v1");
        MessageUtil.sendRaw(player, "  &e/lr leave &7- Quitter la partie");
        MessageUtil.sendRaw(player, "  &e/lr stats &7- Voir les informations");
        MessageUtil.sendRaw(player, "");
        if (player.hasPermission("labyroyale.admin")) {
            MessageUtil.sendRaw(player, "  &c/lr admin &7- Commandes admin");
        }
        MessageUtil.sendRaw(player, MessageUtil.line());
    }

    private void sendAdminHelp(Player player) {
        MessageUtil.sendRaw(player, MessageUtil.line());
        MessageUtil.sendRaw(player, "&c&l   \u2726 LabyRoyale - Admin \u2726");
        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player, "  &c/lr admin list &7- Lister les parties");
        MessageUtil.sendRaw(player, "  &c/lr admin forcestart &7- Forcer le lancement");
        MessageUtil.sendRaw(player, "  &c/lr admin stop &7- Arr\u00eater toutes les parties");
        MessageUtil.sendRaw(player, "  &c/lr admin reload &7- Recharger la config");
        MessageUtil.sendRaw(player, MessageUtil.line());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<String>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("solo", "duo", "duel", "leave", "stats"));
            if (sender.hasPermission("labyroyale.admin")) {
                completions.add("admin");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin") && sender.hasPermission("labyroyale.admin")) {
            completions.addAll(Arrays.asList("list", "forcestart", "stop", "reload"));
        }

        String input = args[args.length - 1].toLowerCase();
        List<String> filtered = new ArrayList<String>();
        for (String c : completions) {
            if (c.toLowerCase().startsWith(input)) {
                filtered.add(c);
            }
        }
        return filtered;
    }
}
