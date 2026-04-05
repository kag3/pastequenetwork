package fr.pasteque.skyblock.gui;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class TabListManager {

    private final PastequeSkyblockPlugin plugin;

    public TabListManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public void setTabList(Player player) {
        double money = plugin.getEconomyManager().getBalance(player.getUniqueId());
        String moneyStr = plugin.getEconomyManager().format(money);

        int level = 0;
        try {
            level = plugin.getPlayerDataService().get(player).getLevel();
        } catch (Exception ignored) {
        }

        String header = PastequeSkyblockPlugin.color(
            "\n&2&lPASTEQUE &5&lSKYBLOCK\n&7Bienvenue &f" + player.getName() + " &7!\n"
        );
        String footer = PastequeSkyblockPlugin.color(
            "\n&8\u258E &5Saison 1 &8\u258E &7Niveau &b" + level + " &8\u258E &a" + moneyStr + "$\n&2play&8.&5pasteque&8.&2world\n"
        );

        try {
            sendTabPacket(player, header, footer);
        } catch (Exception ignored) {
            // Silently skip if reflection fails
        }
    }

    private void sendTabPacket(Player player, String header, String footer) throws Exception {
        String version = getServerVersion();

        // Get CraftPlayer handle
        Object craftPlayer = player.getClass().getMethod("getHandle").invoke(player);
        Object playerConnection = craftPlayer.getClass().getField("playerConnection").get(craftPlayer);

        // Build IChatBaseComponent for header and footer
        Class<?> chatComponentClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");
        Class<?> chatSerializerClass;
        try {
            chatSerializerClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");
        } catch (ClassNotFoundException e) {
            chatSerializerClass = Class.forName("net.minecraft.server." + version + ".ChatSerializer");
        }

        Method aMethod = chatSerializerClass.getMethod("a", String.class);
        Object headerComponent = aMethod.invoke(null, "{\"text\":\"" + escapeJson(header) + "\"}");
        Object footerComponent = aMethod.invoke(null, "{\"text\":\"" + escapeJson(footer) + "\"}");

        // Create PacketPlayOutPlayerListHeaderFooter
        Class<?> packetClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutPlayerListHeaderFooter");
        Object packet;
        try {
            // Try constructor that takes IChatBaseComponent (some versions)
            Constructor<?> constructor = packetClass.getConstructor(chatComponentClass);
            packet = constructor.newInstance(headerComponent);
            // Set footer via reflection
            Field footerField = findField(packetClass, "b");
            if (footerField == null) {
                // Try alternate field names
                for (Field f : packetClass.getDeclaredFields()) {
                    if (chatComponentClass.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        Object current = f.get(packet);
                        if (current == null || !current.equals(headerComponent)) {
                            footerField = f;
                            break;
                        }
                    }
                }
            }
            if (footerField != null) {
                footerField.setAccessible(true);
                footerField.set(packet, footerComponent);
            }
        } catch (NoSuchMethodException e) {
            // No-arg constructor, set both fields via reflection
            packet = packetClass.newInstance();
            Field[] fields = packetClass.getDeclaredFields();
            int chatFieldIndex = 0;
            for (Field f : fields) {
                if (chatComponentClass.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    if (chatFieldIndex == 0) {
                        f.set(packet, headerComponent);
                    } else if (chatFieldIndex == 1) {
                        f.set(packet, footerComponent);
                    }
                    chatFieldIndex++;
                }
            }
        }

        // Send packet
        Method sendPacket = playerConnection.getClass().getMethod("sendPacket",
            Class.forName("net.minecraft.server." + version + ".Packet"));
        sendPacket.invoke(playerConnection, packet);
    }

    private Field findField(Class<?> clazz, String name) {
        try {
            Field f = clazz.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private String getServerVersion() {
        String packageName = org.bukkit.Bukkit.getServer().getClass().getPackage().getName();
        return packageName.substring(packageName.lastIndexOf('.') + 1);
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
