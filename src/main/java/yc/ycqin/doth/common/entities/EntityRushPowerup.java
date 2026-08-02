package yc.ycqin.doth.common.entities;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.world.World;

/**
 * 虫灵快跑的道具实体：悬停、不消失，渲染为对应物品图标（RenderEntityPowerup）。
 * 类型：0=护盾 1=回血 2=加速
 */
public class EntityRushPowerup extends Entity {

    private static final DataParameter<Integer> TYPE = EntityDataManager.createKey(EntityRushPowerup.class, DataSerializers.VARINT);

    public EntityRushPowerup(World worldIn) {
        super(worldIn);
        setSize(0.5F, 0.5F);
        this.isImmuneToFire = true;
    }

    @Override
    protected void entityInit() {
        this.getDataManager().register(TYPE, 0);
    }

    public void setType(int type) {
        this.getDataManager().set(TYPE, type);
    }

    public int getType() {
        return this.getDataManager().get(TYPE);
    }

    /** 道具显示图标 */
    public ItemStack getVisualStack() {
        switch (getType()) {
            case 0: return new ItemStack(net.minecraft.init.Items.SHIELD);
            case 2: return new ItemStack(net.minecraft.init.Items.SUGAR);
            default: return new ItemStack(net.minecraft.init.Items.GOLDEN_APPLE);
        }
    }

    @Override
    public void onEntityUpdate() {
        super.onEntityUpdate();
        this.motionX = 0;
        this.motionZ = 0;
        this.motionY = 0;
        this.setNoGravity(true);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        if (compound.hasKey("RushType")) {
            setType(compound.getInteger("RushType"));
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        compound.setInteger("RushType", getType());
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
