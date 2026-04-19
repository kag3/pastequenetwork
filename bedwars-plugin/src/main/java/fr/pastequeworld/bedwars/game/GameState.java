package fr.pastequeworld.bedwars.game;

/**
 * Cycle de vie d'une arene :
 *   WAITING  -> attente de joueurs dans la map (queue ouverte)
 *   STARTING -> countdown avant le debut
 *   RUNNING  -> partie en cours
 *   ENDING   -> fin de partie, dragons / cinematic
 *   RESETTING-> reset via WorldEdit
 */
public enum GameState {
    WAITING,
    STARTING,
    RUNNING,
    ENDING,
    RESETTING
}
