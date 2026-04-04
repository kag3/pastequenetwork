package fr.pasteque.skyblock.generator;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

public class EmptyChunkGenerator extends ChunkGenerator {
    @Override
    public byte[][] generateBlockSections(World world, Random random, int chunkX, int chunkZ, BiomeGrid biome) {
        byte[][] result = new byte[world.getMaxHeight() / 16][];
        for (int i = 0; i < result.length; i++) {
            result[i] = null;
        }
        return result;
    }

    @Override
    public short[][] generateExtBlockSections(World world, Random random, int chunkX, int chunkZ, BiomeGrid biome) {
        return null;
    }

    @Override
    public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biome) {
        return createChunkData(world);
    }

    @Override
    public boolean canSpawn(World world, int x, int z) {
        return false;
    }
}
