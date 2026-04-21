package fr.pastequeworld.bedwars.lobby;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.game.Arena;
import fr.pastequeworld.bedwars.player.BedWarsPlayer;
import fr.pastequeworld.bedwars.player.PlayerState;
import fr.pastequeworld.bedwars.team.Team;
import fr.pastequeworld.bedwars.team.TeamColor;
import fr.pastequeworld.bedwars.util.ColorUtil;
import fr.pastequeworld.bedwars.util.ItemBuilder;
import fr.pastequeworld.bedwars.util.TitleUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;

/**
 * Gere le lobby de selection de mode :
 *   - Spawn du joueur a l'arrivee sur le serveur BedWars
 *   - Items de menu : selecteur de mode (par NPC), retour au hub (boussole magique)
 *   - Setup du lobby world si inexistant
 *   - Hologrammes d'accueil
 *
 * Gere aussi le respawn et la configuration des spectateurs en partie.
 */
public class LobbyManager {

    private final BedWarsPlugin plugin;

    public LobbyManager(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        // Protection anti-freeze: le paste de grosses schematics peut bloquer le
        // thread principal au boot sur certains hebergeurs.
        if (!plugin.getConfig().getBoolean("lobby.auto-paste-on-startup", true)) {
            plugin.getLogger().info("Lobby auto-paste desactive (lobby.auto-paste-on-startup=false).");
            return;
        }

        // Paste automatique de la schematic de lobby si non deja fait.
        // Resout depuis :
        //   1. plugins/PastequeBedWars/schematics/BedWarsLobbyY.schematic
        //   2. <server-root>/lobbybedwars/BedWarsLobbyY.schematic (depot GitHub)
        //   3. plugins/PastequeBedWars/lobbybedwars/BedWarsLobbyY.schematic
        org.bukkit.Location spawn = plugin.getConfigManager().getLobbySpawn();
        if (spawn == null || spawn.getWorld() == null) return;

        java.io.File marker = new java.io.File(plugin.getDataFolder(), ".lobby-loaded");
        if (marker.exists()) return;

        final java.io.File schematic = findLobbySchematic();
        if (schematic == null) {
            plugin.getLogger().info("Schematic de lobby non trouvee. Le lobby sera vide.");
            return;
        }

        final org.bukkit.Location finalSpawn = spawn.clone();
        final java.io.File finalMarker = marker;
        long delayTicks = plugin.getConfig().getLong("lobby.auto-paste-delay-ticks", 40L);
        if (delayTicks < 1L) delayTicks = 1L;
        plugin.getLogger().info("Lobby auto-paste planifie dans " + delayTicks + " ticks.");

        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (finalMarker.exists()) return;

                fr.pastequeworld.bedwars.map.SchematicLoader loader =
                        new fr.pastequeworld.bedwars.map.SchematicLoader(plugin);
                // Paste centre sous le spawn : on decale X/Z pour approximation
                org.bukkit.Location paste = finalSpawn.clone().subtract(0, 10, 0);
                long started = System.currentTimeMillis();
                boolean ok = loader.paste(schematic, paste);
                long took = System.currentTimeMillis() - started;
                if (ok) {
                    try { finalMarker.createNewFile(); } catch (java.io.IOException ignored) {}
                    plugin.getLogger().info("Lobby paste depuis " + schematic.getAbsolutePath()
                            + " (" + took + " ms).");
                } else {
                    plugin.getLogger().warning("Echec paste lobby depuis " + schematic.getAbsolutePath()
                            + " (" + took + " ms).");
                }
            }
        }, delayTicks);
    }

    private java.io.File findLobbySchematic() {
        java.io.File[] candidates = new java.io.File[] {
                new java.io.File(plugin.getDataFolder(), "schematics/BedWarsLobbyY.schematic"),
                new java.io.File(plugin.getDataFolder(), "schematics/lobby.schematic"),
                new java.io.File(plugin.getDataFolder().getParentFile().getParentFile(),
                        "lobbybedwars/BedWarsLobbyY.schematic"),
                new java.io.File(plugin.getDataFolder(), "lobbybedwars/BedWarsLobbyY.schematic")
        };
        for (java.io.File f : candidates) if (f != null && f.isFile()) return f;
        return null;
    }

    public void setupLobbyPlayer(Player player) {
        BedWarsPlayer bw = plugin.getPlayerDataManager().register(player);
        bw.resetGameSession();
        bw.setState(PlayerState.LOBBY);

        player.teleport(plugin.getConfigManager().getLobbySpawn());
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setExp(0);
        player.setLevel(0);
        player.setAllowFlight(false);
        player.setFlying(false);
        for (PotionEffect e : player.getActivePotionEffects()) player.removePotionEffect(e.getType());

        giveLobbyItems(player);

        TitleUtil.send(player,
                plugin.getMessageManager().get("lobby.welcome-title"),
                plugin.getMessageManager().get("lobby.welcome-subtitle"),
                10, 40, 10);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

        plugin.getScoreboardManager().showLobby(player);
        plugin.getTabManager().refreshLobby(player);
    }

    private void giveLobbyItems(Player player) {
        ItemStack selector = new ItemBuilder(Material.NETHER_STAR)
                .name("&b&lChoisir un mode &7(Clic droit)")
                .lore("&7Ouvre le menu de selection des modes.")
                .build();
        player.getInventory().setItem(0, selector);

        ItemStack shop = new ItemBuilder(Material.CHEST)
                .name("&e&lBoutique du profil &7(Clic droit)")
                .lore("&7Achete des cosmetics et des buffs.")
                .build();
        player.getInventory().setItem(1, shop);

        ItemStack stats = new ItemBuilder(Material.PAPER)
                .name("&d&lMes statistiques &7(Clic droit)")
                .lore("&7Voir vos kills, wins, lits brises...")
                .build();
        player.getInventory().setItem(4, stats);

        ItemStack returnHub = new ItemBuilder(Material.BED)
                .name("&c&lRetour au Hub &7(Clic droit)")
                .lore("&7Retourne au hub principal.")
                .build();
        player.getInventory().setItem(8, returnHub);
    }

    /**
     * Le joueur veut rejoindre un mode : on trouve/cree une arene, on le TP.
     */
    public void joinMode(Player player, fr.pastequeworld.bedwars.game.GameMode mode) {
        Arena arena = plugin.getArenaManager().findOrCreate(mode);
        if (arena == null) {
            player.sendMessage(plugin.getMessageManager().get("errors.no-map-available"));
            return;
        }
        arena.addPlayer(player);
    }

    /**
     * Equipe le joueur avec l'armure en cuir colore de son equipe.
     */
    public void equipTeamArmor(Player player, Team team) {
        ItemStack helmet = leatherColored(Material.LEATHER_HELMET, team.getColor());
        ItemStack chest = leatherColored(Material.LEATHER_CHESTPLATE, team.getColor());
        ItemStack legs = leatherColored(Material.LEATHER_LEGGINGS, team.getColor());
        ItemStack boots = leatherColored(Material.LEATHER_BOOTS, team.getColor());
        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chest);
        player.getInventory().setLeggings(legs);
        player.getInventory().setBoots(boots);

        ItemStack sword = new ItemBuilder(Material.WOOD_SWORD).unbreakable().build();
        player.getInventory().addItem(sword);
    }

    private ItemStack leatherColored(Material type, TeamColor color) {
        ItemStack stack = new ItemStack(type);
        LeatherArmorMeta meta = (LeatherArmorMeta) stack.getItemMeta();
        meta.setColor(org.bukkit.Color.fromRGB(
                plugin.getConfigManager().getTeamAppearance(color).armorR,
                plugin.getConfigManager().getTeamAppearance(color).armorG,
                plugin.getConfigManager().getTeamAppearance(color).armorB));
        meta.spigot().setUnbreakable(true);
        stack.setItemMeta(meta);
        return stack;
    }

    public void setupSpectator(Player player) {
        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage(ColorUtil.color("&7Vous etes desormais spectateur."));
    }

    public void startRespawnCountdown(final Player player, final Arena arena) {
        final BedWarsPlayer bw = plugin.getPlayerDataManager().get(player);
        if (bw == null || bw.getTeam() == null) return;
        final Team team = bw.getTeam();

        player.setGameMode(GameMode.SPECTATOR);
        player.getInventory().clear();

        final int[] seconds = new int[]{plugin.getConfigManager().getRespawnSeconds()};
        final int[] taskId = new int[1];
        taskId[0] = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline() || arena.getState() != fr.pastequeworld.bedwars.game.GameState.RUNNING) {
                    Bukkit.getScheduler().cancelTask(taskId[0]);
                    return;
                }
                if (seconds[0] > 0) {
                    TitleUtil.send(player,
                            plugin.getMessageManager().get("game.respawn-title"),
                            plugin.getMessageManager().get("game.respawn-subtitle")
                                    .replace("%seconds%", String.valueOf(seconds[0])),
                            0, 25, 0);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_HAT, 1f, 1f);
                    seconds[0]--;
                } else {
                    Bukkit.getScheduler().cancelTask(taskId[0]);
                    respawnPlayer(player, team);
                }
            }
        }, 20L, 20L).getTaskId();
    }

    private void respawnPlayer(Player player, Team team) {
        BedWarsPlayer bw = plugin.getPlayerDataManager().get(player);
        if (bw == null) return;
        bw.setState(PlayerState.PLAYING);
        team.markAlive(player.getUniqueId());

        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.teleport(team.getSpawnLocation());
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        equipTeamArmor(player, team);
        applyKeptUpgrades(player, bw, team);

        // Protection au spawn
        int shieldSec = plugin.getConfigManager().getSpawnProtectionSeconds();
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, shieldSec * 20, 4), true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, shieldSec * 20, 0), true);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);

        TitleUtil.send(player, plugin.getMessageManager().get("game.respawn-now-title"),
                plugin.getMessageManager().get("game.respawn-now-subtitle"), 5, 20, 5);
    }

    private void applyKeptUpgrades(Player player, BedWarsPlayer bw, Team team) {
        // Pioche / Hache : se retrouvent a chaque respawn, mais degradent d'un niveau
        int pickaxeLevel = bw.getPickaxeLevel();
        if (pickaxeLevel > 0) {
            Material mat = pickaxeMaterial(pickaxeLevel);
            player.getInventory().addItem(new ItemBuilder(mat).unbreakable().build());
            if (pickaxeLevel > 1) bw.setPickaxeLevel(pickaxeLevel - 1);
        }
        int axeLevel = bw.getAxeLevel();
        if (axeLevel > 0) {
            Material mat = axeMaterial(axeLevel);
            player.getInventory().addItem(new ItemBuilder(mat).unbreakable().build());
            if (axeLevel > 1) bw.setAxeLevel(axeLevel - 1);
        }

        // Sharpness sur l'epee
        if (team.getSharpnessLevel() > 0) {
            for (ItemStack it : player.getInventory().getContents()) {
                if (it != null && it.getType().name().endsWith("_SWORD")) {
                    it.addEnchantment(org.bukkit.enchantments.Enchantment.DAMAGE_ALL, team.getSharpnessLevel());
                }
            }
        }
        // Protection sur l'armure
        if (team.getProtectionLevel() > 0) {
            for (ItemStack it : player.getInventory().getArmorContents()) {
                if (it != null) it.addEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION_ENVIRONMENTAL,
                        team.getProtectionLevel());
            }
        }
        // Haste
        if (team.getHasteLevel() > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING,
                    Integer.MAX_VALUE, team.getHasteLevel() - 1, true, false), true);
        }
    }

    private Material pickaxeMaterial(int level) {
        switch (level) {
            case 1: return Material.WOOD_PICKAXE;
            case 2: return Material.STONE_PICKAXE;
            case 3: return Material.IRON_PICKAXE;
            default: return Material.DIAMOND_PICKAXE;
        }
    }

    private Material axeMaterial(int level) {
        switch (level) {
            case 1: return Material.WOOD_AXE;
            case 2: return Material.STONE_AXE;
            case 3: return Material.IRON_AXE;
            default: return Material.DIAMOND_AXE;
        }
    }

    public void openModeSelector(Player player) {
        String title = ColorUtil.color("&8\u2726 &b&lBedWars &7- &eChoisissez un mode");
        Inventory inv = Bukkit.createInventory(null, 27, title);

        ItemStack border = ItemBuilder.namedGlass(15, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        Collection<fr.pastequeworld.bedwars.config.ConfigManager.ModeDefinition> modes =
                plugin.getConfigManager().getModes().values();

        int[] slots = {11, 13, 15};
        int idx = 0;
        for (fr.pastequeworld.bedwars.config.ConfigManager.ModeDefinition def : modes) {
            if (idx >= slots.length) break;
            Material icon = Material.IRON_SWORD;
            try { icon = Material.valueOf(def.getIcon()); } catch (Exception ignored) {}
            ItemBuilder b = new ItemBuilder(icon)
                    .name(def.getDisplayName())
                    .glow();
            java.util.List<String> lore = new java.util.ArrayList<String>();
            lore.add("");
            for (String s : def.getLines()) lore.add(s);
            lore.add("");
            lore.add("&7Joueurs: &a" + countInQueue(def.getMode()));
            lore.add("&7Equipes: &f" + def.getTeams() + " x " + def.getPlayersPerTeam());
            lore.add("");
            lore.add("&e\u25b6 Clic pour rejoindre");
            b.lore(lore);
            inv.setItem(slots[idx++], b.build());
        }

        player.openInventory(inv);
    }

    private int countInQueue(fr.pastequeworld.bedwars.game.GameMode mode) {
        int total = 0;
        for (Arena a : plugin.getArenaManager().getArenas()) {
            if (a.getMode() == mode && (a.getState() == fr.pastequeworld.bedwars.game.GameState.WAITING
                    || a.getState() == fr.pastequeworld.bedwars.game.GameState.STARTING)) {
                total += a.getPlayers().size();
            }
        }
        return total;
    }
}
