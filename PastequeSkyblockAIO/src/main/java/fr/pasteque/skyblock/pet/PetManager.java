package fr.pasteque.skyblock.pet;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.pet.model.PetType;
import fr.pasteque.skyblock.pet.model.PlayerPet;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PetManager {

    public static final String GUI_TITLE = PastequeSkyblockPlugin.color("&d&lAnimaux de Compagnie");

    private final PastequeSkyblockPlugin plugin;
    private final File file;
    private final HashMap<UUID, List<PlayerPet>> playerPets = new HashMap<UUID, List<PlayerPet>>();
    private final HashMap<UUID, PetType> activePet = new HashMap<UUID, PetType>();

    public PetManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "pets.yml");
    }

    // ── Persistence ──────────────────────────────────────────────────────

    public void load() {
        playerPets.clear();
        activePet.clear();
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
            List<PlayerPet> pets = new ArrayList<PlayerPet>();
            ConfigurationSection petsSec = playerSec.getConfigurationSection("pets");
            if (petsSec != null) {
                for (String key : petsSec.getKeys(false)) {
                    ConfigurationSection petSec = petsSec.getConfigurationSection(key);
                    if (petSec == null) {
                        continue;
                    }
                    try {
                        PetType type = PetType.valueOf(petSec.getString("type"));
                        int level = petSec.getInt("level", 1);
                        int xp = petSec.getInt("xp", 0);
                        boolean active = petSec.getBoolean("active", false);
                        PlayerPet pet = new PlayerPet(type, level, xp, active);
                        pets.add(pet);
                        if (active) {
                            activePet.put(uuid, type);
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            if (!pets.isEmpty()) {
                playerPets.put(uuid, pets);
            }
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, List<PlayerPet>> entry : playerPets.entrySet()) {
            String base = "players." + entry.getKey().toString();
            List<PlayerPet> pets = entry.getValue();
            for (int i = 0; i < pets.size(); i++) {
                PlayerPet pet = pets.get(i);
                String pBase = base + ".pets." + i;
                cfg.set(pBase + ".type", pet.getType().name());
                cfg.set(pBase + ".level", pet.getLevel());
                cfg.set(pBase + ".xp", pet.getXp());
                cfg.set(pBase + ".active", pet.isActive());
            }
        }
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder pets.yml: " + e.getMessage());
        }
    }

    // ── Pet access ───────────────────────────────────────────────────────

    public PlayerPet getActivePet(UUID uuid) {
        PetType type = activePet.get(uuid);
        if (type == null) {
            return null;
        }
        List<PlayerPet> pets = playerPets.get(uuid);
        if (pets == null) {
            return null;
        }
        for (PlayerPet pet : pets) {
            if (pet.getType() == type && pet.isActive()) {
                return pet;
            }
        }
        return null;
    }

    public void setActivePet(UUID uuid, PetType type) {
        // Deactivate current pet
        List<PlayerPet> pets = getPets(uuid);
        for (PlayerPet pet : pets) {
            pet.setActive(false);
        }
        activePet.remove(uuid);

        // Activate new pet
        for (PlayerPet pet : pets) {
            if (pet.getType() == type) {
                pet.setActive(true);
                activePet.put(uuid, type);
                return;
            }
        }
    }

    public void unequipPet(UUID uuid) {
        List<PlayerPet> pets = getPets(uuid);
        for (PlayerPet pet : pets) {
            pet.setActive(false);
        }
        activePet.remove(uuid);
    }

    public void addPet(UUID uuid, PetType type) {
        List<PlayerPet> pets = getPets(uuid);
        // Check if already owned
        for (PlayerPet pet : pets) {
            if (pet.getType() == type) {
                return;
            }
        }
        pets.add(new PlayerPet(type));
    }

    public boolean hasPet(UUID uuid, PetType type) {
        List<PlayerPet> pets = playerPets.get(uuid);
        if (pets == null) {
            return false;
        }
        for (PlayerPet pet : pets) {
            if (pet.getType() == type) {
                return true;
            }
        }
        return false;
    }

    public List<PlayerPet> getPets(UUID uuid) {
        List<PlayerPet> pets = playerPets.get(uuid);
        if (pets == null) {
            pets = new ArrayList<PlayerPet>();
            playerPets.put(uuid, pets);
        }
        return pets;
    }

    public PlayerPet getPlayerPet(UUID uuid, PetType type) {
        List<PlayerPet> pets = playerPets.get(uuid);
        if (pets == null) {
            return null;
        }
        for (PlayerPet pet : pets) {
            if (pet.getType() == type) {
                return pet;
            }
        }
        return null;
    }

    // ── Bonus ────────────────────────────────────────────────────────────

    /**
     * Returns the bonus percentage if the player's active pet has the given skill affinity.
     */
    public double getBonus(UUID uuid, String skillType) {
        PlayerPet pet = getActivePet(uuid);
        if (pet == null) {
            return 0.0;
        }
        if (pet.getType().getSkillAffinity().equalsIgnoreCase(skillType)) {
            return pet.getBonus();
        }
        return 0.0;
    }

    // ── Pet price ────────────────────────────────────────────────────────

    public double getPetPrice(PetType type) {
        switch (type) {
            case WOLF:
            case BEE:
            case RABBIT:
            case SHEEP:
                return 5000; // Common
            case OCELOT:
            case GOLEM:
                return 15000; // Uncommon
            case ENDERMAN:
            case BLAZE:
                return 50000; // Rare
            default:
                return 5000;
        }
    }

    // ── GUI ──────────────────────────────────────────────────────────────

    public void openPetGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);

        // Fill with gray glass panes
        ItemStack filler = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler.clone());
        }

        UUID uuid = player.getUniqueId();
        PetType[] types = PetType.values();
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19};
        for (int i = 0; i < types.length && i < slots.length; i++) {
            PetType type = types[i];
            boolean owned = hasPet(uuid, type);
            PlayerPet pet = getPlayerPet(uuid, type);

            Material iconMat = Material.matchMaterial(type.getIconMaterial());
            if (iconMat == null) {
                iconMat = Material.BARRIER;
            }

            ItemStack item;
            if (owned) {
                item = new ItemStack(iconMat);
            } else {
                // Gray dye for unowned (INK_SACK:8 = gray dye in 1.9)
                item = new ItemStack(Material.INK_SACK, 1, (short) 8);
            }

            ItemMeta meta = item.getItemMeta();
            boolean isActive = pet != null && pet.isActive();

            if (owned) {
                meta.setDisplayName(PastequeSkyblockPlugin.color(
                        type.getColor() + (isActive ? "&l" : "") + type.getDisplayName()
                                + " &7Niv." + pet.getLevel()
                                + (isActive ? " &a&l[ACTIF]" : "")
                ));
            } else {
                meta.setDisplayName(PastequeSkyblockPlugin.color(
                        "&8" + type.getDisplayName() + " &7(Non possede)"
                ));
            }

            List<String> lore = new ArrayList<String>();
            lore.add("");
            if (owned) {
                lore.add(PastequeSkyblockPlugin.color("&7Niveau: &e" + pet.getLevel()));
                lore.add(PastequeSkyblockPlugin.color("&7XP: &e" + pet.getXp() + "/" + pet.getXpRequired()));
                lore.add(PastequeSkyblockPlugin.color("&7Bonus: &a+" + String.format("%.1f", pet.getBonus()) + "%"));
                lore.add(PastequeSkyblockPlugin.color("&7Affinite: &e" + type.getSkillAffinity()));
                lore.add("");
                if (isActive) {
                    lore.add(PastequeSkyblockPlugin.color("&cCliquez pour desequiper"));
                } else {
                    lore.add(PastequeSkyblockPlugin.color("&eCliquez pour equiper"));
                }
            } else {
                lore.add(PastequeSkyblockPlugin.color("&7Affinite: &e" + type.getSkillAffinity()));
                lore.add(PastequeSkyblockPlugin.color("&7Prix: &e" + plugin.getEconomyManager().format(getPetPrice(type))));
                lore.add("");
                lore.add(PastequeSkyblockPlugin.color("&eCliquez pour acheter"));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slots[i], item);
        }

        player.openInventory(inv);
    }

    // ── Getter ───────────────────────────────────────────────────────────

    public PastequeSkyblockPlugin getPlugin() {
        return plugin;
    }
}
