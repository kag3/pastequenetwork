package fr.pastequeworld.bedwars.game;

/**
 * Modes de jeu supportes.
 * Configurations pour chacun : voir config.yml / ModeDefinition.
 */
public enum GameMode {
    SOLO("solo"),
    DUO("duo"),
    TEAMS("teams");

    private final String configKey;

    GameMode(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() { return configKey; }

    public static GameMode fromConfigKey(String key) {
        if (key == null) return null;
        for (GameMode m : values()) if (m.configKey.equalsIgnoreCase(key)) return m;
        return null;
    }
}
