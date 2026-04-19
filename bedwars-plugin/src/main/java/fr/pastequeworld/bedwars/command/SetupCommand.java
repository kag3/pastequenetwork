package fr.pastequeworld.bedwars.command;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.team.TeamColor;
import fr.pastequeworld.bedwars.util.ColorUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

/**
 * Commande de setup des maps :
 *   /bwsetup create <mapId> [modes...] -> cree plugins/PastequeBedWars/maps/<id>.yml
 *   /bwsetup setcorner <mapId> -> set la paste-offset a la location du joueur
 *   /bwsetup setspawn <mapId> <teamColor> -> set le spawn de team
 *   /bwsetup setbed <mapId> <teamColor> -> set position du lit
 *   /bwsetup setshop <mapId> <teamColor> -> set shop
 *   /bwsetup setupgrade <mapId> <teamColor> -> set upgrade
 *   /bwsetup setiron <mapId> <teamColor> -> set iron gen
 *   /bwsetup setgold <mapId> <teamColor> -> set gold gen
 *   /bwsetup adddiamond <mapId> -> ajoute un diamond gen
 *   /bwsetup addemerald <mapId> -> ajoute un emerald gen
 *   /bwsetup setqueue <mapId> -> set le queue-spawn
 *   /bwsetup setschematic <mapId> <filename> -> nom du fichier schematic
 *   /bwsetup addmode <mapId> <solo|duo|teams>
 *
 * Positions sauvegardees en RELATIF a la paste-offset.
 */
public class SetupCommand implements CommandExecutor {

    private final BedWarsPlugin plugin;

    public SetupCommand(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pastequebedwars.admin")) {
            sender.sendMessage(plugin.getMessageManager().get("errors.no-permission"));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("Seul un joueur peut utiliser cette commande.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length < 2) {
            help(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        String mapId = args[1].toLowerCase();
        File file = new File(new File(plugin.getDataFolder(), "maps"), mapId + ".yml");
        FileConfiguration cfg = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();

        switch (sub) {
            case "create": {
                if (!file.exists()) {
                    cfg.set("id", mapId);
                    cfg.set("display-name", mapId);
                    cfg.set("schematic", mapId + ".schematic");
                    cfg.set("modes", java.util.Arrays.asList("solo", "duo", "teams"));
                    writeVector(cfg, "paste-offset", 0, 64, 0);
                    writeVector(cfg, "queue-spawn", 0, 70, 0);
                }
                save(cfg, file, player);
                break;
            }
            case "setcorner": {
                writeVector(cfg, "paste-offset", player.getLocation().getBlockX(),
                        player.getLocation().getBlockY(), player.getLocation().getBlockZ());
                save(cfg, file, player);
                break;
            }
            case "setqueue": {
                Location origin = readOrigin(cfg);
                writeRelative(cfg, "queue-spawn", origin, player.getLocation());
                save(cfg, file, player);
                break;
            }
            case "setspawn":
            case "setbed":
            case "setshop":
            case "setupgrade":
            case "setiron":
            case "setgold": {
                if (args.length < 3) {
                    sender.sendMessage(ColorUtil.color("&cUsage: /bwsetup " + sub + " <mapId> <teamColor>"));
                    return true;
                }
                TeamColor color = parseColor(args[2]);
                if (color == null) {
                    sender.sendMessage(ColorUtil.color("&cCouleur invalide."));
                    return true;
                }
                Location origin = readOrigin(cfg);
                String path = pathFor(sub) + "." + color.name();
                writeRelative(cfg, path, origin, player.getLocation());
                save(cfg, file, player);
                break;
            }
            case "adddiamond":
            case "addemerald": {
                Location origin = readOrigin(cfg);
                String path = sub.equals("adddiamond") ? "generators.diamond" : "generators.emerald";
                java.util.List<java.util.Map<String, Object>> list = new java.util.ArrayList<java.util.Map<String, Object>>();
                java.util.List<?> old = cfg.getMapList(path);
                if (old != null) for (Object o : old) list.add((java.util.Map<String, Object>) o);
                java.util.Map<String, Object> entry = new java.util.LinkedHashMap<String, Object>();
                entry.put("x", player.getLocation().getX() - origin.getX());
                entry.put("y", player.getLocation().getY() - origin.getY());
                entry.put("z", player.getLocation().getZ() - origin.getZ());
                list.add(entry);
                cfg.set(path, list);
                save(cfg, file, player);
                break;
            }
            case "setschematic": {
                if (args.length < 3) {
                    sender.sendMessage(ColorUtil.color("&cUsage: /bwsetup setschematic <mapId> <filename>"));
                    return true;
                }
                cfg.set("schematic", args[2]);
                save(cfg, file, player);
                break;
            }
            case "addmode": {
                if (args.length < 3) {
                    sender.sendMessage(ColorUtil.color("&cUsage: /bwsetup addmode <mapId> <mode>"));
                    return true;
                }
                java.util.List<String> modes = cfg.getStringList("modes");
                if (!modes.contains(args[2])) modes.add(args[2]);
                cfg.set("modes", modes);
                save(cfg, file, player);
                break;
            }
            default:
                help(sender);
                return true;
        }

        return true;
    }

    private Location readOrigin(FileConfiguration cfg) {
        double x = cfg.getDouble("paste-offset.x");
        double y = cfg.getDouble("paste-offset.y");
        double z = cfg.getDouble("paste-offset.z");
        return new Location(null, x, y, z);
    }

    private void writeVector(FileConfiguration cfg, String path, double x, double y, double z) {
        cfg.set(path + ".x", x);
        cfg.set(path + ".y", y);
        cfg.set(path + ".z", z);
    }

    private void writeRelative(FileConfiguration cfg, String path, Location origin, Location abs) {
        cfg.set(path + ".x", abs.getX() - origin.getX());
        cfg.set(path + ".y", abs.getY() - origin.getY());
        cfg.set(path + ".z", abs.getZ() - origin.getZ());
        cfg.set(path + ".yaw", abs.getYaw());
    }

    private String pathFor(String sub) {
        switch (sub) {
            case "setspawn": return "teams.spawns";
            case "setbed": return "teams.beds";
            case "setshop": return "teams.shops";
            case "setupgrade": return "teams.upgrades";
            case "setiron": return "teams.iron-generators";
            case "setgold": return "teams.gold-generators";
        }
        return "unknown";
    }

    private TeamColor parseColor(String name) {
        try { return TeamColor.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private void save(FileConfiguration cfg, File file, Player player) {
        try {
            //noinspection ResultOfMethodCallIgnored
            file.getParentFile().mkdirs();
            cfg.save(file);
            player.sendMessage(ColorUtil.color("&aConfig sauvegardee: &f" + file.getName()));
        } catch (IOException e) {
            player.sendMessage(ColorUtil.color("&cErreur de sauvegarde: " + e.getMessage()));
        }
    }

    private void help(CommandSender sender) {
        sender.sendMessage(ColorUtil.color("&7&m---------------------------------"));
        sender.sendMessage(ColorUtil.color("&c&lSETUP &7- &fCommandes"));
        sender.sendMessage(ColorUtil.color("&e/bwsetup create <id> &7Initialise une map"));
        sender.sendMessage(ColorUtil.color("&e/bwsetup setcorner <id> &7Paste offset"));
        sender.sendMessage(ColorUtil.color("&e/bwsetup setqueue <id> &7Spawn de queue"));
        sender.sendMessage(ColorUtil.color("&e/bwsetup setspawn <id> <color>"));
        sender.sendMessage(ColorUtil.color("&e/bwsetup setbed <id> <color>"));
        sender.sendMessage(ColorUtil.color("&e/bwsetup setshop <id> <color>"));
        sender.sendMessage(ColorUtil.color("&e/bwsetup setupgrade <id> <color>"));
        sender.sendMessage(ColorUtil.color("&e/bwsetup setiron <id> <color>"));
        sender.sendMessage(ColorUtil.color("&e/bwsetup setgold <id> <color>"));
        sender.sendMessage(ColorUtil.color("&e/bwsetup adddiamond <id>"));
        sender.sendMessage(ColorUtil.color("&e/bwsetup addemerald <id>"));
        sender.sendMessage(ColorUtil.color("&e/bwsetup setschematic <id> <fichier>"));
        sender.sendMessage(ColorUtil.color("&e/bwsetup addmode <id> <solo|duo|teams>"));
        sender.sendMessage(ColorUtil.color("&7&m---------------------------------"));
    }
}
