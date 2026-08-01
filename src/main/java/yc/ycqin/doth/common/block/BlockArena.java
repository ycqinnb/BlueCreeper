package yc.ycqin.doth.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import yc.ycqin.doth.common.item.ItemReg;
import yc.ycqin.doth.world.ArenaManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 战斗-爽方块：右键创建斗蛐蛐维度 / 进入观战
 */
public class BlockArena extends Block {

    public BlockArena() {
        super(Material.ROCK);
        setUnlocalizedName("bluecreepersword.block_arena");
        setRegistryName("block_arena");
        setCreativeTab(ItemReg.DOTH_TABLE);
        setHardness(2.0F);
        setResistance(10.0F);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;

        // 维度未注册
        if (!ArenaManager.isDimensionRegistered()) {
            player.sendMessage(new TextComponentString("§c[斗蛐蛐] 维度注册失败，无法使用"));
            return true;
        }

        // 找周围两个有效选手方块
        List<net.minecraft.nbt.NBTTagCompound> fighters = new ArrayList<>();
        for (EnumFacing f : EnumFacing.HORIZONTALS) {
            BlockPos p = pos.offset(f);
            TileEntity te = world.getTileEntity(p);
            if (te instanceof TileEntityFighter) {
                net.minecraft.nbt.NBTTagCompound nbt = ((TileEntityFighter) te).getFighterNbt();
                if (nbt != null && nbt.hasKey("EntityIDs") && nbt.getTagList("EntityIDs", 8).tagCount() > 0) {
                    fighters.add(nbt);
                }
            }
        }

        if (fighters.size() < 2) {
            player.sendMessage(new TextComponentString("§c[斗蛐蛐] 需要在战斗-爽方块两侧放置 2 个有效的选手方块"));
            return true;
        }

        // 未构建 → 构建；已构建但未就绪（打完一场）→ 重置；就绪 → 进入
        if (!ArenaManager.isBuilt()) {
            boolean ok = ArenaManager.buildArena(fighters.get(0), fighters.get(1));
            if (ok) {
                player.sendMessage(new TextComponentString("§a[斗蛐蛐] 场地已创建，再次右键进入"));
            } else {
                player.sendMessage(new TextComponentString("§c[斗蛐蛐] 场地创建失败"));
            }
            return true;
        }

        if (!ArenaManager.isArenaReady()) {
            // 打完一场 → 重置场地（清空旧场地/生物，重建，再生成新选手）
            boolean ok = ArenaManager.buildArena(fighters.get(0), fighters.get(1));
            if (ok) {
                player.sendMessage(new TextComponentString("§a[斗蛐蛐] 场地已重置，再次右键进入"));
            } else {
                player.sendMessage(new TextComponentString("§c[斗蛐蛐] 场地重置失败"));
            }
            return true;
        }

        // 场地就绪 → 进入（战斗中也能进）
        if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
            ArenaManager.enterArena((net.minecraft.entity.player.EntityPlayerMP) player);
        }
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean isOpaqueCube(IBlockState state) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean isFullCube(IBlockState state) {
        return true;
    }
}
