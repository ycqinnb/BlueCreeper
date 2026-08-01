package yc.ycqin.doth.client.render.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import yc.ycqin.doth.common.entities.EntityItemSwordHighlight;

import java.awt.*;
import java.util.Random;

@SideOnly(Side.CLIENT)
public class RenderEntityItemSwordHighlight extends Render<EntityItemSwordHighlight> {

    private final RenderEntityItem renderItem;
    private static final Random rand = new Random();

    public RenderEntityItemSwordHighlight(RenderManager renderManager) {
        super(renderManager);
        renderItem = new RenderEntityItem(renderManager, Minecraft.getMinecraft().getRenderItem());
    }

    @Override
    public void doRender(EntityItemSwordHighlight entity, double x, double y, double z, float entityYaw, float partialTicks) {
        Color currentColor = getAnimatedColor();

        // 画光柱
        renderSparkleEffect(x, y + 0.5, z, currentColor, entity.getEntityId(), entity.ticksExisted);

        // 画物品本体（直接委托原版，不需要 dummy）
        ItemStack stack = entity.getItem();
        if (!stack.isEmpty()) {
            renderItem.doRender(entity, x, y, z, entityYaw, partialTicks);
        }
    }

    private static Color getAnimatedColor() {
        long timeMs = System.nanoTime() / 1_000_000L;
        float hue = 0.65f + (float) Math.sin(timeMs / 300.0f * Math.PI * 2) * 0.10f;
        int rgb = MathHelper.hsvToRGB(hue, 0.7f, 0.95f);
        return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    private static void renderSparkleEffect(double x, double y, double z, Color color, int entityId, int ticksExisted) {
        rand.setSeed(entityId * 16024L + 31);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        int rayCount = 16;
        Tessellator tes = Tessellator.getInstance();
        BufferBuilder vb = tes.getBuffer();

        RenderHelper.disableStandardItemLighting();
        float f1 = (float) (ticksExisted + System.nanoTime() / 100000L % 400) / 400.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 1);
        GlStateManager.disableAlpha();
        GlStateManager.depthMask(false);
        GlStateManager.pushMatrix();

        for (int i = 0; i < rayCount; i++) {
            GlStateManager.rotate(rand.nextFloat() * 360.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(rand.nextFloat() * 360.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(rand.nextFloat() * 360.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(rand.nextFloat() * 360.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(rand.nextFloat() * 360.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(rand.nextFloat() * 360.0F + f1 * 360.0F, 0.0F, 0.0F, 1.0F);

            float rayLength = (rand.nextFloat() * 20.0F + 5.0F + 0.4F * 10.0F) / 3.0F;
            float rayWidth = (rand.nextFloat() * 2.0F + 1.0F + 0.4F * 2.0F) / 3.0F;

            vb.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
            vb.pos(0, 0, 0).color(color.getRed(), color.getGreen(), color.getBlue(), 180).endVertex();
            vb.pos(-0.7D * rayWidth, rayLength, -0.5F * rayWidth)
                    .color(color.getRed(), color.getGreen(), color.getBlue(), 0).endVertex();
            vb.pos(0.7D * rayWidth, rayLength, -0.5F * rayWidth)
                    .color(color.getRed(), color.getGreen(), color.getBlue(), 0).endVertex();
            vb.pos(0.0D, rayLength, 1.0F * rayWidth)
                    .color(color.getRed(), color.getGreen(), color.getBlue(), 0).endVertex();
            vb.pos(-0.7D * rayWidth, rayLength, -0.5F * rayWidth)
                    .color(color.getRed(), color.getGreen(), color.getBlue(), 0).endVertex();
            tes.draw();
        }

        GlStateManager.popMatrix();
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(770, 771);
        GlStateManager.disableBlend();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        RenderHelper.enableStandardItemLighting();

        GlStateManager.popMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityItemSwordHighlight entity) {
        return null;
    }

    public static class Factory implements IRenderFactory<EntityItemSwordHighlight> {
        @Override
        public Render<? super EntityItemSwordHighlight> createRenderFor(RenderManager manager) {
            return new RenderEntityItemSwordHighlight(manager);
        }
    }
}
