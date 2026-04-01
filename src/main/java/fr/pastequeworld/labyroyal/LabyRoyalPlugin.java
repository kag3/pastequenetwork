package fr.pastequeworld.labyroyal;

import fr.pastequeworld.labyroyal.command.LabyRoyalCommand;
import fr.pastequeworld.labyroyal.game.GameManager;
import fr.pastequeworld.labyroyal.game.LabyGameMode;
import fr.pastequeworld.labyroyal.listener.AntiCheatListener;
import fr.pastequeworld.labyroyal.listener.ChatListener;
import fr.pastequeworld.labyroyal.listener.GameListener;
import fr.pastequeworld.labyroyal.util.MessageUtil;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LabyRoyalPlugin extends JavaPlugin implements PluginMessageListener {

    private static final String CHANNEL = "LabyRoyal";

    private GameManager gameManager;
    private AntiCheatListener antiCheatListener;

    // Stocke les joueurs en attente de queue automatique (UUID -> mode)
    private final Map<UUID, String> pendingAutoQueue = new ConcurrentHashMap<UUID, String>();

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Set prefix from config
        String prefix = getConfig().getString("general.prefix",
                "&6&l\u2726 &eLabyRoyal &6&l\u2726 &7\u00bb &f");
        MessageUtil.setPrefix(prefix);

        // Initialize managers
        gameManager = new GameManager(this);

        // Register BungeeCord plugin message channel
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);

        // Register listeners
        antiCheatListener = new AntiCheatListener(this);
        Bukkit.getPluginManager().registerEvents(new GameListener(this), this);
        Bukkit.getPluginManager().registerEvents(antiCheatListener, this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);

        // Register commands
        LabyRoyalCommand cmd = new LabyRoyalCommand(this);
        getCommand("labyroyal").setExecutor(cmd);
        getCommand("labyroyal").setTabCompleter(cmd);

        getLogger().info("========================================");
        getLogger().info("  LabyRoyal v" + getDescription().getVersion());
        getLogger().info("  Mode de jeu Battle Royale en Labyrinthe");
        getLogger().info("  PastequeWorld Network");
        getLogger().info("========================================");
        getLogger().info("Plugin active avec succes !");
    }

    @Override
    public void onDisable() {
        // Unregister channels
        getServer().getMessenger().unregisterIncomingPluginChannel(this, CHANNEL);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, CHANNEL);

        // Shutdown all games and cleanup worlds
        if (gameManager != null) {
            getLogger().info("Arret de toutes les parties en cours...");
            gameManager.shutdownAll();
        }

        getLogger().info("LabyRoyal desactive.");
    }

    /**
     * Recoit les plugin messages du BungeeCord bridge.
     * Format : UUID (string) + MODE ("SOLO" ou "DUO")
     */
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL)) return;

        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            String uuidStr = in.readUTF();
            String mode = in.readUTF().toUpperCase();

            UUID targetUuid = UUID.fromString(uuidStr);

            // Chercher le joueur sur le serveur
            Player target = Bukkit.getPlayer(targetUuid);

            if (target != null && target.isOnline()) {
                // Le joueur est deja la, on le queue directement
                autoQueuePlayer(target, mode);
            } else {
                // Le joueur n'est pas encore totalement charge, on stocke pour le PlayerJoinEvent
                pendingAutoQueue.put(targetUuid, mode);
            }

        } catch (IOException e) {
            getLogger().severe("Erreur lecture plugin message LabyRoyal: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            getLogger().severe("UUID invalide dans plugin message: " + e.getMessage());
        }
    }

    /**
     * Appelee depuis le GameListener lors du PlayerJoinEvent
     * pour verifier s'il y a une queue en attente.
     */
    public void checkPendingAutoQueue(Player player) {
        String mode = pendingAutoQueue.remove(player.getUniqueId());
        if (mode != null) {
            // Petit delai pour que le joueur soit completement charge
            Bukkit.getScheduler().runTaskLater(this, new Runnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        autoQueuePlayer(player, mode);
                    }
                }
            }, 10L); // 0.5 seconde
        }
    }

    private void autoQueuePlayer(Player player, String mode) {
        LabyGameMode gameMode;
        if (mode.equals("SOLO")) {
            gameMode = LabyGameMode.SOLO;
        } else if (mode.equals("DUO")) {
            gameMode = LabyGameMode.DUO;
        } else {
            getLogger().warning("Mode de jeu inconnu pour auto-queue: " + mode);
            return;
        }

        MessageUtil.send(player, "&eConnexion automatique en " + gameMode.getDisplayName() + "...");
        boolean joined = gameManager.joinGame(player, gameMode);
        if (joined) {
            MessageUtil.send(player, "&aVous avez rejoint une partie " + gameMode.getDisplayName() + " !");
        }
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public AntiCheatListener getAntiCheatListener() {
        return antiCheatListener;
    }
}
