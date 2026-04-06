package fr.pasteque.skyblock.guild;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.gui.GuiHelper;
import fr.pasteque.skyblock.manager.DataFile;
import fr.pasteque.skyblock.model.Island;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.*;

public class GuildManager {

    private static final String GUI_MAIN = "&2&lPasteque &5&lGuilde";
    private static final String GUI_ISLANDS = "&2&lPasteque &5&lIles de Guilde";
    private static final String GUI_MEMBERS = "&2&lPasteque &5&lMembres de Guilde";
    private static final double GUILD_COST = 10000.0D;
    private static final int INVITE_EXPIRE_MS = 60000;

    private final PastequeSkyblockPlugin plugin;
    private final DataFile dataFile;

    private final Map<String, Guild> guilds = new HashMap<String, Guild>();
    private final Map<UUID, String> playerGuild = new HashMap<UUID, String>();
    private final Map<UUID, GuildInvite> pendingInvites = new HashMap<UUID, GuildInvite>();

    /* Chat input capture for guild creation */
    private final Set<UUID> awaitingGuildName = new HashSet<UUID>();

    /* Disband confirmation */
    private final Map<UUID, Long> disbandConfirm = new HashMap<UUID, Long>();

    /* Island list pagination */
    private final Map<UUID, Integer> islandPages = new HashMap<UUID, Integer>();

    public GuildManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new DataFile(plugin, "guilds.yml");
        load();
    }

    // =========================================================================
    //  Persistence
    // =========================================================================

    public void load() {
        guilds.clear();
        playerGuild.clear();
        ConfigurationSection section = dataFile.getConfig().getConfigurationSection("guilds");
        if (section == null) return;
        for (String guildId : section.getKeys(false)) {
            ConfigurationSection gs = section.getConfigurationSection(guildId);
            if (gs == null) continue;
            UUID leader = UUID.fromString(gs.getString("leader"));
            Guild guild = new Guild(guildId, gs.getString("displayName", guildId), leader, gs.getLong("createdAt", System.currentTimeMillis()));
            guild.setMotd(gs.getString("motd", ""));
            guild.setLevel(gs.getInt("level", 1));
            guild.setXp(gs.getInt("xp", 0));
            for (String uuid : gs.getStringList("officers")) {
                try {
                    guild.getOfficers().add(UUID.fromString(uuid));
                } catch (IllegalArgumentException ignored) {
                }
            }
            for (String uuid : gs.getStringList("members")) {
                try {
                    UUID member = UUID.fromString(uuid);
                    guild.getMembers().add(member);
                    playerGuild.put(member, guildId);
                } catch (IllegalArgumentException ignored) {
                }
            }
            guilds.put(guildId, guild);
        }
    }

    public void save() {
        dataFile.getConfig().set("guilds", null);
        for (Guild guild : guilds.values()) {
            String path = "guilds." + guild.getId();
            dataFile.getConfig().set(path + ".displayName", guild.getDisplayName());
            dataFile.getConfig().set(path + ".leader", guild.getLeader().toString());
            dataFile.getConfig().set(path + ".motd", guild.getMotd());
            dataFile.getConfig().set(path + ".level", guild.getLevel());
            dataFile.getConfig().set(path + ".xp", guild.getXp());
            dataFile.getConfig().set(path + ".createdAt", guild.getCreatedAt());

            List<String> officerList = new ArrayList<String>();
            for (UUID uuid : guild.getOfficers()) {
                officerList.add(uuid.toString());
            }
            dataFile.getConfig().set(path + ".officers", officerList);

            List<String> memberList = new ArrayList<String>();
            for (UUID uuid : guild.getMembers()) {
                memberList.add(uuid.toString());
            }
            dataFile.getConfig().set(path + ".members", memberList);
        }
        dataFile.save();
    }

    // =========================================================================
    //  Core operations
    // =========================================================================

    public boolean createGuild(Player leader, String name) {
        if (playerGuild.containsKey(leader.getUniqueId())) {
            msg(leader, "&cTu es deja dans une guilde. Quitte-la d'abord.");
            return false;
        }
        String id = name.toLowerCase().replaceAll("[^a-z0-9_]", "");
        if (id.isEmpty() || id.length() > 16) {
            msg(leader, "&cNom invalide. 1-16 caracteres alphanumeriques uniquement.");
            return false;
        }
        if (guilds.containsKey(id)) {
            msg(leader, "&cUne guilde avec ce nom existe deja.");
            return false;
        }
        if (!plugin.getEconomyManager().take(leader.getUniqueId(), GUILD_COST)) {
            msg(leader, "&cTu n'as pas assez de " + plugin.getEconomyManager().getCurrencyName()
                    + ". Il faut &e" + plugin.getEconomyManager().format(GUILD_COST) + "&c.");
            return false;
        }
        Guild guild = new Guild(id, name, leader.getUniqueId(), System.currentTimeMillis());
        guilds.put(id, guild);
        playerGuild.put(leader.getUniqueId(), id);
        save();
        msg(leader, "&aGuilde &d" + name + " &acreee avec succes !");
        return true;
    }

    public boolean disbandGuild(Player player) {
        Guild guild = getGuild(player.getUniqueId());
        if (guild == null) {
            msg(player, "&cTu n'es dans aucune guilde.");
            return false;
        }
        if (!guild.isLeader(player.getUniqueId())) {
            msg(player, "&cSeul le chef peut dissoudre la guilde.");
            return false;
        }
        Long confirmTime = disbandConfirm.get(player.getUniqueId());
        if (confirmTime == null || System.currentTimeMillis() - confirmTime > 15000L) {
            disbandConfirm.put(player.getUniqueId(), System.currentTimeMillis());
            msg(player, "&eTape &c/guild disband &eune nouvelle fois dans 15s pour confirmer.");
            return false;
        }
        disbandConfirm.remove(player.getUniqueId());
        for (UUID member : new HashSet<UUID>(guild.getMembers())) {
            playerGuild.remove(member);
            Player online = Bukkit.getPlayer(member);
            if (online != null) {
                msg(online, "&cLa guilde &d" + guild.getDisplayName() + " &ca ete dissoute.");
            }
        }
        guilds.remove(guild.getId());
        save();
        return true;
    }

    public boolean invitePlayer(UUID inviter, UUID target, String guildId) {
        Guild guild = guilds.get(guildId);
        if (guild == null) return false;
        if (!guild.isLeader(inviter) && !guild.isOfficer(inviter)) return false;
        if (guild.isMember(target)) return false;
        if (playerGuild.containsKey(target)) return false;

        purgeExpiredInvites();
        pendingInvites.put(target, new GuildInvite(inviter, target, guildId, System.currentTimeMillis()));

        Player targetPlayer = Bukkit.getPlayer(target);
        if (targetPlayer != null) {
            OfflinePlayer inviterPlayer = Bukkit.getOfflinePlayer(inviter);
            msg(targetPlayer, "&d" + inviterPlayer.getName() + " &ft'invite a rejoindre la guilde &d"
                    + guild.getDisplayName() + "&f.");
            msg(targetPlayer, "&aTape &e/guild accept &apour accepter ou &c/guild deny &apour refuser. (60s)");
        }
        return true;
    }

    public boolean acceptInvite(UUID player) {
        purgeExpiredInvites();
        GuildInvite invite = pendingInvites.remove(player);
        if (invite == null || invite.isExpired()) return false;

        Guild guild = guilds.get(invite.getGuildId());
        if (guild == null) return false;

        if (playerGuild.containsKey(player)) return false;

        if (!guild.addMember(player)) {
            Player p = Bukkit.getPlayer(player);
            if (p != null) msg(p, "&cLa guilde est pleine.");
            return false;
        }
        playerGuild.put(player, guild.getId());
        save();

        // Notify guild
        Player p = Bukkit.getPlayer(player);
        String name = p != null ? p.getName() : Bukkit.getOfflinePlayer(player).getName();
        broadcastGuild(guild, "&d" + name + " &aa rejoint la guilde !");
        return true;
    }

    public boolean denyInvite(UUID player) {
        GuildInvite invite = pendingInvites.remove(player);
        if (invite == null) return false;
        Player sender = Bukkit.getPlayer(invite.getSender());
        if (sender != null) {
            OfflinePlayer targetOp = Bukkit.getOfflinePlayer(player);
            msg(sender, "&c" + targetOp.getName() + " a refuse l'invitation.");
        }
        return true;
    }

    public boolean leaveGuild(UUID player) {
        Guild guild = getGuild(player);
        if (guild == null) return false;
        if (guild.isLeader(player)) return false;

        guild.removeMember(player);
        playerGuild.remove(player);
        save();

        String name = Bukkit.getOfflinePlayer(player).getName();
        broadcastGuild(guild, "&c" + name + " a quitte la guilde.");
        return true;
    }

    public boolean kickMember(UUID kicker, UUID target) {
        Guild guild = getGuild(kicker);
        if (guild == null) return false;
        if (!guild.isLeader(kicker) && !guild.isOfficer(kicker)) return false;
        if (!guild.isMember(target)) return false;
        if (guild.isLeader(target)) return false;
        if (guild.isOfficer(target) && !guild.isLeader(kicker)) return false;

        guild.removeMember(target);
        playerGuild.remove(target);
        save();

        Player targetPlayer = Bukkit.getPlayer(target);
        if (targetPlayer != null) {
            msg(targetPlayer, "&cTu as ete expulse de la guilde &d" + guild.getDisplayName() + "&c.");
        }
        String kickerName = Bukkit.getOfflinePlayer(kicker).getName();
        String targetName = Bukkit.getOfflinePlayer(target).getName();
        broadcastGuild(guild, "&c" + targetName + " a ete expulse par " + kickerName + ".");
        return true;
    }

    public boolean transferLeadership(UUID leader, UUID newLeader) {
        Guild guild = getGuild(leader);
        if (guild == null) return false;
        if (!guild.isLeader(leader)) return false;
        if (!guild.isMember(newLeader)) return false;

        guild.getOfficers().remove(newLeader);
        guild.setLeader(newLeader);
        save();

        broadcastGuild(guild, "&d" + Bukkit.getOfflinePlayer(newLeader).getName()
                + " &fest maintenant le chef de la guilde !");
        return true;
    }

    public boolean promotePlayer(UUID leader, UUID target) {
        Guild guild = getGuild(leader);
        if (guild == null) return false;
        if (!guild.isLeader(leader)) return false;
        if (!guild.isMember(target) || guild.isLeader(target) || guild.isOfficer(target)) return false;

        guild.promote(target);
        save();

        broadcastGuild(guild, "&d" + Bukkit.getOfflinePlayer(target).getName() + " &fa ete promu officier !");
        return true;
    }

    public boolean demotePlayer(UUID leader, UUID target) {
        Guild guild = getGuild(leader);
        if (guild == null) return false;
        if (!guild.isLeader(leader)) return false;
        if (!guild.isOfficer(target)) return false;

        guild.demote(target);
        save();

        broadcastGuild(guild, "&c" + Bukkit.getOfflinePlayer(target).getName() + " &fa ete retrograde membre.");
        return true;
    }

    public Guild getGuild(UUID player) {
        String id = playerGuild.get(player);
        if (id == null) return null;
        return guilds.get(id);
    }

    public Guild getGuildById(String id) {
        return guilds.get(id);
    }

    public Collection<Guild> getAllGuilds() {
        return guilds.values();
    }

    public boolean isAwaitingGuildName(UUID uuid) {
        return awaitingGuildName.contains(uuid);
    }

    public void setAwaitingGuildName(UUID uuid, boolean awaiting) {
        if (awaiting) {
            awaitingGuildName.add(uuid);
        } else {
            awaitingGuildName.remove(uuid);
        }
    }

    public GuildInvite getPendingInvite(UUID uuid) {
        purgeExpiredInvites();
        return pendingInvites.get(uuid);
    }

    public void addGuildXp(UUID player, int amount) {
        Guild guild = getGuild(player);
        if (guild == null) return;
        guild.addXp(amount);
    }

    // =========================================================================
    //  GUI: Main Menu
    // =========================================================================

    public void openGuildMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, PastequeSkyblockPlugin.color(GUI_MAIN));
        GuiHelper.decorate(inv, GuiHelper.Theme.SOCIAL);

        Guild guild = getGuild(player.getUniqueId());
        if (guild == null) {
            // No guild
            inv.setItem(20, GuiHelper.fluidItem(Material.NETHER_STAR, "&a&lCreer une Guilde",
                    "Fonde ta propre guilde !",
                    new String[]{
                            "Cout: &e" + plugin.getEconomyManager().format(GUILD_COST),
                            "Nom: 1-16 caracteres"
                    },
                    "Clic pour creer"));

            GuildInvite invite = getPendingInvite(player.getUniqueId());
            if (invite != null) {
                Guild invGuild = guilds.get(invite.getGuildId());
                String guildName = invGuild != null ? invGuild.getDisplayName() : invite.getGuildId();
                inv.setItem(24, GuiHelper.fluidItem(Material.BOOK, "&d&lInvitation en attente",
                        "Invitation de " + Bukkit.getOfflinePlayer(invite.getSender()).getName(),
                        new String[]{
                                "Guilde: &d" + guildName,
                                "Expire dans " + Math.max(0, (INVITE_EXPIRE_MS - (System.currentTimeMillis() - invite.getSentAt())) / 1000) + "s"
                        },
                        "Clic gauche = accepter | Clic droit = refuser"));
            } else {
                inv.setItem(24, GuiHelper.fluidItem(Material.PAPER, "&7Invitations",
                        "Aucune invitation en attente",
                        new String[]{},
                        null));
            }
        } else {
            // In a guild - info
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            String date = sdf.format(new Date(guild.getCreatedAt()));
            String xpBar = buildXpBar(guild);

            inv.setItem(13, GuiHelper.fluidItem(Material.BEACON, "&d&l" + guild.getDisplayName(),
                    "Niveau " + guild.getLevel() + " | " + guild.getMembers().size() + "/" + guild.getMaxMembers() + " membres",
                    new String[]{
                            "Chef: &d" + Bukkit.getOfflinePlayer(guild.getLeader()).getName(),
                            "XP: &b" + guild.getXp() + "&7/" + (guild.getXpForNextLevel() > 0 ? guild.getXpForNextLevel() : "MAX"),
                            xpBar,
                            "Cree le: &7" + date,
                            guild.getMotd().isEmpty() ? "" : "MOTD: &f" + guild.getMotd()
                    },
                    null));

            // Member list button
            inv.setItem(29, GuiHelper.fluidItem(Material.SKULL_ITEM, "&b&lMembres",
                    "Voir tous les membres de la guilde",
                    new String[]{"Membres: &b" + guild.getMembers().size() + "/" + guild.getMaxMembers()},
                    "Clic pour ouvrir"));

            // Island list button
            inv.setItem(31, GuiHelper.fluidItem(Material.GRASS, "&a&lIles de Guilde",
                    "Visite les iles des membres",
                    new String[]{"Teleporte-toi en tant que visiteur"},
                    "Clic pour ouvrir"));

            // Settings (leader only)
            if (guild.isLeader(player.getUniqueId())) {
                inv.setItem(33, GuiHelper.fluidItem(Material.REDSTONE_COMPARATOR, "&e&lParametres",
                        "Gere ta guilde",
                        new String[]{
                                "MOTD, invitations, dissolution..."
                        },
                        "Clic pour gerer"));
            }

            // Leave button
            inv.setItem(49, GuiHelper.createItem(Material.REDSTONE_BLOCK, "&c&lQuitter la Guilde",
                    "", "&7Cliquez pour quitter la guilde.",
                    guild.isLeader(player.getUniqueId()) ? "&c(Transfere le lead ou /guild disband)" : ""));
        }

        inv.setItem(45, GuiHelper.closeButton());

        player.openInventory(inv);
        GuiHelper.playOpen(player);
    }

    // =========================================================================
    //  GUI: Island List
    // =========================================================================

    public void openGuildIslands(Player player) {
        openGuildIslands(player, 0);
    }

    public void openGuildIslands(Player player, int page) {
        Guild guild = getGuild(player.getUniqueId());
        if (guild == null) {
            msg(player, "&cTu n'es dans aucune guilde.");
            return;
        }

        // Collect islands for all members, sorted by level desc
        List<IslandEntry> entries = new ArrayList<IslandEntry>();
        for (UUID memberId : guild.getMembers()) {
            Island island = plugin.getIslandManager().getOwnedIsland(memberId);
            if (island == null) continue;
            int level = plugin.getIslandManager().getLevel(island);
            OfflinePlayer op = Bukkit.getOfflinePlayer(memberId);
            String ownerName = op.getName() != null ? op.getName() : memberId.toString().substring(0, 8);
            entries.add(new IslandEntry(memberId, ownerName, island, level));
        }

        // Sort by level descending
        Collections.sort(entries, new Comparator<IslandEntry>() {
            @Override
            public int compare(IslandEntry a, IslandEntry b) {
                return Integer.compare(b.level, a.level);
            }
        });

        int perPage = 21;
        int totalPages = Math.max(1, (entries.size() + perPage - 1) / perPage);
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;
        islandPages.put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 54, PastequeSkyblockPlugin.color(GUI_ISLANDS));
        GuiHelper.decorate(inv, GuiHelper.Theme.SOCIAL);

        int start = page * perPage;
        int end = Math.min(start + perPage, entries.size());
        int slot = 10;
        for (int i = start; i < end; i++) {
            // Skip border slots
            while (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
            }
            if (slot >= 45) break;

            IslandEntry entry = entries.get(i);
            boolean online = Bukkit.getPlayer(entry.ownerId) != null;
            String status = online ? "&a\u25CF En ligne" : "&c\u25CB Hors ligne";

            inv.setItem(slot, GuiHelper.fluidItem(Material.GRASS, "&a&l" + entry.ownerName,
                    "Ile de " + entry.ownerName,
                    new String[]{
                            "Niveau: &e" + entry.level,
                            status
                    },
                    "Clic pour visiter"));
            slot++;
        }

        // Back button
        inv.setItem(45, GuiHelper.backButton());

        // Pagination
        if (page > 0) {
            inv.setItem(48, GuiHelper.createItem(Material.ARROW, "&e&lPage precedente",
                    "", "&7Page " + page + "/" + totalPages));
        }
        inv.setItem(49, GuiHelper.createItem(Material.PAPER, "&7Page " + (page + 1) + "/" + totalPages));
        if (page < totalPages - 1) {
            inv.setItem(50, GuiHelper.createItem(Material.ARROW, "&e&lPage suivante",
                    "", "&7Page " + (page + 2) + "/" + totalPages));
        }

        player.openInventory(inv);
        GuiHelper.playOpen(player);
    }

    // =========================================================================
    //  GUI: Member List
    // =========================================================================

    public void openMemberList(Player player) {
        Guild guild = getGuild(player.getUniqueId());
        if (guild == null) {
            msg(player, "&cTu n'es dans aucune guilde.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, PastequeSkyblockPlugin.color(GUI_MEMBERS));
        GuiHelper.decorate(inv, GuiHelper.Theme.SOCIAL);

        // Sort members: leader first, then officers, then regular members
        List<UUID> sorted = new ArrayList<UUID>(guild.getMembers());
        Collections.sort(sorted, new Comparator<UUID>() {
            @Override
            public int compare(UUID a, UUID b) {
                int ra = getRoleOrder(guild, a);
                int rb = getRoleOrder(guild, b);
                return Integer.compare(ra, rb);
            }
        });

        int slot = 10;
        for (UUID memberId : sorted) {
            while (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
            }
            if (slot >= 45) break;

            OfflinePlayer op = Bukkit.getOfflinePlayer(memberId);
            String name = op.getName() != null ? op.getName() : memberId.toString().substring(0, 8);
            boolean online = op.isOnline();
            String role;
            Material mat;
            if (guild.isLeader(memberId)) {
                role = "&6\u2605 Chef";
                mat = Material.GOLD_INGOT;
            } else if (guild.isOfficer(memberId)) {
                role = "&b\u2606 Officier";
                mat = Material.IRON_INGOT;
            } else {
                role = "&7\u25CB Membre";
                mat = Material.COAL;
            }
            String status = online ? "&a\u25CF En ligne" : "&c\u25CB Hors ligne";

            List<String> details = new ArrayList<String>();
            details.add("Role: " + role);
            details.add(status);

            String cta = null;
            if (guild.isLeader(player.getUniqueId()) && !guild.isLeader(memberId)) {
                cta = "Clic gauche = promouvoir/retrograder | Clic droit = expulser";
            }

            inv.setItem(slot, GuiHelper.fluidItem(mat, "&f&l" + name,
                    null,
                    details.toArray(new String[0]),
                    cta));
            slot++;
        }

        inv.setItem(45, GuiHelper.backButton());

        player.openInventory(inv);
        GuiHelper.playOpen(player);
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    public int getIslandPage(UUID player) {
        Integer page = islandPages.get(player);
        return page != null ? page : 0;
    }

    private int getRoleOrder(Guild guild, UUID uuid) {
        if (guild.isLeader(uuid)) return 0;
        if (guild.isOfficer(uuid)) return 1;
        return 2;
    }

    private void purgeExpiredInvites() {
        Iterator<Map.Entry<UUID, GuildInvite>> it = pendingInvites.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, GuildInvite> entry = it.next();
            if (entry.getValue().isExpired()) {
                it.remove();
            }
        }
    }

    private void broadcastGuild(Guild guild, String message) {
        for (UUID member : guild.getMembers()) {
            Player p = Bukkit.getPlayer(member);
            if (p != null) {
                msg(p, message);
            }
        }
    }

    public void msg(Player player, String message) {
        player.sendMessage(PastequeSkyblockPlugin.color("&2&lPasteque &5&lGuilde &8\u00bb " + message));
    }

    private String buildXpBar(Guild guild) {
        if (guild.getLevel() >= 10) return "&a\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588 &7MAX";
        int next = guild.getXpForNextLevel();
        if (next <= 0) return "";
        int filled = (int) ((double) guild.getXp() / next * 10);
        StringBuilder sb = new StringBuilder("&a");
        for (int i = 0; i < 10; i++) {
            if (i == filled) sb.append("&7");
            sb.append("\u2588");
        }
        return sb.toString();
    }

    /** Simple data holder for island list sorting */
    private static class IslandEntry {
        final UUID ownerId;
        final String ownerName;
        final Island island;
        final int level;

        IslandEntry(UUID ownerId, String ownerName, Island island, int level) {
            this.ownerId = ownerId;
            this.ownerName = ownerName;
            this.island = island;
            this.level = level;
        }
    }

    /** GUI title check helpers */
    public static boolean isGuildMainGui(String title) {
        return PastequeSkyblockPlugin.color(GUI_MAIN).equals(title);
    }

    public static boolean isGuildIslandsGui(String title) {
        return PastequeSkyblockPlugin.color(GUI_ISLANDS).equals(title);
    }

    public static boolean isGuildMembersGui(String title) {
        return PastequeSkyblockPlugin.color(GUI_MEMBERS).equals(title);
    }

    public PastequeSkyblockPlugin getPlugin() {
        return plugin;
    }
}
