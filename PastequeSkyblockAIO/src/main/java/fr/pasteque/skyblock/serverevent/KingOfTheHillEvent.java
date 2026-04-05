package fr.pasteque.skyblock.serverevent;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.generator.EmptyChunkGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Difficulty;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * King of the Hill complet.
 * Monde dedie "skyblockkoth" construit au premier demarrage, zone centrale de 15x15
 * sur une plateforme en quartz entouree d'un coliseum. Les joueurs rejoignent
 * avec /koth pendant que l'event est actif. Le classement final est affiche
 * dans le chat avec feux d'artifice au-dessus du vainqueur.
 */
@SuppressWarnings("deprecation")
public class KingOfTheHillEvent implements ServerEvent {

    public static final int DURATION_SECONDS = 5 * 60;
    public static final int ZONE_RADIUS = 8; // 17x17 zone
    public static final int WINNER_REWARD = 10000;
    public static final int SECOND_REWARD = 5000;
    public static final int THIRD_REWARD = 2500;

    private final PastequeSkyblockPlugin plugin;
    private boolean active = false;
    private long endAt = 0L;
    private int taskId = -1;

    private World kothWorld;
    private Location zoneCenter;

    private final Map<UUID, Integer> timeInZone = new HashMap<UUID, Integer>();

    public KingOfTheHillEvent(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "koth"; }
    @Override public String getDisplayName() { return "King of the Hill"; }
    @Override public boolean isActive() { return active; }
    @Override public long getRemainingSeconds() {
        if (!active) return 0L;
        return Math.max(0L, (endAt - System.currentTimeMillis()) / 1000L);
    }

    public World getKothWorld() { return kothWorld; }
    public Location getZoneCenter() { return zoneCenter == null ? null : zoneCenter.clone(); }

    /**
     * Initialise le monde KOTH et construit l'arene.
     * Appele au demarrage du plugin (une fois).
     */
    public void initializeWorld() {
        String worldName = "skyblockkoth";
        World w = Bukkit.getWorld(worldName);
        if (w == null) {
            WorldCreator creator = new WorldCreator(worldName);
            creator.type(WorldType.NORMAL);
            creator.generator(new EmptyChunkGenerator());
            w = creator.createWorld();
        }
        if (w == null) {
            plugin.getLogger().warning("[KOTH] Impossible de creer le monde skyblockkoth");
            return;
        }
        this.kothWorld = w;
        w.setStorm(false);
        w.setThundering(false);
        w.setWeatherDuration(Integer.MAX_VALUE);
        w.setDifficulty(Difficulty.PEACEFUL);
        w.setPVP(true);
        w.setGameRuleValue("doMobSpawning", "false");
        w.setGameRuleValue("doFireTick", "false");
        w.setGameRuleValue("mobGriefing", "false");
        w.setGameRuleValue("doDaylightCycle", "false");
        w.setGameRuleValue("doWeatherCycle", "false");
        w.setGameRuleValue("naturalRegeneration", "true");
        w.setSpawnLocation(0, 81, 0);

        this.zoneCenter = new Location(w, 0.5, 81, 0.5);

        // Build once (idempotent: checks for existing quartz block at center)
        if (w.getBlockAt(0, 80, 0).getType() != Material.QUARTZ_BLOCK) {
            buildArena(w);
        }
    }

    private void buildArena(World w) {
        int cx = 0, cy = 80, cz = 0;
        // Ground platform: 41x41 stone brick base
        for (int x = -20; x <= 20; x++) {
            for (int z = -20; z <= 20; z++) {
                w.getBlockAt(cx + x, cy - 1, cz + z).setType(Material.SMOOTH_BRICK);
            }
        }
        // Central hill: 17x17 quartz platform (the KOTH zone)
        for (int x = -ZONE_RADIUS; x <= ZONE_RADIUS; x++) {
            for (int z = -ZONE_RADIUS; z <= ZONE_RADIUS; z++) {
                w.getBlockAt(cx + x, cy, cz + z).setType(Material.QUARTZ_BLOCK);
            }
        }
        // Elevated ring of stairs around the hill (visual)
        for (int x = -ZONE_RADIUS - 1; x <= ZONE_RADIUS + 1; x++) {
            w.getBlockAt(cx + x, cy, cz - ZONE_RADIUS - 1).setType(Material.QUARTZ_STAIRS);
            w.getBlockAt(cx + x, cy, cz + ZONE_RADIUS + 1).setType(Material.QUARTZ_STAIRS);
        }
        for (int z = -ZONE_RADIUS - 1; z <= ZONE_RADIUS + 1; z++) {
            w.getBlockAt(cx - ZONE_RADIUS - 1, cy, cz + z).setType(Material.QUARTZ_STAIRS);
            w.getBlockAt(cx + ZONE_RADIUS + 1, cy, cz + z).setType(Material.QUARTZ_STAIRS);
        }
        // 4 corner pillars (obsidian + glowstone top)
        int[][] corners = {
            {-15, -15}, {-15, 15}, {15, -15}, {15, 15}
        };
        for (int[] c : corners) {
            for (int y = 0; y < 8; y++) {
                w.getBlockAt(cx + c[0], cy + y, cz + c[1]).setType(Material.OBSIDIAN);
            }
            w.getBlockAt(cx + c[0], cy + 8, cz + c[1]).setType(Material.GLOWSTONE);
        }
        // Border wall (stone brick, 3 high) around the 41x41 platform
        for (int x = -20; x <= 20; x++) {
            for (int y = 0; y < 3; y++) {
                w.getBlockAt(cx + x, cy + y, cz - 20).setType(Material.COBBLE_WALL);
                w.getBlockAt(cx + x, cy + y, cz + 20).setType(Material.COBBLE_WALL);
            }
        }
        for (int z = -20; z <= 20; z++) {
            for (int y = 0; y < 3; y++) {
                w.getBlockAt(cx - 20, cy + y, cz + z).setType(Material.COBBLE_WALL);
                w.getBlockAt(cx + 20, cy + y, cz + z).setType(Material.COBBLE_WALL);
            }
        }
        // Central beacon block (lime glass on top of the hill) for visibility
        w.getBlockAt(cx, cy + 1, cz).setType(Material.STAINED_GLASS);
        w.getBlockAt(cx, cy + 1, cz).setData((byte) 5); // lime
        plugin.getLogger().info("[KOTH] Arene construite dans le monde skyblockkoth");
    }

    @Override
    public void start() {
        if (active) return;
        if (kothWorld == null) {
            initializeWorld();
        }
        if (kothWorld == null) {
            Bukkit.broadcastMessage(PastequeSkyblockPlugin.color(
                    plugin.getPrefix() + "&cImpossible de demarrer le KOTH (monde indisponible)."));
            return;
        }
        active = true;
        endAt = System.currentTimeMillis() + (DURATION_SECONDS * 1000L);
        timeInZone.clear();

        broadcastStartAnnouncement();

        taskId = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override public void run() {
                if (System.currentTimeMillis() >= endAt) { stop(); return; }
                tick();
            }
        }, 20L, 20L).getTaskId();
    }

    private void broadcastStartAnnouncement() {
        String bar = PastequeSkyblockPlugin.color("&8&m--------------------------------");
        Bukkit.broadcastMessage(bar);
        Bukkit.broadcastMessage(PastequeSkyblockPlugin.color("           &c&l\u265b KING OF THE HILL \u265b"));
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(PastequeSkyblockPlugin.color("  &7Le &cKOTH &7vient de commencer dans le monde dedie !"));
        Bukkit.broadcastMessage(PastequeSkyblockPlugin.color("  &7Duree: &e" + (DURATION_SECONDS / 60) + " minutes"));
        Bukkit.broadcastMessage(PastequeSkyblockPlugin.color("  &7Zone: &aquartz au centre de l'arene"));
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(PastequeSkyblockPlugin.color("  &e\u25B6 Tapez &6&l/koth &epour rejoindre instantanement !"));
        Bukkit.broadcastMessage(PastequeSkyblockPlugin.color("  &7Recompenses : &61er &710000 &8| &e2eme &75000 &8| &c3eme &72500"));
        Bukkit.broadcastMessage(bar);

        // Title to all players
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendTitle(p, "&c&l\u265b KOTH \u265b", "&e/koth &7pour rejoindre !", 10, 60, 20);
        }
    }

    private void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        try {
            java.lang.reflect.Method m = Player.class.getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class);
            m.invoke(player, PastequeSkyblockPlugin.color(title), PastequeSkyblockPlugin.color(subtitle), fadeIn, stay, fadeOut);
        } catch (Throwable t) {
            try {
                java.lang.reflect.Method m = Player.class.getMethod("sendTitle", String.class, String.class);
                m.invoke(player, PastequeSkyblockPlugin.color(title), PastequeSkyblockPlugin.color(subtitle));
            } catch (Throwable ignored) {}
        }
    }

    private void tick() {
        if (zoneCenter == null || zoneCenter.getWorld() == null) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().equals(zoneCenter.getWorld())) continue;
            double dx = p.getLocation().getX() - zoneCenter.getX();
            double dz = p.getLocation().getZ() - zoneCenter.getZ();
            if (Math.abs(dx) <= ZONE_RADIUS + 0.5 && Math.abs(dz) <= ZONE_RADIUS + 0.5) {
                Integer cur = timeInZone.get(p.getUniqueId());
                timeInZone.put(p.getUniqueId(), (cur == null ? 0 : cur) + 1);
            }
        }
        // Every 30s, broadcast current leader
        long remaining = (endAt - System.currentTimeMillis()) / 1000L;
        if (remaining > 0 && remaining % 30 == 0) {
            UUID leader = findLeader();
            if (leader != null) {
                String name = Bukkit.getOfflinePlayer(leader).getName();
                Bukkit.broadcastMessage(PastequeSkyblockPlugin.color(
                        plugin.getPrefix() + "&c&l\u265b &7Leader actuel du KOTH: &6" + name
                                + " &7(" + timeInZone.get(leader) + "s) &8- &eReste " + remaining + "s"));
            }
        }
    }

    private UUID findLeader() {
        UUID best = null;
        int bestScore = 0;
        for (Map.Entry<UUID, Integer> e : timeInZone.entrySet()) {
            if (e.getValue() > bestScore) {
                bestScore = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }

    /**
     * Teleporte un joueur dans l'arene KOTH (appele par la commande /koth).
     */
    public boolean teleportToArena(Player player) {
        if (!active || kothWorld == null || zoneCenter == null) {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                    + "&cAucun KOTH n'est actuellement en cours."));
            return false;
        }
        // Random spawn around the arena edge (not in the zone directly)
        double angle = Math.random() * Math.PI * 2;
        double radius = 16.0;
        double x = zoneCenter.getX() + Math.cos(angle) * radius;
        double z = zoneCenter.getZ() + Math.sin(angle) * radius;
        Location tp = new Location(kothWorld, x, 82, z, (float)(Math.toDegrees(angle) + 180), 0);
        player.teleport(tp);
        player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                + "&aTeleportation dans l'arene KOTH ! &7Tenez la zone centrale !"));
        sendTitle(player, "&c&l\u265b KOTH \u265b", "&eTenez la zone centrale !", 5, 40, 10);
        return true;
    }

    @Override
    public void stop() {
        if (!active) return;
        active = false;
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        taskId = -1;

        // Build podium (top 3)
        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<Map.Entry<UUID, Integer>>(timeInZone.entrySet());
        Collections.sort(sorted, new Comparator<Map.Entry<UUID, Integer>>() {
            @Override public int compare(Map.Entry<UUID, Integer> a, Map.Entry<UUID, Integer> b) {
                return Integer.compare(b.getValue(), a.getValue());
            }
        });

        String bar = PastequeSkyblockPlugin.color("&8&m--------------------------------");
        Bukkit.broadcastMessage(bar);
        Bukkit.broadcastMessage(PastequeSkyblockPlugin.color("          &c&l\u265b FIN DU KOTH \u265b"));
        Bukkit.broadcastMessage("");
        if (sorted.isEmpty()) {
            Bukkit.broadcastMessage(PastequeSkyblockPlugin.color("  &cAucun participant."));
        } else {
            int[] rewards = { WINNER_REWARD, SECOND_REWARD, THIRD_REWARD };
            String[] ranks = { "&6&l1er", "&e&l2eme", "&c&l3eme" };
            for (int i = 0; i < Math.min(3, sorted.size()); i++) {
                Map.Entry<UUID, Integer> e = sorted.get(i);
                String name = Bukkit.getOfflinePlayer(e.getKey()).getName();
                if (name == null) name = "?";
                plugin.getEconomyManager().add(e.getKey(), rewards[i]);
                Bukkit.broadcastMessage(PastequeSkyblockPlugin.color(
                        "  " + ranks[i] + " &8- &f" + name + " &7(" + e.getValue() + "s) &8- &a+"
                                + rewards[i] + " Pasteque"));
            }
            // Winner title + fireworks
            UUID winnerId = sorted.get(0).getKey();
            Player winner = Bukkit.getPlayer(winnerId);
            if (winner != null && winner.isOnline()) {
                sendTitle(winner, "&6&l\u2605 VICTOIRE \u2605", "&eVous etes Roi de la Colline !", 10, 80, 20);
                launchFireworks(winner.getLocation());
            }
        }
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(bar);
        plugin.getEconomyManager().save();

        // Teleport all players out of the koth world back to spawn
        if (kothWorld != null) {
            Location back = Bukkit.getWorlds().get(0).getSpawnLocation();
            for (Player p : new ArrayList<Player>(kothWorld.getPlayers())) {
                p.teleport(back);
            }
        }
        timeInZone.clear();
    }

    private void launchFireworks(final Location loc) {
        final World w = loc.getWorld();
        if (w == null) return;
        for (int i = 0; i < 6; i++) {
            final int delay = i * 10;
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override public void run() {
                    Firework fw = (Firework) w.spawnEntity(loc.clone().add(0, 1, 0), org.bukkit.entity.EntityType.FIREWORK);
                    FireworkMeta meta = fw.getFireworkMeta();
                    FireworkEffect effect = FireworkEffect.builder()
                            .withColor(Color.LIME, Color.FUCHSIA, Color.YELLOW)
                            .withFade(Color.WHITE)
                            .with(FireworkEffect.Type.BALL_LARGE)
                            .trail(true)
                            .flicker(true)
                            .build();
                    meta.addEffect(effect);
                    meta.setPower(1);
                    fw.setFireworkMeta(meta);
                }
            }, delay);
        }
    }
}
