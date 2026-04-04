package fr.pasteque.skyblock.manager;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.util.MessageUtil;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ComplexEntityPart;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.*;

public class EndEventManager {
    private static final long STABILIZATION_MILLIS = 15000L;
    private final PastequeSkyblockPlugin plugin;
    private final DecimalFormat decimal = new DecimalFormat("0.0");
    private boolean active;
    private long endAt;
    private long stabilizationUntil;
    private UUID dragonId;
    private UUID lastHit;
    private final Map<UUID, Double> damage = new HashMap<UUID, Double>();
    private long lastDailyLaunchDay = -1L;
    private long lastReminderMinute = Long.MIN_VALUE;

    public EndEventManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override public void run() {
                Calendar now = Calendar.getInstance();
                long day = now.get(Calendar.YEAR) * 1000L + now.get(Calendar.DAY_OF_YEAR);
                if (!active && now.get(Calendar.HOUR_OF_DAY) == 20 && now.get(Calendar.MINUTE) == 0 && lastDailyLaunchDay != day) {
                    lastDailyLaunchDay = day;
                    launch(false);
                }
                if (active) {
                    long remaining = endAt - System.currentTimeMillis();
                    if (remaining <= 0L) {
                        stop(false);
                        return;
                    }
                    World world = plugin.getWorldManager().getEndEventWorld();
                    EnderDragon dragon = getTrackedDragon(world);
                    if (dragon == null || dragon.isDead() || !dragon.isValid()) {
                        if (world != null) {
                            spawnTrackedDragon(world);
                            broadcast("&d&lALERTE &fLe Pasteque Dragon revient dans l'arene !");
                        }
                        return;
                    }
                    long mins = (long) Math.ceil(remaining / 60000.0D);
                    if (mins != lastReminderMinute && mins > 0 && mins < plugin.getConfig().getLong("end-event.duration-minutes", 30L) && mins % 5L == 0L) {
                        lastReminderMinute = mins;
                        broadcast("&d&lALERTE &fLe Pasteque Dragon est encore en vie. Il reste &e" + mins + " min&f. Utilise &d/endevent&f !");
                    }
                }
            }
        }, 20L, 20L);
    }

    public boolean isActive() { return active; }

    public long getRemainingSeconds() {
        if (!active) return 0L;
        return Math.max(0L, (endAt - System.currentTimeMillis()) / 1000L);
    }

    public void launch(boolean manual) {
        if (active) return;
        World world = plugin.getWorldManager().getEndEventWorld();
        if (world == null) return;
        cleanupWorld(world);
        spawnTrackedDragon(world);
        active = true;
        stabilizationUntil = System.currentTimeMillis() + STABILIZATION_MILLIS;
        endAt = System.currentTimeMillis() + (plugin.getConfig().getLong("end-event.duration-minutes", 30L) * 60000L);
        lastHit = null;
        lastReminderMinute = Long.MIN_VALUE;
        damage.clear();
        broadcast("&d&lALERTE &fLe portail de l'End s'ouvre ! Event &dPasteque Dragon &fpendant &e30 minutes&f. Utilise &d/endevent&f !");
        if (manual) {
            broadcast("&7(Event lancé manuellement par un administrateur)");
        }
    }

    public void stop(boolean dragonKilled) {
        if (!active) return;
        World world = plugin.getWorldManager().getEndEventWorld();
        if (dragonKilled) {
            announceWinners();
        } else {
            broadcast("&5Fin de l'End Event &f: personne n'a réussi à tuer le &dPasteque Dragon&f à temps.");
        }
        if (world != null) cleanupWorld(world);
        active = false;
        dragonId = null;
        lastHit = null;
        stabilizationUntil = 0L;
        lastReminderMinute = Long.MIN_VALUE;
        damage.clear();
    }

    private void announceWinners() {
        List<Map.Entry<UUID, Double>> list = new ArrayList<Map.Entry<UUID, Double>>(damage.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<UUID, Double>>() {
            @Override public int compare(Map.Entry<UUID, Double> a, Map.Entry<UUID, Double> b) {
                return Double.compare(b.getValue(), a.getValue());
            }
        });
        double[] rewards = new double[]{100000.0D, 50000.0D, 25000.0D, 25000.0D};
        String[] labels = new String[]{"1er", "2e", "3e", "4e"};
        broadcast("&aLe &dPasteque Dragon &aa été vaincu ! Récompenses des meilleurs dégâts :");
        for (int i = 0; i < Math.min(4, list.size()); i++) {
            UUID uuid = list.get(i).getKey();
            double amount = rewards[i];
            plugin.getEconomyManager().add(uuid, amount);
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            broadcast("&e" + labels[i] + " &f- &d" + (name == null ? uuid.toString() : name) + " &7(" + decimal.format(list.get(i).getValue()) + " dmg) &f: &a" + ((int) amount) + " Pasteque");
        }
        if (lastHit != null) {
            plugin.getEconomyManager().add(lastHit, 15000.0D);
            String name = Bukkit.getOfflinePlayer(lastHit).getName();
            broadcast("&dDernier coup fatal &f: &e" + (name == null ? lastHit.toString() : name) + " &f+ &a15000 Pasteque");
        }
        plugin.getEconomyManager().save();
    }

    private void cleanupWorld(World world) {
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Player) {
                continue;
            }
            entity.remove();
        }
    }

    public void recordDamage(Player player, Entity entity, double amount) {
        if (!active || player == null || entity == null || dragonId == null || amount <= 0.0D) return;
        UUID hitDragon = null;
        if (entity instanceof EnderDragon) {
            hitDragon = entity.getUniqueId();
        } else if (entity instanceof ComplexEntityPart) {
            Entity parent = ((ComplexEntityPart) entity).getParent();
            if (parent instanceof EnderDragon) {
                hitDragon = parent.getUniqueId();
            }
        }
        if (hitDragon == null || !hitDragon.equals(dragonId)) return;
        damage.put(player.getUniqueId(), damage.containsKey(player.getUniqueId()) ? damage.get(player.getUniqueId()) + amount : amount);
        lastHit = player.getUniqueId();
    }


    private EnderDragon getTrackedDragon(World world) {
        if (world == null) return null;
        if (dragonId != null) {
            for (Entity entity : world.getEntities()) {
                if (entity.getUniqueId().equals(dragonId) && entity instanceof EnderDragon) {
                    return (EnderDragon) entity;
                }
            }
        }
        for (Entity entity : world.getEntities()) {
            if (entity instanceof EnderDragon) {
                dragonId = entity.getUniqueId();
                return (EnderDragon) entity;
            }
        }
        return null;
    }

    public void handleDragonDeath(Player killer, Entity entity) {
        if (!active || entity == null || dragonId == null) return;
        UUID deadDragon = entity.getUniqueId();
        if (entity instanceof ComplexEntityPart) {
            Entity parent = ((ComplexEntityPart) entity).getParent();
            if (parent != null) deadDragon = parent.getUniqueId();
        }
        if (!dragonId.equals(deadDragon)) return;
        if (System.currentTimeMillis() < stabilizationUntil) {
            World world = plugin.getWorldManager().getEndEventWorld();
            if (world != null) {
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override public void run() { if (active) spawnTrackedDragon(world); }
                }, 20L);
            }
            return;
        }
        lastHit = killer == null ? null : killer.getUniqueId();
        stop(true);
    }

    private void spawnTrackedDragon(World world) {
        if (world == null) return;
        for (Entity entity : world.getEntities()) {
            if (entity instanceof EnderDragon) {
                entity.remove();
            }
        }
        Location spawn = getDragonSpawn(world);
        spawn.getChunk().load();
        EnderDragon dragon = (EnderDragon) world.spawnEntity(spawn, EntityType.ENDER_DRAGON);
        dragon.setCustomName(MessageUtil.color("&dPasteque Dragon"));
        dragon.setCustomNameVisible(true);
        try {
            dragon.setRemoveWhenFarAway(false);
        } catch (Throwable ignored) {}
        double maxHealth = plugin.getConfig().getDouble("end-event.dragon-health", 300.0D);
        try {
            dragon.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
        } catch (Throwable ignored) {}
        try {
            dragon.setHealth(Math.min(maxHealth, dragon.getMaxHealth()));
        } catch (Throwable ignored) {}
        dragonId = dragon.getUniqueId();
    }

    private Location getDragonSpawn(World world) {
        Location base = getEventSpawn();
        if (base == null || base.getWorld() == null) {
            base = new Location(world, 0.5D, 90.0D, 0.5D);
        }
        if (!world.getName().equalsIgnoreCase(base.getWorld().getName())) {
            base = new Location(world, base.getX(), base.getY(), base.getZ(), base.getYaw(), base.getPitch());
        }
        int x = (int) Math.floor(base.getX());
        int z = (int) Math.floor(base.getZ());
        int highest = world.getHighestBlockYAt(x, z);
        double y = Math.max(90.0D, Math.max(base.getY() + 18.0D, highest + 14.0D));
        return new Location(world, base.getX(), y, base.getZ(), base.getYaw(), base.getPitch());
    }

    public Location getEventSpawn() {
        World world = plugin.getWorldManager().getEndEventWorld();
        Location cfg = plugin.getPvpManager().getNamedWarp("endevent-spawn");
        if (cfg != null && cfg.getWorld() != null) return cfg;
        if (world == null) return plugin.getWorldManager().getServerSpawn();
        return new Location(world, 0.5D, 70.0D, 0.5D);
    }

    public void setEventSpawn(Location location) {
        plugin.getPvpManager().setNamedWarp("endevent-spawn", location);
    }

    public void teleport(Player player) {
        if (!active) {
            MessageUtil.send(player, plugin.getPrefix(), "&fAucun End Event n'est actif actuellement.");
            return;
        }
        player.teleport(getEventSpawn());
        MessageUtil.send(player, plugin.getPrefix(), "&aTéléportation vers l'End Event.");
    }

    private void broadcast(String msg) {
        Bukkit.broadcastMessage(MessageUtil.color(plugin.getPrefix() + msg));
    }
}
