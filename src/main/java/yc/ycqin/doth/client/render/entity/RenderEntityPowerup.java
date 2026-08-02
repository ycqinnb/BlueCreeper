package yc.ycqin.doth.client.render.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import yc.ycqin.doth.common.entities.EntityRushPowerup;

/**
 * 道具渲染：悬空旋转的物品图标（护盾/金苹果/糖）
 */
@SideOnly(Side.CLIENT)
public class RenderEntityPowerup extends Render<EntityRushPowerup> {

    public RenderEntityPowerup(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public void doRender(EntityRushPowerup entity, double x, double y, double z, float entityYaw, float partialTicks) {
        ItemStack stack = entity.getVisualStack();
        if (stack.isEmpty()) return;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + 0.25D, z);
        GlStateManager.scale(0.8F, 0.8F, 0.8F);
        GlStateManager.rotate((entity.ticksExisted + partialTicks) * 4.0F, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.GROUND);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityRushPowerup entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }

    public static class Factory implements IRenderFactory<EntityRushPowerup> {
        @Override
        public Render<? super EntityRushPowerup> createRenderFor(RenderManager manager) {
            return new RenderEntityPowerup(manager);
        }
    }
}
