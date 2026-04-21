package fr.pastequeworld.bedwars.game;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.config.ConfigManager;
import fr.pastequeworld.bedwars.generator.Generator;
import fr.pastequeworld.bedwars.generator.GeneratorType;
import fr.pastequeworld.bedwars.map.MapTemplate;
import fr.pastequeworld.bedwars.player.BedWarsPlayer;
import fr.pastequeworld.bedwars.player.PlayerState;
import fr.pastequeworld.bedwars.shop.ShopVillager;
import fr.pastequeworld.bedwars.team.Team;
import fr.pastequeworld.bedwars.team.TeamColor;
import fr.pastequeworld.bedwars.util.ColorUtil;
import fr.pastequeworld.bedwars.util.TitleUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Instance d'arene : un monde + une map + des equipes + un etat.
 *
 * Les arenes sont pool\u00e9es :
 *   - Si un joueur demande un mode et qu'une arene WAITING existe, il la rejoint.
 *   - Sinon une nouvelle arene est creee (monde + paste de la schematic).
 *   - A la fin, l'arene est detruite (unload + delete du monde).
 *
 * Une arene gere aussi son propre timer, ses evenements et sa timeline.
 */
public class Arena {

    private final BedWarsPlugin plugin;
    private final String id;
    private final GameMode mode;
    private final MapTemplate template;
    private final World world;

    private GameState state = GameState.WAITING;

    private final Map<TeamColor, Team> teams = new LinkedHashMap<TeamColor, Team>();
    private final Set<UUID> players = new CopyOnWriteArraySet<UUID>();
    private final Set<UUID> spectators = new CopyOnWriteArraySet<UUID>();

    private final List<Generator> generators = new ArrayList<Generator>();
    private final List<ShopVillager> shops = new ArrayList<ShopVillager>();

    private final Location queueSpawn;
    private final Location worldCorner;

    private int startCountdown;
    private int elapsedSeconds;    // depuis le debut de la partie
    private int countdownTaskId = -1;
    private int gameTaskId = -1;

    public Arena(BedWarsPlugin plugin, String id, GameMode mode, MapTemplate template, World world, Location worldCorner) {
        this.plugin = plugin;
        this.id = id;
        this.mode = mode;
        this.template = template;
        this.world = world;
        this.worldCorner = worldCorner;
        this.queueSpawn = materialize(template.getQueueSpawn());
        this.startCountdown = plugin.getConfigManager().getStartCountdown();

        setupTeams();
        buildWaitingCage();
    }

    /**
     * Construit une cage de verre 5x5x5 autour du {@link #queueSpawn} dans le monde
     * d'arene. Les joueurs en WAITING / STARTING y sont teleportes pour eviter
     * qu'ils tombent dans le vide avant le debut de la partie.
     * Au start, on ne nettoie pas : le monde est detruit en fin de partie.
     */
    private void buildWaitingCage() {
        if (queueSpawn == null || queueSpawn.getWorld() == null) return;
        int cx = queueSpawn.getBlockX();
        int cy = queueSpawn.getBlockY();
        int cz = queueSpawn.getBlockZ();
        World w = queueSpawn.getWorld();

        // Sol 5x5 (y - 1)
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                w.getBlockAt(x, cy - 1, z).setType(Material.STAINED_GLASS);
            }
        }
        // Murs 5x5, hauteur 4 (y..y+3)
        for (int y = cy; y <= cy + 3; y++) {
            for (int x = cx - 2; x <= cx + 2; x++) {
                setGlass(w, x, y, cz - 2);
                setGlass(w, x, y, cz + 2);
            }
            for (int z = cz - 2; z <= cz + 2; z++) {
                setGlass(w, cx - 2, y, z);
                setGlass(w, cx + 2, y, z);
            }
        }
        // Plafond
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                setGlass(w, x, cy + 4, z);
            }
        }
    }

    private void setGlass(World w, int x, int y, int z) {
        Block b = w.getBlockAt(x, y, z);
        if (b.getType() == Material.AIR) b.setType(Material.STAINED_GLASS);
    }

    private void setupTeams() {
        ConfigManager.ModeDefinition def = plugin.getConfigManager().getMode(mode);
        int teamCount = def == null ? 4 : def.getTeams();
        TeamColor[] colors = TeamColor.firstN(teamCount);
        for (TeamColor color : colors) {
            Team team = new Team(color);
            team.setSpawnLocation(materialize(template.getSpawnLocations().get(color)));
            team.setBedLocation(materialize(template.getBedLocations().get(color)));
            team.setShopLocation(materialize(template.getShopLocations().get(color)));
            team.setUpgradeLocation(materialize(template.getUpgradeLocations().get(color)));
            team.setIronGeneratorLocation(materialize(template.getIronGenLocations().get(color)));
            team.setGoldGeneratorLocation(materialize(template.getGoldGenLocations().get(color)));
            teams.put(color, team);
        }
    }

    private Location materialize(Vector rel) {
        if (rel == null) return null;
        return new Location(world,
                worldCorner.getX() + rel.getX(),
                worldCorner.getY() + rel.getY(),
                worldCorner.getZ() + rel.getZ());
    }

    // === Joueurs ===

    public boolean canJoin() {
        if (state != GameState.WAITING && state != GameState.STARTING) return false;
        ConfigManager.ModeDefinition def = plugin.getConfigManager().getMode(mode);
        return players.size() < def.getMaxPlayers();
    }

    public void addPlayer(Player player) {
        BedWarsPlayer bw = plugin.getPlayerDataManager().register(player);
        bw.setArena(this);
        bw.setState(PlayerState.QUEUEING);
        players.add(player.getUniqueId());

        // TP dans le monde de l'arene, sur la plateforme d'attente (cage en verre).
        // queueSpawn = centre de la map + 40Y dans le monde d'arene.
        if (queueSpawn != null) {
            player.teleport(queueSpawn);
        } else {
            plugin.getLogger().warning("Arena " + id + " n'a pas de queueSpawn, joueur non teleporte !");
        }
        resetInventoryForLobby(player);

        broadcast(plugin.getMessageManager().get("queue.player-joined",
                "player", player.getName(),
                "current", String.valueOf(players.size())).replace("%max%",
                String.valueOf(plugin.getConfigManager().getMode(mode).getMaxPlayers())));

        checkAutoStart();
        plugin.getScoreboardManager().refresh(this);
    }

    public void removePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        players.remove(uuid);
        spectators.remove(uuid);

        BedWarsPlayer bw = plugin.getPlayerDataManager().get(player);
        if (bw != null) {
            Team team = bw.getTeam();
            if (team != null) {
                team.removeMember(uuid);
                if (state == GameState.RUNNING) checkTeamEliminated(team);
            }
            bw.resetGameSession();
        }

        if (state == GameState.RUNNING) {
            broadcast(plugin.getMessageManager().get("queue.player-left",
                    "player", player.getName(),
                    "current", String.valueOf(players.size())).replace("%max%",
                    String.valueOf(plugin.getConfigManager().getMode(mode).getMaxPlayers())));
            checkWinCondition();
        } else if (state == GameState.WAITING || state == GameState.STARTING) {
            broadcast(plugin.getMessageManager().get("queue.player-left",
                    "player", player.getName(),
                    "current", String.valueOf(players.size())).replace("%max%",
                    String.valueOf(plugin.getConfigManager().getMode(mode).getMaxPlayers())));
            if (state == GameState.STARTING && players.size() < plugin.getConfigManager().getMode(mode).getMinPlayersToStart()) {
                cancelStartCountdown();
            }
        }

        plugin.getScoreboardManager().refresh(this);
    }

    private void resetInventoryForLobby(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setExp(0);
        player.setLevel(0);
        player.setFireTicks(0);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setGameMode(org.bukkit.GameMode.ADVENTURE);
    }

    // === Start / Stop ===

    public void checkAutoStart() {
        ConfigManager.ModeDefinition def = plugin.getConfigManager().getMode(mode);
        if (state != GameState.WAITING) return;
        if (players.size() >= def.getMinPlayersToStart()) {
            startCountdown();
        }
    }

    public void startCountdown() {
        if (state == GameState.STARTING) return;
        state = GameState.STARTING;
        this.startCountdown = plugin.getConfigManager().getStartCountdown();

        this.countdownTaskId = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                tickCountdown();
            }
        }, 20L, 20L).getTaskId();
    }

    private void cancelStartCountdown() {
        if (countdownTaskId != -1) {
            Bukkit.getScheduler().cancelTask(countdownTaskId);
            countdownTaskId = -1;
        }
        state = GameState.WAITING;
        broadcast(plugin.getMessageManager().get("queue.countdown-cancel"));
    }

    private void tickCountdown() {
        if (startCountdown <= 0) {
            cancelStartTaskOnly();
            start();
            return;
        }

        if (startCountdown <= 5 || startCountdown % 5 == 0) {
            String msg = plugin.getMessageManager().get(
                    startCountdown <= 5 ? "queue.countdown-5" : "queue.countdown",
                    "seconds", String.valueOf(startCountdown));
            broadcast(msg);
            for (Player p : onlinePlayers()) {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_PLING, 1f, startCountdown <= 5 ? 2f : 1f);
                if (startCountdown <= 5) {
                    TitleUtil.send(p, "&e" + startCountdown, "&7Preparez-vous...", 0, 22, 4);
                }
            }
        }
        startCountdown--;
    }

    private void cancelStartTaskOnly() {
        if (countdownTaskId != -1) {
            Bukkit.getScheduler().cancelTask(countdownTaskId);
            countdownTaskId = -1;
        }
    }

    public void start() {
        state = GameState.RUNNING;
        elapsedSeconds = 0;

        // Assignation des equipes
        assignTeamsAutomatic();

        // Teleport, inventaire, titre
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            BedWarsPlayer bw = plugin.getPlayerDataManager().get(p);
            if (bw == null || bw.getTeam() == null) continue;
            Team team = bw.getTeam();
            p.teleport(team.getSpawnLocation());
            p.setGameMode(org.bukkit.GameMode.SURVIVAL);
            p.getInventory().clear();
            p.getInventory().setArmorContents(null);
            plugin.getLobbyManager().equipTeamArmor(p, team);
            bw.setState(PlayerState.PLAYING);
            TitleUtil.send(p, plugin.getMessageManager().get("game.start-title"),
                    plugin.getMessageManager().get("game.start-subtitle"), 10, 50, 10);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERDRAGON_GROWL, 1f, 1f);
        }

        // Generateurs d'equipe + publics
        plugin.getArenaManager().spawnGenerators(this);

        // Shops
        plugin.getArenaManager().spawnShops(this);

        // Lits : ils sont deja dans la map schematic. On les tracke logiquement via bed location.

        // Timer principal
        this.gameTaskId = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                tickGame();
            }
        }, 20L, 20L).getTaskId();
    }

    private void assignTeamsAutomatic() {
        ConfigManager.ModeDefinition def = plugin.getConfigManager().getMode(mode);
        int perTeam = def.getPlayersPerTeam();
        List<UUID> list = new ArrayList<UUID>(players);
        Collections.shuffle(list);
        List<Team> teamList = new ArrayList<Team>(teams.values());

        for (int i = 0; i < list.size(); i++) {
            Team target = teamList.get(i % teamList.size());
            // si team pleine, passer a la suivante
            int safety = 0;
            while (target.size() >= perTeam && safety < teamList.size()) {
                safety++;
                target = teamList.get((i + safety) % teamList.size());
            }
            target.addMember(list.get(i));
            BedWarsPlayer bw = plugin.getPlayerDataManager().get(list.get(i));
            if (bw != null) bw.setTeam(target);
        }
    }

    private void tickGame() {
        elapsedSeconds++;
        plugin.getArenaManager().getEventsTimelineManager().tick(this, elapsedSeconds);
        plugin.getScoreboardManager().refresh(this);
    }

    // === Respawn, bed destroy, win ===

    public void onBedBroken(Team team, Player breaker) {
        team.setBedAlive(false);

        BedWarsPlayer bw = plugin.getPlayerDataManager().get(breaker);
        if (bw != null) bw.incrementBedsBroken();

        String breakerColor = bw != null && bw.getTeam() != null
                ? bw.getTeam().getColor().getChatColor().toString() : "&f";
        String msg = plugin.getMessageManager().get("game.bed-broken-broadcast")
                .replace("%team_color%", team.getColor().getChatColor().toString())
                .replace("%team_name%", team.getColor().getFrenchName().toUpperCase())
                .replace("%breaker_color%", breakerColor)
                .replace("%breaker%", breaker.getName());
        broadcast(msg);

        for (UUID uuid : team.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            TitleUtil.send(p, plugin.getMessageManager().get("game.bed-destroyed-title"),
                    plugin.getMessageManager().get("game.bed-destroyed-subtitle"), 10, 50, 10);
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1f, 1f);
        }

        for (Player p : onlinePlayers()) {
            if (!team.contains(p)) {
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERDRAGON_GROWL, 0.5f, 1f);
            }
        }
    }

    public void handleDeath(Player victim, Player killer, PlayerDeathEvent event) {
        BedWarsPlayer bwVictim = plugin.getPlayerDataManager().get(victim);
        if (bwVictim == null || bwVictim.getTeam() == null) return;
        Team victimTeam = bwVictim.getTeam();
        boolean finalKill = !victimTeam.isBedAlive();

        bwVictim.incrementDeaths();
        victimTeam.markDead(victim.getUniqueId());

        String victimColor = victimTeam.getColor().getChatColor().toString();
        String killerColor = "&f";

        if (killer != null && killer != victim) {
            BedWarsPlayer bwKiller = plugin.getPlayerDataManager().get(killer);
            if (bwKiller != null && bwKiller.getTeam() != null) {
                killerColor = bwKiller.getTeam().getColor().getChatColor().toString();
            }
            if (bwKiller != null) {
                if (finalKill) bwKiller.incrementFinalKills();
                else bwKiller.incrementKills();
            }
            String path = finalKill ? "game.final-kill-message" : "game.kill-message";
            broadcast(plugin.getMessageManager().get(path)
                    .replace("%killer_color%", killerColor)
                    .replace("%killer%", killer.getName())
                    .replace("%victim_color%", victimColor)
                    .replace("%victim%", victim.getName()));
        } else {
            broadcast(plugin.getMessageManager().get("game.void-death")
                    .replace("%victim_color%", victimColor)
                    .replace("%victim%", victim.getName()));
        }

        if (event != null) {
            event.setDeathMessage(null);
            event.getDrops().clear();
            event.setDroppedExp(0);
        }

        if (finalKill) {
            bwVictim.setState(PlayerState.SPECTATING);
            spectators.add(victim.getUniqueId());
            plugin.getLobbyManager().setupSpectator(victim);
            checkTeamEliminated(victimTeam);
        } else {
            bwVictim.setState(PlayerState.RESPAWNING);
            plugin.getLobbyManager().startRespawnCountdown(victim, this);
        }

        checkWinCondition();
    }

    private void checkTeamEliminated(Team team) {
        if (team.isEliminated()) {
            broadcast(plugin.getMessageManager().get("game.team-eliminated")
                    .replace("%team_color%", team.getColor().getChatColor().toString())
                    .replace("%team_name%", team.getColor().getFrenchName().toUpperCase()));
            for (Player p : onlinePlayers()) {
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
            }
        }
    }

    public void checkWinCondition() {
        if (state != GameState.RUNNING) return;
        List<Team> alive = new ArrayList<Team>();
        for (Team t : teams.values()) {
            if (!t.isEliminated()) alive.add(t);
        }
        if (alive.size() <= 1) {
            end(alive.isEmpty() ? null : alive.get(0));
        }
    }

    public void end(Team winner) {
        if (state == GameState.ENDING || state == GameState.RESETTING) return;
        state = GameState.ENDING;

        if (gameTaskId != -1) {
            Bukkit.getScheduler().cancelTask(gameTaskId);
            gameTaskId = -1;
        }

        plugin.getArenaManager().stopGenerators(this);

        if (winner != null) {
            for (UUID uuid : winner.getMembers()) {
                BedWarsPlayer bw = plugin.getPlayerDataManager().get(uuid);
                if (bw != null) bw.setWinner(true);
            }
            broadcast(ColorUtil.color("&7&m---------------------------------"));
            broadcast(plugin.getMessageManager().get("game.victory-title"));
            broadcast(plugin.getMessageManager().get("game.victory-subtitle")
                    .replace("%team_color%", winner.getColor().getChatColor().toString())
                    .replace("%team_name%", winner.getColor().getFrenchName().toUpperCase()));
            broadcast(ColorUtil.color("&7&m---------------------------------"));

            for (Player p : onlinePlayers()) {
                if (winner.contains(p)) {
                    TitleUtil.send(p, plugin.getMessageManager().get("game.victory-title"),
                            plugin.getMessageManager().get("game.victory-subtitle")
                                    .replace("%team_color%", winner.getColor().getChatColor().toString())
                                    .replace("%team_name%", winner.getColor().getFrenchName().toUpperCase()),
                            10, 100, 20);
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                } else {
                    TitleUtil.send(p, plugin.getMessageManager().get("game.defeat-title"),
                            plugin.getMessageManager().get("game.defeat-subtitle"),
                            10, 100, 20);
                }
            }
        }

        // Delai avant cleanup
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                teleportAllToLobby();
                plugin.getArenaManager().destroy(Arena.this);
            }
        }, 20L * plugin.getConfigManager().getEndDelay());
    }

    private void teleportAllToLobby() {
        Location lobby = plugin.getConfigManager().getLobbySpawn();
        for (UUID uuid : new ArrayList<UUID>(players)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            p.teleport(lobby);
            BedWarsPlayer bw = plugin.getPlayerDataManager().get(p);
            if (bw != null) bw.resetGameSession();
            plugin.getLobbyManager().setupLobbyPlayer(p);
        }
        for (UUID uuid : new ArrayList<UUID>(spectators)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            p.teleport(lobby);
            BedWarsPlayer bw = plugin.getPlayerDataManager().get(p);
            if (bw != null) bw.resetGameSession();
            plugin.getLobbyManager().setupLobbyPlayer(p);
        }
    }

    // === Utility ===

    public void broadcast(String message) {
        String colored = ColorUtil.color(message);
        for (Player p : onlinePlayers()) p.sendMessage(colored);
    }

    public Collection<Player> onlinePlayers() {
        List<Player> list = new ArrayList<Player>();
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) list.add(p);
        }
        for (UUID uuid : spectators) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) list.add(p);
        }
        return list;
    }

    public Team getTeamAt(Location loc) {
        int r = plugin.getConfigManager().getBedDestroyRadius();
        for (Team team : teams.values()) {
            if (team.getBedLocation() == null) continue;
            if (team.getBedLocation().getWorld() != loc.getWorld()) continue;
            if (team.getBedLocation().distanceSquared(loc) <= r * r) return team;
        }
        return null;
    }

    public Team getTeamOf(Player player) {
        BedWarsPlayer bw = plugin.getPlayerDataManager().get(player);
        return bw == null ? null : bw.getTeam();
    }

    public Collection<Team> getTeams() { return teams.values(); }
    public Team getTeam(TeamColor color) { return teams.get(color); }

    public String getId() { return id; }
    public GameMode getMode() { return mode; }
    public MapTemplate getTemplate() { return template; }
    public World getWorld() { return world; }
    public GameState getState() { return state; }
    public void setState(GameState state) { this.state = state; }
    public Set<UUID> getPlayers() { return players; }
    public Set<UUID> getSpectators() { return spectators; }
    public int getElapsedSeconds() { return elapsedSeconds; }
    public int getStartCountdown() { return startCountdown; }
    public Location getQueueSpawn() { return queueSpawn; }
    public Location getWorldCorner() { return worldCorner; }
    public List<Generator> getGenerators() { return generators; }
    public List<ShopVillager> getShops() { return shops; }
}
