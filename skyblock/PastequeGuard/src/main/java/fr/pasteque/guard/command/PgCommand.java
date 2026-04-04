package fr.pasteque.guard.command;

import fr.pasteque.guard.PastequeGuardPlugin;
import fr.pasteque.guard.gui.PanelGui;
import fr.pasteque.guard.model.SanctionEntry;
import fr.pasteque.guard.model.SanctionType;
import fr.pasteque.guard.service.FilterService;
import fr.pasteque.guard.service.ReportService;
import fr.pasteque.guard.service.SanctionService;
import fr.pasteque.guard.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PgCommand implements CommandExecutor, TabCompleter {

    private final PastequeGuardPlugin plugin;
    private final SanctionService sanctionService;
    private final ReportService reportService;
    private final FilterService filterService;

    public PgCommand(PastequeGuardPlugin plugin, SanctionService sanctionService, ReportService reportService, FilterService filterService) {
        this.plugin = plugin;
        this.sanctionService = sanctionService;
        this.reportService = reportService;
        this.filterService = filterService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("report") || sub.equals("signal") || sub.equals("reporter")) {
            return handleReport(sender, args);
        }

        if (!sender.hasPermission("pastequeguard.staff")) {
            sender.sendMessage(plugin.getPrefix() + plugin.color(plugin.getConfig().getString("messages.no-permission")));
            return true;
        }

        if (sub.equals("panel")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getPrefix() + plugin.color(plugin.getConfig().getString("messages.player-only")));
                return true;
            }
            Player player = (Player) sender;
            player.openInventory(PanelGui.create(plugin, reportService));
            player.sendMessage(plugin.getPrefix() + plugin.color(plugin.getConfig().getString("messages.panel-opened")));
            return true;
        }

        if (sub.equals("ban") || sub.equals("mute") || sub.equals("kick") || sub.equals("warn") || sub.equals("tempban") || sub.equals("tempmute")) {
            return handleSanction(sender, sub, args);
        }
        if (sub.equals("unban")) {
            return handleUnban(sender, args);
        }
        if (sub.equals("unmute")) {
            return handleUnmute(sender, args);
        }
        if (sub.equals("history")) {
            return handleHistory(sender, args);
        }

        sendHelp(sender);
        return true;
    }

    private boolean handleReport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPrefix() + plugin.color(plugin.getConfig().getString("messages.player-only")));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.getPrefix() + plugin.color(plugin.getConfig().getString("messages.syntax").replace("%syntax%", "/pg report <joueur> <motif>")));
            return true;
        }
        Player player = (Player) sender;
        String target = args[1];
        if (target.equalsIgnoreCase(player.getName())) {
            player.sendMessage(plugin.getPrefix() + plugin.color("&dVous ne pouvez pas vous signaler vous-même."));
            return true;
        }
        String reason = join(args, 2);
        reportService.createReport(player.getName(), target, reason);
        player.sendMessage(plugin.getPrefix() + plugin.color(plugin.getConfig().getString("messages.report-received")
                .replace("%target%", target)));
        String notify = plugin.getPrefix() + plugin.color(plugin.getConfig().getString("messages.report-notify")
                .replace("%author%", player.getName())
                .replace("%target%", target)
                .replace("%reason%", reason));
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("pastequeguard.staff")) {
                online.sendMessage(notify);
            }
        }
        return true;
    }

    private boolean handleSanction(CommandSender sender, String sub, String[] args) {
        if (sub.equals("kick") || sub.equals("warn")) {
            if (args.length < 3) {
                sender.sendMessage(plugin.getPrefix() + plugin.color(plugin.getConfig().getString("messages.syntax").replace("%syntax%", "/pg " + sub + " <joueur> <raison>")));
                return true;
            }
        } else if (sub.equals("tempban") || sub.equals("tempmute")) {
            if (args.length < 4) {
                sender.sendMessage(plugin.getPrefix() + plugin.color(plugin.getConfig().getString("messages.syntax").replace("%syntax%", "/pg " + sub + " <joueur> <durée> <raison>")));
                return true;
            }
        } else {
            if (args.length < 3) {
                sender.sendMessage(plugin.getPrefix() + plugin.color(plugin.getConfig().getString("messages.syntax").replace("%syntax%", "/pg " + sub + " <joueur> <raison>")));
                return true;
            }
        }

        String targetName = sanctionService.resolveLastKnownName(args[1]);
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        String staff = sender.getName();

        if (sub.equals("kick")) {
            String reason = join(args, 2);
            sanctionService.disconnectIfOnline(targetName, sanctionService.buildKickScreen(staff, reason));
            sender.sendMessage(plugin.getPrefix() + plugin.color(plugin.getConfig().getString("messages.sanction-applied").replace("%target%", targetName)));
            return true;
        }

        if (sub.equals("warn")) {
            String reason = join(args, 2);
            sanctionService.applySanction(targetName, target.getUniqueId(), staff, SanctionType.WARN, reason, 0L);
            Player online = Bukkit.getPlayerExact(targetName);
            if (online != null) {
                online.sendMessage(plugin.getPrefix() + plugin.color(plugin.getConfig().getString("messages.warn-received").replace("%reason%", reason)));
            }
            sender.sendMessage(plugin.getPrefix() + plugin.color(plugin.getConfig().getString("messages.warn-sent").replace("%target%", targetName)));
            return true;
        }

        if (sub.equals("ban")) {
            String reason = join(args, 2);
            sanctionService.applySanction(targetName, target.getUniqueId(), staff, SanctionType.BAN, reason, 0L);
            SanctionEntry entry = sanctionService.getActiveBan(targetName);
            if (entry != null) {
                sanctionService.disconnectIfOnline(targetName, sanctionService.buildBanScreen(entry));
            }
            sender.sendMessage(plugin.getPrefix() + plugin.color(plugin.getConfig().getString("messages.sanction-applied").replace("%target%", targetName)));
            return true;
        }

        if (sub.equals("mute")) {
            String reason = join(args, 2);
            sanctionService.applySanction(targetName, target.getUniqueId(), staff, SanctionType.MUTE, reason, 0L);
            Player online = Bukkit.getPlayerExact(targetName);
            if (online != null) {
                SanctionEntry entry = sanctionService.getActiveMute(targetName);
                if (entry != null) {
                    online.sendMessage(sanctionService.buildMuteMessage(entry));
                }
            }
            sender.sendMessage(plugin.getPrefix() + plugin.color(plugin.getConfig().getString("messages.sanction-applied").replace("%target%", targetName)));
            return true;
        }

        if (sub.equals("tempban")) {
            long duration = TimeUtil.parseDuration(args[2]);
            if (duration <= 0L) {
                sender.sendMessage(plugin.getPrefix() + plugin.color("&dDurée invalide. Exemple : 30m, 2h, 7d"));
                return true;
            }
            String reason = join(args, 3);
            sanctionService.applySanction(targetName, target.getUniqueId(), staff, SanctionType.TEMPBAN, reason, duration);
            SanctionEntry entry = sanctionService.getActiveBan(targetName);
            if (entry != null) {
                sanctionService.disconnectIfOnline(targetName, sanctionService.buildBanScreen(entry));
            }
            sender.sendMessage(plugin.getPrefix() + plugin.color(plugin.getConfig().getString("messages.sanction-applied").replace("%target%", targetName)));
            return true;
        }

        if (sub.equals("tempmute")) {
            long duration = TimeUtil.parseDuration(args[2]);
            if (duration <= 0L) {
                sender.sendMessage(plugin.getPrefix() + plugin.color("&dDurée invalide. Exemple : 30m, 2h, 7d"));
                return true;
            }
            String reason = join(args, 3);
            sanctionService.applySanction(targetName, target.getUniqueId(), staff, SanctionType.TEMPMUTE, reason, duration);
            Player online = Bukkit.getPlayerExact(targetName);
            if (online != null) {
                SanctionEntry entry = sanctionService.getActiveMute(targetName);
                if (entry != null) {
                    online.sendMessage(sanctionService.buildMuteMessage(entry));
                }
            }
            sender.sendMessage(plugin.getPrefix() + plugin.color(plugin.getConfig().getString("messages.sanction-applied").replace("%target%", targetName)));
            return true;
        }

        return true;
    }

    private boolean handleUnban(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getPrefix() + plugin.color(plugin.getConfig().getString("messages.syntax").replace("%syntax%", "/pg unban <joueur>")));
            return true;
        }
        sanctionService.unban(args[1], sender.getName());
        sender.sendMessage(plugin.getPrefix() + plugin.color(plugin.getConfig().getString("messages.unban-success").replace("%target%", args[1])));
        return true;
    }

    private boolean handleUnmute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getPrefix() + plugin.color(plugin.getConfig().getString("messages.syntax").replace("%syntax%", "/pg unmute <joueur>")));
            return true;
        }
        sanctionService.unmute(args[1], sender.getName());
        sender.sendMessage(plugin.getPrefix() + plugin.color(plugin.getConfig().getString("messages.unmute-success").replace("%target%", args[1])));
        return true;
    }

    private boolean handleHistory(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getPrefix() + plugin.color(plugin.getConfig().getString("messages.syntax").replace("%syntax%", "/pg history <joueur>")));
            return true;
        }
        List<SanctionEntry> history = sanctionService.getHistory(args[1]);
        if (history.isEmpty()) {
            sender.sendMessage(plugin.getPrefix() + plugin.color(plugin.getConfig().getString("messages.no-history").replace("%target%", args[1])));
            return true;
        }
        sender.sendMessage(plugin.getPrefix() + plugin.color(plugin.getConfig().getString("messages.history-header").replace("%target%", args[1])));
        SimpleDateFormat format = new SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE);
        for (SanctionEntry entry : history) {
            sender.sendMessage(plugin.color("&f- &d" + entry.getType().name() + " &f| &d" + entry.getReason() + " &f| &d" + entry.getStaffName() + " &f| &d" + format.format(new Date(entry.getCreatedAt()))));
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getPrefix() + plugin.color("&fCommandes : &d/pg report, /pg panel, /pg ban, /pg tempban, /pg mute, /pg tempmute, /pg kick, /pg warn, /pg unban, /pg unmute, /pg history"));
    }

    private String join(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (builder.length() > 0) builder.append(' ');
            builder.append(args[i]);
        }
        return builder.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("report", "panel", "ban", "tempban", "mute", "tempmute", "kick", "warn", "unban", "unmute", "history"), args[0]);
        }
        if (args.length == 2 && Arrays.asList("report", "ban", "tempban", "mute", "tempmute", "kick", "warn", "unban", "unmute", "history").contains(args[0].toLowerCase(Locale.ROOT))) {
            List<String> players = new ArrayList<String>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                players.add(online.getName());
            }
            return filter(players, args[1]);
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("tempban") || args[0].equalsIgnoreCase("tempmute"))) {
            return filter(Arrays.asList("10m", "30m", "1h", "12h", "1d", "7d"), args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> input, String token) {
        List<String> out = new ArrayList<String>();
        String lowered = token.toLowerCase(Locale.ROOT);
        for (String value : input) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lowered)) {
                out.add(value);
            }
        }
        return out;
    }
}
