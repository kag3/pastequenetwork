package fr.pasteque.skyblock.serverevent;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Chasse au tresor: 5 coffres sont caches aleatoirement autour du spawn general.
 * Chaque coffre ne peut etre ouvert qu'une fois. Le premier joueur a l'ouvrir
 * recoit le contenu. Event se termine quand tous les coffres sont pris ou
 * apres 15 minutes.
 */
public class TreasureHuntEvent implements ServerEvent {
    private final PastequeSkyblockPlugin plugin;
    private boolean active = false;
    private long endAt = 0L;
    private int taskId = -1;
    private final List<Location> chests = new ArrayList<Location>();
    private final Set<Location> looted = new HashSet<Location>();
    private final Random random = new Random();

    public TreasureHuntEvent(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "treasure"; }
    @Override public String getDisplayName() { return "Chasse au Tresor"; }
    @Override public boolean isActive() { return active; }
    @Override public long getRemainingSeconds() {
        if (!active) return 0L;
        return Math.max(0L, (endAt - System.currentTimeMillis()) / 1000L);
    }

    @Override
    public void start() {
        if (active) return;
        active = true;
        endAt = System.currentTimeMillis() + (15L * 60L * 1000L);
        chests.clear();
        looted.clear();

        Location spawn = plugin.getWorldManager().getServerSpawn();
        if (spawn == null || spawn.getWorld() == null) return;
        World w = spawn.getWorld();
        for (int i = 0; i < 5; i++) {
            int ox = random.nextInt(200) - 100;
            int oz = random.nextInt(200) - 100;
            int x = spawn.getBlockX() + ox;
            int z = spawn.getBlockZ() + oz;
            int y = w.getHighestBlockYAt(x, z) + 1;
            Location loc = new Location(w, x, y, z);
            loc.getBlock().setType(Material.CHEST);
            try {
                Chest chest = (Chest) loc.getBlock().getState();
                chest.getBlockInventory().clear();
                chest.getBlockInventory().addItem(new ItemStack(Material.DIAMOND, 3 + random.nextInt(5)));
                chest.getBlockInventory().addItem(new ItemStack(Material.GOLD_INGOT, 10 + random.nextInt(10)));
                chest.getBlockInventory().addItem(new ItemStack(Material.EMERALD, 5 + random.nextInt(5)));
                chest.getBlockInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 2));
                if (random.nextInt(5) == 0)
                    chest.getBlockInventory().addItem(new ItemStack(Material.NETHER_STAR, 1));
                chest.update(true);
            } catch (Throwable ignored) {}
            chests.add(loc);
        }
        Bukkit.broadcastMessage(PastequeSkyblockPlugin.color(
                plugin.getPrefix() + "&b&l\u26b1 CHASSE AU TRESOR &f: &e5 coffres caches &faux alentours du spawn ! &aPremier arrive, premier servi !"));

        taskId = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override public void run() {
                if (System.currentTimeMillis() >= endAt || looted.size() >= chests.size()) stop();
            }
        }, 20L, 20L).getTaskId();
    }

    public boolean onChestOpen(Player player, Location loc) {
        if (!active) return false;
        for (Location c : chests) {
            if (c.getBlockX() == loc.getBlockX() && c.getBlockY() == loc.getBlockY()
                    && c.getBlockZ() == loc.getBlockZ() && c.getWorld().equals(loc.getWorld())) {
                if (looted.contains(c)) return false;
                looted.add(c);
                player.sendMessage(PastequeSkyblockPlugin.color(
                        plugin.getPrefix() + "&b\u26b1 Vous avez trouve un tresor ! &a(" + looted.size() + "/" + chests.size() + ")"));
                Bukkit.broadcastMessage(PastequeSkyblockPlugin.color(
                        plugin.getPrefix() + "&b" + player.getName() + " &fa trouve un tresor cache ! &7(" + looted.size() + "/" + chests.size() + ")"));
                return true;
            }
        }
        return false;
    }

    @Override
    public void stop() {
        if (!active) return;
        active = false;
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        taskId = -1;
        for (Location c : chests) {
            if (c.getBlock().getType() == Material.CHEST) c.getBlock().setType(Material.AIR);
        }
        chests.clear();
        looted.clear();
        Bukkit.broadcastMessage(PastequeSkyblockPlugin.color(
                plugin.getPrefix() + "&bLa chasse au tresor est terminee !"));
    }
}
