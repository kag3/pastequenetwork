package fr.pastequeworld.labyroyal.listener;

import fr.pastequeworld.labyroyal.LabyRoyalPlugin;
import fr.pastequeworld.labyroyal.game.LabyGameMode;
import fr.pastequeworld.labyroyal.util.MessageUtil;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ModeSelectListener implements Listener {

    private static final String GUI_TITLE = ChatColor.translateAlternateColorCodes('&',
            "&2&lLabyRoyale &8- &7Choisis ton mode");

    private final LabyRoyalPlugin plugin;

    public ModeSelectListener(LabyRoyalPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Si le joueur est deja dans une partie, on ne montre pas le GUI
        if (plugin.getGameManager().getPlayerGame(player.getUniqueId()) != null) return;

        // Ouvrir le GUI de selection apres un petit delai (attendre le chargement complet)
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    openModeSelectGUI(player);
                }
            }
        }, 15L); // 0.75 seconde
    }

    public void openModeSelectGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, GUI_TITLE);

        // Fond en vitres noires
        ItemStack filler = createItem(Material.STAINED_GLASS_PANE, (short) 15, " ");
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, filler);
        }

        // Item SOLO (slot 11) - Epee en fer
        ItemStack soloItem = createItem(Material.IRON_SWORD, (short) 0, "&a&lSOLO");
        ItemMeta soloMeta = soloItem.getItemMeta();
        List<String> soloLore = new ArrayList<String>();
        soloLore.add("");
        soloLore.add(color("&7Mode: &fChacun pour soi"));
        soloLore.add(color("&7Joueurs: &f6 a 8"));
        soloLore.add(color("&7Labyrinthe: &f128x128"));
        soloLore.add("");
        soloLore.add(color("&eCliquez pour rejoindre !"));
        soloMeta.setLore(soloLore);
        soloItem.setItemMeta(soloMeta);
        gui.setItem(11, soloItem);

        // Item DUO (slot 15) - Epee en diamant
        ItemStack duoItem = createItem(Material.DIAMOND_SWORD, (short) 0, "&b&lDUO");
        ItemMeta duoMeta = duoItem.getItemMeta();
        List<String> duoLore = new ArrayList<String>();
        duoLore.add("");
        duoLore.add(color("&7Mode: &fEquipes de 2"));
        duoLore.add(color("&7Joueurs: &f8 a 16"));
        duoLore.add(color("&7Labyrinthe: &fPlus grand"));
        duoLore.add("");
        duoLore.add(color("&eCliquez pour rejoindre !"));
        duoMeta.setLore(duoLore);
        duoItem.setItemMeta(duoMeta);
        gui.setItem(15, duoItem);

        // Info au centre (slot 13) - Boussole
        ItemStack infoItem = createItem(Material.COMPASS, (short) 0, "&6&lLabyRoyale");
        ItemMeta infoMeta = infoItem.getItemMeta();
        List<String> infoLore = new ArrayList<String>();
        infoLore.add("");
        infoLore.add(color("&7Battle Royale en Labyrinthe"));
        infoLore.add(color("&7par &2pasteque&7.&dworld"));
        infoLore.add("");
        infoLore.add(color("&8Choisissez un mode a gauche ou a droite"));
        infoMeta.setLore(infoLore);
        infoItem.setItemMeta(infoMeta);
        gui.setItem(13, infoItem);

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (event.getView().getTitle() == null) return;
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null) return;
        Material type = event.getCurrentItem().getType();

        if (type == Material.IRON_SWORD) {
            // SOLO
            player.closeInventory();
            MessageUtil.send(player, "&eRecherche d'une partie Solo...");
            boolean joined = plugin.getGameManager().joinGame(player, LabyGameMode.SOLO);
            if (joined) {
                MessageUtil.send(player, "&aVous avez rejoint une partie Solo !");
            }
        } else if (type == Material.DIAMOND_SWORD) {
            // DUO
            player.closeInventory();
            MessageUtil.send(player, "&eRecherche d'une partie Duo...");
            boolean joined = plugin.getGameManager().joinGame(player, LabyGameMode.DUO);
            if (joined) {
                MessageUtil.send(player, "&aVous avez rejoint une partie Duo !");
            }
        }
    }

    @SuppressWarnings("deprecation")
    private ItemStack createItem(Material material, short data, String name) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));
        item.setItemMeta(meta);
        return item;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
