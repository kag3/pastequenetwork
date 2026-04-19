package fr.pastequeworld.bedwars.team;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;

/**
 * Couleurs d'equipe disponibles avec helpers d'integration Bukkit.
 * L'ordre est important : utilise pour l'assignation automatique des teams
 * quand 4 equipes sont necessaires (RED/BLUE/GREEN/YELLOW).
 */
public enum TeamColor {
    RED(ChatColor.RED, DyeColor.RED, "Rouge", "R"),
    BLUE(ChatColor.BLUE, DyeColor.BLUE, "Bleu", "B"),
    GREEN(ChatColor.GREEN, DyeColor.GREEN, "Vert", "V"),
    YELLOW(ChatColor.YELLOW, DyeColor.YELLOW, "Jaune", "J"),
    AQUA(ChatColor.AQUA, DyeColor.LIGHT_BLUE, "Cyan", "C"),
    WHITE(ChatColor.WHITE, DyeColor.WHITE, "Blanc", "W"),
    PINK(ChatColor.LIGHT_PURPLE, DyeColor.PINK, "Rose", "P"),
    GRAY(ChatColor.DARK_GRAY, DyeColor.GRAY, "Gris", "G");

    private final ChatColor chatColor;
    private final DyeColor dyeColor;
    private final String frenchName;
    private final String prefix;

    TeamColor(ChatColor chatColor, DyeColor dyeColor, String frenchName, String prefix) {
        this.chatColor = chatColor;
        this.dyeColor = dyeColor;
        this.frenchName = frenchName;
        this.prefix = prefix;
    }

    public ChatColor getChatColor() { return chatColor; }

    public DyeColor getDyeColor() { return dyeColor; }

    public String getFrenchName() { return frenchName; }

    public String getPrefix() { return prefix; }

    public byte getWoolData() { return dyeColor.getWoolData(); }

    public static TeamColor[] firstN(int n) {
        TeamColor[] values = values();
        if (n >= values.length) return values;
        TeamColor[] result = new TeamColor[n];
        System.arraycopy(values, 0, result, 0, n);
        return result;
    }
}
