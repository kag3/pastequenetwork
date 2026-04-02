package fr.pastequeworld.friends.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import fr.pastequeworld.friends.PastequeFriends;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FriendManager {

    private final PastequeFriends plugin;
    private final File dataFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // UUID -> FriendData
    private final Map<String, FriendData> playerData = new ConcurrentHashMap<String, FriendData>();

    // Name -> UUID cache (lowercase name keys)
    private final Map<String, String> nameToUuid = new ConcurrentHashMap<String, String>();

    // UUID -> Name cache
    private final Map<String, String> uuidToName = new ConcurrentHashMap<String, String>();

    public FriendManager(PastequeFriends plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "friends.json");
    }

    // ==================== PERSISTENCE ====================

    public void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!dataFile.exists()) {
            playerData.clear();
            return;
        }

        try {
            Reader reader = new FileReader(dataFile);
            Type type = new TypeToken<Map<String, FriendData>>() {}.getType();
            Map<String, FriendData> loaded = gson.fromJson(reader, type);
            reader.close();

            if (loaded != null) {
                playerData.putAll(loaded);

                // Rebuild name cache
                for (Map.Entry<String, FriendData> entry : playerData.entrySet()) {
                    String uuid = entry.getKey();
                    String name = entry.getValue().getLastKnownName();
                    if (name != null) {
                        nameToUuid.put(name.toLowerCase(), uuid);
                        uuidToName.put(uuid, name);
                    }
                }
            }

            plugin.getLogger().info("Chargé " + playerData.size() + " profils d'amis.");
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur chargement friends.json: " + e.getMessage());
        }
    }

    public void save() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            Writer writer = new FileWriter(dataFile);
            gson.toJson(playerData, writer);
            writer.close();
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur sauvegarde friends.json: " + e.getMessage());
        }
    }

    public void saveAsync() {
        plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
            @Override
            public void run() {
                save();
            }
        });
    }

    // ==================== PLAYER TRACKING ====================

    public void updatePlayerName(UUID uuid, String name) {
        String uuidStr = uuid.toString();

        // Remove old name mapping
        String oldName = uuidToName.get(uuidStr);
        if (oldName != null) {
            nameToUuid.remove(oldName.toLowerCase());
        }

        nameToUuid.put(name.toLowerCase(), uuidStr);
        uuidToName.put(uuidStr, name);

        FriendData data = getOrCreate(uuidStr);
        data.setLastKnownName(name);
    }

    public String getUuidByName(String name) {
        return nameToUuid.get(name.toLowerCase());
    }

    public String getNameByUuid(String uuid) {
        return uuidToName.get(uuid);
    }

    // ==================== FRIEND DATA ====================

    public FriendData getOrCreate(String uuid) {
        FriendData data = playerData.get(uuid);
        if (data == null) {
            data = new FriendData();
            playerData.put(uuid, data);
        }
        return data;
    }

    public FriendData getData(String uuid) {
        return playerData.get(uuid);
    }

    // ==================== FRIEND OPERATIONS ====================

    /**
     * Send a friend request from one player to another.
     * Returns: 0 = success, 1 = already friends, 2 = already pending, 3 = request from target exists
     */
    public int sendRequest(String fromUuid, String toUuid) {
        FriendData fromData = getOrCreate(fromUuid);
        FriendData toData = getOrCreate(toUuid);

        if (fromData.getFriends().contains(toUuid)) {
            return 1; // Already friends
        }

        if (toData.getPendingRequests().contains(fromUuid)) {
            return 2; // Already sent
        }

        // If the target already sent US a request, auto-accept
        if (fromData.getPendingRequests().contains(toUuid)) {
            return 3; // Should accept instead
        }

        toData.getPendingRequests().add(fromUuid);
        saveAsync();
        return 0;
    }

    /**
     * Accept a friend request.
     * Returns true if successful.
     */
    public boolean acceptRequest(String playerUuid, String fromUuid) {
        FriendData playerData = getOrCreate(playerUuid);
        FriendData fromData = getOrCreate(fromUuid);

        if (!playerData.getPendingRequests().contains(fromUuid)) {
            return false;
        }

        playerData.getPendingRequests().remove(fromUuid);
        playerData.getFriends().add(fromUuid);
        fromData.getFriends().add(playerUuid);

        saveAsync();
        return true;
    }

    /**
     * Deny a friend request.
     */
    public boolean denyRequest(String playerUuid, String fromUuid) {
        FriendData data = getOrCreate(playerUuid);
        boolean removed = data.getPendingRequests().remove(fromUuid);
        if (removed) {
            saveAsync();
        }
        return removed;
    }

    /**
     * Remove a friend (mutual).
     */
    public boolean removeFriend(String playerUuid, String friendUuid) {
        FriendData playerData = getOrCreate(playerUuid);
        FriendData friendData = getOrCreate(friendUuid);

        boolean removed = playerData.getFriends().remove(friendUuid);
        friendData.getFriends().remove(playerUuid);

        if (removed) {
            saveAsync();
        }
        return removed;
    }

    /**
     * Check if two players are friends.
     */
    public boolean areFriends(String uuid1, String uuid2) {
        FriendData data = playerData.get(uuid1);
        return data != null && data.getFriends().contains(uuid2);
    }

    /**
     * Get the number of friends a player has.
     */
    public int getFriendCount(String uuid) {
        FriendData data = playerData.get(uuid);
        return data == null ? 0 : data.getFriends().size();
    }

    public static final int MAX_FRIENDS = 50;
}
