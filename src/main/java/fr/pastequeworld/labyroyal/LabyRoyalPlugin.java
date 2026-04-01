package fr.pastequeworld.labyroyal;

import fr.pastequeworld.labyroyal.command.LabyRoyalCommand;
import fr.pastequeworld.labyroyal.game.GameManager;
import fr.pastequeworld.labyroyal.listener.AntiCheatListener;
import fr.pastequeworld.labyroyal.listener.ChatListener;
import fr.pastequeworld.labyroyal.listener.GameListener;
import fr.pastequeworld.labyroyal.util.MessageUtil;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class LabyRoyalPlugin extends JavaPlugin {

    private GameManager gameManager;
    private AntiCheatListener antiCheatListener;

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
        // Shutdown all games and cleanup worlds
        if (gameManager != null) {
            getLogger().info("Arret de toutes les parties en cours...");
            gameManager.shutdownAll();
        }

        getLogger().info("LabyRoyal desactive.");
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public AntiCheatListener getAntiCheatListener() {
        return antiCheatListener;
    }
}
