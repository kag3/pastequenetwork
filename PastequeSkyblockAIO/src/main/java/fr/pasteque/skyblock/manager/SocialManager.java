package fr.pasteque.skyblock.manager;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class SocialManager {
    private final DataFile dataFile;
    private final Map<UUID, Set<UUID>> friends = new HashMap<UUID, Set<UUID>>();
    private final Map<UUID, Set<UUID>> enemies = new HashMap<UUID, Set<UUID>>();
    private final Map<UUID, String> alliancesByPlayer = new HashMap<UUID, String>();
    private final Map<String, Set<UUID>> allianceMembers = new HashMap<String, Set<UUID>>();
    private final Map<UUID, String> pendingAlliances = new HashMap<UUID, String>();

    public SocialManager(PastequeSkyblockPlugin plugin) {
        this.dataFile = new DataFile(plugin, "social.yml");
        load();
    }

    public void load() {
        friends.clear(); enemies.clear(); alliancesByPlayer.clear(); allianceMembers.clear(); pendingAlliances.clear();
        ConfigurationSection f = dataFile.getConfig().getConfigurationSection("friends");
        if (f != null) for (String k : f.getKeys(false)) friends.put(UUID.fromString(k), parse(f.getStringList(k)));
        ConfigurationSection e = dataFile.getConfig().getConfigurationSection("enemies");
        if (e != null) for (String k : e.getKeys(false)) enemies.put(UUID.fromString(k), parse(e.getStringList(k)));
        ConfigurationSection a = dataFile.getConfig().getConfigurationSection("alliances");
        if (a != null) for (String name : a.getKeys(false)) {
            Set<UUID> members = parse(a.getStringList(name)); allianceMembers.put(name.toLowerCase(), members);
            for (UUID id : members) alliancesByPlayer.put(id, name.toLowerCase());
        }
        // Load persisted alliance invitations
        ConfigurationSection p = dataFile.getConfig().getConfigurationSection("pending-alliances");
        if (p != null) for (String k : p.getKeys(false)) {
            try {
                pendingAlliances.put(UUID.fromString(k), p.getString(k));
            } catch (Exception ignored) {}
        }
    }

    private Set<UUID> parse(List<String> list) { Set<UUID> out = new HashSet<UUID>(); for (String s : list) try { out.add(UUID.fromString(s)); } catch (Exception ignored) {} return out; }
    private List<String> serialize(Set<UUID> set) { List<String> out = new ArrayList<String>(); for (UUID id : set) out.add(id.toString()); return out; }
    public void save() {
        dataFile.getConfig().set("friends", null); dataFile.getConfig().set("enemies", null); dataFile.getConfig().set("alliances", null); dataFile.getConfig().set("pending-alliances", null);
        for (Map.Entry<UUID, Set<UUID>> e : friends.entrySet()) dataFile.getConfig().set("friends." + e.getKey(), serialize(e.getValue()));
        for (Map.Entry<UUID, Set<UUID>> e : enemies.entrySet()) dataFile.getConfig().set("enemies." + e.getKey(), serialize(e.getValue()));
        for (Map.Entry<String, Set<UUID>> e : allianceMembers.entrySet()) dataFile.getConfig().set("alliances." + e.getKey(), serialize(e.getValue()));
        // Persist pending alliance invitations
        for (Map.Entry<UUID, String> e : pendingAlliances.entrySet()) dataFile.getConfig().set("pending-alliances." + e.getKey().toString(), e.getValue());
        dataFile.save();
    }
    private Set<UUID> bucket(Map<UUID, Set<UUID>> map, UUID who) { if (!map.containsKey(who)) map.put(who, new HashSet<UUID>()); return map.get(who); }
    public boolean addFriend(UUID a, UUID b) { if (a.equals(b)) return false; boolean changed = bucket(friends,a).add(b) | bucket(friends,b).add(a); if (changed) save(); return changed; }
    public boolean removeFriend(UUID a, UUID b) { boolean changed = bucket(friends,a).remove(b) | bucket(friends,b).remove(a); if (changed) save(); return changed; }
    public Set<UUID> getFriends(UUID a) { return new HashSet<UUID>(bucket(friends,a)); }
    public boolean addEnemy(UUID a, UUID b) { if (a.equals(b)) return false; boolean changed = bucket(enemies,a).add(b); if (changed) save(); return changed; }
    public boolean removeEnemy(UUID a, UUID b) { boolean changed = bucket(enemies,a).remove(b); if (changed) save(); return changed; }
    public Set<UUID> getEnemies(UUID a) { return new HashSet<UUID>(bucket(enemies,a)); }
    public String getAlliance(UUID uuid) { return alliancesByPlayer.get(uuid); }
    public Set<UUID> getAllianceMembers(String name) { return name == null || !allianceMembers.containsKey(name.toLowerCase()) ? new HashSet<UUID>() : new HashSet<UUID>(allianceMembers.get(name.toLowerCase())); }
    public boolean createAlliance(UUID owner, String name) { String key = name.toLowerCase(); if (allianceMembers.containsKey(key) || alliancesByPlayer.containsKey(owner)) return false; Set<UUID> members = new HashSet<UUID>(); members.add(owner); allianceMembers.put(key, members); alliancesByPlayer.put(owner, key); save(); return true; }
    public boolean inviteAlliance(UUID target, String name) { if (!allianceMembers.containsKey(name.toLowerCase()) || alliancesByPlayer.containsKey(target)) return false; pendingAlliances.put(target, name.toLowerCase()); save(); return true; }
    public boolean acceptAlliance(UUID target) { String name = pendingAlliances.remove(target); if (name == null || alliancesByPlayer.containsKey(target)) return false; Set<UUID> members = allianceMembers.get(name); if (members == null) return false; members.add(target); alliancesByPlayer.put(target, name); save(); return true; }
    public void denyAlliance(UUID target) { pendingAlliances.remove(target); save(); }
    public boolean leaveAlliance(UUID uuid) { String name = alliancesByPlayer.remove(uuid); if (name == null) return false; Set<UUID> members = allianceMembers.get(name); if (members != null) { members.remove(uuid); if (members.isEmpty()) allianceMembers.remove(name); } save(); return true; }
}
