package fr.pastequeworld.bedwars.ui;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.config.ConfigManager;
import fr.pastequeworld.bedwars.game.Arena;
import fr.pastequeworld.bedwars.generator.Generator;
import fr.pastequeworld.bedwars.generator.GeneratorType;
import fr.pastequeworld.bedwars.team.Team;
import fr.pastequeworld.bedwars.util.ColorUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Gere la timeline des events (Diamond II, Emerald II, Bed Destruction, Sudden Death, Game End).
 * Chaque arene a son propre etat de progression.
 */
public class EventsTimelineManager {

    private final BedWarsPlugin plugin;
    private final Map<String, EventState> states = new HashMap<String, EventState>();

    public EventsTimelineManager(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void tick(Arena arena, int elapsedSeconds) {
        EventState state = states.get(arena.getId());
        if (state == null) {
            state = new EventState();
            states.put(arena.getId(), state);
        }
        ConfigManager.EventsTimeline t = plugin.getConfigManager().getTimeline();

        if (!state.diamondII && elapsedSeconds >= t.diamondII) {
            state.diamondII = true;
            upgradeGenerator(arena, GeneratorType.DIAMOND);
            announce(arena, "events.diamond-ii-announce");
        }
        if (!state.emeraldII && elapsedSeconds >= t.emeraldII) {
            state.emeraldII = true;
            upgradeGenerator(arena, GeneratorType.EMERALD);
            announce(arena, "events.emerald-ii-announce");
        }
        if (!state.diamondIII && elapsedSeconds >= t.diamondIII) {
            state.diamondIII = true;
            upgradeGenerator(arena, GeneratorType.DIAMOND);
            announce(arena, "events.diamond-iii-announce");
        }
        if (!state.emeraldIII && elapsedSeconds >= t.emeraldIII) {
            state.emeraldIII = true;
            upgradeGenerator(arena, GeneratorType.EMERALD);
            announce(arena, "events.emerald-iii-announce");
        }
        if (!state.bedDestruction && elapsedSeconds >= t.bedDestruction) {
            state.bedDestruction = true;
            for (Team team : arena.getTeams()) team.setBedAlive(false);
            announce(arena, "events.bed-destruction-announce");
        }
        if (!state.suddenDeath && elapsedSeconds >= t.suddenDeath) {
            state.suddenDeath = true;
            announce(arena, "events.sudden-death-announce");
        }
        if (!state.gameEnd && elapsedSeconds >= t.gameEnd) {
            state.gameEnd = true;
            announce(arena, "events.game-end-announce");
            // Draw : arene se termine
            arena.end(null);
        }
    }

    private void upgradeGenerator(Arena arena, GeneratorType type) {
        for (Generator g : arena.getGenerators()) {
            if (g.getType() == type) g.upgradeTier();
        }
    }

    private void announce(Arena arena, String path) {
        String msg = plugin.getMessageManager().get(path);
        arena.broadcast(msg);
        for (Player p : arena.onlinePlayers()) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_PLING, 1f, 1.8f);
        }
    }

    public void cleanup(Arena arena) {
        states.remove(arena.getId());
    }

    /**
     * Retourne l'event suivant et le temps restant (en secondes).
     * Utilise dans le scoreboard.
     */
    public NextEventInfo getNextEvent(Arena arena) {
        EventState state = states.get(arena.getId());
        if (state == null) state = new EventState();
        ConfigManager.EventsTimeline t = plugin.getConfigManager().getTimeline();
        int elapsed = arena.getElapsedSeconds();

        if (!state.diamondII && elapsed < t.diamondII) return info("Diamant II", t.diamondII - elapsed);
        if (!state.emeraldII && elapsed < t.emeraldII) return info("Emeraude II", t.emeraldII - elapsed);
        if (!state.diamondIII && elapsed < t.diamondIII) return info("Diamant III", t.diamondIII - elapsed);
        if (!state.emeraldIII && elapsed < t.emeraldIII) return info("Emeraude III", t.emeraldIII - elapsed);
        if (!state.bedDestruction && elapsed < t.bedDestruction) return info("Destruction des lits", t.bedDestruction - elapsed);
        if (!state.suddenDeath && elapsed < t.suddenDeath) return info("Sudden Death", t.suddenDeath - elapsed);
        if (!state.gameEnd && elapsed < t.gameEnd) return info("Fin de partie", t.gameEnd - elapsed);
        return info("Fin imminente", 0);
    }

    private NextEventInfo info(String name, int seconds) {
        NextEventInfo i = new NextEventInfo();
        i.name = name;
        i.secondsRemaining = seconds;
        return i;
    }

    public static class NextEventInfo {
        public String name;
        public int secondsRemaining;
    }

    private static class EventState {
        boolean diamondII, diamondIII, emeraldII, emeraldIII, bedDestruction, suddenDeath, gameEnd;
    }
}
