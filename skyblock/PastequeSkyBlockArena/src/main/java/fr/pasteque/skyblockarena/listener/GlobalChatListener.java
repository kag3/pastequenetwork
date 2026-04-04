package fr.pasteque.skyblockarena.listener;

import fr.pasteque.skyblockarena.PastequeSkyBlockArenaPlugin;
import fr.pasteque.skyblockarena.service.ArenaWorldService;
import fr.pasteque.skyblockarena.service.PlayerDataService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class GlobalChatListener implements Listener {

    private final PastequeSkyBlockArenaPlugin plugin;
    private final ArenaWorldService arenaWorldService;
    private final PlayerDataService playerDataService;

    public GlobalChatListener(PastequeSkyBlockArenaPlugin plugin, ArenaWorldService arenaWorldService, PlayerDataService playerDataService) {
        this.plugin = plugin;
        this.arenaWorldService = arenaWorldService;
        this.playerDataService = playerDataService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onChat(final AsyncPlayerChatEvent event) {
        if (!plugin.getConfig().getBoolean("messages.global-chat-enabled", true)) {
            return;
        }
        event.setCancelled(true);
        final Player sender = event.getPlayer();
        final String format = plugin.getConfig().getString("chat-format", "&8[&dNiv.%level%&8] &f%player% &8» &f%message%");
        final String built = plugin.color(format
                .replace("%level%", String.valueOf(playerDataService.get(sender).getLevel()))
                .replace("%player%", sender.getDisplayName())
                .replace("%message%", event.getMessage()));
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(built);
                }
            }
        });
    }
}
