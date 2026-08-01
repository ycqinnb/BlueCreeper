package yc.ycqin.doth.world;

import net.minecraft.entity.Entity;
import net.minecraft.world.Teleporter;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * 斗蛐蛐专用传送器：不做任何落点搜索，直接定位到目标坐标。
 * 虚空世界没有可搜索的安全落点，默认 Teleporter 会 NPE。
 * 通过反射替换 WorldServer.worldTeleporter 字段来接管默认传送器。
 */
public class ArenaTeleporter extends Teleporter {

    private double targetX, targetY, targetZ;

    public ArenaTeleporter(WorldServer worldIn) {
        super(worldIn);
        this.targetX = 0;
        this.targetY = 50;
        this.targetZ = 0;
    }

    public void setTarget(double x, double y, double z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
    }

    @Override
    public void placeEntity(World world, Entity entity, float yaw) {
        teleportTo(entity);
    }

    @Override
    public boolean placeInExistingPortal(Entity entityIn, float rotationYaw) {
        teleportTo(entityIn);
        return true;
    }

    @Override
    public void placeInPortal(Entity entityIn, float rotationYaw) {
        teleportTo(entityIn);
    }

    @Override
    public boolean makePortal(Entity entityIn) {
        return true;
    }

    private void teleportTo(Entity entity) {
        entity.setPositionAndUpdate(targetX, targetY, targetZ);
        entity.motionX = 0.0D;
        entity.motionY = 0.0D;
        entity.motionZ = 0.0D;
    }
}
