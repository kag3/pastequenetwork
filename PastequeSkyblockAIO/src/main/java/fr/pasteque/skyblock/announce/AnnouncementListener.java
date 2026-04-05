package fr.pasteque.skyblock.announce;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class AnnouncementListener implements Listener {

    private final AnnouncementManager announcementManager;

    public AnnouncementListener(AnnouncementManager announcementManager) {
        this.announcementManager = announcementManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean firstJoin = !player.hasPlayedBefore();
        announcementManager.sendJoinMessages(player, firstJoin);
    }
}
