package fr.pasteque.guard.listener;

import fr.pasteque.guard.PastequeGuardPlugin;
import fr.pasteque.guard.model.SanctionEntry;
import fr.pasteque.guard.service.SanctionService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class ConnectionListener implements Listener {

    private final PastequeGuardPlugin plugin;
    private final SanctionService sanctionService;

    public ConnectionListener(PastequeGuardPlugin plugin, SanctionService sanctionService) {
        this.plugin = plugin;
        this.sanctionService = sanctionService;
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        SanctionEntry ban = sanctionService.getActiveBan(event.getName());
        if (ban != null) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, sanctionService.buildBanScreen(ban));
        }
    }
}
