package yc.ycqin.doth.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 一次性截图辅助：在世界渲染完成后（HUD/GUI 绘制前）执行指定操作。
 * 此时 framebuffer 只有纯世界画面。
 */
@SideOnly(Side.CLIENT)
public class PhotoTaker {

    /** 注册一个只在下一帧世界渲染后执行一次的操作 */
    public static void afterWorldRender(Runnable action) {
        MinecraftForge.EVENT_BUS.register(new OneShot(action));
    }

    @SideOnly(Side.CLIENT)
    public static class OneShot {
        private final Runnable action;

        public OneShot(Runnable action) {
            this.action = action;
        }

        @SubscribeEvent
        public void onRenderWorldLast(RenderWorldLastEvent event) {
            MinecraftForge.EVENT_BUS.unregister(this);
            Minecraft.getMinecraft().addScheduledTask(action::run);
        }
    }
}
