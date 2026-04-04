package fr.pasteque.skyblock.listener.guard;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.guard.SanctionService;
import fr.pasteque.skyblock.guard.model.SanctionEntry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class GuardConnectionListener implements Listener {

    private final PastequeSkyblockPlugin plugin;
    private final SanctionService sanctionService;

    public GuardConnectionListener(PastequeSkyblockPlugin plugin, SanctionService sanctionService) {
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
