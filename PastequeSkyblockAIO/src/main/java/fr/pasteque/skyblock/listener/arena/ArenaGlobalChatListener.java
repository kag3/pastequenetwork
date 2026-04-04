package fr.pasteque.skyblock.listener.arena;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.arena.ArenaWorldService;
import fr.pasteque.skyblock.arena.PlayerDataService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ArenaGlobalChatListener implements Listener {

    private final PastequeSkyblockPlugin plugin;
    private final ArenaWorldService arenaWorldService;
    private final PlayerDataService playerDataService;

    public ArenaGlobalChatListener(PastequeSkyblockPlugin plugin, ArenaWorldService arenaWorldService, PlayerDataService playerDataService) {
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
        final String format = plugin.getConfig().getString("chat-format", "&8[&dNiv.%level%&8] &f%player% &8> &f%message%");
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
