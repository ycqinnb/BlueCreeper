package yc.ycqin.doth.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import yc.ycqin.doth.common.item.ItemReg;

/**
 * 选手方块：NBT 保存在 TileEntityFighter 中（生物照片 + 药水效果）
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

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        super.breakBlock(worldIn, pos, state);
        worldIn.removeTileEntity(pos);
    }
}
