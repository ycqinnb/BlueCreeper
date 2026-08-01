package yc.ycqin.doth.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import yc.ycqin.doth.DOTHMod;
import yc.ycqin.doth.client.DOTHKeyBind;
import yc.ycqin.doth.client.gui.SwordConfigGui;
import yc.ycqin.doth.common.item.BlueCreeperSword;
import yc.ycqin.doth.network.*;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = DOTHMod.MODID)
@SideOnly(Side.CLIENT)
public class KeyInputHandler {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (DOTHKeyBind.OPEN_CONFIG_GUI.isPressed()) {
                Minecraft mc = Minecraft.getMinecraft();
                EntityPlayer player = mc.player;

                if (player != null) {
                    ItemStack stack = player.getHeldItemMainhand();
                    // 只有手持蓝C之剑时才打开 GUI
                    if (stack.getItem() instanceof BlueCreeperSword) {
                        mc.displayGuiScreen(new SwordConfigGui(stack));
                    }
                }
            }
        }
    }
}