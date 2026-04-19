package fr.pastequeworld.bedwars.game;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.config.ConfigManager;
import fr.pastequeworld.bedwars.generator.Generator;
import fr.pastequeworld.bedwars.generator.GeneratorType;
import fr.pastequeworld.bedwars.map.MapAutoDetector;
import fr.pastequeworld.bedwars.map.MapTemplate;
import fr.pastequeworld.bedwars.map.SchematicLoader;
import fr.pastequeworld.bedwars.map.WorldManager;
import fr.pastequeworld.bedwars.map.WorldTemplateLoader;
import fr.pastequeworld.bedwars.shop.ShopVillager;
import fr.pastequeworld.bedwars.team.Team;
import fr.pastequeworld.bedwars.ui.EventsTimelineManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pool et cycle de vie des {@link Arena}s.
 *
 * Flux :
 *   - un joueur demande a rejoindre un mode
 *   - on cherche une arene WAITING disponible
 *   - sinon on en cree une nouvelle (nouveau monde + paste de schematic)
 *   - on l'associe au joueur
 *
 * Le reset d'arene se fait par destruction pure : unload + delete du monde.
 * C'est plus fiable que de repasser la schematic sur un monde "sale".
 */
public class ArenaManager {

    private final BedWarsPlugin plugin;
    private final WorldManager worldManager;
    private final SchematicLoader schematicLoader;
    private final MapAutoDetector autoDetector;
    private final EventsTimelineManager eventsTimelineManager;

    private final Map<String, Arena> arenas = new ConcurrentHashMap<String, Arena>();
    private final AtomicInteger idCounter = new AtomicInteger(1);

    public ArenaManager(BedWarsPlugin plugin) {
        this.plugin = plugin;
        this.worldManager = new WorldManager(plugin);
        this.schematicLoader = new SchematicLoader(plugin);
        this.autoDetector = new MapAutoDetector(plugin);
        this.eventsTimelineManager = new EventsTimelineManager(plugin);
    }

    public Arena findOrCreate(GameMode mode) {
        Arena waiting = findWaiting(mode);
        if (waiting != null) return waiting;
        return create(mode);
    }

    public Arena findWaiting(GameMode mode) {
        ConfigManager.ModeDefinition def = plugin.getConfigManager().getMode(mode);
        int maxPlayers = def.getMaxPlayers();
        Arena best = null;
        int bestSize = -1;
        for (Arena arena : arenas.values()) {
            if (arena.getMode() != mode) continue;
            if (arena.getState() != GameState.WAITING && arena.getState() != GameState.STARTING) continue;
            int size = arena.getPlayers().size();
            if (size >= maxPlayers) continue;
            if (size > bestSize) {
                bestSize = size;
                best = arena;
            }
        }
        return best;
    }

    public Arena create(GameMode mode) {
        MapTemplate template = plugin.getMapRegistry().pickRandomForMode(mode);
        if (template == null) {
            plugin.getLogger().warning("Aucune map disponible pour le mode " + mode);
            return null;
        }

        String arenaId = "bw_" + mode.getConfigKey() + "_" + idCounter.getAndIncrement();
        World world;
        Location worldCorner;

        if (template.isWorldFolderBased()) {
            WorldTemplateLoader wtl = plugin.getMapRegistry().getWorldTemplateLoader();
            world = wtl.copyAndLoad(template.getWorldTemplate(), arenaId);
            if (world == null) {
                plugin.getLogger().warning("Impossible de charger le monde template " + template.getWorldTemplate());
                return null;
            }
            // Les positions detectees sont ABSOLUES dans le monde copie
            worldCorner = new Location(world, 0, 0, 0);

            if (template.isAutoDetect()) {
                autoDetector.detect(world, template, new Location(world, 0, 64, 0), 200);
            }
        } else {
            world = worldManager.createArenaWorld(arenaId);
            if (world == null) {
                plugin.getLogger().warning("Impossible de creer le monde d'arene " + arenaId);
                return null;
            }
            Vector offset = template.getPasteOffset();
            Location pasteTo = new Location(world, offset.getX(), offset.getY(), offset.getZ());
            worldCorner = pasteTo.clone();

            File schematicFile = schematicLoader.resolveSchematic(template.getSchematic());
            if (schematicFile.exists()) {
                boolean ok = schematicLoader.paste(schematicFile, pasteTo);
                if (!ok) {
                    plugin.getLogger().warning("Echec paste schematic " + template.getSchematic());
                }
            } else {
                plugin.getLogger().warning("Schematic manquante (l'arene sera vide): " + schematicFile.getAbsolutePath());
            }
        }

        Arena arena = new Arena(plugin, arenaId, mode, template, world, worldCorner);
        arenas.put(arenaId, arena);
        plugin.getLogger().info("Arene creee: " + arenaId + " (map: " + template.getId() + ")");
        return arena;
    }

    public void destroy(Arena arena) {
        arena.setState(GameState.RESETTING);
        eventsTimelineManager.cleanup(arena);
        for (Generator g : new ArrayList<Generator>(arena.getGenerators())) g.stop();
        arena.getGenerators().clear();
        for (ShopVillager s : new ArrayList<ShopVillager>(arena.getShops())) s.despawn();
        arena.getShops().clear();

        arenas.remove(arena.getId());
        worldManager.unloadAndDelete(arena.getWorld());
    }

    public void shutdownAll() {
        for (Arena arena : new ArrayList<Arena>(arenas.values())) destroy(arena);
    }

    // === Helpers invoques par Arena ===

    public void spawnGenerators(Arena arena) {
        for (Team team : arena.getTeams()) {
            if (team.getIronGeneratorLocation() != null) {
                arena.getGenerators().add(new Generator(plugin, arena, GeneratorType.IRON,
                        team.getIronGeneratorLocation(), team));
            }
            if (team.getGoldGeneratorLocation() != null) {
                arena.getGenerators().add(new Generator(plugin, arena, GeneratorType.GOLD,
                        team.getGoldGeneratorLocation(), team));
            }
        }
        // Diamants / Emeraudes
        for (Vector v : arena.getTemplate().getDiamondGenLocations()) {
            Location loc = new Location(arena.getWorld(),
                    arena.getWorldCorner().getX() + v.getX(),
                    arena.getWorldCorner().getY() + v.getY(),
                    arena.getWorldCorner().getZ() + v.getZ());
            arena.getGenerators().add(new Generator(plugin, arena, GeneratorType.DIAMOND, loc, null));
        }
        for (Vector v : arena.getTemplate().getEmeraldGenLocations()) {
            Location loc = new Location(arena.getWorld(),
                    arena.getWorldCorner().getX() + v.getX(),
                    arena.getWorldCorner().getY() + v.getY(),
                    arena.getWorldCorner().getZ() + v.getZ());
            arena.getGenerators().add(new Generator(plugin, arena, GeneratorType.EMERALD, loc, null));
        }
        for (Generator g : arena.getGenerators()) g.start();
    }

    public void stopGenerators(Arena arena) {
        for (Generator g : arena.getGenerators()) g.stop();
    }

    public void spawnShops(Arena arena) {
        for (Team team : arena.getTeams()) {
            if (team.getShopLocation() != null) {
                ShopVillager v = new ShopVillager(plugin, arena, team, team.getShopLocation(), ShopVillager.Type.ITEMS);
                v.spawn();
                arena.getShops().add(v);
            }
            if (team.getUpgradeLocation() != null) {
                ShopVillager v = new ShopVillager(plugin, arena, team, team.getUpgradeLocation(), ShopVillager.Type.UPGRADES);
                v.spawn();
                arena.getShops().add(v);
            }
        }
    }

    // === Getters ===

    public Collection<Arena> getArenas() { return arenas.values(); }

    public Arena getArenaOfPlayer(org.bukkit.entity.Player player) {
        if (player == null) return null;
        for (Arena arena : arenas.values()) {
            if (arena.getPlayers().contains(player.getUniqueId())
                    || arena.getSpectators().contains(player.getUniqueId())) return arena;
        }
        return null;
    }

    public Arena getArenaByWorld(World world) {
        if (world == null) return null;
        for (Arena arena : arenas.values()) {
            if (arena.getWorld() == world) return arena;
        }
        return null;
    }

    public EventsTimelineManager getEventsTimelineManager() { return eventsTimelineManager; }
}
