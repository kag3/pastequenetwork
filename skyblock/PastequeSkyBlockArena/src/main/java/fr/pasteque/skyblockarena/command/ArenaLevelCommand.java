package fr.pasteque.skyblockarena.command;

import fr.pasteque.skyblockarena.PastequeSkyBlockArenaPlugin;
import fr.pasteque.skyblockarena.model.ArenaPlayerData;
import fr.pasteque.skyblockarena.service.ArenaLevelService;
import fr.pasteque.skyblockarena.service.PlayerDataService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ArenaLevelCommand implements CommandExecutor {

    private final PastequeSkyBlockArenaPlugin plugin;
    private final PlayerDataService playerDataService;
    private final ArenaLevelService arenaLevelService;

    public ArenaLevelCommand(PastequeSkyBlockArenaPlugin plugin, PlayerDataService playerDataService, ArenaLevelService arenaLevelService) {
        this.plugin = plugin;
        this.playerDataService = playerDataService;
        this.arenaLevelService = arenaLevelService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.color(plugin.prefix() + "&fCommande réservée aux joueurs."));
            return true;
        }
        Player player = (Player) sender;
        ArenaPlayerData data = playerDataService.get(player);
        int next = arenaLevelService.getNextRequirement(data.getLevel());
        player.sendMessage(plugin.color(plugin.prefix() + plugin.getConfig().getString("messages.arena-level", "&fNiveau actuel : &d%level% &8| &fXP : &d%xp%/%next%")
                .replace("%level%", String.valueOf(data.getLevel()))
                .replace("%xp%", String.valueOf(data.getXp()))
                .replace("%next%", String.valueOf(next))));
        player.sendMessage(plugin.color(plugin.prefix() + plugin.getConfig().getString("messages.arena-stats", "&fKills : &d%kills% &8| &fMorts : &d%deaths% &8| &fRatio : &d%ratio%")
                .replace("%kills%", String.valueOf(data.getKills()))
                .replace("%deaths%", String.valueOf(data.getDeaths()))
                .replace("%ratio%", playerDataService.formatRatio(player))));
        return true;
    }
}
