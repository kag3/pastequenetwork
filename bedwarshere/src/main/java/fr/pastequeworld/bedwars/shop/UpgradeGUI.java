package fr.pastequeworld.bedwars.shop;

import fr.pastequeworld.bedwars.BedWarsPlugin;
import fr.pastequeworld.bedwars.game.Arena;
import fr.pastequeworld.bedwars.team.Team;
import fr.pastequeworld.bedwars.util.ColorUtil;
import fr.pastequeworld.bedwars.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * GUI des upgrades d'equipe (Sharpened Swords, Reinforced Armor, Haste, HealPool, DragonBuff).
 * Format Hypixel : icones permanentes avec prix progressif.
 */
public class UpgradeGUI {

    public static final String TITLE = ColorUtil.color("&8Ameliorations d'equipe");

    // Costs (diamant) : Hypixel-like
    public static final int[] SHARPNESS_COST = {4};
    public static final int[] PROTECTION_COST = {2, 4, 8, 16};
    public static final int[] HASTE_COST = {2, 4};
    public static final int HEAL_POOL_COST = 1;
    public static final int DRAGON_BUFF_COST = 5;

    private final BedWarsPlugin plugin;

    public UpgradeGUI(BedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Arena arena) {
        Team team = arena == null ? null : plugin.getPlayerDataManager().get(player).getTeam();
        if (team == null) return;

        Inventory inv = Bukkit.createInventory(null, 36, TITLE);

        inv.setItem(10, buildSharp(team));
        inv.setItem(11, buildProt(team));
        inv.setItem(12, buildHaste(team));
        inv.setItem(13, buildHealPool(team));
        inv.setItem(14, buildDragon(team));
        inv.setItem(15, buildTraps(team));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    private ItemStack buildSharp(Team team) {
        return new ItemBuilder(Material.DIAMOND_SWORD)
                .name("&aEpees affutees")
                .lore("",
                        team.getSharpnessLevel() >= 1 ? "&aNIVEAU " + team.getSharpnessLevel() + " ACHETE" : "&7Offre &aSharpness I &7a toutes vos epees.",
                        "",
                        team.getSharpnessLevel() >= 1 ? "&c\u25b6 Maximum atteint" : "&7Cout: &b" + SHARPNESS_COST[0] + " Diamants",
                        team.getSharpnessLevel() >= 1 ? "" : "&e\u25b6 Clic pour acheter")
                .hideAttributes()
                .build();
    }

    private ItemStack buildProt(Team team) {
        int lvl = team.getProtectionLevel();
        int nextCost = lvl < PROTECTION_COST.length ? PROTECTION_COST[lvl] : -1;
        return new ItemBuilder(Material.IRON_CHESTPLATE)
                .name("&aArmure renforcee")
                .lore("",
                        "&7Niveau actuel: &f" + lvl + (lvl >= 4 ? " &c(MAX)" : ""),
                        "&7Ajoute Protection " + (lvl + 1) + " a toute l'armure.",
                        "",
                        nextCost > 0 ? "&7Cout: &b" + nextCost + " Diamants" : "&cMaximum atteint",
                        nextCost > 0 ? "&e\u25b6 Clic pour acheter" : "")
                .hideAttributes()
                .build();
    }

    private ItemStack buildHaste(Team team) {
        int lvl = team.getHasteLevel();
        int nextCost = lvl < HASTE_COST.length ? HASTE_COST[lvl] : -1;
        return new ItemBuilder(Material.GOLD_PICKAXE)
                .name("&aMineurs acharnes")
                .lore("",
                        "&7Niveau actuel: &f" + lvl + (lvl >= 2 ? " &c(MAX)" : ""),
                        "&7Tous les joueurs obtiennent Haste " + (lvl + 1) + ".",
                        "",
                        nextCost > 0 ? "&7Cout: &b" + nextCost + " Diamants" : "&cMaximum atteint",
                        nextCost > 0 ? "&e\u25b6 Clic pour acheter" : "")
                .hideAttributes()
                .build();
    }

    private ItemStack buildHealPool(Team team) {
        return new ItemBuilder(Material.BEACON)
                .name("&aSoin d'equipe")
                .lore("",
                        team.hasHealPool() ? "&aACTIVE" : "&7Active Regeneration I pour",
                        team.hasHealPool() ? "" : "&7tous les alliers pres du spawn.",
                        "",
                        team.hasHealPool() ? "&c\u25b6 Deja achete" : "&7Cout: &b" + HEAL_POOL_COST + " Diamant",
                        team.hasHealPool() ? "" : "&e\u25b6 Clic pour acheter")
                .hideAttributes()
                .build();
    }

    private ItemStack buildDragon(Team team) {
        return new ItemBuilder(Material.DRAGON_EGG)
                .name("&aBuff du Dragon")
                .lore("",
                        team.hasDragonBuff() ? "&aACTIVE" : "&7Fait apparaitre &c2 Dragons &7au",
                        team.hasDragonBuff() ? "" : "&7Sudden Death pour votre equipe.",
                        "",
                        team.hasDragonBuff() ? "&c\u25b6 Deja achete" : "&7Cout: &b" + DRAGON_BUFF_COST + " Diamants",
                        team.hasDragonBuff() ? "" : "&e\u25b6 Clic pour acheter")
                .hideAttributes()
                .build();
    }

    private ItemStack buildTraps(Team team) {
        return new ItemBuilder(Material.TRIPWIRE_HOOK)
                .name("&aPieges d'equipe")
                .lore("",
                        "&7Systeme de pieges (Alarme, Contre-attaque).",
                        "",
                        "&eA venir...")
                .hideAttributes()
                .build();
    }
}
