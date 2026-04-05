package fr.pasteque.skyblock.anticheat;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Basic anti-cheat: reach, fly, speed, killaura angle, nofall.
 * Stores per-player violation levels and auto-kicks / notifies staff when
 * threshold is crossed. Intentionally simple and server-friendly.
 */
public class AntiCheatManager {
    private final PastequeSkyblockPlugin plugin;
    private final Map<UUID, Integer> violations = new HashMap<UUID, Integer>();
    private final Map<UUID, Long> lastMove = new HashMap<UUID, Long>();
    private final Map<UUID, Location> lastLocation = new HashMap<UUID, Location>();

    // Thresholds
    public static final double MAX_REACH_SQ = 4.5 * 4.5;   // 4.5 blocks hit reach
    public static final double MAX_HORIZONTAL_PER_TICK = 0.7; // sprinting ~0.28, jumping ~0.4
    public static final int KICK_VL = 20;

    public AntiCheatManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public void flag(Player player, String reason, int weight) {
        UUID id = player.getUniqueId();
        int vl = violations.containsKey(id) ? violations.get(id) : 0;
        vl += weight;
        violations.put(id, vl);
        notifyStaff(player, reason, vl);
        if (vl >= KICK_VL) {
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override public void run() {
                    player.kickPlayer(PastequeSkyblockPlugin.color("&2&lPasteque &8\u00bb &cSuspicion de triche detectee.\n&7Raison: &f" + reason));
                }
            });
            violations.put(id, 0);
        }
    }

    private void notifyStaff(Player offender, String reason, int vl) {
        String msg = PastequeSkyblockPlugin.color("&2&lAC &8\u00bb &c" + offender.getName() + " &7flag: &f" + reason + " &8[vl=" + vl + "]");
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("pastequeskyblock.staff")) p.sendMessage(msg);
        }
    }

    public int getVl(UUID id) {
        return violations.containsKey(id) ? violations.get(id) : 0;
    }

    public void clear(UUID id) {
        violations.remove(id);
        lastMove.remove(id);
        lastLocation.remove(id);
    }

    public Map<UUID, Long> getLastMove() { return lastMove; }
    public Map<UUID, Location> getLastLocation() { return lastLocation; }
}
