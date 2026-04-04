package fr.pasteque.skyblockarena.service;

import fr.pasteque.skyblockarena.PastequeSkyBlockArenaPlugin;
import fr.pasteque.skyblockarena.model.SafeZone;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class SafeZoneService {

    private final PastequeSkyBlockArenaPlugin plugin;
    private final ArenaWorldService arenaWorldService;
    private final Map<String, SafeZone> zones = new LinkedHashMap<String, SafeZone>();
    private final File file;
    private YamlConfiguration data;

    public SafeZoneService(PastequeSkyBlockArenaPlugin plugin, ArenaWorldService arenaWorldService) {
        this.plugin = plugin;
        this.arenaWorldService = arenaWorldService;
        this.file = new File(plugin.getDataFolder(), "safezones.yml");
    }

    public void load() {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ignored) {
            }
        }
        data = YamlConfiguration.loadConfiguration(file);
        zones.clear();
        ConfigurationSection section = data.getConfigurationSection("zones");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection zoneSection = section.getConfigurationSection(key);
            if (zoneSection == null) {
                continue;
            }
            SafeZone zone = new SafeZone(
                    key,
                    zoneSection.getInt("minX"),
                    zoneSection.getInt("minY"),
                    zoneSection.getInt("minZ"),
                    zoneSection.getInt("maxX"),
                    zoneSection.getInt("maxY"),
                    zoneSection.getInt("maxZ")
            );
            zones.put(key.toLowerCase(), zone);
        }
    }

    public void save() {
        if (data == null) {
            data = new YamlConfiguration();
        }
        data.set("zones", null);
        for (SafeZone zone : zones.values()) {
            String path = "zones." + zone.getName();
            data.set(path + ".minX", zone.getMinX());
            data.set(path + ".minY", zone.getMinY());
            data.set(path + ".minZ", zone.getMinZ());
            data.set(path + ".maxX", zone.getMaxX());
            data.set(path + ".maxY", zone.getMaxY());
            data.set(path + ".maxZ", zone.getMaxZ());
        }
        try {
            data.save(file);
        } catch (IOException ignored) {
        }
    }

    public void create(String name, Location pos1, Location pos2) {
        SafeZone zone = new SafeZone(name.toLowerCase(), pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ(), pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ());
        zones.put(name.toLowerCase(), zone);
        save();
    }

    public void delete(String name) {
        zones.remove(name.toLowerCase());
        save();
    }

    public boolean isSafe(Location location) {
        if (location == null || !arenaWorldService.isArenaWorld(location.getWorld())) {
            return false;
        }
        for (SafeZone zone : zones.values()) {
            if (zone.contains(location)) {
                return true;
            }
        }
        return false;
    }

    public Collection<SafeZone> getZones() {
        return Collections.unmodifiableCollection(zones.values());
    }
}
