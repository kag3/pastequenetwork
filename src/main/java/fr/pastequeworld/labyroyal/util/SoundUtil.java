package fr.pastequeworld.labyroyal.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Collection;

public class SoundUtil {

    public static void play(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public static void playAll(Collection<? extends Player> players, Sound sound, float volume, float pitch) {
        for (Player player : players) {
            play(player, sound, volume, pitch);
        }
    }

    public static void countdown(Player player) {
        play(player, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
    }

    public static void countdownFinal(Player player) {
        play(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
    }

    public static void gameStart(Player player) {
        play(player, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.2f);
    }

    public static void phaseChange(Player player) {
        play(player, Sound.ENTITY_WITHER_SPAWN, 0.6f, 1.0f);
    }

    public static void kill(Player player) {
        play(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.5f);
    }

    public static void death(Player player) {
        play(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 0.8f);
    }

    public static void elimination(Player player) {
        play(player, Sound.ENTITY_BLAZE_DEATH, 1.0f, 1.0f);
    }

    public static void victory(Player player) {
        play(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    public static void stormWarning(Player player) {
        play(player, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.2f);
    }

    public static void playerJoin(Player player) {
        play(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
    }

    public static void playerLeave(Player player) {
        play(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
    }

    public static void chestOpen(Player player) {
        play(player, Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
    }

    public static void tick(Player player) {
        play(player, Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.8f);
    }
}
