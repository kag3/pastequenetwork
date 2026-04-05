package fr.pasteque.skyblock.announce;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import org.bukkit.Bukkit;
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
                    player.sendTitle(
                            PastequeSkyblockPlugin.color("&2&lBienvenue"),
                            PastequeSkyblockPlugin.color("&7sur &2Pasteque &5Skyblock"),
                            10, 70, 20
                    );
                }
            }.runTaskLater(plugin, 20L * 2L);
        }
    }
}
