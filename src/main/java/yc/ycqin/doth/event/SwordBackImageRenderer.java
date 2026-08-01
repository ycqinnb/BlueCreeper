package yc.ycqin.doth.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import yc.ycqin.doth.DOTHMod;
import yc.ycqin.doth.common.item.BlueCreeperSword;

@Mod.EventBusSubscriber(modid = DOTHMod.MODID, value = Side.CLIENT)
public class SwordBackImageRenderer {

    // 图片路径：assets/doth/textures/gui/back_image.png
    private static final ResourceLocation BACK_IMAGE =
            new ResourceLocation(DOTHMod.MODID, "textures/gui/tooltip_decor.png");

    // 透明度（0.0 ~ 1.0），这里设为 0.3 即 30% 透明度
    private static final float OPACITY = 0.3f;

    // 图片大小（在游戏世界中的尺寸，单位：块）
    private static final float IMAGE_WIDTH = 1.2f;
    private static final float IMAGE_HEIGHT = 1.8f;

    // 图片距离玩家的偏移（背后 Z 轴负方向）
    private static final float DISTANCE_BEHIND = 0.8f;

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null) return;

        // 检查是否是本地玩家？通常只对本地玩家渲染，或者所有玩家？这里对所有玩家都渲染，但可以根据需要只对本地玩家
        // 但如果要对所有玩家都显示，就保留；如果只需要自己显示，可以加上 if (player != Minecraft.getMinecraft().player) return;

        // 检查是否手持蓝C之剑
        ItemStack mainHand = player.getHeldItemMainhand();
        if (!(mainHand.getItem() instanceof BlueCreeperSword)) {
            return;
        }

        // 渲染图片
        renderBackImage(player, event.getPartialRenderTick());
    }

    private static void renderBackImage(EntityPlayer player, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();

        // 绑定纹理
        mc.getTextureManager().bindTexture(BACK_IMAGE);

        // 保存 OpenGL 状态
        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();

        // 启用混合（实现透明）
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // 设置颜色（带透明度）
        GlStateManager.color(1.0f, 1.0f, 1.0f, OPACITY);

        // 禁用光照和深度测试（让图片永远在顶层）
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();

        // 获取玩家位置（插值位置，使图片平滑跟随）
        double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        // 获取玩家视角方向（用于计算背后位置）
        float yaw = player.rotationYaw;
        float pitch = player.rotationPitch; // 俯仰角，如果希望图片始终正对摄像机，可以忽略pitch

        // 计算玩家朝向向量 (水平方向)
        float radYaw = (float) Math.toRadians(yaw);
        float cosYaw = (float) Math.cos(radYaw);
        float sinYaw = (float) Math.sin(radYaw);

        // 玩家背后方向（Z轴负方向在世界坐标系中）: 实际上是玩家朝向的反方向
        // 玩家的朝向向量: ( -sinYaw, 0, cosYaw )  (因为Minecraft中Z轴正向为南，Yaw=0时朝向Z正)
        // 背后方向: ( sinYaw, 0, -cosYaw )
        float backX = sinYaw;
        float backZ = -cosYaw;

        // 计算图片中心位置（玩家背后偏移）
        double centerX = x + backX * DISTANCE_BEHIND;
        double centerY = y + 0.2; // 稍微抬高一点，使中心在玩家腰部以上
        double centerZ = z + backZ * DISTANCE_BEHIND;

        // 准备绘制四边形（Billboard，始终面向摄像机）
        // 使用 Tessellator 绘制两个三角形组成矩形
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // 计算四个角的世界坐标（基于玩家的朝向，但始终面向摄像机）
        // 简单方法：使用 GL11 的旋转，但为了始终面向摄像机，我们可以获取摄像机的视角矩阵
        // 这里使用简便方法：将四边形放在玩家背后，但面向摄像机（通过 LookAt 方向）
        // 其实更简单：使用 glPushMatrix 然后 glTranslate 到中心，然后根据摄像机旋转，绘制矩形
        // 但为了不依赖复杂的矩阵运算，我们可以直接使用摄像机坐标计算四个角。

        // 获取摄像机位置
        EntityPlayer renderViewEntity = (EntityPlayer) mc.getRenderViewEntity();
        if (renderViewEntity == null) return;
        double camX = renderViewEntity.lastTickPosX + (renderViewEntity.posX - renderViewEntity.lastTickPosX) * partialTicks;
        double camY = renderViewEntity.lastTickPosY + (renderViewEntity.posY - renderViewEntity.lastTickPosY) * partialTicks;
        double camZ = renderViewEntity.lastTickPosZ + (renderViewEntity.posZ - renderViewEntity.lastTickPosZ) * partialTicks;

        // 计算从图片中心到摄像机的方向向量
        double dx = camX - centerX;
        double dy = camY - centerY;
        double dz = camZ - centerZ;
        double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (dist < 0.001) return;

        // 单位方向向量
        double nx = dx / dist;
        double ny = dy / dist;
        double nz = dz / dist;

        // 计算 UP 向量（世界Y轴）
        double upX = 0, upY = 1, upZ = 0;

        // 计算右向量 = 方向向量 × UP向量 (叉积)
        double rightX = ny * upZ - nz * upY;
        double rightY = nz * upX - nx * upZ;
        double rightZ = nx * upY - ny * upX;
        double rightLen = Math.sqrt(rightX*rightX + rightY*rightY + rightZ*rightZ);
        if (rightLen < 0.001) {
            // 如果方向向量与UP平行（即视角垂直向上/下），用Z轴作为右向量
            rightX = 0; rightY = 0; rightZ = 1;
            rightLen = 1;
        } else {
            rightX /= rightLen;
            rightY /= rightLen;
            rightZ /= rightLen;
        }

        // 重新计算UP向量 = 右向量 × 方向向量
        double newUpX = rightY * nz - rightZ * ny;
        double newUpY = rightZ * nx - rightX * nz;
        double newUpZ = rightX * ny - rightY * nx;

        // 计算四角偏移（宽高的一半）
        float halfW = IMAGE_WIDTH / 2.0f;
        float halfH = IMAGE_HEIGHT / 2.0f;

        // 四个角：左上、右上、右下、左下
        // 角 = 中心 + 右 * offsetX + up * offsetY
        double[][] corners = new double[4][3];
        // 左上 ( -halfW, +halfH )
        corners[0][0] = centerX + rightX * (-halfW) + newUpX * halfH;
        corners[0][1] = centerY + rightY * (-halfW) + newUpY * halfH;
        corners[0][2] = centerZ + rightZ * (-halfW) + newUpZ * halfH;
        // 右上 ( +halfW, +halfH )
        corners[1][0] = centerX + rightX * halfW + newUpX * halfH;
        corners[1][1] = centerY + rightY * halfW + newUpY * halfH;
        corners[1][2] = centerZ + rightZ * halfW + newUpZ * halfH;
        // 右下 ( +halfW, -halfH )
        corners[2][0] = centerX + rightX * halfW + newUpX * (-halfH);
        corners[2][1] = centerY + rightY * halfW + newUpY * (-halfH);
        corners[2][2] = centerZ + rightZ * halfW + newUpZ * (-halfH);
        // 左下 ( -halfW, -halfH )
        corners[3][0] = centerX + rightX * (-halfW) + newUpX * (-halfH);
        corners[3][1] = centerY + rightY * (-halfW) + newUpY * (-halfH);
        corners[3][2] = centerZ + rightZ * (-halfW) + newUpZ * (-halfH);

        // UV 映射：左上(0,0) 右上(1,0) 右下(1,1) 左下(0,1)
        float u0 = 0, v0 = 0;
        float u1 = 1, v1 = 0;
        float u2 = 1, v2 = 1;
        float u3 = 0, v3 = 1;

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        // 左上
        buffer.pos(corners[0][0], corners[0][1], corners[0][2]).tex(u0, v0).endVertex();
        // 右上
        buffer.pos(corners[1][0], corners[1][1], corners[1][2]).tex(u1, v1).endVertex();
        // 右下
        buffer.pos(corners[2][0], corners[2][1], corners[2][2]).tex(u2, v2).endVertex();
        // 左下
        buffer.pos(corners[3][0], corners[3][1], corners[3][2]).tex(u3, v3).endVertex();
        tessellator.draw();

        // 恢复状态
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();

        GlStateManager.popAttrib();
        GlStateManager.popMatrix();
    }
}