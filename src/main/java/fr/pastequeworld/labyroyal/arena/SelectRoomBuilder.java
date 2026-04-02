package fr.pastequeworld.labyroyal.arena;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

/**
 * Construit une petite cabane cosy pour l'ecran de selection Solo/Duo.
 * 7x7 exterieur, 5x5 interieur, ambiance chaleureuse.
 * Position: (0, 150, 0) dans le monde par defaut.
 */
public class SelectRoomBuilder {

    private static final int CX = 0;
    private static final int CZ = 0;
    private static final int BASE_Y = 150;

    /**
     * Construit la cabane si elle n'existe pas deja.
     */
    @SuppressWarnings("deprecation")
    public static void buildIfNeeded(World world) {
        // Verifier si deja construite
        if (world.getBlockAt(CX, BASE_Y, CZ).getType() == Material.WOOD) return;

        int y = BASE_Y;

        // ======== NETTOYAGE : air dans toute la zone ========
        for (int x = CX - 5; x <= CX + 5; x++) {
            for (int z = CZ - 5; z <= CZ + 5; z++) {
                for (int dy = 0; dy <= 10; dy++) {
                    world.getBlockAt(x, y + dy, z).setType(Material.AIR);
                }
            }
        }

        // ======== SOL (y=150) : planches de chene ========
        for (int x = CX - 3; x <= CX + 3; x++) {
            for (int z = CZ - 3; z <= CZ + 3; z++) {
                world.getBlockAt(x, y, z).setType(Material.WOOD); // oak planks
            }
        }

        // Sous le sol : bedrock pour pas tomber
        for (int x = CX - 3; x <= CX + 3; x++) {
            for (int z = CZ - 3; z <= CZ + 3; z++) {
                world.getBlockAt(x, y - 1, z).setType(Material.BEDROCK);
            }
        }

        // ======== TAPIS AU SOL (y=151) : rouge au centre ========
        // Tapis rouge (data 14) au centre 3x3
        for (int x = CX - 1; x <= CX + 1; x++) {
            for (int z = CZ - 1; z <= CZ + 1; z++) {
                world.getBlockAt(x, y + 1, z).setType(Material.CARPET);
                world.getBlockAt(x, y + 1, z).setData((byte) 14); // red
            }
        }
        // Tapis orange (data 1) en bordure
        for (int x = CX - 2; x <= CX + 2; x++) {
            for (int z = CZ - 2; z <= CZ + 2; z++) {
                if (Math.abs(x - CX) > 1 || Math.abs(z - CZ) > 1) {
                    if (Math.abs(x - CX) <= 2 && Math.abs(z - CZ) <= 2) {
                        if (world.getBlockAt(x, y + 1, z).getType() != Material.CARPET) {
                            world.getBlockAt(x, y + 1, z).setType(Material.CARPET);
                            world.getBlockAt(x, y + 1, z).setData((byte) 1); // orange
                        }
                    }
                }
            }
        }

        // ======== MURS (y+1 a y+4) ========
        for (int dy = 1; dy <= 4; dy++) {
            int wy = y + dy;
            for (int x = CX - 3; x <= CX + 3; x++) {
                // Mur nord (z=-3) et sud (z=+3)
                setWall(world, x, wy, CZ - 3, dy);
                setWall(world, x, wy, CZ + 3, dy);
            }
            for (int z = CZ - 2; z <= CZ + 2; z++) {
                // Mur ouest (x=-3) et est (x=+3)
                setWall(world, CX - 3, wy, z, dy);
                setWall(world, CX + 3, wy, z, dy);
            }
        }

        // ======== PILIERS EN BOIS AUX 4 COINS (buches de chene) ========
        for (int dy = 1; dy <= 4; dy++) {
            world.getBlockAt(CX - 3, y + dy, CZ - 3).setType(Material.LOG);
            world.getBlockAt(CX + 3, y + dy, CZ - 3).setType(Material.LOG);
            world.getBlockAt(CX - 3, y + dy, CZ + 3).setType(Material.LOG);
            world.getBlockAt(CX + 3, y + dy, CZ + 3).setType(Material.LOG);
        }

        // ======== FENETRES (vitres) ========
        // Fenetres est (x=+3) : 2 vitres a z=-1 et z=+1, hauteur y+2 et y+3
        for (int dz = -1; dz <= 1; dz += 2) {
            world.getBlockAt(CX + 3, y + 2, CZ + dz).setType(Material.THIN_GLASS);
            world.getBlockAt(CX + 3, y + 3, CZ + dz).setType(Material.THIN_GLASS);
        }
        // Fenetres ouest (x=-3) : pareil
        for (int dz = -1; dz <= 1; dz += 2) {
            world.getBlockAt(CX - 3, y + 2, CZ + dz).setType(Material.THIN_GLASS);
            world.getBlockAt(CX - 3, y + 3, CZ + dz).setType(Material.THIN_GLASS);
        }
        // Fenetre sud (z=+3) : au centre
        world.getBlockAt(CX, y + 2, CZ + 3).setType(Material.THIN_GLASS);
        world.getBlockAt(CX, y + 3, CZ + 3).setType(Material.THIN_GLASS);

        // ======== CHEMINEE (mur nord z=-3, centre x=0) ========
        // Fond cheminee en stone brick
        world.getBlockAt(CX, y + 1, CZ - 3).setType(Material.SMOOTH_BRICK);
        world.getBlockAt(CX, y + 2, CZ - 3).setType(Material.SMOOTH_BRICK);
        world.getBlockAt(CX, y + 3, CZ - 3).setType(Material.SMOOTH_BRICK);
        world.getBlockAt(CX, y + 4, CZ - 3).setType(Material.SMOOTH_BRICK);
        // Netherrack + feu (juste devant le mur, inside)
        world.getBlockAt(CX, y, CZ - 2).setType(Material.NETHERRACK);
        world.getBlockAt(CX, y + 1, CZ - 2).setType(Material.FIRE);
        // Briques autour du foyer
        world.getBlockAt(CX - 1, y + 1, CZ - 2).setType(Material.SMOOTH_BRICK);
        world.getBlockAt(CX + 1, y + 1, CZ - 2).setType(Material.SMOOTH_BRICK);
        world.getBlockAt(CX - 1, y + 2, CZ - 2).setType(Material.SMOOTH_BRICK);
        world.getBlockAt(CX + 1, y + 2, CZ - 2).setType(Material.SMOOTH_BRICK);
        world.getBlockAt(CX, y + 2, CZ - 2).setType(Material.SMOOTH_BRICK); // haut du foyer
        // Cheminee qui monte au-dessus du toit
        world.getBlockAt(CX, y + 5, CZ - 3).setType(Material.SMOOTH_BRICK);
        world.getBlockAt(CX, y + 6, CZ - 3).setType(Material.SMOOTH_BRICK);
        world.getBlockAt(CX, y + 7, CZ - 3).setType(Material.SMOOTH_BRICK);

        // ======== BIBLIOTHEQUES flanquant la cheminee ========
        // Cote gauche cheminee
        world.getBlockAt(CX - 2, y + 1, CZ - 2).setType(Material.BOOKSHELF);
        world.getBlockAt(CX - 2, y + 2, CZ - 2).setType(Material.BOOKSHELF);
        // Cote droit cheminee
        world.getBlockAt(CX + 2, y + 1, CZ - 2).setType(Material.BOOKSHELF);
        world.getBlockAt(CX + 2, y + 2, CZ - 2).setType(Material.BOOKSHELF);

        // ======== PLAFOND (y+5) : planches de chene ========
        for (int x = CX - 3; x <= CX + 3; x++) {
            for (int z = CZ - 3; z <= CZ + 3; z++) {
                if (world.getBlockAt(x, y + 5, z).getType() == Material.AIR) {
                    world.getBlockAt(x, y + 5, z).setType(Material.WOOD);
                }
            }
        }

        // ======== ECLAIRAGE ========
        // Glowstone cache dans le plafond (remplace 2 planches)
        world.getBlockAt(CX - 1, y + 5, CZ).setType(Material.GLOWSTONE);
        world.getBlockAt(CX + 1, y + 5, CZ).setType(Material.GLOWSTONE);
        // Torches aux murs
        world.getBlockAt(CX - 2, y + 3, CZ + 2).setType(Material.TORCH);
        world.getBlockAt(CX + 2, y + 3, CZ + 2).setType(Material.TORCH);

        // ======== TOIT EN ESCALIERS (par-dessus le plafond) ========
        // Pente est-ouest avec escaliers en chene
        for (int z = CZ - 3; z <= CZ + 3; z++) {
            // Cote ouest (data 0 = ascending east)
            world.getBlockAt(CX - 4, y + 5, z).setType(Material.WOOD_STAIRS);
            world.getBlockAt(CX - 4, y + 5, z).setData((byte) 0);
            // Cote est (data 1 = ascending west)
            world.getBlockAt(CX + 4, y + 5, z).setType(Material.WOOD_STAIRS);
            world.getBlockAt(CX + 4, y + 5, z).setData((byte) 1);

            // 2eme etage
            world.getBlockAt(CX - 3, y + 6, z).setType(Material.WOOD_STAIRS);
            world.getBlockAt(CX - 3, y + 6, z).setData((byte) 0);
            world.getBlockAt(CX + 3, y + 6, z).setType(Material.WOOD_STAIRS);
            world.getBlockAt(CX + 3, y + 6, z).setData((byte) 1);

            // Faitage (crete du toit)
            for (int x = CX - 2; x <= CX + 2; x++) {
                world.getBlockAt(x, y + 6, z).setType(Material.WOOD);
            }
        }

        // ======== MEUBLES ========
        // Etabli dans le coin sud-ouest
        world.getBlockAt(CX - 2, y + 1, CZ + 2).setType(Material.WORKBENCH);
        // Fournaise dans le coin sud-est
        world.getBlockAt(CX + 2, y + 1, CZ + 2).setType(Material.FURNACE);
        // Pots de fleurs dans les coins interieurs
        world.getBlockAt(CX - 2, y + 3, CZ - 2).setType(Material.SEA_LANTERN);
        world.getBlockAt(CX + 2, y + 3, CZ - 2).setType(Material.SEA_LANTERN);

        // ======== BARRIER au-dessus pour bloquer ========
        for (int x = CX - 4; x <= CX + 4; x++) {
            for (int z = CZ - 4; z <= CZ + 4; z++) {
                world.getBlockAt(x, y + 7, z).setType(Material.BARRIER);
            }
        }
    }

    private static void setWall(World world, int x, int wy, int z, int dy) {
        // Ne pas ecraser les coins (logs) ni les fenetres/cheminee
        if (world.getBlockAt(x, wy, z).getType() != Material.AIR) return;
        if (dy <= 2) {
            world.getBlockAt(x, wy, z).setType(Material.WOOD); // planches en bas
        } else {
            world.getBlockAt(x, wy, z).setType(Material.WOOD); // planches en haut aussi
        }
    }

    /**
     * Position du joueur dans la cabane : centre, regardant vers la cheminee (nord).
     */
    public static Location getSpawnLocation(World world) {
        Location loc = new Location(world, CX + 0.5, BASE_Y + 1, CZ + 0.5);
        loc.setYaw(180f);   // Regarde vers le nord (vers la cheminee)
        loc.setPitch(10f);  // Leger regard vers le bas (voit le feu)
        return loc;
    }
}
