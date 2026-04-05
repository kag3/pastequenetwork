package fr.pasteque.skyblock.slayer;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.gui.GuiHelper;
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

@SuppressWarnings("deprecation")
public class SlayerManager {

    public static final String SLAYER_GUI_TITLE = PastequeSkyblockPlugin.color("&2&lPasteque &5&lSlayers");
    public static final String TIER_GUI_PREFIX = PastequeSkyblockPlugin.color("&2&lPasteque &5&l");
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
            boss.getEquipment().setItemInHand(new ItemStack(Material.DIAMOND_SWORD));
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
        Inventory inv = Bukkit.createInventory(null, 45, SLAYER_GUI_TITLE);

        // Row 0: decorative border

        // Row 4: decorative border

        PlayerSlayerData data = getData(player.getUniqueId());

        // Display slayer types centered in row 1 (slots 11-15)
        SlayerType[] types = SlayerType.values();
        int[] slots = {11, 12, 13, 14, 15};
        for (int i = 0; i < types.length && i < slots.length; i++) {
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
            lore.add("");
            lore.add(PastequeSkyblockPlugin.color("&8\u258E &7Statistiques"));
            lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Niveau: " + type.getColor() + data.getLevel(type) + "&8/&f5"));
            lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7XP: " + type.getColor() + data.getXp(type)));
            if (data.getLevel(type) < 5) {
                int nextXp = type.getXpForTier(data.getLevel(type) + 1);
                lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Prochain niveau: &e" + nextXp + " XP"));
                lore.add("");
                lore.add(buildProgressBar((double) data.getXp(type) / nextXp));
            } else {
                lore.add("");
                lore.add(PastequeSkyblockPlugin.color("&a&lNIVEAU MAX"));
            }
            lore.add("");
            lore.add(PastequeSkyblockPlugin.color("&e\u25B6 Clic pour voir les tiers"));
            meta.setLore(lore);
            icon.setItemMeta(meta);
            inv.setItem(slots[i], icon);
        }

        // Quest status at slot 31
        ItemStack questInfo = GuiHelper.createItem(Material.BOOK, "&d&lQuete Active");
        ItemMeta questMeta = questInfo.getItemMeta();
        List<String> questLore = new ArrayList<String>();
        questLore.add("");
        if (data.hasActiveQuest()) {
            questLore.add(PastequeSkyblockPlugin.color("&8\u258E &7Progression"));
            questLore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7" + data.getProgress()));
            int pct = (int) ((data.getQuestKillCount() * 100.0) / data.getQuestKillTarget());
            questLore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Avancement: &e" + pct + "%"));
            questLore.add("");
            questLore.add(buildProgressBar(data.getQuestKillCount() / (double) data.getQuestKillTarget()));
        } else {
            questLore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Aucune quete active"));
            questLore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Choisissez un slayer pour commencer !"));
        }
        questMeta.setLore(questLore);
        questInfo.setItemMeta(questMeta);
        inv.setItem(31, questInfo);

        // Back button at bottom-left, close button at bottom-right
        inv.setItem(36, GuiHelper.backButton());
        inv.setItem(44, GuiHelper.closeButton());

        // Fill remaining with black glass
        GuiHelper.decorate(inv, GuiHelper.Theme.PVP);

        player.openInventory(inv);
        GuiHelper.playOpen(player);
    }

    public void openTierGui(Player player, SlayerType type) {
        String title = TIER_GUI_PREFIX + PastequeSkyblockPlugin.color(type.getColor() + type.getDisplayName());
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Row 0: decorative border

        // Row 2: decorative border

        PlayerSlayerData data = getData(player.getUniqueId());

        // Tier items centered at slots 11-15
        for (int tier = 1; tier <= 5; tier++) {
            boolean unlocked = (tier == 1) || (data.getLevel(type) >= tier - 1);
            Material mat = unlocked ? Material.SLIME_BALL : Material.MAGMA_CREAM;

            ItemStack tierItem = new ItemStack(mat, tier);
            ItemMeta meta = tierItem.getItemMeta();
            meta.setDisplayName(PastequeSkyblockPlugin.color(type.getColor() + "&lTier " + tier));

            List<String> lore = new ArrayList<String>();
            lore.add("");
            lore.add(PastequeSkyblockPlugin.color("&8\u258E &7Details"));
            lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Cout: &6" + plugin.getEconomyManager().format(type.getCostForTier(tier))));
            lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Kills requis: &e" + (tier * 10 + 5)));
            lore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7XP recompense: &e" + type.getXpForTier(tier)));
            lore.add("");
            if (!unlocked) {
                lore.add(PastequeSkyblockPlugin.color("&c\u2716 Verrouille - Niveau " + (tier - 1) + " requis"));
            } else if (data.hasActiveQuest()) {
                lore.add(PastequeSkyblockPlugin.color("&c\u2716 Quete deja en cours"));
            } else {
                lore.add(PastequeSkyblockPlugin.color("&e\u25B6 Clic pour demarrer!"));
            }
            meta.setLore(lore);
            tierItem.setItemMeta(meta);
            inv.setItem(10 + tier, tierItem);
        }

        // Back button at bottom-left
        inv.setItem(18, GuiHelper.backButton());

        // Close button at bottom-right
        inv.setItem(26, GuiHelper.closeButton());

        // Fill remaining with black glass
        GuiHelper.decorate(inv, GuiHelper.Theme.PVP);

        player.openInventory(inv);
        GuiHelper.playOpen(player);
    }

    private String buildProgressBar(double progress) {
        int totalBars = 20;
        int filled = (int) (progress * totalBars);
        if (filled > totalBars) {
            filled = totalBars;
        }
        if (filled < 0) {
            filled = 0;
        }
        StringBuilder bar = new StringBuilder();
        bar.append("&a");
        for (int i = 0; i < filled; i++) {
            bar.append("|");
        }
        bar.append("&7");
        for (int i = filled; i < totalBars; i++) {
            bar.append("|");
        }
        return PastequeSkyblockPlugin.color(bar.toString());
    }
}
