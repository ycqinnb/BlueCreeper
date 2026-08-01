package yc.ycqin.doth.common.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextFormatting;

import java.util.List;

/**
 * 蓝C剪大片 + 1~8重压缩蓝C剪大片
 * metadata 0 = 普通, 1 = 1重压缩, ..., 8 = 8重压缩
 */
public class CompressedClipItem extends Item {

    public static final int MAX_LEVEL = 8;

    public CompressedClipItem() {
        setUnlocalizedName("bluecreepersword.compressed_clip");
        setRegistryName("compressed_clip");
        setCreativeTab(ItemReg.DOTH_TABLE);
        setMaxStackSize(64);
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        int level = stack.getMetadata();
        if (level == 0) return super.getUnlocalizedName(stack);
        return super.getUnlocalizedName(stack) + ".level" + level;
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (this.isInCreativeTab(tab)) {
            for (int i = 0; i <= MAX_LEVEL; i++) {
                items.add(new ItemStack(this, 1, i));
            }
        }
    }

    @Override
    public void addInformation(ItemStack stack, net.minecraft.world.World worldIn, List<String> tooltip, net.minecraft.client.util.ITooltipFlag flagIn) {
        int level = stack.getMetadata();
        if (level == 0) {
            tooltip.add(TextFormatting.AQUA + "蓝C剪大片");
        } else {
            tooltip.add(TextFormatting.GOLD +""+level + "重压缩蓝C剪大片");
        }
    }

    /**
     * 创建指定压缩等级的 ItemStack
     */
    public static ItemStack create(int level) {
        return new ItemStack(ItemReg.COMPRESSED_CLIP, 1, Math.min(level, MAX_LEVEL));
    }

    /**
     * 获取压缩等级
     */
    public static int getLevel(ItemStack stack) {
        return stack.getMetadata();
    }
}
