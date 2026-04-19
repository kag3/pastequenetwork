package fr.pastequeworld.bedwars.shop;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.game.Arena;
import fr.pastequeworld.bedwars.team.Team;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;

/**
 * Villageois de shop (items ou upgrades).
 * Non-AI, non-collisionable, marque par metadata pour la detection de click.
 */
public class ShopVillager {

    public enum Type { ITEMS, UPGRADES }

    private final BedWarsPlugin plugin;
    private final Arena arena;
    private final Team team;
    private final Location location;
    private final Type shopType;

    private Villager entity;

    public ShopVillager(BedWarsPlugin plugin, Arena arena, Team team, Location location, Type type) {
        this.plugin = plugin;
        this.arena = arena;
        this.team = team;
        this.location = location.clone().add(0.5, 0, 0.5);
        this.shopType = type;
    }

    public void spawn() {
        if (entity != null && !entity.isDead()) return;
        Villager v = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        v.setAI(false);
        v.setInvulnerable(true);
        v.setSilent(true);
        v.setCollidable(false);
        v.setRemoveWhenFarAway(false);
        v.setCustomNameVisible(true);
        v.setCustomName(formatName());
        v.setMetadata("bw_shop", new org.bukkit.metadata.FixedMetadataValue(plugin, shopType.name()));
        v.setMetadata("bw_arena", new org.bukkit.metadata.FixedMetadataValue(plugin, arena.getId()));
        v.setMetadata("bw_team", new org.bukkit.metadata.FixedMetadataValue(plugin,
                team == null ? "" : team.getColor().name()));
        this.entity = v;
    }

    public void despawn() {
        if (entity != null) {
            entity.remove();
            entity = null;
        }
    }

    private String formatName() {
        if (shopType == Type.ITEMS) return ChatColor.YELLOW + "\u25bc " + ChatColor.BOLD + "MARCHAND " + ChatColor.RESET + ChatColor.YELLOW + "\u25bc";
        return ChatColor.AQUA + "\u2605 " + ChatColor.BOLD + "AMELIORATIONS " + ChatColor.RESET + ChatColor.AQUA + "\u2605";
    }

    public Arena getArena() { return arena; }
    public Team getTeam() { return team; }
    public Type getShopType() { return shopType; }
    public Villager getEntity() { return entity; }
}
