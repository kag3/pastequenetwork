package fr.pasteque.skyblock.skill;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.skill.model.SkillType;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.HashMap;
import java.util.Map;

public class SkillListener implements Listener {

    private final SkillManager skillManager;

    /* ── XP lookup maps ── */
    private static final Map<Material, Integer> ORE_XP = new HashMap<Material, Integer>();
    private static final Map<Material, Integer> CROP_XP = new HashMap<Material, Integer>();
    private static final Map<EntityType, Integer> MOB_XP = new HashMap<EntityType, Integer>();

    static {
        // Mining
        ORE_XP.put(Material.STONE, 1);
        ORE_XP.put(Material.COAL_ORE, 1);
        ORE_XP.put(Material.IRON_ORE, 2);
        ORE_XP.put(Material.REDSTONE_ORE, 2);
        ORE_XP.put(Material.GOLD_ORE, 3);
        ORE_XP.put(Material.LAPIS_ORE, 3);
        ORE_XP.put(Material.DIAMOND_ORE, 5);
        ORE_XP.put(Material.EMERALD_ORE, 5);

        // Farming
        CROP_XP.put(Material.CROPS, 2);          // wheat crop block in 1.9
        CROP_XP.put(Material.POTATO, 2);
        CROP_XP.put(Material.CARROT, 2);
        CROP_XP.put(Material.SUGAR_CANE_BLOCK, 1);
        CROP_XP.put(Material.MELON_BLOCK, 3);
        CROP_XP.put(Material.PUMPKIN, 3);
        CROP_XP.put(Material.NETHER_WARTS, 4);

        // Combat
        MOB_XP.put(EntityType.ZOMBIE, 3);
        MOB_XP.put(EntityType.SKELETON, 3);
        MOB_XP.put(EntityType.SPIDER, 3);
        MOB_XP.put(EntityType.CREEPER, 5);
        MOB_XP.put(EntityType.ENDERMAN, 8);
        MOB_XP.put(EntityType.BLAZE, 10);
        MOB_XP.put(EntityType.WITHER, 15);
    }

    public SkillListener(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    // ── Mining & Farming ─────────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material mat = event.getBlock().getType();

        Integer oreXp = ORE_XP.get(mat);
        if (oreXp != null) {
            skillManager.addXp(player, SkillType.MINING, oreXp);
            return;
        }

        Integer cropXp = CROP_XP.get(mat);
        if (cropXp != null) {
            skillManager.addXp(player, SkillType.FARMING, cropXp);
        }
    }

    // ── Fishing ──────────────────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            skillManager.addXp(event.getPlayer(), SkillType.FISHING, 5);
        }
    }

    // ── Combat ───────────────────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) {
            return;
        }
        Integer xp = MOB_XP.get(entity.getType());
        if (xp != null) {
            skillManager.addXp(killer, SkillType.COMBAT, xp);
        }
    }

    // ── Enchanting ───────────────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        skillManager.addXp(event.getEnchanter(), SkillType.ENCHANTING, 10);
    }

    // ── GUI click protection ─────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() == null) {
            return;
        }
        if (SkillGui.TITLE.equals(event.getInventory().getTitle())) {
            event.setCancelled(true);
            // Back button at slot 27
            if (event.getWhoClicked() instanceof Player && event.getRawSlot() == 27) {
                event.getWhoClicked().closeInventory();
            }
        }
    }
}
