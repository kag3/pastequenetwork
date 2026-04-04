package fr.pasteque.guard.service;

import fr.pasteque.guard.PastequeGuardPlugin;
import fr.pasteque.guard.model.ReportEntry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ReportService {

    private final PastequeGuardPlugin plugin;
    private final File file;
    private final FileConfiguration config;

    public ReportService(PastequeGuardPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "reports.yml");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Impossible de créer reports.yml", e);
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder reports.yml");
        }
    }

    public synchronized String createReport(String author, String target, String reason) {
        String id = String.valueOf(System.currentTimeMillis());
        String path = "reports." + id;
        config.set(path + ".author", author);
        config.set(path + ".target", target);
        config.set(path + ".reason", reason);
        config.set(path + ".createdAt", System.currentTimeMillis());
        config.set(path + ".handled", false);
        save();
        return id;
    }

    public synchronized List<ReportEntry> getPendingReports() {
        ConfigurationSection section = config.getConfigurationSection("reports");
        if (section == null) {
            return Collections.emptyList();
        }
        List<ReportEntry> out = new ArrayList<ReportEntry>();
        for (String id : section.getKeys(false)) {
            String base = "reports." + id;
            boolean handled = config.getBoolean(base + ".handled", false);
            if (!handled) {
                out.add(new ReportEntry(
                        id,
                        config.getString(base + ".author", "?"),
                        config.getString(base + ".target", "?"),
                        config.getString(base + ".reason", "?"),
                        config.getLong(base + ".createdAt"),
                        false
                ));
            }
        }
        Collections.sort(out, new Comparator<ReportEntry>() {
            @Override
            public int compare(ReportEntry a, ReportEntry b) {
                return Long.compare(b.getCreatedAt(), a.getCreatedAt());
            }
        });
        return out;
    }

    public synchronized void markHandled(String id, String handledBy) {
        String base = "reports." + id;
        if (!config.contains(base)) {
            return;
        }
        config.set(base + ".handled", true);
        config.set(base + ".handledBy", handledBy);
        config.set(base + ".handledAt", System.currentTimeMillis());
        save();
    }
}
