package fr.pasteque.skyblock.manager;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Random;

public class InvasionManager {
    private final PastequeSkyblockPlugin plugin;
    private final Random random = new Random();
    public InvasionManager(PastequeSkyblockPlugin plugin) { this.plugin = plugin; }
    public void start() {
        long mins = plugin.getConfig().getLong("invasions.interval-minutes", 20L);
        if (mins <= 0) return;
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override public void run() { if (random.nextBoolean()) startInvasion(); }
        }, 20L * 60L * mins, 20L * 60L * mins);
    }
    public void startInvasion() {
        World world = plugin.getWorldManager().getSpawnWorld();
        if (world == null) return;
        Location spawn = plugin.getWorldManager().getServerSpawn();
        Bukkit.broadcastMessage(MessageUtil.color(plugin.getPrefix() + "&d&lALERTE &fInvasion au Hub ! Rejoignez le combat !"));
        int count = plugin.getConfig().getInt("invasions.mob-count", 8);
        for (int i = 0; i < count; i++) {
            double x = spawn.getX() + (random.nextInt(18) - 9);
            double z = spawn.getZ() + (random.nextInt(18) - 9);
            Location loc = world.getHighestBlockAt((int) x, (int) z).getLocation().add(0.5D, 1.0D, 0.5D);
            EntityType type = random.nextBoolean() ? EntityType.ZOMBIE : EntityType.SKELETON;
            LivingEntity entity = (LivingEntity) world.spawnEntity(loc, type);
            entity.setMetadata("pasteque_invasion", new FixedMetadataValue(plugin, true));
            entity.getEquipment().setHelmet(new ItemStack(Material.MELON_BLOCK, 1));
        }
    }
}
