package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.arena.ArenaLevelService;
import fr.pasteque.skyblock.arena.PlayerDataService;
import fr.pasteque.skyblock.arena.model.ArenaPlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ArenaLevelCommand implements CommandExecutor {

    private final PastequeSkyblockPlugin plugin;
    private final PlayerDataService playerDataService;
    private final ArenaLevelService arenaLevelService;

    public ArenaLevelCommand(PastequeSkyblockPlugin plugin, PlayerDataService playerDataService, ArenaLevelService arenaLevelService) {
        this.plugin = plugin;
        this.playerDataService = playerDataService;
        this.arenaLevelService = arenaLevelService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.color(plugin.getPrefix() + "&fCommande reservee aux joueurs."));
            return true;
        }
        Player player = (Player) sender;
        ArenaPlayerData data = playerDataService.get(player);
        int next = arenaLevelService.getNextRequirement(data.getLevel());
        player.sendMessage(plugin.color(plugin.getPrefix() + plugin.getConfig().getString("messages.arena-level", "&fNiveau actuel : &d%level% &8| &fXP : &d%xp%/%next%")
                .replace("%level%", String.valueOf(data.getLevel()))
                .replace("%xp%", String.valueOf(data.getXp()))
                .replace("%next%", String.valueOf(next))));
        player.sendMessage(plugin.color(plugin.getPrefix() + plugin.getConfig().getString("messages.arena-stats", "&fKills : &d%kills% &8| &fMorts : &d%deaths% &8| &fRatio : &d%ratio%")
                .replace("%kills%", String.valueOf(data.getKills()))
                .replace("%deaths%", String.valueOf(data.getDeaths()))
                .replace("%ratio%", playerDataService.formatRatio(player))));
        return true;
    }
}
