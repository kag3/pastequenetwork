package fr.pasteque.skyblock.listener;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.manager.DataFile;
import fr.pasteque.skyblock.model.CoopIsland;
import fr.pasteque.skyblock.model.Island;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Calendar;

public class ConnectionListener implements Listener {
    private final PastequeSkyblockPlugin plugin;
    private final DataFile dailyRewards;

    public ConnectionListener(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.dailyRewards = new DataFile(plugin, "daily-rewards.yml");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        plugin.getCombatManager().notifyReconnect(player);
        grantDailyReward(player);
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override public void run() { player.teleport(plugin.getWorldManager().getServerSpawn()); }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) { plugin.getCombatManager().handleQuit(event.getPlayer()); }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) { event.setRespawnLocation(plugin.getWorldManager().getServerSpawn()); }

    @EventHandler
    public void onMoveVoid(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getTo().getWorld() == null) return;
        Player player = event.getPlayer();
        boolean soloWorld = event.getTo().getWorld().getName().equalsIgnoreCase(plugin.getWorldManager().getIslandWorldName());
        boolean coopWorld = event.getTo().getWorld().getName().equalsIgnoreCase(plugin.getWorldManager().getCoopWorldName());
        if (!soloWorld && !coopWorld) return;
        int voidY = plugin.getConfig().getInt("island.void-y", 20);
        if (event.getTo().getY() > voidY) return;
        Location location = plugin.getWorldManager().getServerSpawn();
        if (soloWorld) {
            Island island = plugin.getIslandManager().getIslandByPlayer(player.getUniqueId());
            location = island == null ? plugin.getWorldManager().getServerSpawn() : plugin.getIslandManager().getSafeTeleport(island);
        } else {
            CoopIsland coop = plugin.getCoopManager().getFirstIsland(player.getUniqueId());
            location = coop == null ? plugin.getWorldManager().getServerSpawn() : plugin.getCoopManager().getSafeTeleport(coop);
        }
        player.teleport(location);
    }

    private void grantDailyReward(Player player) {
        Calendar now = Calendar.getInstance();
        long today = (now.get(Calendar.YEAR) * 1000L) + now.get(Calendar.DAY_OF_YEAR);
        String path = "players." + player.getUniqueId().toString() + ".day";
        long stored = dailyRewards.getConfig().getLong(path, -1L);
        if (stored == today) {
            return;
        }
        double reward = plugin.getConfig().getDouble("economy.daily-login-reward", 150.0D);
        plugin.getEconomyManager().add(player.getUniqueId(), reward);
        plugin.getEconomyManager().save();
        dailyRewards.getConfig().set(path, today);
        dailyRewards.save();
        String currencyName = plugin.getEconomyManager().getCurrencyName();
        MessageUtil.send(player, plugin.getPrefix(), "&6\u2726 &eBonus quotidien : &a+" + reward + " " + currencyName + " &6\u2726");
    }
}
