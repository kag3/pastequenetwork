package fr.pasteque.skyblock.listener;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.model.Island;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.meta.FireworkMeta;

public class EmbassyListener implements Listener {
    private final PastequeSkyblockPlugin plugin;
    public EmbassyListener(PastequeSkyblockPlugin plugin) { this.plugin = plugin; }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (block == null || !plugin.getIslandManager().isSkyblockWorld(block.getLocation())) return;
        Material embassy = Material.matchMaterial(plugin.getConfig().getString("embassy.unlock-material", "MELON_BLOCK"));
        if (embassy == null || block.getType() != embassy) return;
        Player player = event.getPlayer();
        Island island = plugin.getIslandManager().getOwnedIsland(player.getUniqueId());
        if (island == null || !island.isInside(block.getLocation(), plugin.getConfig().getInt("island.size", 320))) return;
        plugin.getIslandManager().addInviteUnlock(island);
        Location loc = block.getLocation().add(0.5D, 0.5D, 0.5D);
        try { block.getWorld().playEffect(loc, Effect.valueOf("HAPPY_VILLAGER"), 1); } catch (Throwable ignored) {}
        try { block.getWorld().playEffect(loc, Effect.MOBSPAWNER_FLAMES, 1); } catch (Throwable ignored) {}
        try { player.playSound(loc, org.bukkit.Sound.valueOf("ENTITY_PLAYER_LEVELUP"), 1.0F, 1.1F); } catch (Throwable ignored) {}
        MessageUtil.send(player, plugin.getPrefix(), "&aAmbassade activ\u00e9e ! &7Tu as d\u00e9bloqu\u00e9 une invitation. Utilise &f/is invite <joueur>&7.");
        Firework fw = block.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder().withColor(Color.FUCHSIA).withFade(Color.WHITE).with(FireworkEffect.Type.BALL_LARGE).trail(true).build());
        meta.setPower(0);
        fw.setFireworkMeta(meta);
    }
}
