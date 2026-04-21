package fr.pastequeworld.bedwars.map;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.generator.ChunkGenerator;

import java.io.File;
import java.util.Random;

/**
 * Gere la creation/suppression de mondes vides (void worlds) pour les arenes.
 * Chaque arene tourne dans son propre monde : isolation des events, du chat, etc.
 *
 * Le monde est genere "vide" (void generator), puis la map est pastee dedans
 * via le {@link fr.pastequeworld.bedwars.map.SchematicLoader}.
 */
public class WorldManager {

    private final BedWarsPlugin plugin;

    public WorldManager(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public World createArenaWorld(String name) {
        World existing = Bukkit.getWorld(name);
        if (existing != null) return existing;

        WorldCreator creator = new WorldCreator(name);
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);
        creator.generator(new VoidGenerator());
        World world = creator.createWorld();
        if (world != null) {
            world.setAutoSave(false);
            world.setSpawnFlags(false, false);
            world.setPVP(true);
            world.setKeepSpawnInMemory(false);
            world.setGameRuleValue("doDaylightCycle", "false");
            world.setGameRuleValue("doMobSpawning", "false");
            world.setGameRuleValue("doFireTick", "false");
            world.setGameRuleValue("mobGriefing", "false");
            world.setGameRuleValue("announceAdvancements", "false");
            world.setGameRuleValue("showDeathMessages", "false");
            world.setTime(6000L);
            world.setStorm(false);
            world.setThundering(false);
        }
        return world;
    }

    public void unloadAndDelete(World world) {
        if (world == null) return;
        String name = world.getName();
        Bukkit.unloadWorld(world, false);
        File folder = new File(Bukkit.getWorldContainer(), name);
        deleteRecursive(folder);
    }

    private void deleteRecursive(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File c : children) deleteRecursive(c);
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    /**
     * Generateur de chunks completement vide (void).
     * Les chunks sont remplis d'air pur : la map est ensuite pastee via WorldEdit.
     */
    public static class VoidGenerator extends ChunkGenerator {
        @Override
        public byte[] generate(World world, Random random, int cx, int cz) {
            // byte array legacy : 16 * 16 * 256 = 65536 bytes
            return new byte[65536];
        }

        @Override
        public boolean canSpawn(World world, int x, int z) {
            return true;
        }
    }
}
