package fr.pasteque.skyblock.manager;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.model.PvpZone;
import fr.pasteque.skyblock.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PvpManager {
    private final PastequeSkyblockPlugin plugin;
    private final DataFile dataFile;
    private final Map<String, PvpZone> zones = new HashMap<String, PvpZone>();
    private final Map<String, Location> namedWarps = new HashMap<String, Location>();
    private Location hardcoreWarp;

    public PvpManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new DataFile(plugin, "pvp.yml");
        load();
    }

    public void load() {
        zones.clear();
        namedWarps.clear();
        ConfigurationSection section = dataFile.getConfig().getConfigurationSection("zones");
        if (section != null) for (String key : section.getKeys(false)) {
            PvpZone zone = PvpZone.load(key, section.getConfigurationSection(key));
            if (zone != null) zones.put(key.toLowerCase(), zone);
        }
        hardcoreWarp = LocationUtil.load(dataFile.getConfig(), "hardcore-warp");
        ConfigurationSection warps = dataFile.getConfig().getConfigurationSection("named-warps");
        if (warps != null) {
            for (String key : warps.getKeys(false)) {
                Location loc = LocationUtil.load(warps, key);
                if (loc != null) namedWarps.put(key.toLowerCase(), loc);
            }
        }
    }

    public void save() {
        dataFile.getConfig().set("zones", null);
        for (Map.Entry<String, PvpZone> entry : zones.entrySet()) {
            ConfigurationSection section = dataFile.getConfig().createSection("zones." + entry.getKey());
            entry.getValue().save(section);
        }
        LocationUtil.save(dataFile.getConfig(), "hardcore-warp", hardcoreWarp);
        dataFile.getConfig().set("named-warps", null);
        ConfigurationSection named = dataFile.getConfig().createSection("named-warps");
        for (Map.Entry<String, Location> entry : namedWarps.entrySet()) {
            LocationUtil.save(named, entry.getKey(), entry.getValue());
        }
        dataFile.save();
    }

    public void createZone(String name, Location a, Location b) { zones.put(name.toLowerCase(), new PvpZone(name, a.getWorld().getName(), a, b)); save(); }
    public boolean deleteZone(String name) { boolean removed = zones.remove(name.toLowerCase()) != null; if (removed) save(); return removed; }
    public boolean isPvp(Location location) { for (PvpZone zone : zones.values()) if (zone.contains(location)) return true; return false; }
    public Collection<PvpZone> getZones() { return zones.values(); }
    public Location getHardcoreWarp() { return hardcoreWarp; }
    public void setHardcoreWarp(Location hardcoreWarp) { this.hardcoreWarp = hardcoreWarp; save(); }
    public Location getNamedWarp(String id) { return namedWarps.get(id.toLowerCase()); }
    public void setNamedWarp(String id, Location location) { namedWarps.put(id.toLowerCase(), location); save(); }
}
