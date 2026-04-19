package fr.pastequeworld.bedwars.generator;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.config.ConfigManager;
import fr.pastequeworld.bedwars.game.Arena;
import fr.pastequeworld.bedwars.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Generateur de ressources. Peut etre :
 *   - Prive (iron / gold d'une equipe) : pose au spawn de l'equipe
 *   - Public (diamond / emerald) : au centre de la map, tier evoluable via events
 *
 * Drops :
 *   - Spawn timer (interval-ticks)
 *   - Cap : si trop d'items au sol autour du generateur, on skip
 *   - Holograms (armor stand) : affiche le nom et le countdown (pour diamond/emerald)
 */
public class Generator {

    private final BedWarsPlugin plugin;
    private final Arena arena;
    private final GeneratorType type;
    private final Location location;
    private final Team team; // null si public

    private final List<ArmorStand> holograms = new ArrayList<ArmorStand>();

    private int taskId = -1;
    private int hologramTaskId = -1;
    private int currentTier = 0;
    private int ticksUntilNext;

    public Generator(BedWarsPlugin plugin, Arena arena, GeneratorType type, Location location, Team team) {
        this.plugin = plugin;
        this.arena = arena;
        this.type = type;
        this.location = location.clone().add(0.5, 0, 0.5);
        this.team = team;
    }

    public void start() {
        if (location.getWorld() == null) return;
        ticksUntilNext = getCurrentInterval();
        if (type.isTiered()) spawnHolograms();

        this.taskId = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                tick();
            }
        }, 1L, 1L).getTaskId();

        if (type.isTiered()) {
            this.hologramTaskId = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
                @Override
                public void run() {
                    refreshHolograms();
                }
            }, 5L, 5L).getTaskId();
        }
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        if (hologramTaskId != -1) {
            Bukkit.getScheduler().cancelTask(hologramTaskId);
            hologramTaskId = -1;
        }
        despawnHolograms();
    }

    public void upgradeTier() {
        ConfigManager.GeneratorSettings settings = plugin.getConfigManager().getGenerator(type.getConfigKey());
        if (settings == null) return;
        if (currentTier + 1 < settings.tiers.size()) {
            currentTier++;
            ticksUntilNext = getCurrentInterval();
        }
    }

    private int getCurrentInterval() {
        ConfigManager.GeneratorSettings settings = plugin.getConfigManager().getGenerator(type.getConfigKey());
        if (settings == null || settings.tiers.isEmpty()) return 40;
        int idx = Math.min(currentTier, settings.tiers.size() - 1);
        return settings.tiers.get(idx).intervalTicks;
    }

    private void tick() {
        if (team != null && team.isEliminated()) return;
        ticksUntilNext--;
        if (ticksUntilNext <= 0) {
            spawnDrop();
            ticksUntilNext = getCurrentInterval();
        }
    }

    private void spawnDrop() {
        ConfigManager.GeneratorSettings settings = plugin.getConfigManager().getGenerator(type.getConfigKey());
        if (settings == null) return;

        int nearby = countNearbyItems();
        if (nearby >= settings.maxItemsGround) return;

        for (String drop : settings.drops) {
            String[] parts = drop.split(" ");
            Material material;
            try {
                material = Material.valueOf(parts[0]);
            } catch (IllegalArgumentException e) {
                continue;
            }
            int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
            ItemStack stack = new ItemStack(material, amount);
            Item item = location.getWorld().dropItem(location, stack);
            item.setVelocity(new Vector(0, 0.1, 0));
            // Marque l'item pour merge group (1 pile max)
            item.setPickupDelay(10);
        }
    }

    private int countNearbyItems() {
        int count = 0;
        for (org.bukkit.entity.Entity e : location.getWorld().getNearbyEntities(location, 2, 2, 2)) {
            if (!(e instanceof Item)) continue;
            Material m = ((Item) e).getItemStack().getType();
            if (m == type.getItemMaterial()) count += ((Item) e).getItemStack().getAmount();
        }
        return count;
    }

    private void spawnHolograms() {
        double baseY = location.getY() + 1.6;
        holograms.add(spawnLine(new Location(location.getWorld(), location.getX(), baseY + 0.6, location.getZ()),
                type.getColor() + "" + ChatColor.BOLD + type.getDisplayName().toUpperCase()));
        holograms.add(spawnLine(new Location(location.getWorld(), location.getX(), baseY + 0.35, location.getZ()),
                ChatColor.YELLOW + "Tier " + ChatColor.WHITE + currentTierLabel()));
        holograms.add(spawnLine(new Location(location.getWorld(), location.getX(), baseY + 0.1, location.getZ()),
                ChatColor.GRAY + "Apparition dans: " + ChatColor.WHITE + "..."));
    }

    private void refreshHolograms() {
        if (holograms.size() < 3) return;
        ArmorStand tierLine = holograms.get(1);
        ArmorStand timerLine = holograms.get(2);
        if (tierLine == null || timerLine == null) return;
        tierLine.setCustomName(ChatColor.translateAlternateColorCodes('&',
                "&eTier &f" + currentTierLabel()));
        int sec = Math.max(0, ticksUntilNext / 20);
        timerLine.setCustomName(ChatColor.translateAlternateColorCodes('&',
                "&7Apparition dans: &f" + sec + "s"));
    }

    private String currentTierLabel() {
        ConfigManager.GeneratorSettings settings = plugin.getConfigManager().getGenerator(type.getConfigKey());
        if (settings == null) return "I";
        int idx = Math.min(currentTier, settings.tiers.size() - 1);
        String display = settings.tiers.get(idx).display;
        return display == null ? String.valueOf(idx + 1) : ChatColor.translateAlternateColorCodes('&', display);
    }

    private ArmorStand spawnLine(Location loc, String name) {
        ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(loc, org.bukkit.entity.EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setCustomName(name);
        stand.setCustomNameVisible(true);
        stand.setMarker(true);
        stand.setSmall(true);
        return stand;
    }

    private void despawnHolograms() {
        for (ArmorStand s : holograms) {
            if (s != null && !s.isDead()) s.remove();
        }
        holograms.clear();
    }

    public GeneratorType getType() { return type; }
    public Location getLocation() { return location; }
    public Team getTeam() { return team; }
    public int getCurrentTier() { return currentTier; }
}
