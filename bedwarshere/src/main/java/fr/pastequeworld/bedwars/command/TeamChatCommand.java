package fr.pastequeworld.bedwars.command;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.player.BedWarsPlayer;
import fr.pastequeworld.bedwars.team.Team;
import fr.pastequeworld.bedwars.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Chat d'equipe : message envoye uniquement aux allies.
 */
public class TeamChatCommand implements CommandExecutor {

    private final BedWarsPlugin plugin;

    public TeamChatCommand(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        if (args.length == 0) {
            player.sendMessage(ColorUtil.color("&cUsage: /tc <message>"));
            return true;
        }
        BedWarsPlayer bw = plugin.getPlayerDataManager().get(player);
        if (bw == null || bw.getTeam() == null) {
            player.sendMessage(plugin.getMessageManager().get("errors.not-in-game"));
            return true;
        }
        Team team = bw.getTeam();
        StringBuilder msg = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) msg.append(' ');
            msg.append(args[i]);
        }
        String color = team.getColor().getChatColor().toString();
        String formatted = plugin.getMessageManager().getRaw("chat.format-team")
                .replace("%team_color%", color)
                .replace("%player%", player.getName())
                .replace("%message%", msg.toString());
        formatted = ColorUtil.color(formatted);
        for (java.util.UUID uuid : team.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(formatted);
        }
        return true;
    }
}
