package yc.ycqin.doth.common.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import yc.ycqin.doth.world.RushManager;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 生物快跑入场券：右键进入生物快跑维度（消耗一张）。
 * NBT（doth_rush_mob）记录坐骑生物注册名；无 NBT 时默认僵尸（装载 SRP 时默认虫灵）。
 */
public class ItemRushTicket extends Item {

    /** 坐骑生物注册名的 NBT key */
    public static final String NBT_MOB = "doth_rush_mob";

    public ItemRushTicket() {
        setUnlocalizedName("bluecreepersword.rush_ticket");
        setRegistryName("rush_ticket");
        setCreativeTab(ItemReg.DOTH_TABLE);
        setMaxStackSize(16);
    }

    /** 入场券绑定的生物注册名（无 NBT → 默认：SRP 装载时虫灵，否则僵尸） */
    public static String getMobId(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey(NBT_MOB)) {
            String id = tag.getString(NBT_MOB);
            if (id != null && !id.isEmpty()) return id;
        }
        return Loader.isModLoaded("srparasites") ? "srparasites:buglin" : "minecraft:zombie";
    }

    /** 动态显示名：xx快跑入场券（xx = 生物名） */
    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return "§d" + RushManager.getMobDisplayName(getMobId(stack)) + "快跑入场券";
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, net.minecraft.client.util.ITooltipFlag flagIn) {
        tooltip.add("§7生物：" + RushManager.getMobDisplayName(getMobId(stack)));
        tooltip.add("§7右键使用：进入生物快跑（消耗一张）");
        tooltip.add("§7无敌指令：§e/rushdebug invincible on §7开启无敌模式");
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        if (!(player instanceof EntityPlayerMP)) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        if (RushManager.enterRush((EntityPlayerMP) player, getMobId(stack))) {
            stack.shrink(1);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
}
