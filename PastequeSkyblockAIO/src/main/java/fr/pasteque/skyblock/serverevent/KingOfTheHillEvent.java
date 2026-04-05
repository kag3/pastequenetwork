package fr.pasteque.skyblock.serverevent;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * King of the Hill: une zone definie au spawn PvP. Le joueur qui reste le plus
 * longtemps dans la zone sans mourir recoit la recompense. Dure 5 minutes.
 */
public class KingOfTheHillEvent implements ServerEvent {
    private final PastequeSkyblockPlugin plugin;
    private boolean active = false;
    private long endAt = 0L;
    private int taskId = -1;
    private final Map<UUID, Integer> timeInZone = new HashMap<UUID, Integer>();

    public KingOfTheHillEvent(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "koth"; }
    @Override public String getDisplayName() { return "King of the Hill"; }
    @Override public boolean isActive() { return active; }
    @Override public long getRemainingSeconds() {
        if (!active) return 0L;
        return Math.max(0L, (endAt - System.currentTimeMillis()) / 1000L);
    }

    @Override
    public void start() {
        if (active) return;
        active = true;
        endAt = System.currentTimeMillis() + (5L * 60L * 1000L);
        timeInZone.clear();
        Bukkit.broadcastMessage(PastequeSkyblockPlugin.color(
                plugin.getPrefix() + "&c&l\u265b KING OF THE HILL &fau spawn PvP ! &ePlus vous tenez la zone, plus vous gagnez !"));

        taskId = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override public void run() {
                if (System.currentTimeMillis() >= endAt) { stop(); return; }
                tick();
            }
        }, 20L, 20L).getTaskId();
    }

    private void tick() {
        Location zone = plugin.getPvpManager().getNamedWarp("koth-zone");
        if (zone == null || zone.getWorld() == null) zone = plugin.getPvpManager().getNamedWarp("pvp-spawn");
        if (zone == null || zone.getWorld() == null) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().equals(zone.getWorld()) && p.getLocation().distanceSquared(zone) <= 15 * 15) {
                Integer cur = timeInZone.get(p.getUniqueId());
                timeInZone.put(p.getUniqueId(), (cur == null ? 0 : cur) + 1);
            }
        }
    }

    @Override
    public void stop() {
        if (!active) return;
        active = false;
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        taskId = -1;

        UUID winner = null;
        int best = 0;
        for (Map.Entry<UUID, Integer> e : timeInZone.entrySet()) {
            if (e.getValue() > best) {
                best = e.getValue();
                winner = e.getKey();
            }
        }
        if (winner != null) {
            plugin.getEconomyManager().add(winner, 10000);
            String name = Bukkit.getOfflinePlayer(winner).getName();
            Bukkit.broadcastMessage(PastequeSkyblockPlugin.color(
                    plugin.getPrefix() + "&c&l\u265b &fLe roi du King of the Hill est &6" + name + " &favec " + best + "s dans la zone. &a+10000 Pasteque !"));
        } else {
            Bukkit.broadcastMessage(PastequeSkyblockPlugin.color(
                    plugin.getPrefix() + "&cKoth termine: aucun participant."));
        }
        timeInZone.clear();
    }
}
