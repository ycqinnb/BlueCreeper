package yc.ycqin.doth.world;

import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 斗蛐蛐维度 WorldProvider：虚空、终为白日、无天气、无自然生物
 */
public class ArenaWorldProvider extends WorldProvider {

    @Override
    public DimensionType getDimensionType() {
        return ArenaManager.DIMENSION_TYPE;
    }

    @Override
    public IChunkGenerator createChunkGenerator() {
        return new ArenaChunkGenerator(this.world);
    }

    @Override
    public boolean canDoLightning(net.minecraft.world.chunk.Chunk chunk) {
        return false;
    }

    @Override
    public boolean canDoRainSnowIce(net.minecraft.world.chunk.Chunk chunk) {
        return false;
    }

    @Override
    public boolean isSurfaceWorld() {
        return false;
    }

    @Override
    public boolean canRespawnHere() {
        return false;
    }

    // ===== 终为白日 =====
    @Override
    public float getSunBrightnessFactor(float par1) {
        return 1.0F;
    }

    @Override
    public float getSunBrightness(float par1) {
        return 1.0F;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public float getCloudHeight() {
        return 256.0F;
    }

    @Override
    public Vec3d getFogColor(float celestialAngle, float partialTicks) {
        return new Vec3d(0.55D, 0.55D, 0.55D);
    }

    @Override
    public boolean doesXZShowFog(int x, int z) {
        return false;
    }
}
