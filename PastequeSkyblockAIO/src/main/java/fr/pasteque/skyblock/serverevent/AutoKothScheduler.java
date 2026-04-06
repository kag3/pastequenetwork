package fr.pasteque.skyblock.serverevent;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Bukkit;

import java.util.Calendar;

/**
 * Lance automatiquement un KOTH chaque mercredi et samedi a 16h00.
 * Verifie chaque minute si les conditions sont remplies.
 */
public class AutoKothScheduler {

    private final PastequeSkyblockPlugin plugin;
    private long lastLaunchDay = -1L;

    public AutoKothScheduler(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        // Check every 60 seconds (1200 ticks)
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                checkAndLaunch();
            }
        }, 20L * 30L, 20L * 60L);
    }

    private void checkAndLaunch() {
        Calendar now = Calendar.getInstance();
        int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);

        // Wednesday = 4, Saturday = 7 in Calendar
        boolean isKothDay = (dayOfWeek == Calendar.WEDNESDAY || dayOfWeek == Calendar.SATURDAY);
        int kothHour = plugin.getConfig().getInt("koth.auto-hour", 16);

        if (!isKothDay || hour != kothHour || minute != 0) {
            return;
        }

        // Prevent double-launch on the same day
        long dayId = now.get(Calendar.YEAR) * 1000L + now.get(Calendar.DAY_OF_YEAR);
        if (lastLaunchDay == dayId) {
            return;
        }
        lastLaunchDay = dayId;

        // Check if a KOTH is already running
        ServerEvent koth = plugin.getEventManager().get("koth");
        if (koth != null && koth.isActive()) {
            return;
        }

        // 5-minute warning announcement
        String bar = PastequeSkyblockPlugin.color("&8&m--------------------------------");
        Bukkit.broadcastMessage(bar);
        Bukkit.broadcastMessage(PastequeSkyblockPlugin.color(
                "  &c&l\u265b KOTH AUTOMATIQUE \u265b"));
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(PastequeSkyblockPlugin.color(
                "  &7Un &cKOTH &7demarre dans &e5 minutes &7!"));
        Bukkit.broadcastMessage(PastequeSkyblockPlugin.color(
                "  &7Preparez-vous au combat !"));
        Bukkit.broadcastMessage(PastequeSkyblockPlugin.color(
                "  &e\u25B6 &6/koth &epour rejoindre des le lancement"));
        Bukkit.broadcastMessage(bar);

        // 3-minute reminder
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage(PastequeSkyblockPlugin.color(
                        plugin.getPrefix() + "&c&l\u265b &7Le KOTH commence dans &e3 minutes &7! &6/koth"));
            }
        }, 20L * 120L); // 2 min after first announcement = 3 min remaining

        // 1-minute reminder
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage(PastequeSkyblockPlugin.color(
                        plugin.getPrefix() + "&c&l\u265b &7Le KOTH commence dans &e1 minute &7! &6/koth"));
            }
        }, 20L * 240L); // 4 min after

        // Start KOTH after 5 minutes
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                plugin.getEventManager().startEvent("koth");
                plugin.getLogger().info("[AutoKOTH] KOTH automatique lance (" +
                        (now.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY ? "Mercredi" : "Samedi") + " " + kothHour + "h)");
            }
        }, 20L * 300L); // 5 min after
    }
}
