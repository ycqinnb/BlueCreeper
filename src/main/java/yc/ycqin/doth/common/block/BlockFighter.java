package yc.ycqin.doth.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import yc.ycqin.doth.common.item.ItemReg;
import yc.ycqin.doth.world.ArenaManager;

/**
 * 选手方块：NBT 保存在 TileEntityFighter 中（生物照片 + 药水效果 + 铁砧自定义名）
 */
public class BlockFighter extends Block {

    public BlockFighter() {
        super(Material.ROCK);
        setUnlocalizedName("bluecreepersword.block_fighter");
        setRegistryName("block_fighter");
        setCreativeTab(ItemReg.DOTH_TABLE);
        setHardness(2.0F);
        setResistance(10.0F);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityFighter();
    }

    /** 敲掉时保留选手数据 + 自定义名（否则改名后一敲就丢） */
    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        ItemStack stack = new ItemStack(this);
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityFighter) {
            NBTTagCompound fighterNbt = ((TileEntityFighter) te).getFighterNbt();
            if (fighterNbt != null && !fighterNbt.hasNoTags()) {
                NBTTagCompound outer = new NBTTagCompound();
                outer.setTag("FighterNbt", fighterNbt);
                stack.setTagCompound(outer);
            }
            if (fighterNbt != null && fighterNbt.hasKey("TeamName")) {
                String teamName = fighterNbt.getString("TeamName");
                if (!teamName.isEmpty()) {
                    stack.setStackDisplayName(teamName);
                }
            }
        }
        drops.add(stack);
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        super.breakBlock(worldIn, pos, state);
        ArenaManager.unregisterFighter(worldIn, pos);
        worldIn.removeTileEntity(pos);
    }
}
