package fr.pasteque.skyblock.listener;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.manager.DataFile;
import fr.pasteque.skyblock.model.Island;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ChatListener implements Listener {
    private final PastequeSkyblockPlugin plugin;
    private final Set<UUID> islandChatToggles = new HashSet<UUID>();

    public ChatListener(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        loadToggles();
    }

    private void loadToggles() {
        islandChatToggles.clear();
        DataFile dataFile = plugin.getDataFile();
        if (dataFile.getConfig().isConfigurationSection("island-chat-toggles")) {
            for (String key : dataFile.getConfig().getConfigurationSection("island-chat-toggles").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    boolean enabled = dataFile.getConfig().getBoolean("island-chat-toggles." + key);
                    if (enabled) {
                        islandChatToggles.add(uuid);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    private void saveToggles() {
        DataFile dataFile = plugin.getDataFile();
        dataFile.getConfig().set("island-chat-toggles", null);
        for (UUID uuid : islandChatToggles) {
            dataFile.getConfig().set("island-chat-toggles." + uuid.toString(), true);
        }
        dataFile.save();
    }

    public boolean isIslandChatEnabled(UUID uuid) {
        return islandChatToggles.contains(uuid);
    }

    public void toggleIslandChat(UUID uuid) {
        if (islandChatToggles.contains(uuid)) {
            islandChatToggles.remove(uuid);
        } else {
            islandChatToggles.add(uuid);
        }
        saveToggles();
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.getWorld().getName().equalsIgnoreCase(plugin.getWorldManager().getIslandWorldName())) {
            return;
        }
        String message = event.getMessage();
        event.setCancelled(true);

        if (message.startsWith("@")) {
            String clean = message.substring(1).trim();
            String formatted = MessageUtil.color("&8[&aGlobal&8] &a" + player.getName() + "&7: &f" + clean);
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getWorld().getName().equalsIgnoreCase(plugin.getWorldManager().getIslandWorldName())) {
                    online.sendMessage(formatted);
                }
            }
            return;
        }

        Island island = plugin.getIslandManager().getIslandByPlayer(player.getUniqueId());
        boolean forcedIslandChat = isIslandChatEnabled(player.getUniqueId());
        if (forcedIslandChat && island != null) {
            String formatted = MessageUtil.color("&8[&2\u00cele&8] &a" + player.getName() + "&7: &f" + message);
            for (Player online : Bukkit.getOnlinePlayers()) {
                Island targetIsland = plugin.getIslandManager().getIslandByPlayer(online.getUniqueId());
                if (targetIsland != null && targetIsland.getOwner().equals(island.getOwner())) {
                    online.sendMessage(formatted);
                }
            }
            return;
        }

        Island currentIsland = plugin.getIslandManager().getIslandAt(player.getLocation());
        if (currentIsland == null) {
            player.sendMessage(MessageUtil.color("&8[&aGlobal&8] &a" + player.getName() + "&7: &f" + message));
            return;
        }
        String formatted = MessageUtil.color("&8[&2Plot/\u00cele&8] &a" + player.getName() + "&7: &f" + message);
        for (Player online : Bukkit.getOnlinePlayers()) {
            Island other = plugin.getIslandManager().getIslandAt(online.getLocation());
            if (other != null && other.getOwner().equals(currentIsland.getOwner())) {
                online.sendMessage(formatted);
            }
        }
    }
}
