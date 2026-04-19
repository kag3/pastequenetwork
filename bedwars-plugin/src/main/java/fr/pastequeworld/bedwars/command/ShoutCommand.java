package fr.pastequeworld.bedwars.command;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.game.Arena;
import fr.pastequeworld.bedwars.player.BedWarsPlayer;
import fr.pastequeworld.bedwars.util.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Crie dans le chat global de la partie.
 * (Pour le moment, l'isolation est deja par arene. /shout ajoute juste un prefixe)
 */
public class ShoutCommand implements CommandExecutor {

    private final BedWarsPlugin plugin;

    public ShoutCommand(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        if (args.length == 0) {
            player.sendMessage(ColorUtil.color("&cUsage: /shout <message>"));
            return true;
        }
        BedWarsPlayer bw = plugin.getPlayerDataManager().get(player);
        if (bw == null || bw.getArena() == null) {
            player.sendMessage(plugin.getMessageManager().get("errors.not-in-game"));
            return true;
        }
        Arena arena = bw.getArena();
        String color = bw.getTeam() != null ? bw.getTeam().getColor().getChatColor().toString() : "&7";
        StringBuilder msg = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) msg.append(' ');
            msg.append(args[i]);
        }
        arena.broadcast(plugin.getMessageManager().getRaw("chat.format-shout")
                .replace("%team_color%", color)
                .replace("%player%", player.getName())
                .replace("%message%", msg.toString()));
        return true;
    }
}
