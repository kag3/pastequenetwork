package fr.pasteque.skyblock.guild;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.gui.GuiHelper;
import fr.pasteque.skyblock.model.Island;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class GuildListener implements Listener {

    private final PastequeSkyblockPlugin plugin;
    private final GuildManager guildManager;

    public GuildListener(PastequeSkyblockPlugin plugin, GuildManager guildManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
    }

    // =========================================================================
    //  Inventory Click
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();
        if (inv == null) return;
        String title = inv.getTitle();
        if (title == null) return;

        if (GuildManager.isGuildMainGui(title)) {
            event.setCancelled(true);
            handleMainGui(player, event);
        } else if (GuildManager.isGuildIslandsGui(title)) {
            event.setCancelled(true);
            handleIslandsGui(player, event);
        } else if (GuildManager.isGuildMembersGui(title)) {
            event.setCancelled(true);
            handleMembersGui(player, event);
        }
    }

    private void handleMainGui(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        Guild guild = guildManager.getGuild(player.getUniqueId());

        // Close button
        if (slot == 45) {
            player.closeInventory();
            GuiHelper.playClick(player);
            return;
        }

        if (guild == null) {
            // No guild
            if (slot == 20) {
                // Create guild - enter chat input mode
                player.closeInventory();
                GuiHelper.playClick(player);
                guildManager.setAwaitingGuildName(player.getUniqueId(), true);
                guildManager.msg(player, "&eEcris le nom de ta guilde dans le chat. (1-16 caracteres)");
                guildManager.msg(player, "&7Tape &cannuler &7pour annuler.");
                return;
            }
            if (slot == 24) {
                // Pending invite
                GuildInvite invite = guildManager.getPendingInvite(player.getUniqueId());
                if (invite != null && !invite.isExpired()) {
                    if (event.getClick() == ClickType.RIGHT) {
                        guildManager.denyInvite(player.getUniqueId());
                        GuiHelper.playDeny(player);
                        guildManager.msg(player, "&cInvitation refusee.");
                        player.closeInventory();
                    } else {
                        if (guildManager.acceptInvite(player.getUniqueId())) {
                            GuiHelper.playSuccess(player);
                            player.closeInventory();
                        } else {
                            GuiHelper.playDeny(player);
                            guildManager.msg(player, "&cImpossible d'accepter l'invitation.");
                            player.closeInventory();
                        }
                    }
                }
                return;
            }
        } else {
            // In a guild
            if (slot == 29) {
                // Member list
                GuiHelper.playClick(player);
                guildManager.openMemberList(player);
                return;
            }
            if (slot == 31) {
                // Islands
                GuiHelper.playClick(player);
                guildManager.openGuildIslands(player);
                return;
            }
            if (slot == 33 && guild.isLeader(player.getUniqueId())) {
                // Settings (just close and tell them to use commands for now)
                GuiHelper.playClick(player);
                player.closeInventory();
                guildManager.msg(player, "&eParametres disponibles via commandes:");
                guildManager.msg(player, "&7/guild motd <message> &8- &fChanger le MOTD");
                guildManager.msg(player, "&7/guild invite <joueur> &8- &fInviter un joueur");
                guildManager.msg(player, "&7/guild promote <joueur> &8- &fPromouvoir en officier");
                guildManager.msg(player, "&7/guild demote <joueur> &8- &fRetrograder");
                guildManager.msg(player, "&7/guild transfer <joueur> &8- &fTransferer le lead");
                guildManager.msg(player, "&7/guild disband &8- &fDissoudre la guilde");
                return;
            }
            if (slot == 49) {
                // Leave
                if (guild.isLeader(player.getUniqueId())) {
                    GuiHelper.playDeny(player);
                    guildManager.msg(player, "&cLe chef ne peut pas quitter. Transfere le lead ou dissous la guilde.");
                } else {
                    if (guildManager.leaveGuild(player.getUniqueId())) {
                        GuiHelper.playSuccess(player);
                        player.closeInventory();
                    } else {
                        GuiHelper.playDeny(player);
                    }
                }
                return;
            }
        }
    }

    private void handleIslandsGui(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        // Back button
        if (slot == 45) {
            GuiHelper.playClick(player);
            guildManager.openGuildMenu(player);
            return;
        }

        // Previous page
        if (slot == 48) {
            int page = guildManager.getIslandPage(player.getUniqueId());
            if (page > 0) {
                GuiHelper.playClick(player);
                guildManager.openGuildIslands(player, page - 1);
            }
            return;
        }

        // Next page
        if (slot == 50) {
            int page = guildManager.getIslandPage(player.getUniqueId());
            GuiHelper.playClick(player);
            guildManager.openGuildIslands(player, page + 1);
            return;
        }

        // Island click -> teleport
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.GRASS) return;
        if (!clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;

        // Find which island this is by matching owner name from display name
        Guild guild = guildManager.getGuild(player.getUniqueId());
        if (guild == null) return;

        String displayName = clicked.getItemMeta().getDisplayName();
        // Display name is colored "&a&l<name>", extract after color codes
        String stripped = PastequeSkyblockPlugin.color("").isEmpty()
                ? displayName : org.bukkit.ChatColor.stripColor(displayName);

        for (UUID memberId : guild.getMembers()) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(memberId);
            String memberName = op.getName();
            if (memberName != null && memberName.equals(stripped)) {
                Island island = plugin.getIslandManager().getOwnedIsland(memberId);
                if (island != null) {
                    Location target = plugin.getIslandManager().getSafeTeleport(island);
                    player.closeInventory();
                    player.teleport(target);
                    GuiHelper.playSuccess(player);
                    guildManager.msg(player, "&aTeleporte sur l'ile de &d" + memberName + "&a !");
                }
                return;
            }
        }
    }

    private void handleMembersGui(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        // Back button
        if (slot == 45) {
            GuiHelper.playClick(player);
            guildManager.openGuildMenu(player);
            return;
        }

        Guild guild = guildManager.getGuild(player.getUniqueId());
        if (guild == null) return;
        if (!guild.isLeader(player.getUniqueId())) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.STAINED_GLASS_PANE) return;
        if (!clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;

        String stripped = org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        // Find member by name
        for (UUID memberId : guild.getMembers()) {
            if (guild.isLeader(memberId)) continue;
            OfflinePlayer op = Bukkit.getOfflinePlayer(memberId);
            String name = op.getName();
            if (name != null && name.equals(stripped)) {
                if (event.getClick() == ClickType.RIGHT) {
                    // Kick
                    if (guildManager.kickMember(player.getUniqueId(), memberId)) {
                        GuiHelper.playSuccess(player);
                        guildManager.openMemberList(player);
                    } else {
                        GuiHelper.playDeny(player);
                    }
                } else {
                    // Toggle promote/demote
                    if (guild.isOfficer(memberId)) {
                        if (guildManager.demotePlayer(player.getUniqueId(), memberId)) {
                            GuiHelper.playSuccess(player);
                        } else {
                            GuiHelper.playDeny(player);
                        }
                    } else {
                        if (guildManager.promotePlayer(player.getUniqueId(), memberId)) {
                            GuiHelper.playSuccess(player);
                        } else {
                            GuiHelper.playDeny(player);
                        }
                    }
                    guildManager.openMemberList(player);
                }
                return;
            }
        }
    }

    // =========================================================================
    //  Chat input for guild creation
    // =========================================================================

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        if (!guildManager.isAwaitingGuildName(player.getUniqueId())) return;

        event.setCancelled(true);
        guildManager.setAwaitingGuildName(player.getUniqueId(), false);

        final String message = event.getMessage().trim();
        if (message.equalsIgnoreCase("annuler")) {
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    guildManager.msg(player, "&cCreation de guilde annulee.");
                }
            });
            return;
        }

        // Must run on main thread for economy/save
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                guildManager.createGuild(player, message);
            }
        });
    }

    // =========================================================================
    //  Join: show MOTD
    // =========================================================================

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        Guild guild = guildManager.getGuild(player.getUniqueId());
        if (guild == null) return;
        if (guild.getMotd() != null && !guild.getMotd().isEmpty()) {
            // Slight delay so it appears after join messages
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    Guild g = guildManager.getGuild(player.getUniqueId());
                    if (g != null && g.getMotd() != null && !g.getMotd().isEmpty()) {
                        guildManager.msg(player, "&d&l[MOTD] &f" + g.getMotd());
                    }
                }
            }, 40L);
        }
    }

    // =========================================================================
    //  XP from activity
    // =========================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        guildManager.addGuildXp(event.getPlayer().getUniqueId(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            guildManager.addGuildXp(event.getEntity().getKiller().getUniqueId(), 5);
        }
    }
}
