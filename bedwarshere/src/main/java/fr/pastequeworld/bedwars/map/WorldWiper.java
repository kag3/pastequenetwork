package fr.pastequeworld.bedwars.map;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * Wipe-and-rebuild du monde principal au premier demarrage.
 *
 * Objectif : TOUT-EN-UN. Le joueur pose le JAR sur un serveur Spigot neuf, au
 * premier boot, le plugin :
 *   1. Decharge le monde "world" charge par defaut par Bukkit
 *   2. Supprime entierement le dossier du monde (terrain vanilla genere)
 *   3. Recree "world" en void pur (VoidGenerator, pas de structures)
 *   4. Applique les gamerules de lobby (PvP off, time fixe, no mobs...)
 *   5. Colle la schematic du lobby (BedWarsLobbyY.schematic) au spawn configure
 *   6. Ecrit le marker .world-wiped pour ne JAMAIS recommencer
 *
 * Safety : controle par le flag "lobby.wipe-world-on-first-run" (defaut: true).
 * Ne s'execute que si le nom du monde de lobby configure est "world" (nom par
 * defaut Minecraft) ET que le marker .world-wiped n'existe pas encore.
 *
 * Doit etre appele dans onEnable APRES ResourceExtractor (pour avoir acces a la
 * schematic) mais AVANT ConfigManager.load() (sinon ensureLobbyWorld charge
 * l'ancien monde).
 */
public class WorldWiper {

    private final BedWarsPlugin plugin;

    public WorldWiper(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void wipeOnFirstRun() {
        File marker = new File(plugin.getDataFolder(), ".world-wiped");
        if (marker.exists()) return;

        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("lobby.wipe-world-on-first-run", true)) {
            plugin.getLogger().info("Wipe-world desactive par config. Skip.");
            try { marker.createNewFile(); } catch (IOException ignored) {}
            return;
        }

        String lobbyWorldName = cfg.getString("lobby.world", "world");
        if (!"world".equalsIgnoreCase(lobbyWorldName)) {
            plugin.getLogger().info("Le monde de lobby est '" + lobbyWorldName
                    + "' (pas 'world'). Pas de wipe.");
            try { marker.createNewFile(); } catch (IOException ignored) {}
            return;
        }

        plugin.getLogger().info("===== PREMIER BOOT : wipe-and-rebuild de 'world' =====");

        // 1. Unload du monde vanilla charge par Bukkit au demarrage
        World existing = Bukkit.getWorld(lobbyWorldName);
        if (existing != null) {
            existing.setAutoSave(false);
            if (!existing.getPlayers().isEmpty()) {
                // Aucun joueur ne devrait etre present en onEnable, mais securite.
                plugin.getLogger().warning("Joueurs presents dans 'world' en onEnable (anormal). Annulation du wipe.");
                return;
            }
            boolean unloaded = Bukkit.unloadWorld(existing, false);
            if (!unloaded) {
                plugin.getLogger().info("Impossible de decharger 'world' (possible selon l'hebergeur/boot order). "
                        + "Fallback active sur monde existant.");

                // Fallback robuste: on applique quand meme les regles de lobby sur le monde deja charge.
                applyLobbyRules(existing);
                FileConfiguration fallbackCfg = plugin.getConfig();
                Location fallbackSpawn = new Location(existing,
                        fallbackCfg.getDouble("lobby.spawn.x", 0.5),
                        fallbackCfg.getDouble("lobby.spawn.y", 100.0),
                        fallbackCfg.getDouble("lobby.spawn.z", 0.5),
                        (float) fallbackCfg.getDouble("lobby.spawn.yaw", 0.0),
                        (float) fallbackCfg.getDouble("lobby.spawn.pitch", 0.0));
                existing.setSpawnLocation(fallbackSpawn.getBlockX(), fallbackSpawn.getBlockY(), fallbackSpawn.getBlockZ());

                boolean pastedInFallback = false;
                // Optionnel: le paste de lobby peut etre tres lourd. Desactive par defaut
                // pour eviter un boot-loop CPU sur certains panneaux.
                if (fallbackCfg.getBoolean("lobby.paste-on-unload-fail", false)) {
                    plugin.getLogger().info("paste-on-unload-fail=true: paste du lobby sur le monde existant.");
                    pastedInFallback = pasteLobbySchematicFallback(existing);
                } else {
                    plugin.getLogger().info("paste-on-unload-fail=false: pas de paste automatique au fallback.");
                }

                try { marker.createNewFile(); } catch (IOException ignored) {}
                if (pastedInFallback) {
                    File lobbyMarker = new File(plugin.getDataFolder(), ".lobby-loaded");
                    try { lobbyMarker.createNewFile(); } catch (IOException ignored) {}
                } else {
                    plugin.getLogger().info("Marker .lobby-loaded non cree (lobby non paste).");
                }
                return;
            }
        }

        // 2. Supprime le dossier world/
        File worldFolder = new File(Bukkit.getWorldContainer(), lobbyWorldName);
        if (worldFolder.exists()) {
            if (!deleteRecursive(worldFolder)) {
                plugin.getLogger().warning("Suppression partielle de " + worldFolder.getAbsolutePath()
                        + ". Certains fichiers sont verrouilles.");
            } else {
                plugin.getLogger().info("Dossier 'world' supprime.");
            }
        }

        // 3. Recree 'world' en VOID
        WorldCreator creator = new WorldCreator(lobbyWorldName);
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);
        creator.generator(new WorldManager.VoidGenerator());
        World fresh = creator.createWorld();
        if (fresh == null) {
            plugin.getLogger().severe("Echec recreation du monde 'world' en void.");
            return;
        }
        plugin.getLogger().info("Monde 'world' recree en void.");

        // 4. Gamerules de lobby
        applyLobbyRules(fresh);

        // 5. Paste de la schematic au spawn configure
        Location spawn = new Location(fresh,
                cfg.getDouble("lobby.spawn.x", 0.5),
                cfg.getDouble("lobby.spawn.y", 100.0),
                cfg.getDouble("lobby.spawn.z", 0.5),
                (float) cfg.getDouble("lobby.spawn.yaw", 0.0),
                (float) cfg.getDouble("lobby.spawn.pitch", 0.0));

        File schem = findLobbySchematic();
        boolean lobbyPasted = false;
        if (schem == null) {
            plugin.getLogger().warning("Schematic de lobby introuvable. Le monde reste vide.");
        } else {
            // Paste sous le spawn : on descend de 10 blocs pour laisser le joueur en l'air
            Location paste = spawn.clone().subtract(0, 10, 0);
            SchematicLoader loader = new SchematicLoader(plugin);
            boolean ok = loader.paste(schem, paste);
            if (ok) {
                lobbyPasted = true;
                plugin.getLogger().info("Lobby paste depuis " + schem.getAbsolutePath());
            } else {
                plugin.getLogger().warning("Echec paste du lobby.");
            }
        }

        // Fixe le spawn point du monde a la position de spawn du lobby
        fresh.setSpawnLocation(spawn.getBlockX(), spawn.getBlockY(), spawn.getBlockZ());
        fresh.save();

        // 6. Marker
        try {
            if (!marker.createNewFile()) {
                plugin.getLogger().warning("Impossible de creer le marker .world-wiped");
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Erreur creation .world-wiped : " + e.getMessage());
        }

        // Marque .lobby-loaded uniquement si le lobby a vraiment ete paste.
        if (lobbyPasted) {
            File lobbyMarker = new File(plugin.getDataFolder(), ".lobby-loaded");
            try { lobbyMarker.createNewFile(); } catch (IOException ignored) {}
        } else {
            plugin.getLogger().info("Marker .lobby-loaded non cree (lobby non paste).");
        }

        plugin.getLogger().info("===== Wipe-and-rebuild termine =====");
    }

    private boolean pasteLobbySchematicFallback(World world) {
        File schem = findLobbySchematic();
        if (schem == null) return false;
        FileConfiguration cfg = plugin.getConfig();
        Location spawn = new Location(world,
                cfg.getDouble("lobby.spawn.x", 0.5),
                cfg.getDouble("lobby.spawn.y", 100.0),
                cfg.getDouble("lobby.spawn.z", 0.5));
        Location paste = spawn.clone().subtract(0, 10, 0);
        return new SchematicLoader(plugin).paste(schem, paste);
    }

    private File findLobbySchematic() {
        File[] candidates = new File[] {
                new File(plugin.getDataFolder(), "schematics/BedWarsLobbyY.schematic"),
                new File(plugin.getDataFolder(), "schematics/lobby.schematic"),
                new File(plugin.getDataFolder().getParentFile().getParentFile(),
                        "lobbybedwars/BedWarsLobbyY.schematic"),
                new File(plugin.getDataFolder(), "lobbybedwars/BedWarsLobbyY.schematic")
        };
        for (File f : candidates) if (f != null && f.isFile()) return f;
        return null;
    }

    private void applyLobbyRules(World w) {
        w.setSpawnFlags(false, false);
        w.setPVP(false);
        w.setKeepSpawnInMemory(true);
        w.setAutoSave(true);
        w.setStorm(false);
        w.setThundering(false);
        w.setTime(6000L);
        w.setDifficulty(Difficulty.PEACEFUL);
        w.setGameRuleValue("doDaylightCycle", "false");
        w.setGameRuleValue("doWeatherCycle", "false");
        w.setGameRuleValue("doMobSpawning", "false");
        w.setGameRuleValue("doFireTick", "false");
        w.setGameRuleValue("mobGriefing", "false");
        w.setGameRuleValue("naturalRegeneration", "true");
        w.setGameRuleValue("showDeathMessages", "false");
        w.setGameRuleValue("announceAdvancements", "false");
    }

    private boolean deleteRecursive(File file) {
        if (file == null || !file.exists()) return true;
        boolean ok = true;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File c : children) {
                    if (!deleteRecursive(c)) ok = false;
                }
            }
        }
        return file.delete() && ok;
    }
}
