package fr.pastequeworld.labyroyal.game;

import fr.pastequeworld.labyroyal.LabyRoyalPlugin;
import fr.pastequeworld.labyroyal.util.MessageUtil;

import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GameManager {

    private final LabyRoyalPlugin plugin;
    private final Map<String, Game> games = new LinkedHashMap<>();
    private final Map<UUID, String> playerGameMap = new HashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public GameManager(LabyRoyalPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean joinGame(Player player, LabyGameMode mode) {
        // Check if already in a game
        if (playerGameMap.containsKey(player.getUniqueId())) {
            MessageUtil.send(player, "&cVous \u00eates d\u00e9j\u00e0 dans une partie !");
            return false;
        }

        // Find available game
        Game game = findAvailableGame(mode);

        if (game == null) {
            // Create new game
            game = createGame(mode);
            if (game == null) {
                MessageUtil.send(player, "&cErreur lors de la cr\u00e9ation de la partie. R\u00e9essayez.");
                return false;
            }
        }

        // Add player to game
        if (game.addPlayer(player)) {
            playerGameMap.put(player.getUniqueId(), game.getId());
            return true;
        }

        MessageUtil.send(player, "&cImpossible de rejoindre la partie.");
        return false;
    }

    public void leaveGame(Player player) {
        String gameId = playerGameMap.get(player.getUniqueId());
        if (gameId == null) {
            MessageUtil.send(player, "&cVous n'\u00eates dans aucune partie !");
            return;
        }

        Game game = games.get(gameId);
        if (game != null) {
            game.removePlayer(player);
        }
        playerGameMap.remove(player.getUniqueId());
        MessageUtil.send(player, "&aVous avez quitt\u00e9 la partie.");
    }

    private Game findAvailableGame(LabyGameMode mode) {
        for (Game game : games.values()) {
            if (game.getGameMode() == mode && game.canJoin()) {
                return game;
            }
        }
        return null;
    }

    private Game createGame(LabyGameMode mode) {
        String id = String.valueOf(nextId.getAndIncrement());
        Game game = new Game(id, mode, plugin);

        // Add to map BEFORE world creation so concurrent joins find this game
        games.put(id, game);

        plugin.getLogger().info("Cr\u00e9ation du monde LabyRoyale #" + id + " (" + mode.getDisplayName() + ")...");

        if (!game.createWorld()) {
            plugin.getLogger().severe("\u00c9chec de la cr\u00e9ation du monde pour la partie #" + id);
            games.remove(id);
            return null;
        }

        plugin.getLogger().info("Partie LabyRoyale #" + id + " cr\u00e9\u00e9e avec succ\u00e8s !");
        return game;
    }

    public void removeGame(Game game) {
        // Remove all player mappings
        for (UUID uuid : game.getPlayers().keySet()) {
            playerGameMap.remove(uuid);
        }
        games.remove(game.getId());
        game.cleanup();
        plugin.getLogger().info("Partie LabyRoyale #" + game.getId() + " supprim\u00e9e.");
    }

    public Game getPlayerGame(UUID uuid) {
        String gameId = playerGameMap.get(uuid);
        if (gameId != null) {
            return games.get(gameId);
        }
        return null;
    }

    public void removePlayerMapping(UUID uuid) {
        playerGameMap.remove(uuid);
    }

    public void shutdownAll() {
        for (Game game : new ArrayList<>(games.values())) {
            game.cleanup();
        }
        games.clear();
        playerGameMap.clear();
    }

    public Collection<Game> getGames() {
        return games.values();
    }

    public int getActiveGameCount() {
        return games.size();
    }

    public int getTotalPlayers() {
        return playerGameMap.size();
    }
}
