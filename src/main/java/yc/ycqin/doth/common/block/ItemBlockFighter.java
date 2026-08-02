package yc.ycqin.doth.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 选手方块的 ItemBlock：放置时把 ItemStack NBT 写入 TileEntity
 */
public class ItemBlockFighter extends ItemBlock {

    public ItemBlockFighter(Block block) {
        super(block);
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos,
                                EnumFacing side, float hitX, float hitY, float hitZ, IBlockState newState) {
        if (!super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState)) {
            return false;
        }
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityFighter) {
            NBTTagCompound fighterNbt = new NBTTagCompound();
            if (stack.hasTagCompound() && stack.getTagCompound().hasKey("FighterNbt")) {
                fighterNbt = stack.getTagCompound().getCompoundTag("FighterNbt");
            }
            // 铁砧改名 → 把自定义名写进选手数据，战斗/结算时显示
            if (stack.hasDisplayName()) {
                fighterNbt.setString("TeamName", stack.getDisplayName());
            }
            ((TileEntityFighter) te).setFighterNbt(fighterNbt);
        }
        return true;
    }
}
