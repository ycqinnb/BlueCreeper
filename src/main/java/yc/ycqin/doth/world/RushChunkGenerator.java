package yc.ycqin.doth.world;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
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
 * 虫灵快跑维度：虚空区块生成器。
 * z >= 0 时生成所有活跃赛道的连续跑道（3 车道相连，中间不隔虚空），z < 0 为虚空。
 */
public class RushChunkGenerator implements IChunkGenerator {

    private final World world;

    public RushChunkGenerator(World world) {
        this.world = world;
    }

    @Override
    public Chunk generateChunk(int x, int z) {
        ChunkPrimer primer = new ChunkPrimer();
        if (z >= 0) {
            for (double[] track : RushManager.getActiveTrackCenters()) {
                double centerX = track[0];
                int cx1 = (int) Math.floor(centerX - RushManager.TRACK_HALF);
                int cx2 = (int) Math.floor(centerX + RushManager.TRACK_HALF);
                int startX = Math.max(cx1, x * 16);
                int endX = Math.min(cx2, x * 16 + 15);
                for (int wx = startX; wx <= endX; wx++) {
                    for (int wy = RushManager.TRACK_Y1; wy <= RushManager.TRACK_Y2; wy++) {
                        primer.setBlockState(wx & 15, wy, z & 15, Blocks.QUARTZ_BLOCK.getDefaultState());
                    }
                }
            }
        }
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
