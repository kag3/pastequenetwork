package fr.pasteque.skyblock.command;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.pet.PetManager;
import fr.pasteque.skyblock.pet.model.PetType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PetCommand implements CommandExecutor {

    private final PetManager petManager;

    public PetCommand(PetManager petManager) {
        this.petManager = petManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PastequeSkyblockPlugin.color("&cCommande joueur uniquement."));
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            petManager.openPetGui(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("equip") && args.length >= 2) {
            String petName = args[1].toUpperCase();
            PetType type = null;
            for (PetType pt : PetType.values()) {
                if (pt.name().equalsIgnoreCase(petName)
                        || pt.getDisplayName().equalsIgnoreCase(args[1])) {
                    type = pt;
                    break;
                }
            }
            if (type == null) {
                player.sendMessage(PastequeSkyblockPlugin.color("&cAnimal inconnu."));
                return true;
            }
            if (!petManager.hasPet(player.getUniqueId(), type)) {
                player.sendMessage(PastequeSkyblockPlugin.color("&cVous ne possedez pas cet animal !"));
                return true;
            }
            petManager.setActivePet(player.getUniqueId(), type);
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "&6&l>> " + type.getColor() + type.getDisplayName() + " &aequipe !"
            ));
            return true;
        }

        if (sub.equals("unequip") || sub.equals("desequiper")) {
            petManager.unequipPet(player.getUniqueId());
            player.sendMessage(PastequeSkyblockPlugin.color("&6&l>> &7Animal desequipe."));
            return true;
        }

        player.sendMessage(PastequeSkyblockPlugin.color("&eUtilisation: /pet, /pet equip <nom>, /pet unequip"));
        return true;
    }
}
