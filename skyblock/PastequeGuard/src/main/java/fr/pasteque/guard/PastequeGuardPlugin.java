package fr.pasteque.guard;

import fr.pasteque.guard.command.PgCommand;
import fr.pasteque.guard.listener.ChatListener;
import fr.pasteque.guard.listener.ConnectionListener;
import fr.pasteque.guard.listener.PanelListener;
import fr.pasteque.guard.service.FilterService;
import fr.pasteque.guard.service.ReportService;
import fr.pasteque.guard.service.SanctionService;
import fr.pasteque.guard.util.ColorUtil;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class PastequeGuardPlugin extends JavaPlugin {

    private SanctionService sanctionService;
    private ReportService reportService;
    private FilterService filterService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.sanctionService = new SanctionService(this);
        this.reportService = new ReportService(this);
        this.filterService = new FilterService(this, sanctionService);

        PgCommand pgCommand = new PgCommand(this, sanctionService, reportService, filterService);
        PluginCommand command = getCommand("pg");
        if (command != null) {
            command.setExecutor(pgCommand);
            command.setTabCompleter(pgCommand);
        }

        getServer().getPluginManager().registerEvents(new ChatListener(this, sanctionService, filterService), this);
        getServer().getPluginManager().registerEvents(new ConnectionListener(this, sanctionService), this);
        getServer().getPluginManager().registerEvents(new PanelListener(this, reportService), this);
    }

    public String color(String input) {
        return ColorUtil.color(input);
    }

    public String getPrefix() {
        return color(getConfig().getString("prefix", "&2&lPasteque&b&lGuard &f» "));
    }

    public SanctionService getSanctionService() {
        return sanctionService;
    }

    public ReportService getReportService() {
        return reportService;
    }

    public FilterService getFilterService() {
        return filterService;
    }
}
