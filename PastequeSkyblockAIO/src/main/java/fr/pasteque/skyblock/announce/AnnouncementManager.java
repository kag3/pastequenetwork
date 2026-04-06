package fr.pasteque.skyblock.announce;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class AnnouncementManager {

    private final PastequeSkyblockPlugin plugin;
    private final List<String> autoAnnouncements;
    private int currentIndex = 0;

    public AnnouncementManager(PastequeSkyblockPlugin plugin) {
        this.plugin = plugin;
        this.autoAnnouncements = new ArrayList<String>();
        autoAnnouncements.add("&6\u2726 &eNouveau: &fPasse de Combat Saison 1 &7- 30 paliers de recompenses ! &d/pass");
        autoAnnouncements.add("&6\u2726 &eNouveau: &fArbre de Competences &7- 5 skills a maitriser ! &d/skills");
        autoAnnouncements.add("&6\u2726 &eNouveau: &fSysteme de Pets &7- 8 compagnons uniques ! &d/pet");
        autoAnnouncements.add("&6\u2726 &eNouveau: &fMinions &7- Tes ouvriers automatiques ! &d/menu");
        autoAnnouncements.add("&6\u2726 &eNouveau: &fSlayers &7- Invoque et terrasse des boss ! &d/slayer");
        autoAnnouncements.add("&6\u2726 &eNouveau: &fVente Sombre &7- Items legendaires aux encheres ! &d/da");
        autoAnnouncements.add("&6\u2726 &eNouveau: &fDuels 1v1 &7- Defie n'importe qui ! &d/duel");
        autoAnnouncements.add("&6\u2726 &eNouveau: &fCollections &7- Traque tes ressources ! &d/collection");
        autoAnnouncements.add("&6\u2726 &eNouveau: &fBounties &7- Mets des primes sur les joueurs ! &d/bounty");
        autoAnnouncements.add("&6\u2726 &eNouveau: &fClassement ELO &7- Grimpe les rangs ! &d/elo");
        autoAnnouncements.add("&d\u2726 &5Astuce: &7Utilise &f/menu &7pour acceder a toutes les features !");
        autoAnnouncements.add("&d\u2726 &5Astuce: &7Ameliore ton ile avec &f/isupgrade");
        autoAnnouncements.add("&d\u2726 &5Astuce: &7Cree des warps sur ton ile avec &f/iswarp set <nom>");

        // New feature announcements
        autoAnnouncements.add("&6\u2726 &eNouveau: &fDonjons instancies &7- 3 donjons, 4 vagues, boss epiques ! &d/dungeon");
        autoAnnouncements.add("&6\u2726 &eNouveau: &fFarming Custom &7- 6 cultures exclusives a cultiver ! &d/farming");
        autoAnnouncements.add("&6\u2726 &eNouveau: &fTrade Securise &7- Echange anti-arnaque avec compte a rebours ! &d/trade <joueur>");
        autoAnnouncements.add("&6\u2726 &eNouveau: &fGuildes &7- Cree ta guilde et visite les iles des autres ! &d/guild");
        autoAnnouncements.add("&6\u2726 &eNouveau: &fEnchantements Custom &7- 8 enchants uniques a debloquer ! &d/enchant");
        autoAnnouncements.add("&6\u2726 &eNouveau: &fMissions &7- Quetes en chaine avec recompenses ! &d/quest");
        autoAnnouncements.add("&6\u2726 &eNouveau: &fKOTH &7- Roi de la colline chaque Mercredi et Samedi a 16h !");
        autoAnnouncements.add("&d\u2726 &5Astuce: &7Enchante tes outils avec &f/enchant &7pour des effets uniques !");
        autoAnnouncements.add("&d\u2726 &5Astuce: &7Rejoins une guilde avec &f/guild join &7ou cree la tienne !");
        autoAnnouncements.add("&d\u2726 &5Astuce: &7Lance un donjon avec &f/dungeon start <type> &7en groupe !");
    }

    public void startAutoAnnouncements() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getOnlinePlayers().isEmpty()) {
                    return;
                }
                if (currentIndex >= autoAnnouncements.size()) {
                    currentIndex = 0;
                }
                String message = PastequeSkyblockPlugin.color(autoAnnouncements.get(currentIndex));
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(message);
                }
                currentIndex++;
            }
        }.runTaskTimer(plugin, 20L * 60L * 5L, 20L * 60L * 5L);
    }

    public void sendJoinMessages(final Player player, final boolean firstJoin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    return;
                }
                player.sendMessage("");
                player.sendMessage(PastequeSkyblockPlugin.color("&8&m                                                    "));
                player.sendMessage(PastequeSkyblockPlugin.color("&2&lPASTEQUE &5&lSKYBLOCK"));
                player.sendMessage("");
                player.sendMessage(PastequeSkyblockPlugin.color("&7Bienvenue &f" + player.getName() + " &7!"));
                player.sendMessage("");
                player.sendMessage(PastequeSkyblockPlugin.color("&8\u25b8 &7Tape &e/menu &7pour commencer"));
                player.sendMessage(PastequeSkyblockPlugin.color("&8\u25b8 &7Tape &e/is create &7pour creer ton ile"));
                player.sendMessage(PastequeSkyblockPlugin.color("&8\u25b8 &7Rejoins l'arene avec &e/arena"));
                player.sendMessage("");
                player.sendMessage(PastequeSkyblockPlugin.color("&8&m                                                    "));
                player.sendMessage("");
            }
        }.runTaskLater(plugin, 20L * 3L);

        if (firstJoin) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        return;
                    }
                    sendTitle(player,
                            PastequeSkyblockPlugin.color("&2&lBienvenue"),
                            PastequeSkyblockPlugin.color("&7sur &2Pasteque &5Skyblock"));
                }
            }.runTaskLater(plugin, 20L * 2L);

            // Beautiful motivational welcome message for new players
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        return;
                    }
                    player.sendMessage("");
                    player.sendMessage(PastequeSkyblockPlugin.color("&8&m                                                    "));
                    player.sendMessage(PastequeSkyblockPlugin.color("  &6\u2B50 &e&lTES PREMIERES MISSIONS T'ATTENDENT !"));
                    player.sendMessage("");
                    player.sendMessage(PastequeSkyblockPlugin.color("  &7Des &aquetes guidees &7sont disponibles pour bien"));
                    player.sendMessage(PastequeSkyblockPlugin.color("  &7demarrer ton aventure et gagner des &arecompenses&7 !"));
                    player.sendMessage("");
                    player.sendMessage(PastequeSkyblockPlugin.color("  &8\u25B8 &eTape &a/quest &epour voir tes missions"));
                    player.sendMessage(PastequeSkyblockPlugin.color("  &8\u25B8 &7Chaque quete terminee en debloque une nouvelle !"));
                    player.sendMessage("");
                    player.sendMessage(PastequeSkyblockPlugin.color("  &8\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"));
                    player.sendMessage("");
                    player.sendMessage(PastequeSkyblockPlugin.color("  &d\u2726 &5&lDECOUVRE NOS FEATURES EXCLUSIVES :"));
                    player.sendMessage(PastequeSkyblockPlugin.color("  &8\u25B8 &bDonjons &7en equipe avec boss epiques &8- &d/dungeon"));
                    player.sendMessage(PastequeSkyblockPlugin.color("  &8\u25B8 &bFarming Custom &7avec 6 cultures uniques &8- &d/farming"));
                    player.sendMessage(PastequeSkyblockPlugin.color("  &8\u25B8 &b8 Enchantements &7exclusifs a debloquer &8- &d/enchant"));
                    player.sendMessage(PastequeSkyblockPlugin.color("  &8\u25B8 &bTrade Securise &7anti-arnaque &8- &d/trade <joueur>"));
                    player.sendMessage(PastequeSkyblockPlugin.color("  &8\u25B8 &bGuildes &7et visite d'iles &8- &d/guild"));
                    player.sendMessage(PastequeSkyblockPlugin.color("  &8\u25B8 &bKOTH &7chaque Mercredi et Samedi !"));
                    player.sendMessage("");
                    player.sendMessage(PastequeSkyblockPlugin.color("  &a&lBonne aventure sur Pasteque Skyblock !"));
                    player.sendMessage(PastequeSkyblockPlugin.color("&8&m                                                    "));
                    player.sendMessage("");
                }
            }.runTaskLater(plugin, 20L * 7L);
        }
    }

    private static void sendTitle(Player player, String title, String subtitle) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "title " + player.getName() + " times 10 70 20");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "title " + player.getName() + " title {\"text\":\"" + escapeJson(title) + "\"}");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "title " + player.getName() + " subtitle {\"text\":\"" + escapeJson(subtitle) + "\"}");
    }

    private static String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
