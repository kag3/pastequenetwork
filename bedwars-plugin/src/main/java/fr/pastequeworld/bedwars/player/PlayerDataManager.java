package fr.pastequeworld.bedwars.player;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registre des {@link BedWarsPlayer} par UUID.
 * Instanciation a la connexion, suppression a la deconnexion.
 */
public class PlayerDataManager {

    @SuppressWarnings("unused")
    private final BedWarsPlugin plugin;

    private final ConcurrentHashMap<UUID, BedWarsPlayer> players = new ConcurrentHashMap<UUID, BedWarsPlayer>();

    public PlayerDataManager(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public BedWarsPlayer register(Player player) {
        BedWarsPlayer existing = players.get(player.getUniqueId());
        if (existing != null) return existing;
        BedWarsPlayer created = new BedWarsPlayer(player);
        players.put(player.getUniqueId(), created);
        return created;
    }

    public void unregister(UUID uuid) {
        players.remove(uuid);
    }

    public BedWarsPlayer get(Player player) {
        return player == null ? null : players.get(player.getUniqueId());
    }

    public BedWarsPlayer get(UUID uuid) {
        return uuid == null ? null : players.get(uuid);
    }

    public Collection<BedWarsPlayer> all() {
        return players.values();
    }
}
