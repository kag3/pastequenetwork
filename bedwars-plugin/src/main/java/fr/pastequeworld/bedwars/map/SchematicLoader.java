package fr.pastequeworld.bedwars.map;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.session.ClipboardHolder;
import fr.pastequeworld.bedwars.BedWarsPlugin;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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

        ClipboardFormat format = ClipboardFormat.findByFile(schematicFile);
        if (format == null) {
            plugin.getLogger().warning("Format schematic non reconnu: " + schematicFile.getName());
            return false;
        }

        FileInputStream fis = null;
        ClipboardReader reader = null;
        try {
            fis = new FileInputStream(schematicFile);
            reader = format.getReader(fis);
            Clipboard clipboard = reader.read(new BukkitWorld(pasteTo.getWorld()).getWorldData());
            World bukkitWorld = pasteTo.getWorld();

            EditSession editSession = WorldEdit.getInstance().getEditSessionFactory()
                    .getEditSession(new BukkitWorld(bukkitWorld), -1);
            editSession.enableQueue();

            ClipboardHolder holder = new ClipboardHolder(clipboard,
                    new BukkitWorld(bukkitWorld).getWorldData());
            Vector to = new Vector(pasteTo.getBlockX(), pasteTo.getBlockY(), pasteTo.getBlockZ());
            Operation op = holder.createPaste(editSession, new BukkitWorld(bukkitWorld).getWorldData())
                    .to(to)
                    .ignoreAirBlocks(false)
                    .build();
            Operations.completeLegacy(op);
            editSession.flushQueue();
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur de paste schematic: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            closeQuietly(reader);
            closeQuietly(fis);
        }
    }

    private void closeQuietly(Closeable c) {
        if (c == null) return;
        try { c.close(); } catch (IOException ignored) {}
    }

    public File resolveSchematic(String name) {
        File folder = new File(plugin.getDataFolder(), plugin.getConfigManager().getSchematicFolder());
        if (!folder.exists()) //noinspection ResultOfMethodCallIgnored
            folder.mkdirs();
        return new File(folder, name);
    }
}
