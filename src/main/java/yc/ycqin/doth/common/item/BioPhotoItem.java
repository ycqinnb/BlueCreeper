package yc.ycqin.doth.common.item;

import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import yc.ycqin.doth.client.gui.GuiPhoto;

import java.util.List;

public class BioPhotoItem extends Item {

    public BioPhotoItem() {
        setUnlocalizedName("bluecreepersword.bio_photo");
        setRegistryName("bio_photo");
        setCreativeTab(ItemReg.DOTH_TABLE);
        setMaxStackSize(64);
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true; // 始终带附魔光效
    }

    @Override
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, net.minecraft.client.util.ITooltipFlag flagIn) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey("PhotoPath")) {
            String path = tag.getString("PhotoPath");
            String name = path.substring(Math.max(0, path.lastIndexOf('/') + 1));
            tooltip.add(TextFormatting.GRAY + "照片: " + name);
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("PhotoPath")) return new ActionResult<>(EnumActionResult.PASS, stack);

        if (world.isRemote) {
            openPhotoGui(stack);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @SideOnly(Side.CLIENT)
    private void openPhotoGui(ItemStack stack) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiPhoto(stack));
    }

    /**
     * 创建一个带有照片路径的 BioPhotoItem
     */
    public static ItemStack createPhoto(String filePath) {
        ItemStack stack = new ItemStack(ItemReg.BIO_PHOTO);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("PhotoPath", filePath);
        stack.setTagCompound(tag);
        return stack;
    }

    /**
     * 合成时保留 NBT（压缩配方需要）
     */
    @Override
    public boolean hasContainerItem(ItemStack stack) {
        return false;
    }
}
