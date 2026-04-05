package fr.pasteque.skyblock.minion;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.gui.GuiHelper;
import fr.pasteque.skyblock.minion.model.MinionType;
import fr.pasteque.skyblock.minion.model.PlacedMinion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class MinionManager {

    public static final String GUI_TITLE_PREFIX = PastequeSkyblockPlugin.color("&2&lPasteque &5&lMinion - ");
    public static final String MINION_TAG = "\u00A7r\u00A70\u00A7minion";

    private final PastequeSkyblockPlugin plugin;
    private final File file;
    private final HashMap<UUID, List<PlacedMinion>> playerMinions = new HashMap<UUID, List<PlacedMinion>>();

    public MinionManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "minions.yml");
    }

    // -- Persistence ----------------------------------------------------------

    public void load() {
        playerMinions.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = cfg.getConfigurationSection("players");
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
            ConfigurationSection playerSec = players.getConfigurationSection(uuidStr);
            if (playerSec == null) {
                continue;
            }
            List<PlacedMinion> minions = new ArrayList<PlacedMinion>();
            for (String key : playerSec.getKeys(false)) {
                ConfigurationSection mSec = playerSec.getConfigurationSection(key);
                if (mSec == null) {
                    continue;
                }
                try {
                    MinionType type = MinionType.valueOf(mSec.getString("type"));
                    String world = mSec.getString("world");
                    double x = mSec.getDouble("x");
                    double y = mSec.getDouble("y");
                    double z = mSec.getDouble("z");
                    int level = mSec.getInt("level", 1);
                    long lastCollect = mSec.getLong("lastCollect", System.currentTimeMillis());

                    HashMap<String, Integer> storage = new HashMap<String, Integer>();
                    ConfigurationSection storageSec = mSec.getConfigurationSection("storage");
                    if (storageSec != null) {
                        for (String mat : storageSec.getKeys(false)) {
                            storage.put(mat, storageSec.getInt(mat));
                        }
                    }

                    PlacedMinion minion = new PlacedMinion(uuid, type, world, x, y, z, level, storage, lastCollect);
                    minions.add(minion);
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (!minions.isEmpty()) {
                playerMinions.put(uuid, minions);
            }
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, List<PlacedMinion>> entry : playerMinions.entrySet()) {
            String base = "players." + entry.getKey().toString();
            List<PlacedMinion> minions = entry.getValue();
            for (int i = 0; i < minions.size(); i++) {
                PlacedMinion m = minions.get(i);
                String mBase = base + "." + i;
                cfg.set(mBase + ".type", m.getType().name());
                cfg.set(mBase + ".world", m.getWorld());
                cfg.set(mBase + ".x", m.getX());
                cfg.set(mBase + ".y", m.getY());
                cfg.set(mBase + ".z", m.getZ());
                cfg.set(mBase + ".level", m.getLevel());
                cfg.set(mBase + ".lastCollect", m.getLastCollectTime());
                for (Map.Entry<String, Integer> s : m.getStorage().entrySet()) {
                    cfg.set(mBase + ".storage." + s.getKey(), s.getValue());
                }
            }
        }
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder minions.yml: " + e.getMessage());
        }
    }

    // -- Minion placement -----------------------------------------------------

    public boolean placeMinion(Player player, MinionType type, Location location) {
        List<PlacedMinion> minions = getMinions(player.getUniqueId());
        if (minions.size() >= getMaxMinions(player)) {
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "&cVous avez atteint le nombre maximum de minions ! (" + getMaxMinions(player) + ")"
            ));
            return false;
        }

        PlacedMinion minion = new PlacedMinion(player.getUniqueId(), type, location, 1);
        minions.add(minion);
        spawnMinionEntity(minion);
        player.sendMessage(PastequeSkyblockPlugin.color(
                "&6&l>> &a" + type.getDisplayName() + " &7place avec succes !"
        ));
        return true;
    }

    public MinionType removeMinion(Player player, Location location) {
        List<PlacedMinion> minions = getMinions(player.getUniqueId());
        Iterator<PlacedMinion> it = minions.iterator();
        while (it.hasNext()) {
            PlacedMinion m = it.next();
            Location mLoc = m.getLocation();
            if (mLoc != null && mLoc.getBlockX() == location.getBlockX()
                    && mLoc.getBlockY() == location.getBlockY()
                    && mLoc.getBlockZ() == location.getBlockZ()
                    && mLoc.getWorld().getName().equals(location.getWorld().getName())) {
                it.remove();
                removeMinionEntity(mLoc);
                return m.getType();
            }
        }
        return null;
    }

    // -- Tick -----------------------------------------------------------------

    public void tick() {
        long now = System.currentTimeMillis();
        for (List<PlacedMinion> minions : playerMinions.values()) {
            for (PlacedMinion minion : minions) {
                long elapsed = (now - minion.getLastCollectTime()) / 1000;
                int interval = minion.getInterval();
                if (interval <= 0) {
                    interval = 1;
                }
                if (elapsed >= interval) {
                    int produces = (int) (elapsed / interval);
                    for (int i = 0; i < produces; i++) {
                        minion.addProduce();
                    }
                    minion.setLastCollectTime(now);
                }
            }
        }
    }

    public void startTicking() {
        new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    // -- Entity management ----------------------------------------------------

    public void spawnMinionEntity(PlacedMinion minion) {
        Location loc = minion.getLocation();
        if (loc == null || loc.getWorld() == null) {
            return;
        }
        for (Entity entity : loc.getChunk().getEntities()) {
            if (entity instanceof Villager) {
                Villager v = (Villager) entity;
                if (v.getCustomName() != null && v.getCustomName().contains(MINION_TAG)) {
                    double dx = v.getLocation().getX() - loc.getX();
                    double dy = v.getLocation().getY() - loc.getY();
                    double dz = v.getLocation().getZ() - loc.getZ();
                    if (dx * dx + dy * dy + dz * dz < 2.25) {
                        return;
                    }
                }
            }
        }

        Villager villager = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
        villager.setCustomNameVisible(true);
        villager.setCustomName(PastequeSkyblockPlugin.color(
                "&6" + minion.getType().getDisplayName() + " &7[Niv." + minion.getLevel() + "]"
        ) + MINION_TAG);
        villager.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 255));
        villager.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 255));
    }

    public void removeMinionEntity(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        for (Entity entity : location.getChunk().getEntities()) {
            if (entity instanceof Villager) {
                Villager v = (Villager) entity;
                if (v.getCustomName() != null && v.getCustomName().contains(MINION_TAG)) {
                    double dx = v.getLocation().getX() - location.getX();
                    double dy = v.getLocation().getY() - location.getY();
                    double dz = v.getLocation().getZ() - location.getZ();
                    if (dx * dx + dy * dy + dz * dz < 2.25) {
                        v.remove();
                    }
                }
            }
        }
    }

    public void respawnMinionsInChunk(org.bukkit.Chunk chunk) {
        for (List<PlacedMinion> minions : playerMinions.values()) {
            for (PlacedMinion minion : minions) {
                if (!minion.getWorld().equals(chunk.getWorld().getName())) {
                    continue;
                }
                int minionChunkX = (int) minion.getX() >> 4;
                int minionChunkZ = (int) minion.getZ() >> 4;
                if (minionChunkX == chunk.getX() && minionChunkZ == chunk.getZ()) {
                    spawnMinionEntity(minion);
                }
            }
        }
    }

    // -- GUI ------------------------------------------------------------------

    public void openMinionGui(Player player, PlacedMinion minion) {
        String title = GUI_TITLE_PREFIX + PastequeSkyblockPlugin.color("&e" + minion.getType().getDisplayName());
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Row 0: decorative border
        GuiHelper.addTopBorder(inv);

        // Row 2: decorative border
        GuiHelper.addBottomBorder(inv);

        // Center (slot 13): minion info item
        Material iconMat = Material.matchMaterial(minion.getType().getIconMaterial());
        if (iconMat == null) {
            iconMat = Material.BEDROCK;
        }
        ItemStack info = new ItemStack(iconMat);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(PastequeSkyblockPlugin.color(
                "&6&l" + minion.getType().getDisplayName() + " &7Niv." + minion.getLevel()
        ));
        List<String> infoLore = new ArrayList<String>();
        infoLore.add("");
        infoLore.add(PastequeSkyblockPlugin.color("&8\u258E &7Statistiques"));
        infoLore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Stockage: &e" + minion.getStorageCount() + "&8/&f" + minion.getMaxStorage()));
        infoLore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Intervalle: &e" + minion.getInterval() + "s"));
        infoLore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Produit: &e" + minion.getType().getProduceMaterial()));
        infoMeta.setLore(infoLore);
        info.setItemMeta(infoMeta);
        inv.setItem(13, info);

        // Storage display (slot 11)
        ItemStack storageItem = new ItemStack(Material.CHEST);
        ItemMeta storageMeta = storageItem.getItemMeta();
        storageMeta.setDisplayName(PastequeSkyblockPlugin.color("&e&lStockage"));
        List<String> storageLore = new ArrayList<String>();
        storageLore.add("");
        if (minion.getStorage().isEmpty()) {
            storageLore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7Vide"));
        } else {
            storageLore.add(PastequeSkyblockPlugin.color("&8\u258E &7Contenu"));
            for (Map.Entry<String, Integer> entry : minion.getStorage().entrySet()) {
                storageLore.add(PastequeSkyblockPlugin.color("&8\u25B8 &7" + entry.getKey() + ": &f" + entry.getValue()));
            }
        }
        storageMeta.setLore(storageLore);
        storageItem.setItemMeta(storageMeta);
        inv.setItem(11, storageItem);

        // Collect all button (slot 12): HOPPER
        inv.setItem(12, GuiHelper.createItem(Material.HOPPER,
                "&a&lCollecter Tout",
                "",
                "&8\u25B8 &7Cliquez pour recuperer",
                "&8\u25B8 &7tous les objets stockes.",
                "",
                "&e\u25B6 Clic pour collecter!"));

        // Upgrade button (slot 14): ANVIL
        if (minion.getLevel() < 5) {
            int nextLevel = minion.getLevel() + 1;
            double cost = getMinionPrice(minion.getType(), nextLevel);
            inv.setItem(14, GuiHelper.createItem(Material.ANVIL,
                    "&e&lAmeliorer",
                    "",
                    "&8\u258E &7Amelioration",
                    "&8\u25B8 &7Niveau suivant: &e" + nextLevel,
                    "&8\u25B8 &7Cout: &e" + plugin.getEconomyManager().format(cost),
                    "",
                    "&e\u25B6 Clic pour ameliorer!"));
        } else {
            inv.setItem(14, GuiHelper.createItem(Material.NETHER_STAR,
                    "&a&lNiveau Maximum !",
                    "",
                    "&a\u2714 Ce minion est au max !"));
        }

        // Back button at bottom-left
        inv.setItem(18, GuiHelper.backButton());

        // Close button at bottom-right
        inv.setItem(26, GuiHelper.closeButton());

        // Fill remaining with black glass
        GuiHelper.fillEmpty(inv);

        player.openInventory(inv);
    }

    // -- Utilities ------------------------------------------------------------

    public List<PlacedMinion> getMinions(UUID uuid) {
        List<PlacedMinion> minions = playerMinions.get(uuid);
        if (minions == null) {
            minions = new ArrayList<PlacedMinion>();
            playerMinions.put(uuid, minions);
        }
        return minions;
    }

    public PlacedMinion getMinionAt(Location location) {
        for (List<PlacedMinion> minions : playerMinions.values()) {
            for (PlacedMinion minion : minions) {
                Location mLoc = minion.getLocation();
                if (mLoc != null && mLoc.getBlockX() == location.getBlockX()
                        && mLoc.getBlockY() == location.getBlockY()
                        && mLoc.getBlockZ() == location.getBlockZ()
                        && mLoc.getWorld().getName().equals(location.getWorld().getName())) {
                    return minion;
                }
            }
        }
        return null;
    }

    public int getMaxMinions(Player player) {
        int islandLevel = 0;
        if (plugin.getIslandManager() != null) {
            try {
                fr.pasteque.skyblock.model.Island island =
                        plugin.getIslandManager().getIslandByPlayer(player.getUniqueId());
                if (island != null) {
                    islandLevel = plugin.getIslandManager().getLevel(island);
                }
            } catch (Exception ignored) {
            }
        }
        int max = 5 + (islandLevel / 10);
        return Math.min(max, 20);
    }

    public double getMinionPrice(MinionType type, int level) {
        return 5000.0 * level;
    }

    public PastequeSkyblockPlugin getPlugin() {
        return plugin;
    }
}
