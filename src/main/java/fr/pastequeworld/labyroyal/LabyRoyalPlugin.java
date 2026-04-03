package fr.pastequeworld.labyroyal;

import fr.pastequeworld.labyroyal.arena.SelectRoomBuilder;
import fr.pastequeworld.labyroyal.command.LabyRoyalCommand;
import fr.pastequeworld.labyroyal.game.GameManager;
import fr.pastequeworld.labyroyal.listener.AntiCheatListener;
import fr.pastequeworld.labyroyal.listener.ChatListener;
import fr.pastequeworld.labyroyal.listener.CombatListener;
import fr.pastequeworld.labyroyal.listener.GameListener;
import fr.pastequeworld.labyroyal.listener.ModeSelectListener;
import fr.pastequeworld.labyroyal.listener.PartyChannelListener;
import fr.pastequeworld.labyroyal.util.MessageUtil;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class LabyRoyalPlugin extends JavaPlugin {

    private GameManager gameManager;
    private AntiCheatListener antiCheatListener;
    private ModeSelectListener modeSelectListener;
    private CombatListener combatListener;
    private PartyChannelListener partyChannelListener;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Set prefix from config
        String prefix = getConfig().getString("general.prefix",
                "&6&l\u2726 &eLabyRoyale &6&l\u2726 &7\u00bb &f");
        MessageUtil.setPrefix(prefix);

        // Register BungeeCord channel for server transfers
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // Register PastequeParty channel for party integration
        partyChannelListener = new PartyChannelListener(this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "PastequeParty");
        getServer().getMessenger().registerIncomingPluginChannel(this, "PastequeParty", partyChannelListener);

        // Construire la cabane de selection dans le monde par defaut
        org.bukkit.World defaultWorld = Bukkit.getWorlds().get(0);
        if (defaultWorld != null) {
            defaultWorld.setGameRuleValue("doFireTick", "false");
            SelectRoomBuilder.buildIfNeeded(defaultWorld);
            getLogger().info("Cabane de selection construite dans " + defaultWorld.getName());
        }

        // Initialize managers
        gameManager = new GameManager(this);

        // Register listeners
        antiCheatListener = new AntiCheatListener(this);
        modeSelectListener = new ModeSelectListener(this);
        combatListener = new CombatListener(this);
        Bukkit.getPluginManager().registerEvents(new GameListener(this), this);
        Bukkit.getPluginManager().registerEvents(antiCheatListener, this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
        Bukkit.getPluginManager().registerEvents(modeSelectListener, this);
        Bukkit.getPluginManager().registerEvents(combatListener, this);

        // Register commands
        LabyRoyalCommand cmd = new LabyRoyalCommand(this);
        getCommand("labyroyale").setExecutor(cmd);
        getCommand("labyroyale").setTabCompleter(cmd);

        getLogger().info("========================================");
        getLogger().info("  LabyRoyale v" + getDescription().getVersion());
        getLogger().info("  Mode de jeu Battle Royale en Labyrinthe");
        getLogger().info("  PastequeWorld Network");
        getLogger().info("========================================");
        getLogger().info("Plugin active avec succes !");
    }

    @Override
    public void onDisable() {
        // Shutdown all games and cleanup worlds
        if (gameManager != null) {
            getLogger().info("Arret de toutes les parties en cours...");
            gameManager.shutdownAll();
        }

        getLogger().info("LabyRoyale desactive.");
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public AntiCheatListener getAntiCheatListener() {
        return antiCheatListener;
    }

    public ModeSelectListener getModeSelectListener() {
        return modeSelectListener;
    }

    public PartyChannelListener getPartyChannelListener() {
        return partyChannelListener;
    }

    /**
     * Envoie un joueur vers le serveur hub via BungeeCord.
     */
    public void sendToHub(Player player) {
        String hubServer = getConfig().getString("general.hub-server", "hub");
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            out.writeUTF("Connect");
            out.writeUTF(hubServer);
            player.sendPluginMessage(this, "BungeeCord", b.toByteArray());
        } catch (IOException e) {
            getLogger().severe("Erreur envoi vers le hub: " + e.getMessage());
            player.kickPlayer(MessageUtil.color("&cImpossible de vous renvoyer au hub."));
        }
    }
}
