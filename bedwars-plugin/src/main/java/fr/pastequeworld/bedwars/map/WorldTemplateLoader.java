package fr.pastequeworld.bedwars.map;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Charge une map persistante a partir d'un dossier de monde Minecraft complet
 * (level.dat + .mca). La source peut etre :
 *   - plugins/PastequeBedWars/maps-worlds/<id>/
 *   - <server-root>/<id>/   (conventionnel : depot GitHub clone)
 *
 * Copie le dossier template vers le world-container sous un nouveau nom, puis
 * le charge via Bukkit. La map reste isolee pour chaque arene, et est
 * supprimee via {@link WorldManager#unloadAndDelete(World)}.
 */
public class WorldTemplateLoader {

    private final BedWarsPlugin plugin;

    public WorldTemplateLoader(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Resout le dossier source d'un template. Ordre de recherche :
     *   1. plugins/PastequeBedWars/maps-worlds/<templateId>/
     *   2. <server-root>/<templateId>/
     *   3. plugins/PastequeBedWars/<templateId>/
     */
    public File resolveTemplateFolder(String templateId) {
        File[] candidates = new File[] {
                new File(plugin.getDataFolder(), "maps-worlds/" + templateId),
                new File(Bukkit.getWorldContainer().getParentFile(), templateId),
                new File(plugin.getDataFolder().getParentFile().getParentFile(), templateId),
                new File(plugin.getDataFolder(), templateId)
        };
        for (File f : candidates) {
            if (f != null && f.isDirectory() && new File(f, "level.dat").isFile()) return f;
        }
        return null;
    }

    /**
     * Copie le dossier template -> <world-container>/<arenaWorldName>/, ouvre le
     * monde via Bukkit, et renvoie le World charge.
     * Retourne null si la source est introuvable ou si l'ouverture echoue.
     */
    public World copyAndLoad(String templateId, String arenaWorldName) {
        File src = resolveTemplateFolder(templateId);
        if (src == null) {
            plugin.getLogger().warning("Template monde introuvable: " + templateId);
            return null;
        }
        File dst = new File(Bukkit.getWorldContainer(), arenaWorldName);
        if (dst.exists()) deleteRecursive(dst);
        try {
            copyDirectory(src, dst);
        } catch (IOException e) {
            plugin.getLogger().severe("Echec copie template " + templateId + " -> " + arenaWorldName + ": " + e.getMessage());
            return null;
        }
        // Supprime les verrous susceptibles de bloquer le chargement
        new File(dst, "session.lock").delete();
        new File(dst, "uid.dat").delete();

        // Normalisation : certains exports mettent les .mca a la racine, Minecraft les
        // attend dans region/. On normalise ici pour coller au format serveur attendu.
        normalizeRegionFolder(dst);

        WorldCreator creator = new WorldCreator(arenaWorldName);
        creator.generateStructures(false);
        try {
            World world = creator.createWorld();
            if (world != null) {
                world.setAutoSave(false);
                world.setKeepSpawnInMemory(false);
                world.setPVP(true);
                world.setDifficulty(org.bukkit.Difficulty.NORMAL);
                world.setTime(6000L);
                world.setStorm(false);
                world.setThundering(false);
                world.setGameRuleValue("doDaylightCycle", "false");
                world.setGameRuleValue("doWeatherCycle", "false");
                world.setGameRuleValue("doMobSpawning", "false");
                world.setGameRuleValue("doFireTick", "false");
                world.setGameRuleValue("mobGriefing", "false");
                world.setGameRuleValue("announceAdvancements", "false");
                world.setGameRuleValue("showDeathMessages", "false");
                world.setGameRuleValue("naturalRegeneration", "true");
                world.setSpawnFlags(false, false);
            }
            return world;
        } catch (Exception e) {
            plugin.getLogger().severe("Echec chargement monde " + arenaWorldName + ": " + e.getMessage());
            return null;
        }
    }

    private void normalizeRegionFolder(File worldFolder) {
        File region = new File(worldFolder, "region");
        if (region.isDirectory()) return;
        if (!region.mkdirs()) return;
        File[] files = worldFolder.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (!f.isFile()) continue;
            String name = f.getName();
            if (name.startsWith("r.") && name.endsWith(".mca")) {
                //noinspection ResultOfMethodCallIgnored
                f.renameTo(new File(region, name));
            }
        }
    }

    private void copyDirectory(File src, File dst) throws IOException {
        if (!dst.exists() && !dst.mkdirs()) throw new IOException("Impossible de creer " + dst);
        File[] files = src.listFiles();
        if (files == null) return;
        for (File f : files) {
            File out = new File(dst, f.getName());
            if (f.isDirectory()) {
                copyDirectory(f, out);
            } else {
                copyFile(f, out);
            }
        }
    }

    private void copyFile(File src, File dst) throws IOException {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(src);
            fos = new FileOutputStream(dst);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) > 0) fos.write(buffer, 0, read);
        } finally {
            if (fis != null) try { fis.close(); } catch (IOException ignored) {}
            if (fos != null) try { fos.close(); } catch (IOException ignored) {}
        }
    }

    private void deleteRecursive(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File c : children) deleteRecursive(c);
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }
}
