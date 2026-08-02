package yc.ycqin.doth.common.block;

import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import yc.ycqin.doth.world.ArenaManager;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 战斗-爽方块的 ItemBlock：手持右键增大场地范围，shift+右键减小场地范围。
 * 只在右键空气时触发，右键方块放置/激活逻辑不受影响。
 */
public class ItemBlockArena extends ItemBlock {

    public ItemBlockArena(Block block) {
        super(block);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        if (!worldIn.isRemote && playerIn instanceof EntityPlayerMP) {
            ArenaManager.changeSize((EntityPlayerMP) playerIn, playerIn.isSneaking() ? -1 : 1);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, playerIn.getHeldItem(handIn));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add("§7右键方块：自动检测最近 2 个选手方块，构建场地 / 进入观战");
        tooltip.add("§7手持右键空气：场地 +1（默认 15×15）；§e潜行§7+右键：-1");
        tooltip.add("§7选手方块下方方块、所在群系会变成场地地板与群系");
        tooltip.add("§7选手方块可在铁砧改名，战斗与结算时显示名字");
        tooltip.add("§7合成：生物照片 + 钻石/药水瓶（可混搭，药水提供效果）");
    }
}
