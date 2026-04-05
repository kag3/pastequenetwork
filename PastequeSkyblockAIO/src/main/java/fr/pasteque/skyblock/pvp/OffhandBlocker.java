package fr.pasteque.skyblock.pvp;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

/**
 * Registers a PlayerSwapHandItemsEvent listener via reflection,
 * so it compiles even if the event class doesn't exist in the API stub.
 * At runtime on Spigot 1.9+, the event exists and the listener works.
 */
public class OffhandBlocker {

    public static void register(final PastequeSkyblockPlugin plugin) {
        try {
            // Check if the event class exists at runtime
            final Class<?> eventClass = Class.forName("org.bukkit.event.player.PlayerSwapHandItemsEvent");

            // Create a listener that cancels the event
            Listener listener = new Listener() {};

            // Register using Bukkit's event system via reflection
            // We register a generic handler that cancels the event
            org.bukkit.plugin.EventExecutor executor = new org.bukkit.plugin.EventExecutor() {
                @Override
                public void execute(Listener listener, org.bukkit.event.Event event) {
                    if (eventClass.isInstance(event)) {
                        try {
                            java.lang.reflect.Method setCancelled = event.getClass().getMethod("setCancelled", boolean.class);
                            setCancelled.invoke(event, true);
                        } catch (Throwable ignored) {}
                    }
                }
            };

            Bukkit.getPluginManager().registerEvent(
                (Class<? extends org.bukkit.event.Event>) eventClass,
                listener,
                org.bukkit.event.EventPriority.HIGHEST,
                executor,
                plugin,
                true // ignoreCancelled
            );

            plugin.getLogger().info("[PvP] Offhand swap (touche F) desactive.");
        } catch (ClassNotFoundException ignored) {
            // Event doesn't exist on this server version, skip
        }
    }
}
