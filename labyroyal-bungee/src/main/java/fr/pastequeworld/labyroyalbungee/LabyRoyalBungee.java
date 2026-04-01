package fr.pastequeworld.labyroyalbungee;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Plugin BungeeCord qui fait le pont entre le Hub et le serveur LabyRoyale.
 *
 * Flux :
 * 1. Le Hub envoie un plugin message sur le channel "LabyRoyale" avec "SOLO" ou "DUO"
 * 2. Ce plugin intercepte, stocke le mode, et connecte le joueur au serveur LabyRoyale
 * 3. Une fois connecte, il forward le mode au serveur LabyRoyale via plugin message
 * 4. Le plugin Spigot LabyRoyale recoit et auto-queue le joueur
 */
public class LabyRoyalBungee extends Plugin implements Listener {

    // Channel utilise pour la communication Hub -> Bungee et Bungee -> Spigot
    private static final String CHANNEL = "LabyRoyale";

    // Nom du serveur LabyRoyale dans la config BungeeCord (config.yml de bungee)
    private static final String GAME_SERVER = "labyroyale";

    // Stocke le mode en attente : UUID -> "SOLO" ou "DUO"
    private final Map<UUID, String> pendingQueue = new ConcurrentHashMap<UUID, String>();

    @Override
    public void onEnable() {
        // Enregistrer le channel des deux cotes
        getProxy().registerChannel(CHANNEL);
        getProxy().getPluginManager().registerListener(this, this);

        getLogger().info("LabyRoyaleBungee active !");
        getLogger().info("Channel: " + CHANNEL + " | Serveur cible: " + GAME_SERVER);
    }

    @Override
    public void onDisable() {
        getProxy().unregisterChannel(CHANNEL);
        pendingQueue.clear();
        getLogger().info("LabyRoyaleBungee desactive.");
    }

    /**
     * Intercepte les plugin messages venant du Hub.
     * Format attendu : un DataOutputStream avec un seul UTF = "SOLO" ou "DUO"
     */
    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equals(CHANNEL)) return;

        // Verifier que ca vient d'un serveur (pas d'un joueur qui spoof)
        if (!(event.getSender() instanceof Server)) return;

        // Verifier que le receveur est un joueur
        if (!(event.getReceiver() instanceof ProxiedPlayer)) return;

        ProxiedPlayer player = (ProxiedPlayer) event.getReceiver();

        // Empecher le message de continuer vers le serveur actuel
        event.setCancelled(true);

        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()));
            String mode = in.readUTF().toUpperCase();

            if (!mode.equals("SOLO") && !mode.equals("DUO")) {
                getLogger().warning("Mode invalide recu pour " + player.getName() + ": " + mode);
                return;
            }

            // Stocker le mode en attente
            pendingQueue.put(player.getUniqueId(), mode);

            // Connecter le joueur au serveur LabyRoyale
            ServerInfo server = getProxy().getServerInfo(GAME_SERVER);
            if (server == null) {
                getLogger().severe("Serveur '" + GAME_SERVER + "' introuvable dans la config BungeeCord !");
                player.sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                        "\u00a7c\u00a7lErreur: \u00a77Le serveur LabyRoyale est indisponible."));
                pendingQueue.remove(player.getUniqueId());
                return;
            }

            player.connect(server);

            // Timeout : nettoyer si le joueur ne se connecte jamais (10 secondes)
            getProxy().getScheduler().schedule(this, new Runnable() {
                @Override
                public void run() {
                    pendingQueue.remove(player.getUniqueId());
                }
            }, 10, TimeUnit.SECONDS);

        } catch (IOException e) {
            getLogger().severe("Erreur lecture plugin message: " + e.getMessage());
        }
    }

    /**
     * Quand le joueur arrive sur le serveur LabyRoyale,
     * on forward le mode de jeu au serveur Spigot.
     */
    @EventHandler
    public void onServerConnected(ServerConnectedEvent event) {
        ProxiedPlayer player = event.getPlayer();
        String mode = pendingQueue.remove(player.getUniqueId());

        if (mode == null) return;

        // Verifier que le joueur arrive bien sur le serveur LabyRoyale
        if (!event.getServer().getInfo().getName().equalsIgnoreCase(GAME_SERVER)) return;

        // Envoyer le mode au serveur Spigot via plugin message
        // Petit delai pour s'assurer que le joueur est bien charge cote Spigot
        final Server server = event.getServer();
        getProxy().getScheduler().schedule(this, new Runnable() {
            @Override
            public void run() {
                try {
                    ByteArrayOutputStream b = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(b);
                    out.writeUTF(player.getUniqueId().toString());
                    out.writeUTF(mode);
                    server.sendData(CHANNEL, b.toByteArray());
                } catch (IOException e) {
                    getLogger().severe("Erreur envoi mode a LabyRoyale: " + e.getMessage());
                }
            }
        }, 500, TimeUnit.MILLISECONDS);
    }
}
