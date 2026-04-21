package fr.pastequeworld.bedwars.lobby;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.game.GameMode;
import fr.pastequeworld.bedwars.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NPCs du lobby de selection de mode.
 * Utilise des entites "fake" vivantes (Villager) non-IA avec metadata.
 * Chaque NPC correspond a un mode. Click droit = rejoindre la queue.
 *
 * Les hologrammes au-dessus sont des armor-stands invisibles.
 */
public class NPCManager {

    public static final String META_NPC_MODE = "bw_npc_mode";

    private final BedWarsPlugin plugin;
    private final Map<GameMode, LivingEntity> npcs = new HashMap<GameMode, LivingEntity>();
    private final Map<GameMode, List<ArmorStand>> holograms = new HashMap<GameMode, List<ArmorStand>>();

    public NPCManager(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void spawnAll() {
        despawnAll();
        Map<GameMode, Location> locations = plugin.getConfigManager().getNpcLocations();
        for (Map.Entry<GameMode, Location> entry : locations.entrySet()) {
            spawn(entry.getKey(), entry.getValue());
        }
    }

    public void spawn(GameMode mode, Location location) {
        if (location == null || location.getWorld() == null) return;
        LivingEntity npc = (LivingEntity) location.getWorld().spawnEntity(location, selectEntity(mode));
        npc.setAI(false);
        npc.setInvulnerable(true);
        npc.setSilent(true);
        npc.setCollidable(false);
        npc.setCustomName(formatName(mode));
        npc.setCustomNameVisible(false);  // on utilise les armor stands
        npc.setMetadata(META_NPC_MODE, new FixedMetadataValue(plugin, mode.name()));
        npc.setRemoveWhenFarAway(false);
        if (npc instanceof Villager) {
            ((Villager) npc).setProfession(professionFor(mode));
        } else if (npc instanceof Zombie) {
            // outfit hack : donner equipement colore
        }
        equipNPC(npc, mode);
        npcs.put(mode, npc);

        // Hologrammes au-dessus : 3 lignes
        double y = location.getY() + 2.3;
        List<ArmorStand> lines = new ArrayList<ArmorStand>();
        lines.add(spawnLine(location, y + 0.5, ChatColor.YELLOW + "" + ChatColor.BOLD + "\u2605 " + strip(formatName(mode)) + " \u2605"));
        lines.add(spawnLine(location, y + 0.25, ChatColor.GRAY + "Clic-droit pour rejoindre"));
        lines.add(spawnLine(location, y, ChatColor.GREEN + "Joueurs en attente: " + ChatColor.WHITE + "0"));
        holograms.put(mode, lines);
    }

    private EntityType selectEntity(GameMode mode) {
        // On utilise des villageois pour plus de personnalite
        return EntityType.VILLAGER;
    }

    private Villager.Profession professionFor(GameMode mode) {
        switch (mode) {
            case SOLO: return Villager.Profession.BLACKSMITH;
            case DUO: return Villager.Profession.LIBRARIAN;
            case TEAMS: return Villager.Profession.PRIEST;
            default: return Villager.Profession.FARMER;
        }
    }

    private String formatName(GameMode mode) {
        switch (mode) {
            case SOLO: return ColorUtil.color("&a&lSolo");
            case DUO: return ColorUtil.color("&b&lDuo");
            case TEAMS: return ColorUtil.color("&c&lTeams");
            default: return mode.name();
        }
    }

    private String strip(String s) {
        return ChatColor.stripColor(s);
    }

    private void equipNPC(LivingEntity npc, GameMode mode) {
        // No-op pour villager : meta par defaut.
    }

    private ArmorStand spawnLine(Location base, double y, String name) {
        Location loc = new Location(base.getWorld(), base.getX(), y, base.getZ());
        ArmorStand stand = (ArmorStand) base.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setCustomName(name);
        stand.setCustomNameVisible(true);
        stand.setMarker(true);
        stand.setSmall(true);
        return stand;
    }

    public void refreshHologramCounts() {
        for (Map.Entry<GameMode, List<ArmorStand>> entry : holograms.entrySet()) {
            GameMode mode = entry.getKey();
            int count = 0;
            for (fr.pastequeworld.bedwars.game.Arena arena : plugin.getArenaManager().getArenas()) {
                if (arena.getMode() == mode
                        && (arena.getState() == fr.pastequeworld.bedwars.game.GameState.WAITING
                        || arena.getState() == fr.pastequeworld.bedwars.game.GameState.STARTING)) {
                    count += arena.getPlayers().size();
                }
            }
            List<ArmorStand> lines = entry.getValue();
            if (lines != null && lines.size() >= 3 && lines.get(2) != null) {
                lines.get(2).setCustomName(ChatColor.GREEN + "Joueurs en attente: " + ChatColor.WHITE + count);
            }
        }
    }

    public void despawnAll() {
        for (LivingEntity e : npcs.values()) if (e != null && !e.isDead()) e.remove();
        npcs.clear();
        for (List<ArmorStand> list : holograms.values()) {
            for (ArmorStand s : list) if (s != null && !s.isDead()) s.remove();
        }
        holograms.clear();
    }

    public void moveNpc(GameMode mode, Location location) {
        LivingEntity old = npcs.get(mode);
        if (old != null && !old.isDead()) old.remove();
        npcs.remove(mode);
        List<ArmorStand> oldHolo = holograms.remove(mode);
        if (oldHolo != null) for (ArmorStand s : oldHolo) if (s != null && !s.isDead()) s.remove();

        plugin.getConfigManager().setNpcLocation(mode, location);
        spawn(mode, location);
    }

    public boolean isNPC(org.bukkit.entity.Entity entity) {
        return entity != null && entity.hasMetadata(META_NPC_MODE);
    }

    public GameMode getModeOf(org.bukkit.entity.Entity entity) {
        if (!isNPC(entity)) return null;
        return GameMode.valueOf(entity.getMetadata(META_NPC_MODE).get(0).asString());
    }
}
