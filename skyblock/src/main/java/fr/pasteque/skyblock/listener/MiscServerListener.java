package fr.pasteque.skyblock.listener;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;

public class MiscServerListener implements Listener {
    private final PastequeSkyblockPlugin plugin;
    public MiscServerListener(PastequeSkyblockPlugin plugin) { this.plugin = plugin; }

    @EventHandler(ignoreCancelled = true)
    public void onBlockedCommands(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().toLowerCase();
        if (message.equals("/shop") || message.startsWith("/shop ") || message.equals("/sell") || message.startsWith("/sell ")) {
            event.setCancelled(true);
            MessageUtil.send(event.getPlayer(), plugin.getPrefix(), "&fLes commandes /shop et /sell sont désactivées. Déplace-toi physiquement au Hub pour commercer.");
        }
    }

    @EventHandler
    public void onInvasionDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.hasMetadata("pasteque_invasion")) return;
        event.getDrops().add(new ItemStack(Material.MELON, 2));
        Player killer = entity.getKiller();
        if (killer != null) {
            plugin.getEconomyManager().add(killer.getUniqueId(), plugin.getConfig().getDouble("invasions.kill-reward", 25.0D));
            MessageUtil.send(killer, plugin.getPrefix(), "&aTu as gagné des Pastèque en repoussant l'invasion du Hub.");
        }
    }
}
