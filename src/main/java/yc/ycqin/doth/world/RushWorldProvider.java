package yc.ycqin.doth.world;

import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 虫灵快跑维度 WorldProvider：虚空、永为白日晴天、无天气、无自然生物
 */
public class RushWorldProvider extends WorldProvider {

    @Override
    public DimensionType getDimensionType() {
        return RushManager.DIMENSION_TYPE;
    }

    @Override
    public IChunkGenerator createChunkGenerator() {
        return new RushChunkGenerator(this.world);
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
