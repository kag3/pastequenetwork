package fr.pasteque.skyblock.listener.guard;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.guard.FilterService;
import fr.pasteque.skyblock.guard.SanctionService;
import fr.pasteque.skyblock.guard.model.SanctionEntry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class GuardChatListener implements Listener {

    private final PastequeSkyblockPlugin plugin;
    private final SanctionService sanctionService;
    private final FilterService filterService;

    public GuardChatListener(PastequeSkyblockPlugin plugin, SanctionService sanctionService, FilterService filterService) {
        this.plugin = plugin;
        this.sanctionService = sanctionService;
        this.filterService = filterService;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent event) {
        SanctionEntry mute = sanctionService.getActiveMute(event.getPlayer().getName());
        if (mute != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(sanctionService.buildMuteMessage(mute));
            return;
        }
        if (filterService.shouldBlock(event.getPlayer(), event.getMessage())) {
            event.setCancelled(true);
        }
    }
}
