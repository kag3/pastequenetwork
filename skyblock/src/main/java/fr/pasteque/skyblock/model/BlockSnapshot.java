package fr.pasteque.skyblock.model;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;

public class BlockSnapshot {
    private final int x;
    private final int y;
    private final int z;
    private final Material type;
    private final byte data;
    private final ItemStack[] inventory;

    public BlockSnapshot(int x, int y, int z, Material type, byte data, ItemStack[] inventory) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
        this.data = data;
        this.inventory = inventory;
    }

    public static BlockSnapshot capture(Block block) {
        ItemStack[] contents = null;
        BlockState state = block.getState();
        if (state instanceof Chest) {
            ItemStack[] source = ((Chest) state).getBlockInventory().getContents();
            contents = new ItemStack[source.length];
            for (int i = 0; i < source.length; i++) {
                contents[i] = source[i] == null ? null : source[i].clone();
            }
        }
        return new BlockSnapshot(block.getX(), block.getY(), block.getZ(), block.getType(), block.getData(), contents);
    }

    @SuppressWarnings("deprecation")
    public void restore(org.bukkit.World world) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(type);
        block.setData(data);
        BlockState state = block.getState();
        if (state instanceof Chest && inventory != null) {
            Chest chest = (Chest) state;
            chest.getBlockInventory().clear();
            for (int i = 0; i < inventory.length; i++) {
                chest.getBlockInventory().setItem(i, inventory[i] == null ? null : inventory[i].clone());
            }
            chest.update(true, false);
        }
    }
}
