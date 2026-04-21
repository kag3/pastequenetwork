package fr.pastequeworld.bedwars.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Envoi d'actionbars via reflection NMS (1.9.4).
 * Fallback : message classique si la reflection echoue.
 */
public final class ActionBarUtil {

    private static final String VERSION;
    private static Class<?> chatBaseComponent;
    private static Class<?> packetPlayOutChatClass;
    private static Class<?> chatSerializer;
    private static Method chatSerializerMethod;
    private static Constructor<?> packetConstructor;
    private static Method sendPacketMethod;
    private static Method getHandleMethod;
    private static java.lang.reflect.Field playerConnectionField;
    private static boolean initialized;

    static {
        String name = Bukkit.getServer().getClass().getPackage().getName();
        VERSION = name.substring(name.lastIndexOf('.') + 1);
        try {
            chatBaseComponent = Class.forName("net.minecraft.server." + VERSION + ".IChatBaseComponent");
            packetPlayOutChatClass = Class.forName("net.minecraft.server." + VERSION + ".PacketPlayOutChat");
            try {
                chatSerializer = Class.forName("net.minecraft.server." + VERSION + ".IChatBaseComponent$ChatSerializer");
            } catch (ClassNotFoundException e) {
                chatSerializer = Class.forName("net.minecraft.server." + VERSION + ".ChatSerializer");
            }
            chatSerializerMethod = chatSerializer.getMethod("a", String.class);
            packetConstructor = packetPlayOutChatClass.getConstructor(chatBaseComponent, byte.class);

            Class<?> craftPlayer = Class.forName("org.bukkit.craftbukkit." + VERSION + ".entity.CraftPlayer");
            getHandleMethod = craftPlayer.getMethod("getHandle");
            Class<?> entityPlayer = Class.forName("net.minecraft.server." + VERSION + ".EntityPlayer");
            playerConnectionField = entityPlayer.getField("playerConnection");
            Class<?> playerConnection = Class.forName("net.minecraft.server." + VERSION + ".PlayerConnection");
            Class<?> packetClass = Class.forName("net.minecraft.server." + VERSION + ".Packet");
            sendPacketMethod = playerConnection.getMethod("sendPacket", packetClass);
            initialized = true;
        } catch (Exception e) {
            initialized = false;
        }
    }

    private ActionBarUtil() {}

    public static void send(Player player, String message) {
        if (player == null || !player.isOnline()) return;
        String colored = ColorUtil.color(message);
        if (!initialized) {
            player.sendMessage(colored);
            return;
        }
        try {
            String json = "{\"text\":\"" + escape(colored) + "\"}";
            Object component = chatSerializerMethod.invoke(null, json);
            Object packet = packetConstructor.newInstance(component, (byte) 2);
            Object handle = getHandleMethod.invoke(player);
            Object connection = playerConnectionField.get(handle);
            sendPacketMethod.invoke(connection, packet);
        } catch (Exception e) {
            player.sendMessage(colored);
        }
    }

    private static String escape(String raw) {
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
