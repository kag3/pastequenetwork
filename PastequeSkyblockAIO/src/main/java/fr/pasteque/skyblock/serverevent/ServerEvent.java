package fr.pasteque.skyblock.serverevent;

/**
 * A server event runs independently of all other events. Multiple events can
 * be active simultaneously. Each event manages its own lifecycle, spawn zone,
 * rewards and cleanup.
 */
public interface ServerEvent {
    String getId();
    String getDisplayName();
    boolean isActive();
    void start();
    void stop();
    long getRemainingSeconds();
}
