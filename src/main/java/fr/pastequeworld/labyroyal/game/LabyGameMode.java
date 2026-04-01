package fr.pastequeworld.labyroyal.game;

public enum LabyGameMode {
    SOLO("Solo", 1),
    DUO("Duo", 2);

    private final String displayName;
    private final int teamSize;

    LabyGameMode(String displayName, int teamSize) {
        this.displayName = displayName;
        this.teamSize = teamSize;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getTeamSize() {
        return teamSize;
    }
}
