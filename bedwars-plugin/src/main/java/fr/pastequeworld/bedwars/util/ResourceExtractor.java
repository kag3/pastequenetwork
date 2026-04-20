package fr.pastequeworld.bedwars.util;

import fr.pastequeworld.bedwars.BedWarsPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Extrait les ressources "bundled/*" du JAR du plugin vers le disque du
 * serveur au premier demarrage. Objectif : "all-in-one, pose le JAR c'est fini".
 *
 * - bundled/lobbybedwars/*.schematic   -> plugins/PastequeBedWars/schematics/
 * - bundled/schematics/*.schematic     -> plugins/PastequeBedWars/schematics/
 *
 * Un marqueur .extracted empeche la re-extraction a chaque boot.
 * Si l'utilisateur a modifie les fichiers, l'extraction est skip.
 */
public class ResourceExtractor {

    private final BedWarsPlugin plugin;

    public ResourceExtractor(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void extractOnFirstRun() {
        File marker = new File(plugin.getDataFolder(), ".extracted");
        if (marker.exists()) return;

        plugin.getLogger().info("Extraction des ressources bundled (premier demarrage)...");
        File pluginJar = resolvePluginJar();
        if (pluginJar == null) {
            plugin.getLogger().warning("Impossible de localiser le JAR du plugin.");
            return;
        }

        JarFile jar = null;
        try {
            jar = new JarFile(pluginJar);
            Enumeration<JarEntry> entries = jar.entries();
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                plugin.getLogger().warning("Impossible de creer " + dataFolder);
                return;
            }

            int count = 0;
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith("bundled/")) continue;
                if (entry.isDirectory()) continue;

                String rel = name.substring("bundled/".length());
                File target = mapEntryTarget(rel);
                if (target == null) continue;

                if (!target.getParentFile().exists() && !target.getParentFile().mkdirs()) {
                    plugin.getLogger().warning("Impossible de creer " + target.getParentFile());
                    continue;
                }

                InputStream in = null;
                FileOutputStream out = null;
                try {
                    in = jar.getInputStream(entry);
                    out = new FileOutputStream(target);
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) > 0) out.write(buffer, 0, read);
                    count++;
                } finally {
                    if (in != null) try { in.close(); } catch (IOException ignored) {}
                    if (out != null) try { out.close(); } catch (IOException ignored) {}
                }
            }

            try { marker.createNewFile(); } catch (IOException ignored) {}
            plugin.getLogger().info("Extraction terminee : " + count + " fichiers ecrits.");
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur pendant l'extraction des ressources : " + e.getMessage());
        } finally {
            if (jar != null) try { jar.close(); } catch (IOException ignored) {}
        }
    }

    /**
     * Mapping entre entree bundled/ et destination sur disque :
     *   bundled/lobbybedwars/<f>     -> <plugin-data>/schematics/<f>
     *   bundled/schematics/<f>       -> <plugin-data>/schematics/<f>
     * Les fichiers "HERE" et WorldDownloader.txt sont skip.
     */
    private File mapEntryTarget(String rel) {
        // Skip cruft
        String fileName = rel.substring(rel.lastIndexOf('/') + 1);
        if (fileName.equalsIgnoreCase("HERE")) return null;
        if (fileName.equalsIgnoreCase("WorldDownloader.txt")) return null;
        if (fileName.equalsIgnoreCase("session.lock")) return null;

        int slash = rel.indexOf('/');
        if (slash < 0) return null;
        String dir = rel.substring(0, slash);
        String remainder = rel.substring(slash + 1);

        if (dir.equals("lobbybedwars") || dir.equals("schematics")) {
            return new File(plugin.getDataFolder(), "schematics/" + remainder);
        }
        return null;
    }

    private File resolvePluginJar() {
        try {
            URL loc = BedWarsPlugin.class.getProtectionDomain().getCodeSource().getLocation();
            if (loc == null) return null;
            File f = new File(loc.toURI());
            if (f.isFile()) return f;
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
