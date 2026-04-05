package fr.pasteque.skyblock.arena;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BountyService {

    private final PastequeSkyblockPlugin plugin;
    private final File file;
    private final HashMap<UUID, Double> bounties = new HashMap<UUID, Double>();

    public BountyService(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "bounties.yml");
    }

    // =========================================================================
    //  Persistence
    // =========================================================================

    public void load() {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        bounties.clear();
        ConfigurationSection section = config.getConfigurationSection("bounties");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    bounties.put(uuid, section.getDouble(key));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Double> entry : bounties.entrySet()) {
            config.set("bounties." + entry.getKey().toString(), entry.getValue());
        }
        try {
            config.save(file);
        } catch (IOException ignored) {}
    }

    // =========================================================================
    //  Bounty logic
    // =========================================================================

    @SuppressWarnings("deprecation")
    public void placeBounty(Player placer, String targetName, double amount) {
        if (amount <= 0) {
            placer.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cLe montant doit etre superieur a 0."));
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            placer.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cJoueur introuvable."));
            return;
        }

        if (target.getUniqueId().equals(placer.getUniqueId())) {
            placer.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cVous ne pouvez pas placer une prime sur vous-meme."));
            return;
        }

        if (!plugin.getEconomyManager().take(placer.getUniqueId(), amount)) {
            placer.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cVous n'avez pas assez de " + plugin.getEconomyManager().getCurrencyName() + "."));
            return;
        }

        double current = getBounty(target.getUniqueId());
        bounties.put(target.getUniqueId(), current + amount);
        save();

        placer.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&fVous avez place une prime de &d" + (int) amount + " " + plugin.getEconomyManager().getCurrencyName() + " &fsur &c" + target.getName() + "&f."));

        target.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&c&lATTENTION ! &fUne prime de &d" + (int) amount + " " + plugin.getEconomyManager().getCurrencyName() + " &fa ete placee sur votre tete !"));

        // Broadcast
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getUniqueId().equals(placer.getUniqueId()) && !p.getUniqueId().equals(target.getUniqueId())) {
                p.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&c" + target.getName() + " &fa une prime de &d" + (int) (current + amount) + " " + plugin.getEconomyManager().getCurrencyName() + " &fsur sa tete !"));
            }
        }
    }

    public double getBounty(UUID uuid) {
        if (!bounties.containsKey(uuid)) {
            return 0.0;
        }
        return bounties.get(uuid);
    }

    public void claimBounty(Player killer, UUID victimId) {
        double bounty = getBounty(victimId);
        if (bounty <= 0) {
            return;
        }

        plugin.getEconomyManager().add(killer.getUniqueId(), bounty);
        bounties.remove(victimId);
        save();

        killer.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&a&lPRIME RECLAMEE ! &fVous recevez &d" + (int) bounty + " " + plugin.getEconomyManager().getCurrencyName() + "&f !"));

        // Broadcast
        Player victim = Bukkit.getPlayer(victimId);
        String victimName = victim != null ? victim.getName() : victimId.toString();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getUniqueId().equals(killer.getUniqueId())) {
                p.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&d" + killer.getName() + " &fa reclame la prime de &c" + (int) bounty + " " + plugin.getEconomyManager().getCurrencyName() + " &fsur &d" + victimName + "&f !"));
            }
        }
    }

    // =========================================================================
    //  Leaderboard
    // =========================================================================

    public List<Map.Entry<UUID, Double>> getTopBounties(int count) {
        List<Map.Entry<UUID, Double>> entries = new ArrayList<Map.Entry<UUID, Double>>(bounties.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<UUID, Double>>() {
            @Override
            public int compare(Map.Entry<UUID, Double> a, Map.Entry<UUID, Double> b) {
                return Double.compare(b.getValue(), a.getValue());
            }
        });
        if (entries.size() > count) {
            return entries.subList(0, count);
        }
        return entries;
    }
}
