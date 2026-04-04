package fr.pasteque.skyblock.arena;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.arena.model.ArenaPlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class ArenaLevelService {

    private final PastequeSkyblockPlugin plugin;
    private final PlayerDataService playerDataService;

    public ArenaLevelService(PastequeSkyblockPlugin plugin, PlayerDataService playerDataService) {
        this.plugin = plugin;
        this.playerDataService = playerDataService;
    }

    public int getNextRequirement(int level) {
        int base = plugin.getConfig().getInt("level-formula.base", 75);
        int linear = plugin.getConfig().getInt("level-formula.linear", 25);
        int quadratic = plugin.getConfig().getInt("level-formula.quadratic", 5);
        return base + (linear * Math.max(0, level)) + (quadratic * level * level);
    }

    public void addXp(Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        ArenaPlayerData data = playerDataService.get(player);
        data.addXp(amount);
        levelUpIfNeeded(player, data);
    }

    public void grantActivityXp() {
        int every = 60;
        int xp = plugin.getConfig().getInt("activity-xp-per-minute", 4);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!plugin.getArenaWorldService().isArenaWorld(player.getWorld())) {
                continue;
            }
            ArenaPlayerData data = playerDataService.get(player);
            if (data.getActivitySeconds() > 0 && data.getActivitySeconds() % every == 0) {
                addXp(player, xp);
            }
        }
    }

    private void levelUpIfNeeded(Player player, ArenaPlayerData data) {
        int maxLevel = plugin.getConfig().getInt("max-level", 101);
        while (data.getLevel() < maxLevel) {
            int required = getNextRequirement(data.getLevel());
            if (data.getXp() < required) {
                break;
            }
            data.setXp(data.getXp() - required);
            int from = data.getLevel();
            int to = from + 1;
            data.setLevel(to);
            player.sendTitle(plugin.color(plugin.getConfig().getString("messages.rankup-title", "&d[ %from% ] &f-> &5[ %to% ]").replace("%from%", String.valueOf(from)).replace("%to%", String.valueOf(to))),
                    plugin.color(plugin.getConfig().getString("messages.rankup-subtitle", "&fVotre experience d'arene progresse.")));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.15F);
            player.sendMessage(plugin.color(plugin.prefix("arena") + plugin.getConfig().getString("messages.level-up-chat", "&fVous passez au niveau &d%to%&f. Continuez comme ca !").replace("%to%", String.valueOf(to))));
        }
    }
}
