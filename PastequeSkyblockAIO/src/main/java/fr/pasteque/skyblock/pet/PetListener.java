package fr.pasteque.skyblock.pet;

import fr.pasteque.skyblock.PastequeSkyblockPlugin;
import fr.pasteque.skyblock.pet.model.PetType;
import fr.pasteque.skyblock.pet.model.PlayerPet;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.UUID;

public class PetListener implements Listener {

    private final PetManager petManager;

    public PetListener(PetManager petManager) {
        this.petManager = petManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() == null || !(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (!PetManager.GUI_TITLE.equals(event.getInventory().getTitle())) {
            return;
        }
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();
        int slot = event.getRawSlot();

        // Map slots to pet types
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19};
        PetType[] types = PetType.values();
        PetType clickedType = null;
        for (int i = 0; i < slots.length && i < types.length; i++) {
            if (slot == slots[i]) {
                clickedType = types[i];
                break;
            }
        }

        if (clickedType == null) {
            return;
        }

        if (petManager.hasPet(uuid, clickedType)) {
            // Toggle equip/unequip
            PlayerPet pet = petManager.getPlayerPet(uuid, clickedType);
            if (pet != null && pet.isActive()) {
                petManager.unequipPet(uuid);
                player.sendMessage(PastequeSkyblockPlugin.color(
                        "&6&l>> &7" + clickedType.getDisplayName() + " &cdesequipe."
                ));
            } else {
                petManager.setActivePet(uuid, clickedType);
                player.sendMessage(PastequeSkyblockPlugin.color(
                        "&6&l>> " + clickedType.getColor() + clickedType.getDisplayName() + " &aequipe !"
                ));
            }
        } else {
            // Try to buy
            double price = petManager.getPetPrice(clickedType);
            if (!petManager.getPlugin().getEconomyManager().take(uuid, price)) {
                player.sendMessage(PastequeSkyblockPlugin.color(
                        "&cVous n'avez pas assez de "
                                + petManager.getPlugin().getEconomyManager().getCurrencyName() + " !"
                ));
                return;
            }
            petManager.addPet(uuid, clickedType);
            player.sendMessage(PastequeSkyblockPlugin.color(
                    "&6&l>> &aVous avez achete " + clickedType.getColor()
                            + clickedType.getDisplayName() + " &a!"
            ));
        }
        // Refresh GUI
        petManager.openPetGui(player);
    }
}
