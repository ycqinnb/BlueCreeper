package yc.ycqin.doth.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import yc.ycqin.doth.client.RushClientState;

/**
 * 虫灵快跑客户端：左上角分数 HUD
 */
@SideOnly(Side.CLIENT)
public class RushClientHandler {

    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Text event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (!RushClientState.active || mc.player == null) return;
        if (mc.player.dimension != RushClientState.dimId) {
            RushClientState.active = false;
            return;
        }
        ScaledResolution res = new ScaledResolution(mc);
        FontRenderer fr = mc.fontRenderer;
        int x = 2, y = 2;
        fr.drawStringWithShadow("§e§l虫灵快跑", x, y, 0xFFFFFF);
        fr.drawStringWithShadow("§f分数 §6" + RushClientState.score, x, y + 10, 0xFFFFFF);
        fr.drawStringWithShadow("§f距离 §a" + RushClientState.distance + "m", x, y + 20, 0xFFFFFF);
        fr.drawStringWithShadow("§f金币 §e" + RushClientState.coins, x, y + 30, 0xFFFFFF);
        fr.drawStringWithShadow("§7A/D 左右 · Shift 下区弃权", x, y + 40, 0xFFFFFF);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.player == null || mc.world == null) return;

        EntityPlayer player = mc.player;

        // 强制上马重试（实体同步到客户端后立即骑上）
        if (RushClientState.pendingMountId != -1 && player != null) {
            Entity mount = mc.world.getEntityByID(RushClientState.pendingMountId);
            if (mount != null) {
                if (!player.isRiding()) {
                    player.startRiding(mount, true);
                }
                RushClientState.pendingMountId = -1;
            }
        }

        // 左右移动：走原版骑乘输入管线（ASM canBeSteered → CPacketInput → travel），无需自定义包

        // 离开维度 → 关闭 HUD
        if (RushClientState.active && player.dimension != RushClientState.dimId) {
            RushClientState.active = false;
        }
    }
}
