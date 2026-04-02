package fr.pastequeworld.labyroyal.game;

public enum GameState {
    WAITING("En attente"),
    STARTING("D\u00e9marrage"),
    PREPARATION("Pr\u00e9paration"),
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
