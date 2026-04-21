package fr.pastequeworld.bedwars.command;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.game.Arena;
import fr.pastequeworld.bedwars.game.GameMode;
import fr.pastequeworld.bedwars.game.GameState;
import fr.pastequeworld.bedwars.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Commande /bwadmin - administration complete du plugin BedWars.
 *
 * Sous-commandes :
 *   movnpc <solo|duo|teams>         Deplace le NPC a votre position
 *   setwait <solo|duo|teams>        Definit la zone d'attente de la file
 *   setlobbyspawn                   Definit le spawn du lobby
 *   forcestart <solo|duo|teams>     Force le demarrage d'une partie (meme avec peu de joueurs)
 *   forcestop                       Force l'arret de la partie ou vous vous trouvez
 *   spawnall                        Supprime et respawn tous les NPC
 *   kick <joueur>                   Expulse un joueur de sa partie vers le lobby
 *   tp <arenaId>                    Vous teleporte dans une arene
 *   list                            Liste toutes les arenes actives
 *   reload                          Recharge la configuration
 */
public class AdminCommand implements CommandExecutor, TabCompleter {

    private static final String PERM = "pastequebedwars.admin";

    private final BedWarsPlugin plugin;

    public AdminCommand(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERM)) {
            sender.sendMessage(plugin.getMessageManager().get("errors.no-permission"));
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {

            // --- movnpc <mode> ---
            case "movnpc": {
                if (!(sender instanceof Player)) { sender.sendMessage("Joueur requis."); return true; }
                if (args.length < 2) { sender.sendMessage(ColorUtil.color("&cUsage: /bwadmin movnpc <solo|duo|teams>")); return true; }
                GameMode mode = GameMode.fromConfigKey(args[1]);
                if (mode == null) { sender.sendMessage(ColorUtil.color("&cMode invalide. Utilisez solo, duo ou teams.")); return true; }
                Player p = (Player) sender;
                plugin.getNpcManager().moveNpc(mode, p.getLocation());
                sender.sendMessage(ColorUtil.color("&aNPC &f" + mode.name() + " &adeplaced a votre position et sauvegarde."));
                break;
            }

            // --- setwait <mode> ---
            case "setwait": {
                if (!(sender instanceof Player)) { sender.sendMessage("Joueur requis."); return true; }
                if (args.length < 2) { sender.sendMessage(ColorUtil.color("&cUsage: /bwadmin setwait <solo|duo|teams>")); return true; }
                GameMode mode = GameMode.fromConfigKey(args[1]);
                if (mode == null) { sender.sendMessage(ColorUtil.color("&cMode invalide.")); return true; }
                Player p = (Player) sender;
                plugin.getConfigManager().setLobbyWaitingSpawn(mode, p.getLocation());
                sender.sendMessage(ColorUtil.color("&aZone d'attente &f" + mode.name() + " &adefinit a votre position."));
                break;
            }

            // --- setlobbyspawn ---
            case "setlobbyspawn": {
                if (!(sender instanceof Player)) { sender.sendMessage("Joueur requis."); return true; }
                Player p = (Player) sender;
                plugin.getConfigManager().setLobbySpawn(p.getLocation());
                sender.sendMessage(ColorUtil.color("&aSpawn du lobby defini a votre position et sauvegarde."));
                break;
            }

            // --- forcestart <mode> ---
            case "forcestart": {
                if (args.length < 2) { sender.sendMessage(ColorUtil.color("&cUsage: /bwadmin forcestart <solo|duo|teams>")); return true; }
                GameMode mode = GameMode.fromConfigKey(args[1]);
                if (mode == null) { sender.sendMessage(ColorUtil.color("&cMode invalide.")); return true; }

                Arena target = null;
                for (Arena a : plugin.getArenaManager().getArenas()) {
                    if (a.getMode() == mode && (a.getState() == GameState.WAITING || a.getState() == GameState.STARTING)) {
                        target = a;
                        break;
                    }
                }
                if (target == null) {
                    sender.sendMessage(ColorUtil.color("&cAucune arene en attente pour le mode &f" + mode.name() + "&c."));
                    return true;
                }
                if (target.getPlayers().isEmpty()) {
                    sender.sendMessage(ColorUtil.color("&cL'arene est vide, impossible de demarrer."));
                    return true;
                }
                target.start();
                sender.sendMessage(ColorUtil.color("&aPartie &f" + target.getId() + " &ademarree de force (" + target.getPlayers().size() + " joueurs)."));
                break;
            }

            // --- forcestop ---
            case "forcestop": {
                if (!(sender instanceof Player)) { sender.sendMessage("Joueur requis."); return true; }
                Arena arena = plugin.getArenaManager().getArenaOfPlayer((Player) sender);
                if (arena == null) {
                    sender.sendMessage(plugin.getMessageManager().get("errors.not-in-game"));
                    return true;
                }
                arena.end(null);
                sender.sendMessage(ColorUtil.color("&aPartie arretee de force."));
                break;
            }

            // --- spawnall ---
            case "spawnall": {
                plugin.getNpcManager().despawnAll();
                plugin.getNpcManager().spawnAll();
                sender.sendMessage(ColorUtil.color("&aTous les NPC ont ete respawnes."));
                break;
            }

            // --- kick <joueur> ---
            case "kick": {
                if (args.length < 2) { sender.sendMessage(ColorUtil.color("&cUsage: /bwadmin kick <joueur>")); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage(ColorUtil.color("&cJoueur introuvable : &f" + args[1])); return true; }
                Arena arena = plugin.getArenaManager().getArenaOfPlayer(target);
                if (arena == null) {
                    sender.sendMessage(ColorUtil.color("&f" + target.getName() + " &cn'est pas en partie."));
                    return true;
                }
                arena.removePlayer(target);
                plugin.getLobbyManager().setupLobbyPlayer(target);
                target.sendMessage(ColorUtil.color("&cVous avez ete expulse de la partie par un administrateur."));
                sender.sendMessage(ColorUtil.color("&f" + target.getName() + " &aexpulse vers le lobby."));
                break;
            }

            // --- tp <arenaId> ---
            case "tp": {
                if (!(sender instanceof Player)) { sender.sendMessage("Joueur requis."); return true; }
                if (args.length < 2) { sender.sendMessage(ColorUtil.color("&cUsage: /bwadmin tp <arenaId>")); return true; }
                Arena arena = null;
                for (Arena a : plugin.getArenaManager().getArenas()) {
                    if (a.getId().equalsIgnoreCase(args[1])) { arena = a; break; }
                }
                if (arena == null) { sender.sendMessage(ColorUtil.color("&cArene introuvable : &f" + args[1])); return true; }
                Player p = (Player) sender;
                if (arena.getQueueSpawn() != null) {
                    p.teleport(arena.getQueueSpawn());
                    sender.sendMessage(ColorUtil.color("&aTeleporte dans l'arene &f" + arena.getId() + "&a."));
                } else {
                    sender.sendMessage(ColorUtil.color("&cPas de spawn disponible dans cette arene."));
                }
                break;
            }

            // --- list ---
            case "list": {
                List<Arena> all = new ArrayList<Arena>(plugin.getArenaManager().getArenas());
                if (all.isEmpty()) {
                    sender.sendMessage(ColorUtil.color("&7Aucune arene active."));
                    return true;
                }
                sender.sendMessage(ColorUtil.color("&e&lARENES ACTIVES &7(" + all.size() + ") :"));
                for (Arena a : all) {
                    sender.sendMessage(ColorUtil.color(
                            "&7- &b" + a.getId()
                            + " &8[" + a.getMode().getConfigKey().toUpperCase() + "]"
                            + " &f" + a.getState().name()
                            + " &7" + a.getPlayers().size() + "/" + plugin.getConfigManager().getMode(a.getMode()).getMaxPlayers() + " joueurs"));
                }
                break;
            }

            // --- reload ---
            case "reload": {
                plugin.reloadConfig();
                plugin.getConfigManager().load();
                plugin.getMessageManager().load();
                plugin.getShopConfig().load();
                plugin.getMapRegistry().load();
                sender.sendMessage(ColorUtil.color("&aConfiguration rechargee."));
                break;
            }

            default:
                showHelp(sender);
        }
        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.color("&7&m-------------------------------------"));
        sender.sendMessage(ColorUtil.color("&c&lBwAdmin &7- Commandes d'administration"));
        sender.sendMessage(ColorUtil.color("&e/bwadmin movnpc <mode>      &7Deplace un NPC a votre position"));
        sender.sendMessage(ColorUtil.color("&e/bwadmin setwait <mode>     &7Zone d'attente de la file"));
        sender.sendMessage(ColorUtil.color("&e/bwadmin setlobbyspawn      &7Spawn du lobby"));
        sender.sendMessage(ColorUtil.color("&e/bwadmin forcestart <mode>  &7Force le demarrage d'une partie"));
        sender.sendMessage(ColorUtil.color("&e/bwadmin forcestop          &7Force l'arret de votre partie"));
        sender.sendMessage(ColorUtil.color("&e/bwadmin spawnall           &7Respawn tous les NPC"));
        sender.sendMessage(ColorUtil.color("&e/bwadmin kick <joueur>      &7Expulse du jeu vers le lobby"));
        sender.sendMessage(ColorUtil.color("&e/bwadmin tp <arenaId>       &7Teleporte dans une arene"));
        sender.sendMessage(ColorUtil.color("&e/bwadmin list               &7Liste les arenes actives"));
        sender.sendMessage(ColorUtil.color("&e/bwadmin reload             &7Recharge la configuration"));
        sender.sendMessage(ColorUtil.color("&7&m-------------------------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERM)) return new ArrayList<String>();
        if (args.length == 1) {
            return filterStart(args[0], "movnpc", "setwait", "setlobbyspawn", "forcestart", "forcestop",
                    "spawnall", "kick", "tp", "list", "reload");
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if ("movnpc".equals(sub) || "setwait".equals(sub) || "forcestart".equals(sub)) {
                return filterStart(args[1], "solo", "duo", "teams");
            }
            if ("kick".equals(sub)) {
                List<String> names = new ArrayList<String>();
                for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
                return filterStart(args[1], names.toArray(new String[0]));
            }
            if ("tp".equals(sub)) {
                List<String> ids = new ArrayList<String>();
                for (Arena a : plugin.getArenaManager().getArenas()) ids.add(a.getId());
                return filterStart(args[1], ids.toArray(new String[0]));
            }
        }
        return new ArrayList<String>();
    }

    private List<String> filterStart(String prefix, String... options) {
        List<String> result = new ArrayList<String>();
        for (String o : options) {
            if (o.toLowerCase().startsWith(prefix.toLowerCase())) result.add(o);
        }
        return result;
    }
}
