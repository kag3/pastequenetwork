package fr.pasteque.skyblock.arena;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.UUID;

public class KillStreakService {

    private final PastequeSkyblockPlugin plugin;
    private final HashMap<UUID, Integer> streaks = new HashMap<UUID, Integer>();

    public KillStreakService(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public void recordKill(Player killer) {
        UUID id = killer.getUniqueId();
        int streak = getStreak(id) + 1;
        streaks.put(id, streak);

        if (streak == 3) {
            killer.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&e3 kills d'affilee !"));
            killer.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 20, 0));
        } else if (streak == 5) {
            killer.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&65 kills ! Inarretable !"));
            killer.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 10, 0));
            broadcast("&d" + killer.getName() + " &fest sur une serie de &c5 kills &f!");
        } else if (streak == 10) {
            killer.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&c10 KILLS ! LEGENDAIRE !"));
            plugin.getEconomyManager().add(id, 500.0);
            killer.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&a+500 " + plugin.getEconomyManager().getCurrencyName() + " bonus !"));
            broadcast("&4&l" + killer.getName() + " &c&la atteint 10 KILLS D'AFFILEE !");
        } else if (streak >= 15 && streak % 5 == 0) {
            broadcast("&4&l" + killer.getName() + " &c&lest INARRETABLE avec " + streak + " kills d'affilee !");
        }
    }

    public void resetStreak(UUID uuid) {
        streaks.remove(uuid);
    }

    public int getStreak(UUID uuid) {
        if (!streaks.containsKey(uuid)) {
            return 0;
        }
        return streaks.get(uuid);
    }

    private void broadcast(String message) {
        String formatted = PastequeSkyblockPlugin.color(plugin.getPrefix() + message);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(formatted);
        }
    }
}
