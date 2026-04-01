package fr.pastequeworld.labyroyal.game;

public enum GameState {
    WAITING("En attente"),
    STARTING("Demarrage"),
    PREPARATION("Preparation"),
    PVP("Combat"),
    ENDING("Fin");

    private final String displayName;

    GameState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
