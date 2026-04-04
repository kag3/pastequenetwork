package fr.pasteque.skyblockarena.service;

import fr.pasteque.skyblockarena.PastequeSkyBlockArenaPlugin;
import fr.pasteque.skyblockarena.model.ArenaPlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class CombatTagService {

    private final PastequeSkyBlockArenaPlugin plugin;
    private final EconomyBridge economyBridge;
    private final PlayerDataService playerDataService;
    private final ArenaWorldService arenaWorldService;
    private final Map<UUID, Integer> combatSeconds = new HashMap<UUID, Integer>();

    public CombatTagService(PastequeSkyBlockArenaPlugin plugin, EconomyBridge economyBridge, PlayerDataService playerDataService, ArenaWorldService arenaWorldService) {
        this.plugin = plugin;
        this.economyBridge = economyBridge;
        this.playerDataService = playerDataService;
        this.arenaWorldService = arenaWorldService;
    }

    public void tag(Player first, Player second) {
        int seconds = plugin.getConfig().getInt("combat-tag-seconds", 30);
        applyTag(first, seconds);
        applyTag(second, seconds);
    }

    private void applyTag(Player player, int seconds) {
        Integer old = combatSeconds.put(player.getUniqueId(), seconds);
        if (old == null || old <= 0) {
            player.sendMessage(plugin.color(plugin.prefix() + plugin.getConfig().getString("messages.combat-enter", "&fMode combat activé. Ne vous déconnectez pas pendant &d%seconds%s&f.").replace("%seconds%", String.valueOf(seconds))));
        }
    }

    public boolean isInCombat(Player player) {
        Integer remaining = combatSeconds.get(player.getUniqueId());
        return remaining != null && remaining > 0;
    }

    public void tick() {
        Iterator<Map.Entry<UUID, Integer>> iterator = combatSeconds.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int next = entry.getValue() - 1;
            if (next <= 0) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    player.sendMessage(plugin.color(plugin.prefix() + plugin.getConfig().getString("messages.combat-leave", "&fMode combat terminé. Vous pouvez souffler un peu.")));
                }
                iterator.remove();
            } else {
                entry.setValue(next);
            }
        }
    }

    public void handleCombatLogout(Player player) {
        if (!arenaWorldService.isArenaWorld(player.getWorld()) || !isInCombat(player)) {
            return;
        }
        double amount = plugin.getConfig().getDouble("combat-logout-penalty", 500.0D);
        ArenaPlayerData data = playerDataService.get(player);
        data.setPendingCombatPenalty(true);
        data.setPendingPenaltyAmount(amount);
        economyBridge.withdraw(player.getUniqueId(), player.getName(), amount);
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        combatSeconds.remove(player.getUniqueId());
    }

    public void notifyIfPending(Player player) {
        ArenaPlayerData data = playerDataService.get(player);
        if (!data.isPendingCombatPenalty()) {
            return;
        }
        player.sendMessage(plugin.color(plugin.prefix() + plugin.getConfig().getString("messages.combat-penalty-notice", "&fLors de votre dernière déconnexion en combat, vous avez perdu &d%amount% Pasteque &fet votre inventaire a été vidé.").replace("%amount%", String.valueOf((int) data.getPendingPenaltyAmount()))));
        data.setPendingCombatPenalty(false);
        data.setPendingPenaltyAmount(0.0D);
    }
}
