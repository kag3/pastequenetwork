package fr.pastequeworld.hub;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HubPlugin extends JavaPlugin implements Listener, PluginMessageListener {

    // ===================== CONFIG =====================

    private static final int COMPASS_SLOT = 4;

    private static final String[] SERVER_NAMES = {
            "pastequeskyblock",
            "pastequebuild",
            "raft",
            "labyroyale"
    };

    // Stocke les joueurs connectes par serveur
    private final Map<String, Integer> playerCounts = new ConcurrentHashMap<String, Integer>();

    // ===================== ENABLE / DISABLE =====================

    @Override
    public void onEnable() {
        // Register BungeeCord channels
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);

        // Register events
        Bukkit.getPluginManager().registerEvents(this, this);

        // Initialiser les compteurs a 0
        for (String server : SERVER_NAMES) {
            playerCounts.put(server, 0);
        }

        // Rafraichir les compteurs toutes les 3 secondes
        new BukkitRunnable() {
            @Override
            public void run() {
                refreshPlayerCounts();
            }
        }.runTaskTimer(this, 60L, 60L);

        getLogger().info("========================================");
        getLogger().info("  PastequeHub v1.0.0");
        getLogger().info("  pasteque.world Network");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().unregisterIncomingPluginChannel(this, "BungeeCord", this);
    }

    // ===================== BUNGEE PLAYER COUNTS =====================

    private void refreshPlayerCounts() {
        if (Bukkit.getOnlinePlayers().isEmpty()) return;
        Player carrier = Bukkit.getOnlinePlayers().iterator().next();

        for (String server : SERVER_NAMES) {
            try {
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(b);
                out.writeUTF("PlayerCount");
                out.writeUTF(server);
                carrier.sendPluginMessage(this, "BungeeCord", b.toByteArray());
            } catch (IOException e) {
                // ignore
            }
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) return;

        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            String subchannel = in.readUTF();

            if (subchannel.equals("PlayerCount")) {
                String server = in.readUTF();
                int count = in.readInt();
                playerCounts.put(server, count);
            }
        } catch (IOException e) {
            // ignore
        }
    }

    // ===================== COMPASS ON JOIN =====================

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        Player player = event.getPlayer();

        // Petit delai pour que le joueur soit charge
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                giveCompass(player);
            }
        }.runTaskLater(this, 5L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
    }

    private void giveCompass(Player player) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        meta.setDisplayName(color("&2&lpasteque&7.&d&lworld"));
        List<String> lore = new ArrayList<String>();
        lore.add("");
        lore.add(color("&7Clic pour ouvrir le &amenu des serveurs"));
        lore.add("");
        meta.setLore(lore);
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        compass.setItemMeta(meta);

        player.getInventory().setItem(COMPASS_SLOT, compass);
    }

    // ===================== COMPASS CLICK =====================

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK
                && event.getAction() != Action.LEFT_CLICK_AIR
                && event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.COMPASS) return;

        event.setCancelled(true);
        openServerSelector(player);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (event.getItemDrop().getItemStack().getType() == Material.COMPASS) {
            event.setCancelled(true);
        }
    }

    // ===================== SERVER SELECTOR GUI =====================

    @SuppressWarnings("deprecation")
    private void openServerSelector(Player player) {
        String title = color("&8\u2726 &2&lpasteque&7.&d&lworld &8\u2726");
        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, 54, title);

        // === FOND ===

        // Bordure exterieure : verre vert fonce (data 13)
        ItemStack borderGreen = createGlass((short) 13, " ");
        // Filler interieur : verre noir (data 15)
        ItemStack borderBlack = createGlass((short) 15, " ");
        // Accent : verre lime (data 5) pour les coins
        ItemStack borderLime = createGlass((short) 5, " ");

        // Remplir tout en noir d'abord
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, borderBlack);
        }

        // Bordure haut (row 0)
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, (i == 0 || i == 8) ? borderLime : borderGreen);
        }
        // Bordure bas (row 5)
        for (int i = 45; i < 54; i++) {
            gui.setItem(i, (i == 45 || i == 53) ? borderLime : borderGreen);
        }
        // Bordure gauche/droite
        for (int row = 1; row < 5; row++) {
            gui.setItem(row * 9, borderGreen);       // gauche
            gui.setItem(row * 9 + 8, borderGreen);   // droite
        }

        // === DECORATION CENTRALES ===

        // Ligne separatrice row 2 (entre les 2 rangees d'items)
        ItemStack accentGlass = createGlass((short) 2, " "); // magenta
        gui.setItem(22, accentGlass); // centre row 2

        // === ITEMS SERVEURS ===

        // Slot 11 (row1, col2) : PastequeSkyBlock
        gui.setItem(11, createSkyBlockItem());

        // Slot 15 (row1, col6) : PastequeBuild
        gui.setItem(15, createBuildItem());

        // Slot 29 (row3, col2) : Raft
        gui.setItem(29, createRaftItem());

        // Slot 33 (row3, col6) : LabyRoyale
        gui.setItem(33, createLabyRoyaleItem());

        // === ITEM INFO CENTRAL ===

        // Slot 13 (row1, col4) : Etoile du Nether = info
        ItemStack infoItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName(color("&2&lpasteque&7.&d&lworld"));
        List<String> infoLore = new ArrayList<String>();
        infoLore.add("");
        infoLore.add(color("&7Bienvenue sur le r\u00e9seau"));
        infoLore.add(color("&2pasteque&7.&dworld &7!"));
        infoLore.add("");
        infoLore.add(color("&7Choisis un mode de jeu"));
        infoLore.add(color("&7et amuse-toi bien !"));
        infoLore.add("");
        infoMeta.setLore(infoLore);
        infoMeta.addEnchant(Enchantment.DURABILITY, 1, true);
        infoMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        infoItem.setItemMeta(infoMeta);
        gui.setItem(13, infoItem);

        // Slot 31 (row3, col4) : info joueurs total
        ItemStack statsItem = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        ItemMeta statsMeta = statsItem.getItemMeta();
        statsMeta.setDisplayName(color("&e&lJoueurs en ligne"));
        List<String> statsLore = new ArrayList<String>();
        statsLore.add("");
        int total = 0;
        for (String server : SERVER_NAMES) {
            total += playerCounts.containsKey(server) ? playerCounts.get(server) : 0;
        }
        total += Bukkit.getOnlinePlayers().size(); // + hub
        statsLore.add(color("&7Total: &a" + total + " joueurs"));
        statsLore.add("");
        statsMeta.setLore(statsLore);
        statsItem.setItemMeta(statsMeta);
        gui.setItem(31, statsItem);

        // === BOUTON FERMER ===

        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName(color("&c&lFermer"));
        closeItem.setItemMeta(closeMeta);
        gui.setItem(49, closeItem);

        player.openInventory(gui);
    }

    // ===================== ITEMS SERVEURS =====================

    private ItemStack createSkyBlockItem() {
        ItemStack item = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color("&b&lPastequeSkyBlock &7v1.1"));

        List<String> lore = new ArrayList<String>();
        lore.add("");
        lore.add(color("&7Un skyblock complet avec des"));
        lore.add(color("&7fonctionnalit\u00e9s &ain\u00e9dites &7et une"));
        lore.add(color("&7dimension &etr\u00e8s sociale&7."));
        lore.add("");
        lore.add(color("&7Joueurs: &a" + getCount("pastequeskyblock")));
        lore.add("");
        lore.add(color("&e\u25b6 Cliquez pour rejoindre"));
        meta.setLore(lore);

        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBuildItem() {
        ItemStack item = new ItemStack(Material.GRASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color("&a&lPastequeBuild"));

        List<String> lore = new ArrayList<String>();
        lore.add("");
        lore.add(color("&7Un Freecube &7plus &alibre &7avec"));
        lore.add(color("&7moins de restrictions et &ebeaucoup"));
        lore.add(color("&7plus social&7. Construis sans limites !"));
        lore.add("");
        lore.add(color("&7Joueurs: &a" + getCount("pastequebuild")));
        lore.add("");
        lore.add(color("&e\u25b6 Cliquez pour rejoindre"));
        meta.setLore(lore);

        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRaftItem() {
        ItemStack item = new ItemStack(Material.BOAT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color("&9&lRaft"));

        List<String> lore = new ArrayList<String>();
        lore.add("");
        lore.add(color("&7La reproduction du jeu vid\u00e9o"));
        lore.add(color("&bRaft &7adapt\u00e9e dans Minecraft !"));
        lore.add(color("&7Survis sur ton radeau en pleine mer."));
        lore.add("");
        lore.add(color("&8&oAcc\u00e8s en 1.16.5"));
        lore.add("");
        lore.add(color("&7Joueurs: &a" + getCount("raft")));
        lore.add("");
        lore.add(color("&e\u25b6 Cliquez pour rejoindre"));
        meta.setLore(lore);

        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createLabyRoyaleItem() {
        ItemStack item = new ItemStack(Material.GOLD_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color("&6&lLabyRoyale"));

        List<String> lore = new ArrayList<String>();
        lore.add("");
        lore.add(color("&7Battle Royale dans un &6labyrinthe"));
        lore.add(color("&7g\u00e9n\u00e9r\u00e9 al\u00e9atoirement ! Mine des"));
        lore.add(color("&7ressources, \u00e9quipe-toi et &c\u00e9limine"));
        lore.add(color("&7tous tes adversaires. &cLast man standing."));
        lore.add("");
        lore.add(color("&7Modes: &aSolo &7| &6Duel 1v1 &7| &bDuo"));
        lore.add(color("&7Joueurs: &a" + getCount("labyroyale")));
        lore.add("");
        lore.add(color("&e\u25b6 Cliquez pour rejoindre"));
        meta.setLore(lore);

        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    // ===================== GUI CLICK HANDLER =====================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();
        String expectedTitle = color("&8\u2726 &2&lpasteque&7.&d&lworld &8\u2726");

        if (!title.equals(expectedTitle)) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null) return;

        int slot = event.getRawSlot();

        switch (slot) {
            case 11:
                connectToServer(player, "pastequeskyblock");
                break;
            case 15:
                connectToServer(player, "pastequebuild");
                break;
            case 29:
                connectToServer(player, "raft");
                break;
            case 33:
                connectToServer(player, "labyroyale");
                break;
            case 49:
                player.closeInventory();
                break;
            default:
                break;
        }
    }

    // Empecher le joueur de bouger la boussole dans son inventaire
    @EventHandler
    public void onInventoryClickProtect(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.COMPASS) {
            String title = event.getView().getTitle();
            String selectorTitle = color("&8\u2726 &2&lpasteque&7.&d&lworld &8\u2726");
            if (!title.equals(selectorTitle)) {
                event.setCancelled(true);
            }
        }
    }

    // ===================== BUNGEE CONNECT =====================

    private void connectToServer(Player player, String server) {
        player.closeInventory();
        player.sendMessage(color("&2&l\u2726 &aConnexion \u00e0 &f" + server + "&a..."));

        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            out.writeUTF("Connect");
            out.writeUTF(server);
            player.sendPluginMessage(this, "BungeeCord", b.toByteArray());
        } catch (IOException e) {
            player.sendMessage(color("&c&l\u2726 &cErreur de connexion ! R\u00e9essayez."));
        }
    }

    // ===================== UTILS =====================

    @SuppressWarnings("deprecation")
    private ItemStack createGlass(short data, String name) {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));
        item.setItemMeta(meta);
        return item;
    }

    private int getCount(String server) {
        Integer count = playerCounts.get(server);
        return count != null ? count : 0;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
