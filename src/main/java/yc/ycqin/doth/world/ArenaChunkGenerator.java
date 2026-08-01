package yc.ycqin.doth.world;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.IChunkGenerator;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * 斗蛐蛐维度：全虚空区块生成器，不生成任何结构/生物/矿物
 */
public class ArenaChunkGenerator implements IChunkGenerator {

    private final World world;

    public ArenaChunkGenerator(World world) {
        this.world = world;
    }

    @Override
    public Chunk generateChunk(int x, int z) {
        ChunkPrimer primer = new ChunkPrimer();
        Chunk chunk = new Chunk(this.world, primer, x, z);
        chunk.generateSkylightMap();
        return chunk;
    }

    @Override
    public void populate(int x, int z) {
        // 不生成任何东西
    }

    @Override
    public boolean generateStructures(Chunk chunkIn, int x, int z) {
        return false;
    }

    @Override
    public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public BlockPos getNearestStructurePos(World worldIn, String structureName, BlockPos position, boolean findUnexplored) {
        return null;
    }

    @Override
    public boolean isInsideStructure(World worldIn, String structureName, BlockPos pos) {
        return false;
    }

    @Override
    public void recreateStructures(Chunk chunkIn, int x, int z) {
    }
}
