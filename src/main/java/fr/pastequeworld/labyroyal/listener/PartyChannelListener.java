package fr.pastequeworld.labyroyal.listener;

import fr.pastequeworld.labyroyal.LabyRoyalPlugin;
import fr.pastequeworld.labyroyal.game.LabyGameMode;
import fr.pastequeworld.labyroyal.util.MessageUtil;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Receives plugin messages from BungeeCord PastequeParty plugin.
 * Handles auto-joining party members to the leader's chosen game mode.
 */
public class PartyChannelListener implements PluginMessageListener {

    private final LabyRoyalPlugin plugin;

    public PartyChannelListener(LabyRoyalPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player dummy, byte[] data) {
        if (!"PastequeParty".equals(channel)) return;

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        try {
            String subChannel = in.readUTF();

            if ("PARTY_JOIN".equals(subChannel)) {
                String playerUuidStr = in.readUTF();
                String modeName = in.readUTF();

                final UUID playerUuid = UUID.fromString(playerUuidStr);
                final LabyGameMode gameMode = parseGameMode(modeName);

                if (gameMode == null) {
                    plugin.getLogger().warning("Mode de jeu inconnu reçu du party: " + modeName);
                    return;
                }

                // Schedule on main thread with a small delay to let the player load
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() {
                        Player player = Bukkit.getPlayer(playerUuid);
                        if (player == null || !player.isOnline()) return;

                        // Mark as having chosen so ModeSelectListener doesn't open the GUI
                        plugin.getModeSelectListener().markChosen(playerUuid);

                        // Unfreeze the player (in case they were frozen)
                        player.setWalkSpeed(0.2f);
                        player.setFlySpeed(0.1f);

                        // Auto-join the game
                        MessageUtil.send(player, "&d\u25B6 &7Votre chef de groupe a choisi &e" + gameMode.getDisplayName() + "&7 !");
                        MessageUtil.send(player, "&eRecherche d'une partie " + gameMode.getDisplayName() + "...");

                        boolean joined = plugin.getGameManager().joinGame(player, gameMode);
                        if (joined) {
                            MessageUtil.send(player, "&aVous avez rejoint la partie !");
                        }
                    }
                }, 20L);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Erreur lecture message PastequeParty: " + e.getMessage());
        }
    }

    private LabyGameMode parseGameMode(String name) {
        try {
            return LabyGameMode.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
