package fr.pasteque.skyblock.manager;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class CombatManager {
    private final PastequeSkyblockPlugin plugin;
    private final DataFile dataFile;
    private final Map<UUID, Long> taggedUntil = new HashMap<UUID, Long>();
    private final Set<UUID> warned = new HashSet<UUID>();

    public CombatManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new DataFile(plugin, "combatlog.yml");
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override public void run() { tick(); }
        }, 20L, 20L);
    }
    public void save() { dataFile.save(); }
    public void tag(Player a, Player b) { long until = System.currentTimeMillis() + (plugin.getConfig().getLong("combat.tag-seconds", 30) * 1000L); tag(a, until); tag(b, until); }
    private void tag(Player p, long until) { boolean entering = !isTagged(p.getUniqueId()); taggedUntil.put(p.getUniqueId(), until); if (entering) MessageUtil.send(p, plugin.getPrefix(), "&dMode combat activé pendant 30 secondes. Déconnexion interdite."); warned.remove(p.getUniqueId()); }
    public boolean isTagged(UUID id) { return taggedUntil.containsKey(id) && taggedUntil.get(id) > System.currentTimeMillis(); }
    private void tick() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> it = taggedUntil.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> e = it.next();
            if (e.getValue() <= now) {
                Player player = Bukkit.getPlayer(e.getKey());
                if (player != null) MessageUtil.send(player, plugin.getPrefix(), "&aTu es sorti du mode combat.");
                warned.remove(e.getKey());
                it.remove();
            } else if (!warned.contains(e.getKey())) {
                warned.add(e.getKey());
            }
        }
    }
    public void handleQuit(Player player) {
        if (!isTagged(player.getUniqueId())) return;
        player.getInventory().clear(); player.getInventory().setArmorContents(null);
        double loss = plugin.getConfig().getDouble("combat.logout-loss", 500.0D);
        plugin.getEconomyManager().take(player.getUniqueId(), loss);
        dataFile.getConfig().set("messages." + player.getUniqueId(), "&dSanction combat-log : ton inventaire a été vidé et tu as perdu " + loss + " " + plugin.getEconomyManager().getCurrencyName() + ".");
        dataFile.save();
        taggedUntil.remove(player.getUniqueId()); warned.remove(player.getUniqueId());
    }
    public void notifyReconnect(Player player) {
        String path = "messages." + player.getUniqueId();
        String message = dataFile.getConfig().getString(path);
        if (message != null && !message.isEmpty()) {
            MessageUtil.send(player, plugin.getPrefix(), message);
            dataFile.getConfig().set(path, null);
            dataFile.save();
        }
    }
}
