package fr.pasteque.skyblock.npc;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gere les NPCs natifs (Villager invincible + slow + glowing name).
 * Chaque NPC a un ID, une action (command:<cmd> ou gui:<id>) et une position.
 * Au click droit, l'action s'execute. Invisible pour la corruption: tag special
 * dans le customName et re-spawn au chunk load. Persiste dans npcs.yml.
 */
@SuppressWarnings("deprecation")
public class NpcManager {

    public static final String NPC_TAG = "\u00A7r\u00A70\u00A7npc\u00A7r";

    private final PastequeSkyblockPlugin plugin;
    private final File file;
    private final Map<String, NpcData> npcs = new HashMap<String, NpcData>();

    public NpcManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "npcs.yml");
    }

    public void load() {
        npcs.clear();
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = cfg.getConfigurationSection("npcs");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection n = sec.getConfigurationSection(id);
            if (n == null) continue;
            String worldName = n.getString("world");
            double x = n.getDouble("x");
            double y = n.getDouble("y");
            double z = n.getDouble("z");
            float yaw = (float) n.getDouble("yaw", 0.0);
            String name = n.getString("name", "&aNPC");
            String action = n.getString("action", "");
            World w = Bukkit.getWorld(worldName);
            if (w == null) continue;
            Location loc = new Location(w, x, y, z, yaw, 0f);
            NpcData data = new NpcData(id, loc, name, action);
            npcs.put(id, data);
        }
        // Spawn all NPCs after load (delayed 1 tick so worlds are ready)
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override public void run() { respawnAll(); }
        }, 20L);
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (NpcData data : npcs.values()) {
            String base = "npcs." + data.id;
            Location loc = data.location;
            cfg.set(base + ".world", loc.getWorld().getName());
            cfg.set(base + ".x", loc.getX());
            cfg.set(base + ".y", loc.getY());
            cfg.set(base + ".z", loc.getZ());
            cfg.set(base + ".yaw", loc.getYaw());
            cfg.set(base + ".name", data.name);
            cfg.set(base + ".action", data.action);
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("[NPC] Impossible de sauvegarder npcs.yml: " + e.getMessage());
        }
    }

    /**
     * Cree un NPC a la position donnee.
     * action: "command:<cmd_sans_slash>" ou "gui:<menu_id>"
     */
    public boolean createNpc(String id, Location location, String name, String action) {
        if (npcs.containsKey(id)) return false;
        NpcData data = new NpcData(id, location, name, action);
        npcs.put(id, data);
        spawnEntity(data);
        save();
        return true;
    }

    public boolean removeNpc(String id) {
        NpcData data = npcs.remove(id);
        if (data == null) return false;
        despawnEntity(data);
        save();
        return true;
    }

    public NpcData getNpc(String id) { return npcs.get(id); }
    public Map<String, NpcData> getAll() { return npcs; }

    /**
     * Appele quand un joueur interagit avec un Villager: retourne l'action a executer si c'est un NPC.
     */
    public String findActionByEntity(Entity entity) {
        if (!(entity instanceof Villager)) return null;
        Villager v = (Villager) entity;
        if (v.getCustomName() == null || !v.getCustomName().contains(NPC_TAG)) return null;
        // Match by location (closest NPC within 2 blocks)
        for (NpcData data : npcs.values()) {
            if (!data.location.getWorld().equals(v.getWorld())) continue;
            double dx = v.getLocation().getX() - data.location.getX();
            double dy = v.getLocation().getY() - data.location.getY();
            double dz = v.getLocation().getZ() - data.location.getZ();
            if (dx * dx + dy * dy + dz * dz < 4.0) {
                return data.action;
            }
        }
        return null;
    }

    public boolean isNpc(Entity entity) {
        if (!(entity instanceof Villager)) return false;
        Villager v = (Villager) entity;
        return v.getCustomName() != null && v.getCustomName().contains(NPC_TAG);
    }

    public void respawnAll() {
        for (NpcData data : npcs.values()) {
            // Only if chunk is loaded
            if (data.location.getWorld() != null
                    && data.location.getWorld().isChunkLoaded(data.location.getBlockX() >> 4, data.location.getBlockZ() >> 4)) {
                spawnEntity(data);
            }
        }
    }

    public void respawnInChunk(org.bukkit.Chunk chunk) {
        for (NpcData data : npcs.values()) {
            if (!data.location.getWorld().equals(chunk.getWorld())) continue;
            int cx = data.location.getBlockX() >> 4;
            int cz = data.location.getBlockZ() >> 4;
            if (cx == chunk.getX() && cz == chunk.getZ()) {
                spawnEntity(data);
            }
        }
    }

    private void spawnEntity(final NpcData data) {
        // Remove any existing entity with our tag near the spot first
        despawnEntity(data);
        Location loc = data.location;
        if (loc.getWorld() == null) return;
        Villager v = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
        v.setCustomNameVisible(true);
        v.setCustomName(PastequeSkyblockPlugin.color(data.name) + NPC_TAG);
        v.setProfession(Villager.Profession.LIBRARIAN);
        v.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 255));
        v.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 250));
        v.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 255));
        // Keep facing the configured yaw
        new BukkitRunnable() {
            @Override public void run() {
                if (v.isValid() && !v.isDead()) {
                    Location fix = data.location.clone();
                    v.teleport(fix);
                }
            }
        }.runTaskLater(plugin, 2L);
    }

    private void despawnEntity(NpcData data) {
        World w = data.location.getWorld();
        if (w == null) return;
        int cx = data.location.getBlockX() >> 4;
        int cz = data.location.getBlockZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!w.isChunkLoaded(cx + dx, cz + dz)) continue;
                for (Entity e : w.getChunkAt(cx + dx, cz + dz).getEntities()) {
                    if (!(e instanceof Villager)) continue;
                    Villager v = (Villager) e;
                    if (v.getCustomName() == null || !v.getCustomName().contains(NPC_TAG)) continue;
                    if (v.getLocation().distanceSquared(data.location) > 4.0) continue;
                    v.remove();
                }
            }
        }
    }

    public static class NpcData {
        public final String id;
        public final Location location;
        public final String name;
        public final String action;

        public NpcData(String id, Location location, String name, String action) {
            this.id = id;
            this.location = location;
            this.name = name;
            this.action = action;
        }
    }
}
