package yc.ycqin.doth.common.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import yc.ycqin.doth.world.RushManager;

/**
 * 虫灵快跑入场券：右键进入虫灵快跑维度（消耗一张）。
 * 仅在装载 srparasites 模组时可用。
 */
public class ItemRushTicket extends Item {

    public ItemRushTicket() {
        setUnlocalizedName("bluecreepersword.rush_ticket");
        setRegistryName("rush_ticket");
        setCreativeTab(ItemReg.DOTH_TABLE);
        setMaxStackSize(16);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        if (!Loader.isModLoaded("srparasites")) {
            player.sendMessage(new net.minecraft.util.text.TextComponentString("§c[虫灵快跑] 需要装载 SRP（Scape and Run Parasites）模组"));
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        if (!(player instanceof EntityPlayerMP)) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        if (RushManager.enterRush((EntityPlayerMP) player)) {
            stack.shrink(1);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
}
