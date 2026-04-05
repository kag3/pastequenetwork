package fr.pasteque.skyblock.serverevent;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

/**
 * Pluie de meteores: des blocs "meteore" tombent du ciel autour du spawn pvp.
 * Quand un joueur casse un meteore au sol, il recoit des recompenses aleatoires
 * (lingot d'or, diamant, emeraude, XP). Dure 10 minutes, lance independamment
 * de tous les autres events.
 */
public class MeteorShowerEvent implements ServerEvent {
    private final PastequeSkyblockPlugin plugin;
    private boolean active = false;
    private long endAt = 0L;
    private int taskId = -1;
    private final Random random = new Random();

    public MeteorShowerEvent(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "meteor"; }
    @Override public String getDisplayName() { return "Pluie de Meteores"; }
    @Override public boolean isActive() { return active; }
    @Override public long getRemainingSeconds() {
        if (!active) return 0L;
        return Math.max(0L, (endAt - System.currentTimeMillis()) / 1000L);
    }

    @Override
    public void start() {
        if (active) return;
        active = true;
        endAt = System.currentTimeMillis() + (10L * 60L * 1000L);
        Bukkit.broadcastMessage(PastequeSkyblockPlugin.color(
                plugin.getPrefix() + "&6&l\u2600 PLUIE DE METEORES &fen cours au spawn PvP ! &eCassez les meteores pour des recompenses !"));

        taskId = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override public void run() {
                if (System.currentTimeMillis() >= endAt) { stop(); return; }
                spawnMeteor();
            }
        }, 60L, 60L).getTaskId();
    }

    private void spawnMeteor() {
        Location spawn = plugin.getWorldManager().getServerSpawn();
        if (spawn == null || spawn.getWorld() == null) return;
        World w = spawn.getWorld();
        int radius = 60;
        int ox = random.nextInt(radius * 2) - radius;
        int oz = random.nextInt(radius * 2) - radius;
        int x = spawn.getBlockX() + ox;
        int z = spawn.getBlockZ() + oz;
        int y = w.getHighestBlockYAt(x, z) + 25;
        final Location drop = new Location(w, x + 0.5, y, z + 0.5);
        w.strikeLightningEffect(drop);

        new BukkitRunnable() {
            @Override public void run() {
                if (drop.getWorld() == null) return;
                int gy = drop.getWorld().getHighestBlockYAt(drop.getBlockX(), drop.getBlockZ());
                Location place = new Location(drop.getWorld(), drop.getBlockX(), gy, drop.getBlockZ());
                place.getBlock().setType(Material.OBSIDIAN);
                place.getWorld().strikeLightningEffect(place);
            }
        }.runTaskLater(plugin, 20L);
    }

    /**
     * Called by listener when a player breaks a meteor block (obsidian near spawn).
     */
    public boolean onMeteorBreak(Player player, Location loc) {
        if (!active) return false;
        Location spawn = plugin.getWorldManager().getServerSpawn();
        if (spawn == null || spawn.getWorld() == null || !spawn.getWorld().equals(loc.getWorld())) return false;
        if (spawn.distanceSquared(loc) > 120 * 120) return false;

        int roll = random.nextInt(100);
        ItemStack reward;
        if (roll < 50) reward = new ItemStack(Material.GOLD_INGOT, 3 + random.nextInt(4));
        else if (roll < 80) reward = new ItemStack(Material.DIAMOND, 1 + random.nextInt(2));
        else if (roll < 95) reward = new ItemStack(Material.EMERALD, 2 + random.nextInt(3));
        else reward = new ItemStack(Material.NETHER_STAR, 1);

        player.getInventory().addItem(reward);
        plugin.getEconomyManager().add(player.getUniqueId(), 100 + random.nextInt(400));
        player.sendMessage(PastequeSkyblockPlugin.color(
                plugin.getPrefix() + "&6+ " + reward.getAmount() + " " + reward.getType().name().toLowerCase() + " &fet &a+" + (100 + random.nextInt(400)) + " Pasteque"));
        return true;
    }

    @Override
    public void stop() {
        if (!active) return;
        active = false;
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        taskId = -1;
        Bukkit.broadcastMessage(PastequeSkyblockPlugin.color(
                plugin.getPrefix() + "&6La pluie de meteores est terminee !"));
    }
}
