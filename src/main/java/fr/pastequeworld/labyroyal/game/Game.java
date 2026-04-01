package fr.pastequeworld.labyroyal.game;

import fr.pastequeworld.labyroyal.LabyRoyalPlugin;
import fr.pastequeworld.labyroyal.arena.LobbyBuilder;
import fr.pastequeworld.labyroyal.arena.MazeGenerator;
import fr.pastequeworld.labyroyal.arena.VoidGenerator;
import fr.pastequeworld.labyroyal.manager.ScoreboardManager;
import fr.pastequeworld.labyroyal.manager.StormManager;
import fr.pastequeworld.labyroyal.util.MessageUtil;
import fr.pastequeworld.labyroyal.util.SoundUtil;

import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class Game {

    private final String id;
    private final LabyGameMode gameMode;
    private final LabyRoyalPlugin plugin;
    private GameState state;

    private World world;
    private MazeGenerator mazeGenerator;
    private LobbyBuilder lobbyBuilder;
    private StormManager stormManager;
    private ScoreboardManager scoreboardManager;

    private final Map<UUID, PlayerData> players = new LinkedHashMap<>();
    private final List<Team> teams = new ArrayList<>();
    private int nextTeamId = 1;

    private BukkitTask countdownTask;
    private BukkitTask gameTask;
    private BukkitTask waitTimeoutTask;
    private int countdown;

    // Config values
    private final int minPlayers;
    private final int maxPlayers;
    private final int mazeCells;
    private final int preparationTime;
    private final int pvpTime;
    private final int stormStartDelay;
    private final int stormDuration;
    private final int countdownTime;
    private final int waitTimeout;
    private final int wallHeight;
    private final int baseY;
    private final double oreChance;
    private final int specialRooms;
    private final int bonusChests;
    private final int lobbyY;
    private final int lobbySize;

    private int phaseTimer;
    private boolean stormStarted;

    public Game(String id, LabyGameMode gameMode, LabyRoyalPlugin plugin) {
        this.id = id;
        this.gameMode = gameMode;
        this.plugin = plugin;
        this.state = GameState.WAITING;
        this.stormStarted = false;

        // Load config
        String modePath = gameMode.name().toLowerCase();
        this.minPlayers = plugin.getConfig().getInt(modePath + ".min-players");
        this.maxPlayers = plugin.getConfig().getInt(modePath + ".max-players");
        this.mazeCells = plugin.getConfig().getInt(modePath + ".maze-cells");
        this.preparationTime = plugin.getConfig().getInt(modePath + ".preparation-time");
        this.pvpTime = plugin.getConfig().getInt(modePath + ".pvp-time");
        this.stormStartDelay = plugin.getConfig().getInt(modePath + ".storm-start-delay");
        this.stormDuration = plugin.getConfig().getInt(modePath + ".storm-duration");
        this.countdownTime = plugin.getConfig().getInt(modePath + ".countdown");
        this.waitTimeout = plugin.getConfig().getInt(modePath + ".wait-timeout");
        this.wallHeight = plugin.getConfig().getInt("maze.wall-height");
        this.baseY = plugin.getConfig().getInt("maze.base-y");
        this.oreChance = plugin.getConfig().getDouble("maze.ore-chance");
        this.specialRooms = plugin.getConfig().getInt("maze.special-rooms");
        this.bonusChests = plugin.getConfig().getInt("maze.bonus-chests");
        this.lobbyY = plugin.getConfig().getInt("lobby.y-level");
        this.lobbySize = plugin.getConfig().getInt("lobby.size");

        this.scoreboardManager = new ScoreboardManager(this);
    }

    // ==================== WORLD CREATION ====================

    public boolean createWorld() {
        String worldName = "labyroyal_" + id;

        WorldCreator creator = new WorldCreator(worldName);
        creator.generator(new VoidGenerator());
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);

        world = Bukkit.createWorld(creator);
        if (world == null) return false;

        world.setDifficulty(Difficulty.NORMAL);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.MOB_GRIEFING, false);
        world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
        world.setGameRule(GameRule.NATURAL_REGENERATION, true);
        world.setGameRule(GameRule.KEEP_INVENTORY, false);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.setTime(6000); // Noon
        world.setStorm(false);

        // Generate maze
        mazeGenerator = new MazeGenerator(mazeCells, wallHeight, baseY, oreChance, specialRooms, bonusChests);
        mazeGenerator.generate(world);

        // Build lobby
        lobbyBuilder = new LobbyBuilder(lobbyY, lobbySize);
        lobbyBuilder.build(world);

        // Set world border to maze size initially (large)
        WorldBorder border = world.getWorldBorder();
        border.setCenter(0, 0);
        border.setSize(mazeGenerator.getMazeBlockSize() + 10);
        border.setDamageAmount(2.0);
        border.setDamageBuffer(0);
        border.setWarningDistance(5);
        border.setWarningTime(10);

        return true;
    }

    // ==================== PLAYER MANAGEMENT ====================

    public boolean addPlayer(Player player) {
        if (state != GameState.WAITING && state != GameState.STARTING) return false;
        if (players.size() >= maxPlayers) return false;
        if (players.containsKey(player.getUniqueId())) return false;

        PlayerData data = new PlayerData(player.getUniqueId(), player.getName());
        players.put(player.getUniqueId(), data);

        // Handle team assignment for duo
        if (gameMode == LabyGameMode.DUO) {
            Team availableTeam = findAvailableTeam();
            if (availableTeam == null) {
                availableTeam = new Team(nextTeamId++);
                teams.add(availableTeam);
            }
            availableTeam.addMember(player.getUniqueId());
            data.setTeam(availableTeam);
        }

        // Teleport to lobby
        preparePlayer(player);
        player.teleport(lobbyBuilder.getSpawnLocation(world));

        // Broadcast join message
        String joinMsg = "&a+ &f" + player.getName() + " &7a rejoint la partie &8(&e"
                + players.size() + "&7/&e" + maxPlayers + "&8)";
        broadcast(joinMsg);
        SoundUtil.playAll(getOnlinePlayers(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);

        // Update scoreboards
        scoreboardManager.updateAll(getOnlinePlayers());

        // Check if we can start
        checkStartConditions();

        return true;
    }

    public void removePlayer(Player player) {
        PlayerData data = players.get(player.getUniqueId());
        if (data == null) return;

        if (state == GameState.WAITING || state == GameState.STARTING) {
            // Remove from team
            if (data.getTeam() != null) {
                data.getTeam().removeMember(player.getUniqueId());
                if (data.getTeam().isEmpty()) {
                    teams.remove(data.getTeam());
                }
            }
            players.remove(player.getUniqueId());

            String leaveMsg = "&c- &f" + player.getName() + " &7a quitte la partie &8(&e"
                    + players.size() + "&7/&e" + maxPlayers + "&8)";
            broadcast(leaveMsg);
            SoundUtil.playAll(getOnlinePlayers(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);

            // Cancel countdown if not enough players
            if (players.size() < minPlayers && countdownTask != null) {
                cancelCountdown();
                broadcast("&cPas assez de joueurs ! Compte a rebours annule.");
            }

            resetPlayer(player);
            sendToHub(player);
            scoreboardManager.remove(player);
            scoreboardManager.updateAll(getOnlinePlayers());

            // If no players left, cleanup
            if (players.isEmpty()) {
                plugin.getGameManager().removeGame(this);
            }
        } else if (state == GameState.PREPARATION || state == GameState.PVP) {
            eliminatePlayer(player, null, true);
        }
    }

    private Team findAvailableTeam() {
        for (Team team : teams) {
            if (!team.isFull(gameMode.getTeamSize())) {
                return team;
            }
        }
        return null;
    }

    // ==================== GAME FLOW ====================

    private void checkStartConditions() {
        if (state != GameState.WAITING) return;

        if (players.size() >= maxPlayers) {
            // Full - start countdown immediately
            startCountdown();
        } else if (players.size() >= minPlayers) {
            // Enough players - start wait timeout if not already running
            if (waitTimeoutTask == null) {
                broadcast("&eAssez de joueurs ! La partie commence dans &6" + waitTimeout + "s &esi personne d'autre ne rejoint.");
                startWaitTimeout();
            }
        }
    }

    private void startWaitTimeout() {
        waitTimeoutTask = new BukkitRunnable() {
            int timer = waitTimeout;

            @Override
            public void run() {
                if (state != GameState.WAITING) {
                    cancel();
                    return;
                }
                timer--;
                if (timer <= 0) {
                    cancel();
                    waitTimeoutTask = null;
                    if (players.size() >= minPlayers) {
                        startCountdown();
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void startCountdown() {
        if (state != GameState.WAITING) return;
        state = GameState.STARTING;
        countdown = countdownTime;

        if (waitTimeoutTask != null) {
            waitTimeoutTask.cancel();
            waitTimeoutTask = null;
        }

        broadcast("&6&lLa partie commence dans &e&l" + countdown + " secondes&6&l !");

        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                countdown--;

                if (countdown <= 0) {
                    cancel();
                    countdownTask = null;
                    startGame();
                    return;
                }

                Collection<Player> online = getOnlinePlayers();

                if (countdown <= 5 || countdown == 10) {
                    String color = countdown <= 3 ? "&c&l" : "&e&l";
                    MessageUtil.broadcastTitle(online,
                            color + countdown,
                            "&7Preparez-vous...",
                            5, 15, 5);

                    for (Player p : online) {
                        if (countdown <= 3) {
                            SoundUtil.countdownFinal(p);
                        } else {
                            SoundUtil.countdown(p);
                        }
                    }
                }

                scoreboardManager.updateAll(online);
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        state = GameState.WAITING;
    }

    private void startGame() {
        state = GameState.PREPARATION;
        phaseTimer = preparationTime;

        List<Location> spawns = mazeGenerator.getSpawnPoints(world, players.size());

        int index = 0;
        for (Map.Entry<UUID, PlayerData> entry : players.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) continue;

            Location spawn = spawns.get(index % spawns.size());
            entry.getValue().setSpawnLocation(spawn);
            player.teleport(spawn);
            index++;

            // Place starter chest next to each player
            mazeGenerator.placeStarterChest(spawn);

            // Freeze for 5 seconds
            player.setWalkSpeed(0);
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, false));
            player.setGameMode(org.bukkit.GameMode.SURVIVAL);
            player.setHealth(20);
            player.setFoodLevel(20);
            player.setSaturation(20);
            player.getInventory().clear();
            player.setLevel(0);
            player.setExp(0);
        }

        // Countdown 5-4-3-2-1-GO
        new BukkitRunnable() {
            int tick = 5;

            @Override
            public void run() {
                Collection<Player> online = getOnlinePlayers();

                if (tick > 0) {
                    MessageUtil.broadcastTitle(online,
                            "&e&l" + tick,
                            "&7Vos marques...",
                            0, 25, 0);
                    SoundUtil.playAll(online, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                } else {
                    MessageUtil.broadcastTitle(online,
                            "&a&lPARTEZ !",
                            "&6Bonne chance !",
                            0, 40, 10);

                    for (Player p : online) {
                        p.setWalkSpeed(0.2f);
                        p.removePotionEffect(PotionEffectType.BLINDNESS);
                        SoundUtil.gameStart(p);
                    }

                    cancel();
                    startPreparationPhase();
                }
                tick--;
            }
        }.runTaskTimer(plugin, 20L, 20L);

        broadcast("&6&l\u2694 &ePhase de Preparation &6&l\u2694");
        broadcast("&7Minez les murs pour recuperer des ressources !");
        broadcast("&7Trouvez les salles d'enchantement cachees !");
        broadcast("&7Ouvrez votre coffre de depart a cote de vous !");
    }

    private void startPreparationPhase() {
        gameTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.PREPARATION) {
                    cancel();
                    return;
                }

                phaseTimer--;
                Collection<Player> online = getOnlinePlayers();

                // Time announcements
                if (phaseTimer == 60) {
                    broadcast("&c&l\u26a0 &eLa phase de combat commence dans &c60 secondes &e!");
                    SoundUtil.playAll(online, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                } else if (phaseTimer == 30) {
                    broadcast("&c&l\u26a0 &eCombat dans &c30 secondes &e! Preparez-vous !");
                    SoundUtil.playAll(online, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                } else if (phaseTimer == 10) {
                    broadcast("&c&l\u26a0 &cCOMBAT DANS 10 SECONDES !");
                    SoundUtil.playAll(online, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
                } else if (phaseTimer <= 5 && phaseTimer > 0) {
                    MessageUtil.broadcastTitle(online,
                            "&c&l" + phaseTimer,
                            "&7Phase de combat imminente...",
                            0, 25, 0);
                    SoundUtil.playAll(online, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                }

                if (phaseTimer <= 0) {
                    cancel();
                    startPvpPhase();
                    return;
                }

                // Update scoreboards
                scoreboardManager.updateAll(online);

                // Action bar timer
                MessageUtil.broadcastActionBar(online,
                        "&6\u2694 Preparation &7- &e" + MessageUtil.formatTime(phaseTimer));
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void startPvpPhase() {
        state = GameState.PVP;
        phaseTimer = pvpTime;
        stormStarted = false;

        Collection<Player> online = getOnlinePlayers();

        MessageUtil.broadcastTitle(online,
                "&c&l\u2694 COMBAT ! \u2694",
                "&7Eliminez tous vos adversaires !",
                10, 60, 20);

        broadcast("&c&l\u2694 &4Phase de Combat &c&l\u2694");
        broadcast("&7Les minerais ne sont plus exploitables !");
        broadcast("&7La tempete va bientot se rapprocher...");

        // Apply mining fatigue to prevent mining
        for (Player p : online) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, Integer.MAX_VALUE, 2, false, false));
            SoundUtil.phaseChange(p);
        }

        // Initialize storm manager
        stormManager = new StormManager(world, mazeGenerator.getMazeBlockSize());

        gameTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.PVP) {
                    cancel();
                    return;
                }

                phaseTimer--;
                Collection<Player> online = getOnlinePlayers();

                // Start storm after delay
                int stormCountdown = pvpTime - stormStartDelay - (pvpTime - phaseTimer);
                if (!stormStarted && phaseTimer <= pvpTime - stormStartDelay) {
                    stormStarted = true;
                    stormManager.startShrinking(stormDuration);
                    broadcast("&5&l\u26a1 &dLa tempete se rapproche ! &5&l\u26a1");
                    SoundUtil.playAll(online, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.2f);
                }

                // Storm warning
                if (!stormStarted && (pvpTime - stormStartDelay - phaseTimer) == -10) {
                    broadcast("&5\u26a1 &dLa tempete arrive dans &510 secondes&d !");
                    SoundUtil.playAll(online, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.3f, 1.5f);
                }

                // Time announcements
                if (phaseTimer == 60) {
                    broadcast("&c\u26a0 Il reste &660 secondes &cde combat !");
                } else if (phaseTimer == 30) {
                    broadcast("&c\u26a0 &4Il reste 30 secondes !");
                } else if (phaseTimer == 10) {
                    broadcast("&c&l\u26a0 DERNIERES 10 SECONDES !");
                }

                if (phaseTimer <= 0) {
                    cancel();
                    // Force end - smallest border, damage everyone outside
                    broadcast("&4&lTemps ecoule ! La tempete consume tout !");
                    return;
                }

                // Apply storm damage to players outside border
                if (stormStarted) {
                    stormManager.applyDamage(getAlivePlayers());
                }

                scoreboardManager.updateAll(online);

                MessageUtil.broadcastActionBar(online,
                        "&c\u2694 Combat &7- &e" + MessageUtil.formatTime(phaseTimer)
                                + (stormStarted ? " &7| &5\u26a1 Tempete active" : ""));
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    // ==================== ELIMINATION ====================

    public void eliminatePlayer(Player player, Player killer, boolean disconnected) {
        PlayerData data = players.get(player.getUniqueId());
        if (data == null || !data.isAlive()) return;

        data.setAlive(false);
        int alive = getAliveCount();

        if (disconnected) {
            broadcast("&c\u2620 &f" + player.getName() + " &7a ete elimine (deconnexion) &8[&e" + alive + " restants&8]");
        } else if (killer != null) {
            PlayerData killerData = players.get(killer.getUniqueId());
            if (killerData != null) {
                killerData.incrementKills();
                SoundUtil.kill(killer);
                broadcast("&c\u2620 &f" + player.getName() + " &7a ete tue par &e" + killer.getName()
                        + " &8[&e" + alive + " restants&8]");

                // Kill feed events
                int kills = killerData.getKills();
                if (kills == 3) {
                    broadcast("&6\u2b50 &e" + killer.getName() + " &6est en serie de kills ! &7(3 kills)");
                } else if (kills == 5) {
                    broadcast("&c\u2b50 &4" + killer.getName() + " &cest INARRETABLE ! &7(5 kills)");
                }
            }
        } else {
            broadcast("&c\u2620 &f" + player.getName() + " &7a ete elimine &8[&e" + alive + " restants&8]");
        }

        // Death effects
        SoundUtil.playAll(getOnlinePlayers(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 0.8f);

        // Set spectator
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        player.removePotionEffect(PotionEffectType.SLOW_DIGGING);

        // Check for duo team elimination
        if (gameMode == LabyGameMode.DUO && data.getTeam() != null) {
            if (data.getTeam().isEliminated(players)) {
                broadcast("&4\u2620 &cL'equipe " + data.getTeam().getId() + " a ete eliminee !");
                SoundUtil.playAll(getOnlinePlayers(), Sound.ENTITY_BLAZE_DEATH, 1.0f, 1.0f);
            }
        }

        scoreboardManager.updateAll(getOnlinePlayers());
        checkWinCondition();
    }

    private void checkWinCondition() {
        if (gameMode == LabyGameMode.SOLO) {
            int alive = getAliveCount();
            if (alive <= 1) {
                PlayerData winner = getLastAlivePlayer();
                endGame(winner);
            }
        } else {
            // Duo: check if only one team remains
            List<Team> aliveTeams = teams.stream()
                    .filter(t -> !t.isEliminated(players))
                    .collect(Collectors.toList());
            if (aliveTeams.size() <= 1) {
                if (aliveTeams.size() == 1) {
                    // Winning team
                    Team winTeam = aliveTeams.get(0);
                    endGame(winTeam);
                } else {
                    endGame((PlayerData) null);
                }
            }
        }
    }

    // ==================== END GAME ====================

    private void endGame(PlayerData winner) {
        if (state == GameState.ENDING) return;
        state = GameState.ENDING;
        cancelAllTasks();

        Collection<Player> online = getOnlinePlayers();

        if (winner != null) {
            Player winPlayer = Bukkit.getPlayer(winner.getUuid());
            String winName = winner.getName();

            MessageUtil.broadcastTitle(online,
                    "&6&l\u2726 VICTOIRE \u2726",
                    "&e" + winName + " &7remporte le LabyRoyal !",
                    10, 80, 20);

            broadcast(MessageUtil.line());
            broadcast("&6&l       \u2726 LABYROYAL - VICTOIRE \u2726");
            broadcast("");
            broadcast("   &e\u2b50 Gagnant: &f&l" + winName);
            broadcast("   &7\u2694 Kills: &f" + winner.getKills());
            broadcast("");
            broadcast(MessageUtil.line());

            if (winPlayer != null) {
                SoundUtil.victory(winPlayer);
                spawnFireworks(winPlayer.getLocation());
            }
        } else {
            MessageUtil.broadcastTitle(online,
                    "&6&l\u2726 FIN DE PARTIE \u2726",
                    "&7Aucun gagnant",
                    10, 80, 20);
        }

        SoundUtil.playAll(online, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        // Teleport all back to hub after 10 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : new ArrayList<>(players.keySet())) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        resetPlayer(p);
                        sendToHub(p);
                        scoreboardManager.remove(p);
                    }
                }
                players.clear();

                // Cleanup world after a short delay
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        cleanup();
                        plugin.getGameManager().removeGame(Game.this);
                    }
                }.runTaskLater(plugin, 40L);
            }
        }.runTaskLater(plugin, 200L); // 10 seconds
    }

    private void endGame(Team winTeam) {
        if (state == GameState.ENDING) return;
        state = GameState.ENDING;
        cancelAllTasks();

        Collection<Player> online = getOnlinePlayers();

        StringBuilder memberNames = new StringBuilder();
        for (UUID uuid : winTeam.getMembers()) {
            PlayerData pd = players.get(uuid);
            if (pd != null) {
                if (memberNames.length() > 0) memberNames.append(" &7& &f&l");
                memberNames.append(pd.getName());
            }
        }

        MessageUtil.broadcastTitle(online,
                "&6&l\u2726 VICTOIRE \u2726",
                "&eEquipe " + winTeam.getId() + " &7remporte le LabyRoyal !",
                10, 80, 20);

        broadcast(MessageUtil.line());
        broadcast("&6&l       \u2726 LABYROYAL - VICTOIRE \u2726");
        broadcast("");
        broadcast("   &e\u2b50 Gagnants: &f&l" + memberNames);
        broadcast("");
        broadcast(MessageUtil.line());

        for (Player p : winTeam.getOnlinePlayers()) {
            SoundUtil.victory(p);
            spawnFireworks(p.getLocation());
        }

        SoundUtil.playAll(online, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : new ArrayList<>(players.keySet())) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        resetPlayer(p);
                        sendToHub(p);
                        scoreboardManager.remove(p);
                    }
                }
                players.clear();

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        cleanup();
                        plugin.getGameManager().removeGame(Game.this);
                    }
                }.runTaskLater(plugin, 40L);
            }
        }.runTaskLater(plugin, 200L);
    }

    private void spawnFireworks(Location loc) {
        for (int i = 0; i < 5; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (loc.getWorld() == null) return;
                    Firework fw = loc.getWorld().spawn(
                            loc.clone().add(
                                    (Math.random() - 0.5) * 4,
                                    1,
                                    (Math.random() - 0.5) * 4),
                            Firework.class);
                    FireworkMeta meta = fw.getFireworkMeta();
                    meta.addEffect(FireworkEffect.builder()
                            .with(FireworkEffect.Type.BALL_LARGE)
                            .withColor(Color.YELLOW, Color.ORANGE, Color.RED)
                            .withFade(Color.WHITE)
                            .flicker(true)
                            .trail(true)
                            .build());
                    meta.setPower(1);
                    fw.setFireworkMeta(meta);
                }
            }.runTaskLater(plugin, i * 15L);
        }
    }

    // ==================== UTILITY ====================

    public void broadcast(String message) {
        MessageUtil.broadcast(getOnlinePlayers(), message);
    }

    public Collection<Player> getOnlinePlayers() {
        List<Player> online = new ArrayList<>();
        for (UUID uuid : players.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                online.add(p);
            }
        }
        return online;
    }

    public List<Player> getAlivePlayers() {
        List<Player> alive = new ArrayList<>();
        for (Map.Entry<UUID, PlayerData> entry : players.entrySet()) {
            if (entry.getValue().isAlive()) {
                Player p = Bukkit.getPlayer(entry.getKey());
                if (p != null && p.isOnline()) {
                    alive.add(p);
                }
            }
        }
        return alive;
    }

    public int getAliveCount() {
        int count = 0;
        for (PlayerData data : players.values()) {
            if (data.isAlive()) count++;
        }
        return count;
    }

    private PlayerData getLastAlivePlayer() {
        for (PlayerData data : players.values()) {
            if (data.isAlive()) return data;
        }
        return null;
    }

    private void preparePlayer(Player player) {
        player.setGameMode(org.bukkit.GameMode.ADVENTURE);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.getInventory().clear();
        player.setLevel(0);
        player.setExp(0);
        player.setWalkSpeed(0.2f);
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));

        scoreboardManager.setup(player);
    }

    private void resetPlayer(Player player) {
        player.setGameMode(org.bukkit.GameMode.ADVENTURE);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.getInventory().clear();
        player.setWalkSpeed(0.2f);
        player.setLevel(0);
        player.setExp(0);
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
        player.setFireTicks(0);
    }

    private void sendToHub(Player player) {
        String hubWorldName = plugin.getConfig().getString("general.hub-world", "world");
        World hubWorld = Bukkit.getWorld(hubWorldName);
        if (hubWorld != null) {
            player.teleport(hubWorld.getSpawnLocation());
        }
    }

    private void cancelAllTasks() {
        if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
        if (gameTask != null) { gameTask.cancel(); gameTask = null; }
        if (waitTimeoutTask != null) { waitTimeoutTask.cancel(); waitTimeoutTask = null; }
    }

    public void cleanup() {
        cancelAllTasks();
        if (world != null) {
            String worldName = world.getName();
            // Move remaining players out
            for (Player p : world.getPlayers()) {
                sendToHub(p);
            }
            Bukkit.unloadWorld(world, false);
            // Delete world folder
            deleteWorldFolder(new File(Bukkit.getWorldContainer(), worldName));
        }
    }

    private void deleteWorldFolder(File folder) {
        if (folder.exists()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        deleteWorldFolder(f);
                    } else {
                        f.delete();
                    }
                }
            }
            folder.delete();
        }
    }

    // ==================== GETTERS ====================

    public String getId() { return id; }
    public LabyGameMode getGameMode() { return gameMode; }
    public GameState getState() { return state; }
    public World getWorld() { return world; }
    public Map<UUID, PlayerData> getPlayers() { return players; }
    public List<Team> getTeams() { return teams; }
    public int getMinPlayers() { return minPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public int getPhaseTimer() { return phaseTimer; }
    public int getCountdown() { return countdown; }
    public boolean isStormStarted() { return stormStarted; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }

    public boolean hasPlayer(UUID uuid) {
        return players.containsKey(uuid);
    }

    public boolean isFull() {
        return players.size() >= maxPlayers;
    }

    public boolean canJoin() {
        return (state == GameState.WAITING || state == GameState.STARTING) && !isFull();
    }
}
