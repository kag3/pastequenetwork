package fr.pasteque.guard.listener;

import fr.pasteque.guard.PastequeGuardPlugin;
import fr.pasteque.guard.model.SanctionEntry;
import fr.pasteque.guard.service.FilterService;
import fr.pasteque.guard.service.SanctionService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final PastequeGuardPlugin plugin;
    private final SanctionService sanctionService;
    private final FilterService filterService;

    public ChatListener(PastequeGuardPlugin plugin, SanctionService sanctionService, FilterService filterService) {
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
