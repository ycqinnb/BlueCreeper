package yc.ycqin.doth.common.item;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import yc.ycqin.doth.client.gui.GuiCamera;

public class CameraItem extends Item {

    public CameraItem() {
        setUnlocalizedName("bluecreepersword.camera");
        setRegistryName("camera");
        setCreativeTab(ItemReg.DOTH_TABLE);
        setMaxStackSize(1);
        setMaxDamage(0);
    }

    @Override
    public boolean isDamageable() {
        return false;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return 0;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (world.isRemote) {
            openCameraGui(hand);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @SideOnly(Side.CLIENT)
    private void openCameraGui(EnumHand hand) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiCamera(hand));
    }
}
