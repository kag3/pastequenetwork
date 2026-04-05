package fr.pasteque.skyblock.slayer;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.manager.DataFile;
import fr.pasteque.skyblock.slayer.model.PlayerSlayerData;
import fr.pasteque.skyblock.slayer.model.SlayerType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SlayerManager {

    public static final String SLAYER_GUI_TITLE = "Slayers";
    public static final String TIER_GUI_PREFIX = "Slayer - ";
    public static final String BOSS_NAME_TAG = "&c[Boss Slayer]";

    private final PastequeSkyblockPlugin plugin;
    private final DataFile dataFile;
    private final HashMap<UUID, PlayerSlayerData> cache = new HashMap<UUID, PlayerSlayerData>();
    private final HashMap<UUID, UUID> bossTargets = new HashMap<UUID, UUID>(); // boss entity UUID -> player UUID
    private final HashMap<UUID, SlayerType> bossTypes = new HashMap<UUID, SlayerType>();
    private final HashMap<UUID, Integer> bossTiers = new HashMap<UUID, Integer>();

    public SlayerManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new DataFile(plugin, "slayers.yml");
        load();
    }

    // ------------------------------------------------------------------
    //  Persistence
    // ------------------------------------------------------------------

    public void load() {
        cache.clear();
        ConfigurationSection players = dataFile.getConfig().getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String uuidStr : players.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                continue;
            }
            PlayerSlayerData data = new PlayerSlayerData();
            ConfigurationSection pSection = players.getConfigurationSection(uuidStr);
            if (pSection == null) {
                continue;
            }

            for (SlayerType type : SlayerType.values()) {
                String key = type.name().toLowerCase();
                data.setXp(type, pSection.getInt("xp." + key, 0));
                data.setLevel(type, pSection.getInt("level." + key, 0));
            }

            // Restore active quest
            if (pSection.contains("quest.type")) {
                String questType = pSection.getString("quest.type");
                SlayerType qt = SlayerType.fromName(questType);
                if (qt != null) {
                    data.startQuest(qt, pSection.getInt("quest.tier", 1));
                    int kills = pSection.getInt("quest.kills", 0);
                    for (int i = 0; i < kills; i++) {
                        data.incrementKillCount();
                    }
                }
            }

            cache.put(uuid, data);
        }
    }

    public void save() {
        dataFile.getConfig().set("players", null);
        for (Map.Entry<UUID, PlayerSlayerData> entry : cache.entrySet()) {
            String base = "players." + entry.getKey().toString();
            PlayerSlayerData data = entry.getValue();

            for (SlayerType type : SlayerType.values()) {
                String key = type.name().toLowerCase();
                dataFile.getConfig().set(base + ".xp." + key, data.getXp(type));
                dataFile.getConfig().set(base + ".level." + key, data.getLevel(type));
            }

            if (data.hasActiveQuest()) {
                dataFile.getConfig().set(base + ".quest.type", data.getActiveQuest().name());
                dataFile.getConfig().set(base + ".quest.tier", data.getActiveQuestTier());
                dataFile.getConfig().set(base + ".quest.kills", data.getQuestKillCount());
            }
        }
        dataFile.save();
    }

    // ------------------------------------------------------------------
    //  Data access
    // ------------------------------------------------------------------

    public PlayerSlayerData getData(UUID uuid) {
        PlayerSlayerData data = cache.get(uuid);
        if (data == null) {
            data = new PlayerSlayerData();
            cache.put(uuid, data);
        }
        return data;
    }

    // ------------------------------------------------------------------
    //  Quest lifecycle
    // ------------------------------------------------------------------

    public void startQuest(Player player, SlayerType type, int tier) {
        if (tier < 1 || tier > 5) {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix() + "&cTier invalide (1-5)."));
            return;
        }

        PlayerSlayerData data = getData(player.getUniqueId());

        if (data.hasActiveQuest()) {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                    + "&cVous avez deja une quete active ! Terminez-la d'abord."));
            return;
        }

        // Check level requirement: tier N requires level N-1
        if (tier > 1 && data.getLevel(type) < tier - 1) {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                    + "&cVous devez etre niveau &e" + (tier - 1) + " &cen " + type.getDisplayName() + " pour ce tier."));
            return;
        }

        int cost = type.getCostForTier(tier);
        if (!plugin.getEconomyManager().take(player.getUniqueId(), cost)) {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                    + "&cVous n'avez pas assez d'argent. Cout: &6" + plugin.getEconomyManager().format(cost)));
            return;
        }

        data.startQuest(type, tier);
        player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                + type.getColor() + "&lQuete Slayer demarree ! &7Tuez &e"
                + data.getQuestKillTarget() + " " + type.getMobType().toLowerCase() + "s &7pour invoquer le boss."));
    }

    public void recordKill(Player player, String mobType) {
        PlayerSlayerData data = getData(player.getUniqueId());
        if (!data.hasActiveQuest()) {
            return;
        }

        SlayerType questType = data.getActiveQuest();
        if (!questType.getMobType().equalsIgnoreCase(mobType)) {
            return;
        }

        data.incrementKillCount();

        if (data.isQuestComplete()) {
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                    + "&a&lBoss invoque ! Preparez-vous au combat !"));
            spawnBoss(player, questType, data.getActiveQuestTier());
        } else {
            int remaining = data.getQuestKillTarget() - data.getQuestKillCount();
            if (remaining % 5 == 0 || remaining <= 3) {
                player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                        + "&7Progression: &e" + data.getQuestKillCount() + "/" + data.getQuestKillTarget()
                        + " &7(" + remaining + " restants)"));
            }
        }
    }

    @SuppressWarnings("deprecation")
    public void spawnBoss(Player player, SlayerType type, int tier) {
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(type.getMobType());
        } catch (IllegalArgumentException e) {
            entityType = EntityType.ZOMBIE;
        }

        LivingEntity boss = (LivingEntity) player.getWorld().spawnEntity(player.getLocation(), entityType);

        String bossName = PastequeSkyblockPlugin.color(type.getColor() + type.getDisplayName() + " &c[Tier " + tier + "]");
        boss.setCustomName(bossName);
        boss.setCustomNameVisible(true);

        double maxHp = tier * 50 + 100;
        boss.setMaxHealth(maxHp);
        boss.setHealth(maxHp);

        // Equipment for higher tiers
        if (tier >= 4 && boss.getEquipment() != null) {
            boss.getEquipment().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
            boss.getEquipment().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
            boss.getEquipment().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
            boss.getEquipment().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
            boss.getEquipment().setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
        } else if (tier >= 2 && boss.getEquipment() != null) {
            boss.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
            boss.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        }

        // Track boss
        bossTargets.put(boss.getUniqueId(), player.getUniqueId());
        bossTypes.put(boss.getUniqueId(), type);
        bossTiers.put(boss.getUniqueId(), tier);
    }

    public void onBossDeath(Player player, SlayerType type, int tier) {
        PlayerSlayerData data = getData(player.getUniqueId());

        // Award XP
        int xpReward = type.getXpForTier(tier);
        data.addXp(type, xpReward);
        player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                + type.getColor() + "+" + xpReward + " XP Slayer " + type.getDisplayName() + " !"));

        // Money reward
        double moneyReward = tier * 500;
        plugin.getEconomyManager().add(player.getUniqueId(), moneyReward);
        player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                + "&a+" + plugin.getEconomyManager().format(moneyReward)));

        // Check level up
        checkLevelUp(player, type, data);

        // Complete quest
        data.completeQuest();

        player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                + "&a&lQuete Slayer terminee ! Bravo !"));
    }

    private void checkLevelUp(Player player, SlayerType type, PlayerSlayerData data) {
        int currentLevel = data.getLevel(type);
        if (currentLevel >= 5) {
            return; // Max level
        }
        int requiredXp = type.getXpForTier(currentLevel + 1);
        if (data.getXp(type) >= requiredXp) {
            data.setLevel(type, currentLevel + 1);
            player.sendMessage(PastequeSkyblockPlugin.color(plugin.getPrefix()
                    + type.getColor() + "&l&kM&r " + type.getColor() + "&lSlayer " + type.getDisplayName()
                    + " niveau " + (currentLevel + 1) + " ! &l&kM"));
        }
    }

    // ------------------------------------------------------------------
    //  Boss tracking
    // ------------------------------------------------------------------

    public UUID getBossOwner(UUID bossEntityId) {
        return bossTargets.get(bossEntityId);
    }

    public SlayerType getBossType(UUID bossEntityId) {
        return bossTypes.get(bossEntityId);
    }

    public Integer getBossTier(UUID bossEntityId) {
        return bossTiers.get(bossEntityId);
    }

    public void removeBoss(UUID bossEntityId) {
        bossTargets.remove(bossEntityId);
        bossTypes.remove(bossEntityId);
        bossTiers.remove(bossEntityId);
    }

    // ------------------------------------------------------------------
    //  GUIs
    // ------------------------------------------------------------------

    public void openSlayerGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, PastequeSkyblockPlugin.color(SLAYER_GUI_TITLE));

        // Fill border
        ItemStack filler = createItem(Material.STAINED_GLASS_PANE, (short) 15, " ");
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, filler);
        }

        PlayerSlayerData data = getData(player.getUniqueId());

        // Display slayer types in a row (slots 11-15)
        SlayerType[] types = SlayerType.values();
        for (int i = 0; i < types.length; i++) {
            SlayerType type = types[i];
            Material mat;
            try {
                mat = Material.valueOf(type.getIconMaterial());
            } catch (IllegalArgumentException e) {
                mat = Material.STONE;
            }

            ItemStack icon = new ItemStack(mat, 1);
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName(PastequeSkyblockPlugin.color(type.getColor() + "&l" + type.getDisplayName()));

            List<String> lore = new ArrayList<String>();
            lore.add(PastequeSkyblockPlugin.color("&7Niveau: " + type.getColor() + data.getLevel(type) + "/5"));
            lore.add(PastequeSkyblockPlugin.color("&7XP: " + type.getColor() + data.getXp(type)));
            if (data.getLevel(type) < 5) {
                lore.add(PastequeSkyblockPlugin.color("&7Prochain: &e" + type.getXpForTier(data.getLevel(type) + 1) + " XP"));
            } else {
                lore.add(PastequeSkyblockPlugin.color("&a&lNIVEAU MAX"));
            }
            lore.add("");
            lore.add(PastequeSkyblockPlugin.color("&eClic pour voir les tiers"));
            meta.setLore(lore);
            icon.setItemMeta(meta);
            inv.setItem(11 + i, icon);
        }

        // Quest status at slot 31
        ItemStack questInfo = createItem(Material.BOOK, (short) 0, "&d&lQuete Active");
        ItemMeta questMeta = questInfo.getItemMeta();
        List<String> questLore = new ArrayList<String>();
        if (data.hasActiveQuest()) {
            questLore.add(PastequeSkyblockPlugin.color("&7" + data.getProgress()));
            int pct = (int) ((data.getQuestKillCount() * 100.0) / data.getQuestKillTarget());
            questLore.add(PastequeSkyblockPlugin.color("&7Progression: &e" + pct + "%"));
        } else {
            questLore.add(PastequeSkyblockPlugin.color("&7Aucune quete active"));
            questLore.add(PastequeSkyblockPlugin.color("&7Choisissez un slayer pour commencer !"));
        }
        questMeta.setLore(questLore);
        questInfo.setItemMeta(questMeta);
        inv.setItem(31, questInfo);

        player.openInventory(inv);
    }

    public void openTierGui(Player player, SlayerType type) {
        String title = TIER_GUI_PREFIX + type.getDisplayName();
        Inventory inv = Bukkit.createInventory(null, 27, PastequeSkyblockPlugin.color(title));

        ItemStack filler = createItem(Material.STAINED_GLASS_PANE, (short) 15, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        PlayerSlayerData data = getData(player.getUniqueId());

        // Tier items at slots 11-15
        for (int tier = 1; tier <= 5; tier++) {
            boolean unlocked = (tier == 1) || (data.getLevel(type) >= tier - 1);
            Material mat = unlocked ? Material.SLIME_BALL : Material.MAGMA_CREAM;
            short durability = 0;

            ItemStack tierItem = new ItemStack(mat, tier);
            ItemMeta meta = tierItem.getItemMeta();
            meta.setDisplayName(PastequeSkyblockPlugin.color(type.getColor() + "&lTier " + tier));

            List<String> lore = new ArrayList<String>();
            lore.add(PastequeSkyblockPlugin.color("&7Cout: &6" + plugin.getEconomyManager().format(type.getCostForTier(tier))));
            lore.add(PastequeSkyblockPlugin.color("&7Kills requis: &e" + (tier * 10 + 5)));
            lore.add(PastequeSkyblockPlugin.color("&7XP recompense: &e" + type.getXpForTier(tier)));
            lore.add("");
            if (!unlocked) {
                lore.add(PastequeSkyblockPlugin.color("&c&lVERROUILLE - Niveau " + (tier - 1) + " requis"));
            } else if (data.hasActiveQuest()) {
                lore.add(PastequeSkyblockPlugin.color("&c&lQuete deja en cours"));
            } else {
                lore.add(PastequeSkyblockPlugin.color("&a&lClic pour demarrer"));
            }
            meta.setLore(lore);
            tierItem.setItemMeta(meta);
            inv.setItem(10 + tier, tierItem);
        }

        // Back button at slot 22
        ItemStack back = createItem(Material.ARROW, (short) 0, "&cRetour");
        inv.setItem(22, back);

        player.openInventory(inv);
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------

    private ItemStack createItem(Material material, short data, String name) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(PastequeSkyblockPlugin.color(name));
        item.setItemMeta(meta);
        return item;
    }
}
