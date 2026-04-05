package fr.pasteque.skyblock.serverevent;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central registry for all server events. Supports running MULTIPLE events
 * in parallel (e.g. Dragon + Meteor Shower + King of the Hill simultaneously).
 */
public class EventManager {
    private final PastequeSkyblockPlugin plugin;
    private final Map<String, ServerEvent> events = new LinkedHashMap<String, ServerEvent>();

    public EventManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(ServerEvent event) {
        events.put(event.getId().toLowerCase(), event);
    }

    public ServerEvent get(String id) {
        return events.get(id.toLowerCase());
    }

    public Collection<ServerEvent> getAll() {
        return events.values();
    }

    public boolean startEvent(String id) {
        ServerEvent ev = get(id);
        if (ev == null || ev.isActive()) return false;
        ev.start();
        return true;
    }

    public boolean stopEvent(String id) {
        ServerEvent ev = get(id);
        if (ev == null || !ev.isActive()) return false;
        ev.stop();
        return true;
    }

    public int getActiveCount() {
        int n = 0;
        for (ServerEvent e : events.values()) if (e.isActive()) n++;
        return n;
    }

    public void stopAll() {
        for (ServerEvent e : events.values()) if (e.isActive()) e.stop();
    }

    public PastequeSkyblockPlugin getPlugin() {
        return plugin;
    }
}
