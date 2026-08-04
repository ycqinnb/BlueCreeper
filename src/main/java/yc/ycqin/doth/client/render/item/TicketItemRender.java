package yc.ycqin.doth.client.render.item;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import yc.ycqin.doth.util.ColorfulTextHelper;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * 入场券渲染：先画材质本体，再在券面上叠加四边彩色文字：
 * 上：顶富就玩 / 下：虫灵快跑 / 左：富玩镐（竖排）/ 右：穷砍树（竖排）
 */
@SideOnly(Side.CLIENT)
public class TicketItemRender extends WrappedItemRenderer {

    /** 文字缩放（相对 16px 物品图标，1.0 = 原版 8px 字号） */
    private static final float TEXT_SCALE = 0.3F;

    private static final String TOP_TEXT = "顶富就玩";
    private static final String BOTTOM_TEXT = "虫灵快跑";
    private static final String LEFT_TEXT = "富玩镐";
    private static final String RIGHT_TEXT = "穷砍树";

    // 文字坐标系：物品面中心为原点，1 个物品单位 = 16px。
    // 缩放 TEXT_SCALE 后：字形 8x8，字距 8，图标半宽 H = 8/TEXT_SCALE
    private static final float H = 8.0F / TEXT_SCALE;          // 图标半宽
    private static final float TOP_X = -16.0F;                 // 横排 4 字居中起点 x
    private static final float TOP_Y = -H;                     // 上边文字 y（占 [-H, -H+8]）
    private static final float BOTTOM_X = -16.0F;
    private static final float BOTTOM_Y = H - 8.0F;            // 下边文字 y（占 [H-8, H]）
    private static final float LEFT_X = -H;                    // 左边竖排 x（占 [-H, -H+8]）
    private static final float RIGHT_X = H - 8.0F;             // 右边竖排 x（占 [H-8, H]）
    private static final float COL_Y0 = -12.0F;                // 竖排 3 字垂直居中起始 y
    private static final float CHAR_STEP = 8.0F;

    public TicketItemRender(IModelState state, IBakedModel wrapped) {
        super(state, wrapped);
    }

    @Override
    public void renderItem(ItemStack item, TransformType transformType) {
        // 材质本体
        renderModel(wrapped, item);
        drawTicketText();
    }

    private void drawTicketText() {
        boolean depthWasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean cullWasEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();

        // 关深度测试/深度写入/面剔除：文字永远画在物品上方，不受视角翻转影响
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_CULL_FACE);

        // 锚定到物品面中心（与 wrapped 模型同一坐标系：生成型物品面为 [0,1]x[0,1] @ z=7.5/16）
        // 先乘 → 作用于缩放后的模型坐标，无论外层矩阵如何都能对准物品中心
        GlStateManager.translate(0.5F, 0.5F, 7.5F / 16.0F + 0.01F);
        GlStateManager.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        GlStateManager.scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
        // FontRenderer 约定 +y 向下（GUI 投影方向），物品矩阵里 +y 向上，翻转 Y 使文字正立
        GlStateManager.scale(1.0F, -1.0F, 1.0F);

        ColorfulTextHelper font = ColorfulTextHelper.getFont();
        if (font != null) {
            // 上：顶富就玩（居中）
            font.drawStringWithShadow(TOP_TEXT, TOP_X, TOP_Y, 0xFFFFFFFF);
            // 下：虫灵快跑（居中）
            font.drawStringWithShadow(BOTTOM_TEXT, BOTTOM_X, BOTTOM_Y, 0xFFFFFFFF);
            // 左：富玩镐（竖排，字正立）
            drawColumn(font, LEFT_TEXT, LEFT_X);
            // 右：穷砍树（竖排，字正立）
            drawColumn(font, RIGHT_TEXT, RIGHT_X);
        }

        if (depthWasEnabled) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        } else {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }
        GL11.glDepthMask(true);
        if (cullWasEnabled) {
            GL11.glEnable(GL11.GL_CULL_FACE);
        } else {
            GL11.glDisable(GL11.GL_CULL_FACE);
        }
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /** 竖排文字：逐字向下排，保持字正立 */
    private void drawColumn(ColorfulTextHelper font, String text, float x) {
        for (int i = 0; i < text.length(); i++) {
            font.drawStringWithShadow(String.valueOf(text.charAt(i)), x, COL_Y0 + i * CHAR_STEP, 0xFFFFFFFF);
        }
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState iBlockState, @Nullable EnumFacing enumFacing, long l) {
        return Collections.emptyList();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return null;
    }
}
