package fr.pastequeworld.friends;

import fr.pastequeworld.friends.command.FriendCommand;
import fr.pastequeworld.friends.data.FriendManager;
import fr.pastequeworld.friends.listener.FriendListener;

import net.md_5.bungee.api.plugin.Plugin;

public class PastequeFriends extends Plugin {

    private FriendManager friendManager;

    @Override
    public void onEnable() {
        friendManager = new FriendManager(this);
        friendManager.load();

        getProxy().getPluginManager().registerCommand(this, new FriendCommand(this));
        getProxy().getPluginManager().registerListener(this, new FriendListener(this));

        getLogger().info("========================================");
        getLogger().info("  PastequeFriends v" + getDescription().getVersion());
        getLogger().info("  Système d'amis - PastequeWorld Network");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        if (friendManager != null) {
            friendManager.save();
        }
        getLogger().info("PastequeFriends désactivé.");
    }

    public FriendManager getFriendManager() {
        return friendManager;
    }
}
