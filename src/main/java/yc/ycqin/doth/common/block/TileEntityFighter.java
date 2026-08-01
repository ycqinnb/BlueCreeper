package yc.ycqin.doth.common.block;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/**
 * 选手方块 TileEntity：保存生物照片 NBT + 药水效果
 */
public class TileEntityFighter extends TileEntity {

    private NBTTagCompound fighterNbt = new NBTTagCompound();

    public void setFighterNbt(NBTTagCompound nbt) {
        this.fighterNbt = nbt != null ? nbt : new NBTTagCompound();
        markDirty();
    }

    public NBTTagCompound getFighterNbt() {
        return fighterNbt;
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
