package yc.ycqin.doth.common.entities;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * 虫灵快跑的金币实体：无重力、原地悬停、不消失。
 * 渲染为金粒（RenderEntityCoin），撞到即收集。
 */
public class EntityCoin extends Entity {

    public EntityCoin(World worldIn) {
        super(worldIn);
        setSize(0.4F, 0.4F);
        this.isImmuneToFire = true;
    }

    @Override
    protected void entityInit() {
    }

    @Override
    public void onEntityUpdate() {
        super.onEntityUpdate();
        // 悬停 + 轻微上下浮动
        this.motionX = 0;
        this.motionZ = 0;
        this.motionY = 0;
        this.setNoGravity(true);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }
}
