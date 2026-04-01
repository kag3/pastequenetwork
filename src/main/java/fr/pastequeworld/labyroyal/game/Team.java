package fr.pastequeworld.labyroyal.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Team {

    private final int id;
    private final List<UUID> members;

    public Team(int id) {
        this.id = id;
        this.members = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public List<UUID> getMembers() {
        return members;
    }

    public void addMember(UUID uuid) {
        members.add(uuid);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public boolean isFull(int maxSize) {
        return members.size() >= maxSize;
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    public int getAliveCount(java.util.Map<UUID, PlayerData> playerDataMap) {
        int count = 0;
        for (UUID uuid : members) {
            PlayerData data = playerDataMap.get(uuid);
            if (data != null && data.isAlive()) {
                count++;
            }
        }
        return count;
    }

    public boolean isEliminated(java.util.Map<UUID, PlayerData> playerDataMap) {
        return getAliveCount(playerDataMap) == 0;
    }

    public List<Player> getOnlinePlayers() {
        List<Player> online = new ArrayList<>();
        for (UUID uuid : members) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                online.add(p);
            }
        }
        return online;
    }
}
