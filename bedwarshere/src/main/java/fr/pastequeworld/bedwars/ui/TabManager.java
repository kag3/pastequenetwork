package fr.pastequeworld.bedwars.ui;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.util.ColorUtil;
import org.bukkit.entity.Player;

/**
 * Applique un header/footer custom a la tab list.
 * Pour la 1.9.4, on passe par une methode Spigot standard.
 */
public class TabManager {

    private final BedWarsPlugin plugin;

    public TabManager(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void refreshLobby(Player player) {
        String header = ColorUtil.color("&e&lBEDWARS &8| &f" + plugin.getConfigManager().getServerName());
        String footer = ColorUtil.color("\n&7Lobby &8| &fChoisis un mode de jeu&7\n"
                + "&bmerci de jouer sur pasteque.world");
        sendHeaderFooter(player, header, footer);
    }

    public void refreshGame(Player player, fr.pastequeworld.bedwars.game.Arena arena) {
        String header = ColorUtil.color("&e&lBEDWARS &8| &f" + plugin.getConfigManager().getServerName());
        String footer = ColorUtil.color("&7Partie " + arena.getId() + "\n&fBon match !");
        sendHeaderFooter(player, header, footer);
    }

    private void sendHeaderFooter(Player player, String header, String footer) {
        try {
            java.lang.reflect.Method method = player.getClass()
                    .getMethod("setPlayerListHeaderFooter", String.class, String.class);
            method.invoke(player, header, footer);
        } catch (Exception ignored) {
            // fallback silencieux sur les versions qui n'exposent pas la methode publique
        }
    }
}
