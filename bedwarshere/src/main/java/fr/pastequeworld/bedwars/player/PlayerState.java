package fr.pastequeworld.bedwars.player;

/**
 * Etat d'un joueur dans l'univers BedWars.
 * Sert a router les events : un joueur LOBBY ne recoit pas
 * les events de game, un SPECTATOR est invisible / intangible, etc.
 */
public enum PlayerState {
    LOBBY,       // dans le lobby de selection
    QUEUEING,    // en file d'attente sur une arene (map affichee, bulle invulnerable)
    PLAYING,     // en partie active
    RESPAWNING,  // mort attend respawn
    SPECTATING   // spectateur (lit tombe ou apres elimination finale)
}
