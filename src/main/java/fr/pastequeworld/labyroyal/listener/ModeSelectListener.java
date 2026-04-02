package fr.pastequeworld.labyroyal.listener;

import fr.pastequeworld.labyroyal.LabyRoyalPlugin;
import fr.pastequeworld.labyroyal.game.LabyGameMode;
import fr.pastequeworld.labyroyal.util.MessageUtil;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ModeSelectListener implements Listener {

    private static final String GUI_TITLE = ChatColor.translateAlternateColorCodes('&',
            "&2&lLabyRoyale &8- &7Choisis ton mode");

    private final LabyRoyalPlugin plugin;

    // Joueurs qui ont fait un choix (pour ne pas re-ouvrir le GUI)
    private final Set<UUID> hasChosen = new HashSet<UUID>();

    public ModeSelectListener(LabyRoyalPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Si le joueur est deja dans une partie, on ne montre pas le GUI
        if (plugin.getGameManager().getPlayerGame(player.getUniqueId()) != null) return;

        hasChosen.remove(player.getUniqueId());

        // Ouvrir le GUI de selection apres un petit delai
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline() && !hasChosen.contains(player.getUniqueId())) {
                    openModeSelectGUI(player);
                }
            }
        }, 15L);
    }

    @SuppressWarnings("deprecation")
    public void openModeSelectGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, GUI_TITLE);

        // Fond en vitres noires
        ItemStack filler = createGlass((short) 15, " ");
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, filler);
        }

        // Bordure verte en haut et en bas
        ItemStack border = createGlass((short) 13, " ");
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, border);
        }
        for (int i = 18; i < 27; i++) {
            gui.setItem(i, border);
        }

        // === SOLO (slot 11) ===
        ItemStack soloItem = new ItemStack(Material.IRON_SWORD);
        ItemMeta soloMeta = soloItem.getItemMeta();
        soloMeta.setDisplayName(color("&a&lSOLO"));
        List<String> soloLore = new ArrayList<String>();
        soloLore.add("");
        soloLore.add(color("&7Mode: &fChacun pour soi"));
        soloLore.add(color("&7Joueurs: &f6 a 8"));
        soloLore.add(color("&7Labyrinthe: &f128x128"));
        soloLore.add("");
        soloLore.add(color("&e\u25b6 Cliquez pour rejoindre"));
        soloMeta.setLore(soloLore);
        soloMeta.addEnchant(Enchantment.DURABILITY, 1, true);
        soloMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        soloMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        soloItem.setItemMeta(soloMeta);
        gui.setItem(11, soloItem);

        // === INFO CENTRE (slot 13) ===
        ItemStack infoItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName(color("&6&lLabyRoyale"));
        List<String> infoLore = new ArrayList<String>();
        infoLore.add("");
        infoLore.add(color("&7Battle Royale en Labyrinthe"));
        infoLore.add(color("&7par &2pasteque&7.&dworld"));
        infoLore.add("");
        infoLore.add(color("&8Choisissez un mode"));
        infoMeta.setLore(infoLore);
        infoMeta.addEnchant(Enchantment.DURABILITY, 1, true);
        infoMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        infoItem.setItemMeta(infoMeta);
        gui.setItem(13, infoItem);

        // === DUO (slot 15) ===
        ItemStack duoItem = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta duoMeta = duoItem.getItemMeta();
        duoMeta.setDisplayName(color("&b&lDUO"));
        List<String> duoLore = new ArrayList<String>();
        duoLore.add("");
        duoLore.add(color("&7Mode: &fEquipes de 2"));
        duoLore.add(color("&7Joueurs: &f8 a 16"));
        duoLore.add(color("&7Labyrinthe: &fPlus grand"));
        duoLore.add("");
        duoLore.add(color("&e\u25b6 Cliquez pour rejoindre"));
        duoMeta.setLore(duoLore);
        duoMeta.addEnchant(Enchantment.DURABILITY, 1, true);
        duoMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        duoMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        duoItem.setItemMeta(duoMeta);
        gui.setItem(15, duoItem);

        // === RETOUR AU HUB (slot 22, centre bas) ===
        ItemStack backItem = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(color("&c&lRetour au Hub"));
        List<String> backLore = new ArrayList<String>();
        backLore.add("");
        backLore.add(color("&7Cliquez pour retourner au hub"));
        backMeta.setLore(backLore);
        backItem.setItemMeta(backMeta);
        gui.setItem(22, backItem);

        player.openInventory(gui);
    }

    // ===== CLIC DANS LE GUI =====

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (event.getView().getTitle() == null) return;
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null) return;

        int slot = event.getRawSlot();

        if (slot == 11) {
            // SOLO
            hasChosen.add(player.getUniqueId());
            player.closeInventory();
            MessageUtil.send(player, "&eRecherche d'une partie Solo...");
            boolean joined = plugin.getGameManager().joinGame(player, LabyGameMode.SOLO);
            if (joined) {
                MessageUtil.send(player, "&aVous avez rejoint une partie Solo !");
            }
        } else if (slot == 15) {
            // DUO
            hasChosen.add(player.getUniqueId());
            player.closeInventory();
            MessageUtil.send(player, "&eRecherche d'une partie Duo...");
            boolean joined = plugin.getGameManager().joinGame(player, LabyGameMode.DUO);
            if (joined) {
                MessageUtil.send(player, "&aVous avez rejoint une partie Duo !");
            }
        } else if (slot == 22) {
            // RETOUR AU HUB
            hasChosen.add(player.getUniqueId());
            player.closeInventory();
            plugin.sendToHub(player);
        }
    }

    // ===== FERMETURE DU GUI SANS CHOISIR → RE-OUVRIR =====

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        final Player player = (Player) event.getPlayer();

        if (event.getView().getTitle() == null) return;
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;

        // Si le joueur n'a pas fait de choix, re-ouvrir apres un petit delai
        if (!hasChosen.contains(player.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    if (player.isOnline()
                            && !hasChosen.contains(player.getUniqueId())
                            && plugin.getGameManager().getPlayerGame(player.getUniqueId()) == null) {
                        openModeSelectGUI(player);
                    }
                }
            }, 20L); // 1 seconde
        }
    }

    public void clearPlayer(UUID uuid) {
        hasChosen.remove(uuid);
    }

    // ===== UTILS =====

    @SuppressWarnings("deprecation")
    private ItemStack createGlass(short data, String name) {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));
        item.setItemMeta(meta);
        return item;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
