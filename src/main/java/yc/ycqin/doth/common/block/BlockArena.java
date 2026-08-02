package yc.ycqin.doth.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import yc.ycqin.doth.common.item.ItemReg;
import yc.ycqin.doth.world.ArenaManager;

import java.util.List;

/**
 * 战斗-爽方块：右键创建斗蛐蛐维度 / 进入观战。
 * 构建时自动检测距离最近的 2 个选手方块（X 小者为左场），并显示构建结果。
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

        // 用注册表找最近 2 个有效选手方块（同维度，避免全图遍历）
        List<ArenaManager.FighterRef> fighters = ArenaManager.findNearestFighters(pos, player.dimension, 2);
        if (fighters.size() < 2) {
            player.sendMessage(new TextComponentString("§c[斗蛐蛐] 需要至少 2 个选手方块（放置后等 1 秒自动注册），当前只找到 " + fighters.size() + " 个"));
            return true;
        }

        // X 小的一侧为左场，X 大的一侧为右场
        ArenaManager.FighterRef f1 = fighters.get(0);
        ArenaManager.FighterRef f2 = fighters.get(1);
        if (f1.pos.getX() > f2.pos.getX()) {
            ArenaManager.FighterRef tmp = f1;
            f1 = f2;
            f2 = tmp;
        }

        // 未构建 → 构建；已构建但未就绪（打完一场）→ 重置；就绪 → 进入
        if (!ArenaManager.isBuilt()) {
            boolean ok = ArenaManager.buildArena(f1, f2);
            if (ok) {
                player.sendMessage(new TextComponentString("§a[斗蛐蛐] 场地已创建（" + ArenaManager.sizeLabel() + "）"));
                player.sendMessage(new TextComponentString("§a[斗蛐蛐] " + ArenaManager.describeMatchup()));
                player.sendMessage(new TextComponentString("§a[斗蛐蛐] 再次右键进入观战"));
            } else {
                player.sendMessage(new TextComponentString("§c[斗蛐蛐] 场地创建失败：选手方块数据无效"));
            }
            return true;
        }

        if (!ArenaManager.isArenaReady()) {
            // 打完一场 → 重置场地（清空旧场地/生物，重建，再生成新选手）
            boolean ok = ArenaManager.buildArena(f1, f2);
            if (ok) {
                player.sendMessage(new TextComponentString("§a[斗蛐蛐] 场地已重置（" + ArenaManager.sizeLabel() + "）"));
                player.sendMessage(new TextComponentString("§a[斗蛐蛐] " + ArenaManager.describeMatchup()));
                player.sendMessage(new TextComponentString("§a[斗蛐蛐] 再次右键进入观战"));
            } else {
                player.sendMessage(new TextComponentString("§c[斗蛐蛐] 场地重置失败：选手方块数据无效"));
            }
            return true;
        }

        // 场地就绪 → 进入（战斗中也能进）
        if (player instanceof EntityPlayerMP) {
            ArenaManager.enterArena((EntityPlayerMP) player);
        }
        return true;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return true;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return true;
    }
}
