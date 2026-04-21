package fr.pastequeworld.bedwars.map;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.schematic.SchematicFormat;
import fr.pastequeworld.bedwars.BedWarsPlugin;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;

/**
 * Colle une schematic WorldEdit dans un monde a une location donnee.
 * Utilise l'API WE 6.x compatible Bukkit 1.9.
 */
public class SchematicLoader {

    private final BedWarsPlugin plugin;

    public SchematicLoader(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Charge la schematic et la colle a la location indiquee.
     *
     * @param schematicFile fichier .schematic (format MCEdit) dans plugins/PastequeBedWars/schematics/
     * @param pasteTo coin de paste (generalement calcule a partir de l'offset du template)
     * @return true si succes
     */
    public boolean paste(File schematicFile, Location pasteTo) {
        if (!schematicFile.exists()) {
            plugin.getLogger().warning("Schematic introuvable: " + schematicFile.getAbsolutePath());
            return false;
        }

        SchematicFormat format = SchematicFormat.getFormat(schematicFile);
        if (format == null) {
            plugin.getLogger().warning("Format schematic non reconnu: " + schematicFile.getName());
            return false;
        }

        try {
            CuboidClipboard clipboard = format.load(schematicFile);
            World bukkitWorld = pasteTo.getWorld();

            EditSession editSession = WorldEdit.getInstance().getEditSessionFactory()
                    .getEditSession(new BukkitWorld(bukkitWorld), -1);
            editSession.enableQueue();

            Vector to = new Vector(pasteTo.getBlockX(), pasteTo.getBlockY(), pasteTo.getBlockZ());
            // Important perf: en 1.9.4 les grosses schematics peuvent faire tomber le watchdog
            // si on recolle aussi tout l'air (clear complet de la bounding box).
            // Par defaut on ignore l'air pour limiter drastiquement le nombre de blocs modifies.
            boolean ignoreAir = plugin.getConfig().getBoolean("schematics.ignore-air-on-paste", true);
            clipboard.paste(editSession, to, ignoreAir);
            editSession.flushQueue();
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur de paste schematic: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public File resolveSchematic(String name) {
        File folder = new File(plugin.getDataFolder(), plugin.getConfigManager().getSchematicFolder());
        if (!folder.exists()) //noinspection ResultOfMethodCallIgnored
            folder.mkdirs();
        return new File(folder, name);
    }
}
