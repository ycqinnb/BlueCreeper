package yc.ycqin.doth.client.render.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import yc.ycqin.doth.common.entities.EntityCoin;

/**
 * 金币渲染：悬空旋转的金粒
 */
@SideOnly(Side.CLIENT)
public class RenderEntityCoin extends Render<EntityCoin> {

    public RenderEntityCoin(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public void doRender(EntityCoin entity, double x, double y, double z, float entityYaw, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + 0.25D, z);
        GlStateManager.scale(0.6F, 0.6F, 0.6F);
        GlStateManager.rotate((entity.ticksExisted + partialTicks) * 6.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate((entity.ticksExisted + partialTicks) * 2.0F, 1.0F, 0.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        Minecraft.getMinecraft().getRenderItem().renderItem(
                new ItemStack(Items.GOLD_NUGGET), ItemCameraTransforms.TransformType.GROUND);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityCoin entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }

    public static class Factory implements IRenderFactory<EntityCoin> {
        @Override
        public Render<? super EntityCoin> createRenderFor(RenderManager manager) {
            return new RenderEntityCoin(manager);
        }
    }
}
