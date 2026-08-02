package yc.ycqin.doth.common.block;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import yc.ycqin.doth.world.ArenaManager;

/**
 * 选手方块 TileEntity：保存生物照片 NBT + 药水效果 + 铁砧自定义名。
 * 每秒向 ArenaManager 注册表登记自己，构建场地时无需遍历全图方块。
 */
public class TileEntityFighter extends TileEntity implements ITickable {

    private NBTTagCompound fighterNbt = new NBTTagCompound();
    private int regTick = 0;

    public void setFighterNbt(NBTTagCompound nbt) {
        this.fighterNbt = nbt != null ? nbt : new NBTTagCompound();
        markDirty();
    }

    public NBTTagCompound getFighterNbt() {
        return fighterNbt;
    }

    @Override
    public void update() {
        // 每秒检测：不在选手方块列表就加进去（避免构建时遍历方块的开销）
        if (this.world != null && !this.world.isRemote) {
            if (--regTick <= 0) {
                regTick = 20;
                ArenaManager.registerFighter(this);
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("FighterNbt")) {
            this.fighterNbt = compound.getCompoundTag("FighterNbt");
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("FighterNbt", fighterNbt);
        return compound;
    }
}
