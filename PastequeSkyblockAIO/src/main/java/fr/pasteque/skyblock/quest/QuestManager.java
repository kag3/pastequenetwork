package fr.pasteque.skyblock.quest;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.gui.GuiHelper;
import fr.pasteque.skyblock.manager.DataFile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Core quest/mission manager. Loads quest definitions from config.yml ("quests"
 * section), persists player progress to quest-progress.yml, and provides the
 * GUI interfaces for browsing / claiming quests.
 *
 * <p>Expected YAML format in config.yml:
 * <pre>
 * quests:
 *   break_first_tree:
 *     name: "&aCouper du bois"
 *     description: "Coupez 10 buches pour commencer votre aventure."
 *     type: BREAK_BLOCK
 *     target: LOG
 *     amount: 10
 *     money-reward: 100
 *     xp-reward: 10
 *     next-quest: craft_planks
 *     npc: ""
 *     first-join: true
 *   craft_planks:
 *     name: "&aCraft de planches"
 *     description: "Transformez vos buches en 40 planches."
 *     type: CRAFT_ITEM
 *     target: WOOD
 *     amount: 40
 *     money-reward: 150
 *     xp-reward: 15
 *     next-quest: build_cobble_gen
 *     npc: ""
 *     first-join: false
 *   build_cobble_gen:
 *     name: "&aCobble Generator"
 *     description: "Placez 1 seau d'eau et 1 seau de lave."
 *     type: PLACE_BLOCK
 *     target: LAVA
 *     amount: 1
 *     money-reward: 200
 *     xp-reward: 20
 *     next-quest: mine_cobble
 *     npc: ""
 *     first-join: false
 *   mine_cobble:
 *     name: "&aMiner du Cobble"
 *     description: "Minez 64 blocs de cobblestone."
 *     type: BREAK_BLOCK
 *     target: COBBLESTONE
 *     amount: 64
 *     money-reward: 300
 *     xp-reward: 30
 *     next-quest: craft_furnace
 *     npc: ""
 *     first-join: false
 *   craft_furnace:
 *     name: "&aCraft d'un Four"
 *     description: "Fabriquez un four pour fondre vos minerais."
 *     type: CRAFT_ITEM
 *     target: FURNACE
 *     amount: 1
 *     money-reward: 350
 *     xp-reward: 35
 *     next-quest: smelt_iron
 *     npc: ""
 *     first-join: false
 *   smelt_iron:
 *     name: "&aFondre du Fer"
 *     description: "Collectez 16 lingots de fer."
 *     type: COLLECT_ITEM
 *     target: IRON_INGOT
 *     amount: 16
 *     money-reward: 500
 *     xp-reward: 50
 *     next-quest: expand_island
 *     npc: ""
 *     first-join: false
 *   expand_island:
 *     name: "&aAgrandir l'ile"
 *     description: "Placez 200 blocs pour agrandir votre ile."
 *     type: PLACE_BLOCK
 *     target: ANY
 *     amount: 200
 *     money-reward: 600
 *     xp-reward: 60
 *     next-quest: start_farming
 *     npc: ""
 *     first-join: false
 *   start_farming:
 *     name: "&aDebut du Farming"
 *     description: "Plantez 10 graines de ble."
 *     type: PLACE_BLOCK
 *     target: WHEAT_SEEDS
 *     amount: 10
 *     money-reward: 700
 *     xp-reward: 70
 *     next-quest: first_money
 *     npc: ""
 *     first-join: false
 *   first_money:
 *     name: "&aPremier Capital"
 *     description: "Atteignez 1000 pasteques sur votre compte."
 *     type: REACH_LEVEL
 *     target: MONEY
 *     amount: 1000
 *     money-reward: 800
 *     xp-reward: 80
 *     next-quest: visit_arena
 *     npc: ""
 *     first-join: false
 *   visit_arena:
 *     name: "&aVisiter l'arene"
 *     description: "Rendez-vous dans le monde de l'arene."
 *     type: VISIT_LOCATION
 *     target: arena
 *     amount: 1
 *     money-reward: 1000
 *     xp-reward: 100
 *     next-quest: ""
 *     npc: ""
 *     first-join: false
 * </pre>
 */
public class QuestManager {

    public static final String GUI_TITLE = "&2&lPasteque &5&lMissions";
    public static final String DETAIL_TITLE_PREFIX = "&5&lMission: ";

    private final PastequeSkyblockPlugin plugin;
    private final DataFile progressFile;
    private final Map<String, Quest> quests;
    private final Map<UUID, Map<String, QuestProgress>> playerProgress;

    public QuestManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.progressFile = new DataFile(plugin, "quest-progress.yml");
        this.quests = new HashMap<String, Quest>();
        this.playerProgress = new HashMap<UUID, Map<String, QuestProgress>>();
    }

    // =========================================================================
    //  Loading / saving
    // =========================================================================

    /**
     * Loads quest definitions from the "quests" section of config.yml.
     */
    public void load() {
        quests.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("quests");
        if (section == null) {
            plugin.getLogger().warning("[Quests] Aucune section 'quests' trouvee dans config.yml");
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection qs = section.getConfigurationSection(id);
            if (qs == null) continue;

            String name = qs.getString("name", id);
            String description = qs.getString("description", "");
            String typeStr = qs.getString("type", "BREAK_BLOCK");
            QuestType type;
            try {
                type = QuestType.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[Quests] Type inconnu '" + typeStr + "' pour la quete " + id + ", ignoring.");
                continue;
            }
            String target = qs.getString("target", "");
            int amount = qs.getInt("amount", 1);
            double moneyReward = qs.getDouble("money-reward", 0);
            int xpReward = qs.getInt("xp-reward", 0);
            String nextQuestId = qs.getString("next-quest", "");
            String npcName = qs.getString("npc", "");
            boolean firstJoin = qs.getBoolean("first-join", false);

            Quest quest = new Quest(id, name, description, type, target, amount,
                    moneyReward, xpReward, nextQuestId, npcName, firstJoin);
            quests.put(id, quest);
        }
        plugin.getLogger().info("[Quests] " + quests.size() + " quete(s) chargee(s).");
        loadProgress();
    }

    /**
     * Loads all player progress from quest-progress.yml.
     */
    private void loadProgress() {
        playerProgress.clear();
        ConfigurationSection section = progressFile.getConfig().getConfigurationSection("players");
        if (section == null) return;

        for (String uuidStr : section.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                continue;
            }
            ConfigurationSection pSection = section.getConfigurationSection(uuidStr);
            if (pSection == null) continue;

            Map<String, QuestProgress> map = new HashMap<String, QuestProgress>();
            for (String questId : pSection.getKeys(false)) {
                ConfigurationSection qpSec = pSection.getConfigurationSection(questId);
                if (qpSec == null) continue;
                int current = qpSec.getInt("current", 0);
                boolean completed = qpSec.getBoolean("completed", false);
                boolean claimed = qpSec.getBoolean("claimed", false);
                map.put(questId, new QuestProgress(questId, current, completed, claimed));
            }
            playerProgress.put(uuid, map);
        }
    }

    /**
     * Saves all player progress to quest-progress.yml.
     */
    public void save() {
        progressFile.getConfig().set("players", null);
        for (Map.Entry<UUID, Map<String, QuestProgress>> entry : playerProgress.entrySet()) {
            String base = "players." + entry.getKey().toString();
            for (Map.Entry<String, QuestProgress> qe : entry.getValue().entrySet()) {
                QuestProgress qp = qe.getValue();
                String path = base + "." + qe.getKey();
                progressFile.getConfig().set(path + ".current", qp.getCurrent());
                progressFile.getConfig().set(path + ".completed", qp.isCompleted());
                progressFile.getConfig().set(path + ".claimed", qp.isClaimed());
            }
        }
        progressFile.save();
    }

    /**
     * Reloads quest definitions from config (admin command).
     */
    public void reload() {
        plugin.reloadConfig();
        load();
    }

    // =========================================================================
    //  Quest accessors
    // =========================================================================

    public Quest getQuest(String id) {
        return quests.get(id);
    }

    public Map<String, Quest> getQuests() {
        return quests;
    }

    // =========================================================================
    //  Player progress
    // =========================================================================

    private Map<String, QuestProgress> getProgressMap(UUID uuid) {
        Map<String, QuestProgress> map = playerProgress.get(uuid);
        if (map == null) {
            map = new HashMap<String, QuestProgress>();
            playerProgress.put(uuid, map);
        }
        return map;
    }

    public QuestProgress getProgress(UUID uuid, String questId) {
        return getProgressMap(uuid).get(questId);
    }

    /**
     * Returns all active (assigned, not yet claimed) quests for the player.
     */
    public List<Quest> getActiveQuests(UUID uuid) {
        List<Quest> result = new ArrayList<Quest>();
        Map<String, QuestProgress> map = getProgressMap(uuid);
        for (Map.Entry<String, QuestProgress> entry : map.entrySet()) {
            QuestProgress qp = entry.getValue();
            if (!qp.isClaimed()) {
                Quest q = quests.get(entry.getKey());
                if (q != null) {
                    result.add(q);
                }
            }
        }
        return result;
    }

    /**
     * Returns all quests marked as first-join.
     */
    public List<Quest> getFirstJoinQuests() {
        List<Quest> result = new ArrayList<Quest>();
        for (Quest q : quests.values()) {
            if (q.isFirstJoin()) {
                result.add(q);
            }
        }
        return result;
    }

    /**
     * Assigns first-join quests to a new player.
     */
    public void assignFirstJoinQuests(UUID uuid) {
        Map<String, QuestProgress> map = getProgressMap(uuid);
        for (Quest q : getFirstJoinQuests()) {
            if (!map.containsKey(q.getId())) {
                map.put(q.getId(), new QuestProgress(q.getId()));
            }
        }
    }

    /**
     * Manually assigns a quest to a player (admin command).
     */
    public void assignQuest(UUID uuid, String questId) {
        Map<String, QuestProgress> map = getProgressMap(uuid);
        if (!map.containsKey(questId)) {
            map.put(questId, new QuestProgress(questId));
        }
    }

    // =========================================================================
    //  Advancement
    // =========================================================================

    /**
     * Called by listeners to advance matching quests for the player.
     *
     * @param uuid   player UUID
     * @param type   quest type that happened
     * @param target material/entity name that happened
     * @param amount amount to add
     */
    public void advanceQuest(UUID uuid, QuestType type, String target, int amount) {
        Map<String, QuestProgress> map = getProgressMap(uuid);
        for (Map.Entry<String, QuestProgress> entry : map.entrySet()) {
            QuestProgress qp = entry.getValue();
            if (qp.isCompleted()) continue;

            Quest quest = quests.get(entry.getKey());
            if (quest == null) continue;
            if (quest.getType() != type) continue;

            // Target matching: "ANY" matches everything, otherwise exact match
            if (!quest.getTarget().equalsIgnoreCase("ANY")
                    && !quest.getTarget().equalsIgnoreCase(target)) {
                continue;
            }

            int newAmount = qp.getCurrent() + amount;
            qp.setCurrent(Math.min(newAmount, quest.getAmount()));

            if (qp.getCurrent() >= quest.getAmount()) {
                qp.setCompleted(true);
                notifyCompletion(uuid, quest);
            }
        }
    }

    /**
     * Special check for REACH_LEVEL / MONEY quests — called periodically or on
     * economy changes.
     */
    public void checkReachLevelQuests(UUID uuid) {
        Map<String, QuestProgress> map = getProgressMap(uuid);
        for (Map.Entry<String, QuestProgress> entry : map.entrySet()) {
            QuestProgress qp = entry.getValue();
            if (qp.isCompleted()) continue;

            Quest quest = quests.get(entry.getKey());
            if (quest == null) continue;
            if (quest.getType() != QuestType.REACH_LEVEL) continue;

            if ("MONEY".equalsIgnoreCase(quest.getTarget())) {
                double balance = plugin.getEconomyManager().getBalance(uuid);
                int current = (int) Math.min(balance, quest.getAmount());
                qp.setCurrent(current);
                if (current >= quest.getAmount()) {
                    qp.setCompleted(true);
                    notifyCompletion(uuid, quest);
                }
            }
        }
    }

    /**
     * Notify a player that a quest is completed (title + sound).
     */
    private void notifyCompletion(UUID uuid, Quest quest) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) return;

        player.sendMessage(PastequeSkyblockPlugin.color(
                "&2&lPasteque &5&lMissions &8\u00bb &aMission terminee: " + quest.getName() + "&a !"));
        player.sendMessage(PastequeSkyblockPlugin.color(
                "&7Utilisez &e/quest &7pour recuperer votre recompense."));

        // Title effect — try 5-arg (1.11+) then 2-arg (1.9) via reflection
        try {
            String titleText = PastequeSkyblockPlugin.color("&a&lMission Terminee !");
            String subtitleText = PastequeSkyblockPlugin.color("&7" + quest.getName());
            try {
                player.getClass().getMethod("sendTitle", String.class, String.class,
                        int.class, int.class, int.class)
                        .invoke(player, titleText, subtitleText, 10, 60, 20);
            } catch (Throwable e1) {
                try {
                    player.getClass().getMethod("sendTitle", String.class, String.class)
                            .invoke(player, titleText, subtitleText);
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }

        GuiHelper.playSuccess(player);
    }

    // =========================================================================
    //  Reward claiming
    // =========================================================================

    /**
     * Claims the reward for a completed quest and unlocks the next in chain.
     *
     * @return true if the reward was successfully claimed
     */
    public boolean claimReward(UUID uuid, String questId) {
        QuestProgress qp = getProgress(uuid, questId);
        if (qp == null || !qp.isCompleted() || qp.isClaimed()) {
            return false;
        }

        Quest quest = quests.get(questId);
        if (quest == null) return false;

        qp.setClaimed(true);

        // Money reward
        if (quest.getMoneyReward() > 0) {
            plugin.getEconomyManager().add(uuid, quest.getMoneyReward());
        }

        // XP reward (general mining XP as a default fallback)
        if (quest.getXpReward() > 0) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.giveExpLevels(quest.getXpReward());
            }
        }

        // Unlock next quest in chain
        String nextId = quest.getNextQuestId();
        if (nextId != null && nextId.length() > 0 && quests.containsKey(nextId)) {
            assignQuest(uuid, nextId);
        }

        // Notification
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "&2&lPasteque &5&lMissions &8\u00bb &6+" + (int) quest.getMoneyReward()
                            + " pasteques &7et &b+" + quest.getXpReward() + " XP &7recus !"));
            GuiHelper.playSuccess(player);
        }

        return true;
    }

    // =========================================================================
    //  GUIs
    // =========================================================================

    /**
     * Opens the main quest overview GUI (54 slots).
     */
    public void openQuestGui(Player player) {
        String title = PastequeSkyblockPlugin.color(GUI_TITLE);
        Inventory inv = Bukkit.createInventory(null, 54, title);
        GuiHelper.decorate(inv, GuiHelper.Theme.EVENT);

        UUID uuid = player.getUniqueId();
        Map<String, QuestProgress> map = getProgressMap(uuid);

        // Collect all quests the player has been assigned
        List<String> assignedIds = new ArrayList<String>(map.keySet());

        // Also show quests that chain from assigned quests but are locked
        List<String> allIds = new ArrayList<String>();
        for (String id : assignedIds) {
            if (!allIds.contains(id)) {
                allIds.add(id);
            }
        }

        // Add locked chain quests for display
        for (String id : assignedIds) {
            Quest q = quests.get(id);
            if (q != null && q.getNextQuestId() != null && q.getNextQuestId().length() > 0) {
                String nextId = q.getNextQuestId();
                if (!allIds.contains(nextId) && quests.containsKey(nextId)) {
                    allIds.add(nextId);
                }
            }
        }

        // Slots 10-16, 19-25, 28-34 (3 rows of 7 = 21 quest slots)
        int[] slots = {10, 11, 12, 13, 14, 15, 16,
                       19, 20, 21, 22, 23, 24, 25,
                       28, 29, 30, 31, 32, 33, 34};

        for (int i = 0; i < slots.length && i < allIds.size(); i++) {
            String questId = allIds.get(i);
            Quest quest = quests.get(questId);
            if (quest == null) continue;

            QuestProgress qp = map.get(questId);
            ItemStack item;

            if (qp == null) {
                // Locked quest
                item = GuiHelper.createItem(Material.STAINED_GLASS_PANE, 14,
                        "&c&l\u2716 " + quest.getName(),
                        "",
                        "&7" + quest.getDescription(),
                        "",
                        "&c\u25B8 &7Verrouillee",
                        "&7Terminez la mission precedente.");
            } else if (qp.isClaimed()) {
                // Completed and claimed
                item = GuiHelper.createItem(Material.STAINED_GLASS_PANE, 5,
                        "&a&l\u2714 " + quest.getName(),
                        "",
                        "&7" + quest.getDescription(),
                        "",
                        "&a\u25B8 &aTerminee et reclamee !");
            } else if (qp.isCompleted()) {
                // Completed, not claimed — clickable
                item = GuiHelper.createItem(Material.STAINED_GLASS_PANE, 4,
                        "&e&l\u2605 " + quest.getName(),
                        "",
                        "&7" + quest.getDescription(),
                        "",
                        "&a\u25B8 &aProgression: &e" + qp.getCurrent() + "&7/&e" + quest.getAmount(),
                        buildProgressBar(qp.getCurrent(), quest.getAmount()),
                        "",
                        "&6Recompense: &e" + (int) quest.getMoneyReward() + " pasteques &7+ &b" + quest.getXpReward() + " XP",
                        "",
                        "&e\u25B6 Cliquez pour recuperer !");
            } else {
                // In progress
                item = GuiHelper.createItem(Material.STAINED_GLASS_PANE, 4,
                        "&e\u25B8 " + quest.getName(),
                        "",
                        "&7" + quest.getDescription(),
                        "",
                        "&e\u25B8 &7Progression: &e" + qp.getCurrent() + "&7/&e" + quest.getAmount(),
                        buildProgressBar(qp.getCurrent(), quest.getAmount()),
                        "",
                        "&6Recompense: &e" + (int) quest.getMoneyReward() + " pasteques &7+ &b" + quest.getXpReward() + " XP",
                        "",
                        "&e\u25B6 Cliquez pour voir les details");
            }
            inv.setItem(slots[i], item);
        }

        // Close button
        inv.setItem(49, GuiHelper.closeButton());

        player.openInventory(inv);
        GuiHelper.playOpen(player);
    }

    /**
     * Opens the detail GUI for a specific quest.
     */
    public void openQuestDetail(Player player, String questId) {
        Quest quest = quests.get(questId);
        if (quest == null) return;

        String title = PastequeSkyblockPlugin.color(DETAIL_TITLE_PREFIX + quest.getName());
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }
        Inventory inv = Bukkit.createInventory(null, 27, title);
        GuiHelper.decorate(inv, GuiHelper.Theme.EVENT);

        UUID uuid = player.getUniqueId();
        QuestProgress qp = getProgress(uuid, questId);

        // Quest info item (center slot 13)
        Material infoMat = materialForQuestType(quest.getType());
        String statusLine;
        if (qp == null) {
            statusLine = "&c\u25B8 Verrouillee";
        } else if (qp.isClaimed()) {
            statusLine = "&a\u25B8 Terminee et reclamee";
        } else if (qp.isCompleted()) {
            statusLine = "&a\u25B8 Terminee ! Reclamez votre recompense";
        } else {
            statusLine = "&e\u25B8 En cours: &e" + qp.getCurrent() + "&7/&e" + quest.getAmount();
        }

        String progressBar = (qp != null && !qp.isClaimed())
                ? buildProgressBar(qp.getCurrent(), quest.getAmount())
                : "";

        ItemStack info = GuiHelper.createItem(infoMat,
                "&d&l" + quest.getName(),
                "",
                "&7" + quest.getDescription(),
                "",
                statusLine,
                progressBar,
                "",
                "&6Recompense:",
                "&8\u25B8 &e" + (int) quest.getMoneyReward() + " pasteques",
                "&8\u25B8 &b" + quest.getXpReward() + " XP");
        inv.setItem(13, info);

        // Claim button (slot 22) — only if completed and not claimed
        if (qp != null && qp.isCompleted() && !qp.isClaimed()) {
            ItemStack claim = GuiHelper.createItem(Material.EMERALD,
                    "&a&lReclamer la recompense",
                    "",
                    "&6+" + (int) quest.getMoneyReward() + " pasteques",
                    "&b+" + quest.getXpReward() + " XP",
                    "",
                    "&e\u25B6 Cliquez pour reclamer !");
            inv.setItem(22, claim);
        }

        // Back button
        inv.setItem(18, GuiHelper.backButton());

        player.openInventory(inv);
        GuiHelper.playOpen(player);
    }

    // =========================================================================
    //  Progress bar helper
    // =========================================================================

    private String buildProgressBar(int current, int total) {
        if (total <= 0) return "";
        int bars = 20;
        int filled = (int) ((double) current / total * bars);
        if (filled > bars) filled = bars;

        StringBuilder sb = new StringBuilder("&8[");
        for (int i = 0; i < bars; i++) {
            if (i < filled) {
                sb.append("&a\u2588");
            } else {
                sb.append("&7\u2588");
            }
        }
        sb.append("&8]");
        return sb.toString();
    }

    /**
     * Returns a representative material icon for the given quest type.
     */
    private Material materialForQuestType(QuestType type) {
        switch (type) {
            case BREAK_BLOCK:    return Material.DIAMOND_PICKAXE;
            case PLACE_BLOCK:    return Material.GRASS;
            case KILL_MOB:       return Material.DIAMOND_SWORD;
            case CRAFT_ITEM:     return Material.WORKBENCH;
            case FISH:           return Material.FISHING_ROD;
            case COLLECT_ITEM:   return Material.CHEST;
            case REACH_LEVEL:    return Material.EXP_BOTTLE;
            case TALK_NPC:       return Material.SKULL_ITEM;
            case VISIT_LOCATION: return Material.COMPASS;
            default:             return Material.PAPER;
        }
    }

    // =========================================================================
    //  Getters
    // =========================================================================

    public PastequeSkyblockPlugin getPlugin() {
        return plugin;
    }
}
