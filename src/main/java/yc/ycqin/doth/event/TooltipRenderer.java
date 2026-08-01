package yc.ycqin.doth.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import yc.ycqin.doth.DOTHMod;
import yc.ycqin.doth.common.item.BlueCreeperSword;
import yc.ycqin.doth.util.ColorfulTextHelper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = DOTHMod.MODID)
public class TooltipRenderer {

    // 装饰图片路径
    private static final ResourceLocation DECOR_TEXTURE =
            new ResourceLocation(DOTHMod.MODID, "textures/gui/tooltip_decor.png");

    @SideOnly(Side.CLIENT)
    public static void onTooltipRender(int x,int y) {
        List<String> newLines = new ArrayList<>();
        newLines.add("✦ 蓝C的小剑剑 ✦");
        newLines.add("“本源降下神谕的那一刻，世界便已无可救药。”");
        newLines.add("执律者们沉入地底，寄生虫席卷每一条街道。");
        newLines.add("格林市沦为巨大的棺材场，");
        newLines.add("被寄群选中的人，终将走向同一场葬礼。");
        newLines.add("本源在天空中凝视，");
        newLines.add("墨灵靠北，低声呢喃：“下一个是谁？”");
        newLines.add("让我们死在一起，在爆炸的照耀下，");
        newLines.add("地府再见吧——这是最后的慈悲。");
        newLines.add("\n攻击伤害 +BlueCreeper0923");

        // ----- 计算尺寸 -----
        ColorfulTextHelper colorfulFont = ColorfulTextHelper.getFont();
        if (colorfulFont == null) {
            // 防御：如果获取失败，使用原版字体（但不会有彩虹效果）
            colorfulFont = (ColorfulTextHelper) Minecraft.getMinecraft().fontRenderer;
        }

        int maxTextWidth = 0;
        // 注意：ColorfulTextHelper 无法直接计算格式化后的宽度，用原版 FontRenderer 计算更准确
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        for (String line : newLines) {
            // 去除颜色代码后计算宽度，因为 ColorfulTextHelper 会覆盖颜色
            String plainText = net.minecraft.util.text.TextFormatting.getTextWithoutFormattingCodes(line);
            int w = font.getStringWidth(plainText);
            if (w > maxTextWidth) maxTextWidth = w;
        }

        int padding = 12;
        int bgWidth = maxTextWidth + padding * 2;
        int bgHeight = newLines.size() * (font.FONT_HEIGHT + 2) + padding * 2 - 2;

        // ----- 保存状态 & 绘制背景 -----
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableDepth();

        // 动态渐变背景（深蓝 -> 青绿）
        float hue = (System.currentTimeMillis() % 6000) / 6000.0f;
        Color color1 = Color.getHSBColor(0.55f + hue * 0.1f, 0.6f, 0.15f);
        Color color2 = Color.getHSBColor(0.45f + hue * 0.1f, 0.8f, 0.25f);
        drawGradientRect(x, y, x + bgWidth, y + bgHeight,
                new Color(0, 0, 0, 180).getRGB(),
                new Color(20, 40, 80, 220).getRGB());
        drawGradientBorder(x, y, bgWidth, bgHeight, color1.getRGB(), color2.getRGB(), 2);

        // ----- 绘制装饰图片（25% 透明度） -----
        GlStateManager.color(1.0F, 1.0F, 1.0F, 0.12F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(DECOR_TEXTURE);
        int iconSize = Math.min(bgWidth, bgHeight) * 4 / 5;
        int iconX = x + (bgWidth - iconSize) / 2;
        int iconY = y + (bgHeight - iconSize) / 2;
        drawTexturedRect(iconX, iconY, iconSize, iconSize);

        // ----- 绘制彩色文字（使用 ColorfulTextHelper） -----
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F); // 恢复不透明

        int currentY = y + padding;
        for (int i = 0; i < newLines.size(); i++) {
            String line = newLines.get(i);

            // 第一行添加装饰（如果还没有的话）
            if (i == 0 && !line.startsWith("✦")) {
                line = "✦ " + line + " ✦";
            }

            // 用彩色渲染器绘制（颜色参数会被内部覆盖）
            colorfulFont.drawStringWithShadow(
                    line,
                    x + padding + (bgWidth - maxTextWidth) / 2,
                    currentY,
                    0xFFFFFF  // 这个参数会被忽略
            );
            currentY += colorfulFont.FONT_HEIGHT + 2;
        }

        // ----- 恢复状态 -----
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onTooltipRender1(RenderTooltipEvent.Pre event) {
        int x = event.getX();
        int y = event.getY();
        if (!(event.getStack().getItem() instanceof BlueCreeperSword)) return;
        event.setCanceled(true);

        List<String> newLines = new ArrayList<>();
        newLines.add("✦ 蓝C的小剑剑 ✦");
        newLines.add("“本源降下神谕的那一刻，世界便已无可救药。”");
        newLines.add("执律者们沉入地底，寄生虫席卷每一条街道。");
        newLines.add("格林市沦为巨大的棺材场，");
        newLines.add("被寄群选中的人，终将走向同一场葬礼。");
        newLines.add("本源在天空中凝视，");
        newLines.add("墨灵靠北，低声呢喃：“下一个是谁？”");
        newLines.add("让我们死在一起，在爆炸的照耀下，");
        newLines.add("地府再见吧——这是最后的慈悲。");
        newLines.add("\n攻击伤害 +BlueCreeper0923");

        // ----- 计算尺寸 -----
        ColorfulTextHelper colorfulFont = ColorfulTextHelper.getFont();
        if (colorfulFont == null) {
            // 防御：如果获取失败，使用原版字体（但不会有彩虹效果）
            colorfulFont = (ColorfulTextHelper) Minecraft.getMinecraft().fontRenderer;
        }

        int maxTextWidth = 0;
        // 注意：ColorfulTextHelper 无法直接计算格式化后的宽度，用原版 FontRenderer 计算更准确
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        for (String line : newLines) {
            // 去除颜色代码后计算宽度，因为 ColorfulTextHelper 会覆盖颜色
            String plainText = net.minecraft.util.text.TextFormatting.getTextWithoutFormattingCodes(line);
            int w = font.getStringWidth(plainText);
            if (w > maxTextWidth) maxTextWidth = w;
        }

        int padding = 12;
        int bgWidth = maxTextWidth + padding * 2;
        int bgHeight = newLines.size() * (font.FONT_HEIGHT + 2) + padding * 2 - 2;

        // ----- 保存状态 & 绘制背景 -----
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableDepth();

        // 动态渐变背景（深蓝 -> 青绿）
        float hue = (System.currentTimeMillis() % 6000) / 6000.0f;
        Color color1 = Color.getHSBColor(0.55f + hue * 0.1f, 0.6f, 0.15f);
        Color color2 = Color.getHSBColor(0.45f + hue * 0.1f, 0.8f, 0.25f);
        drawGradientRect(x, y, x + bgWidth, y + bgHeight,
                new Color(0, 0, 0, 180).getRGB(),
                new Color(20, 40, 80, 220).getRGB());
        drawGradientBorder(x, y, bgWidth, bgHeight, color1.getRGB(), color2.getRGB(), 2);

        // ----- 绘制装饰图片（25% 透明度） -----
        GlStateManager.color(1.0F, 1.0F, 1.0F, 0.12F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(DECOR_TEXTURE);
        int iconSize = Math.min(bgWidth, bgHeight) * 4 / 5;
        int iconX = x + (bgWidth - iconSize) / 2;
        int iconY = y + (bgHeight - iconSize) / 2;
        drawTexturedRect(iconX, iconY, iconSize, iconSize);

        // ----- 绘制彩色文字（使用 ColorfulTextHelper） -----
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F); // 恢复不透明

        int currentY = y + padding;
        for (int i = 0; i < newLines.size(); i++) {
            String line = newLines.get(i);

            // 第一行添加装饰（如果还没有的话）
            if (i == 0 && !line.startsWith("✦")) {
                line = "✦ " + line + " ✦";
            }

            // 用彩色渲染器绘制（颜色参数会被内部覆盖）
            colorfulFont.drawStringWithShadow(
                    line,
                    x + padding + (bgWidth - maxTextWidth) / 2,
                    currentY,
                    0xFFFFFF  // 这个参数会被忽略
            );
            currentY += colorfulFont.FONT_HEIGHT + 2;
        }

        // ----- 恢复状态 -----
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();

    }



    // ============ 辅助绘图方法 ============

    /**
     * 绘制纯色/渐变矩形（使用 Tessellator）
     */
    private static void drawGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
        float f = (startColor >> 24 & 255) / 255.0F;
        float f1 = (startColor >> 16 & 255) / 255.0F;
        float f2 = (startColor >> 8 & 255) / 255.0F;
        float f3 = (startColor & 255) / 255.0F;
        float f4 = (endColor >> 24 & 255) / 255.0F;
        float f5 = (endColor >> 16 & 255) / 255.0F;
        float f6 = (endColor >> 8 & 255) / 255.0F;
        float f7 = (endColor & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(right, top, 0).color(f1, f2, f3, f).endVertex();
        buffer.pos(left, top, 0).color(f1, f2, f3, f).endVertex();
        buffer.pos(left, bottom, 0).color(f5, f6, f7, f4).endVertex();
        buffer.pos(right, bottom, 0).color(f5, f6, f7, f4).endVertex();
        tessellator.draw();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    /**
     * 绘制边框（在矩形边缘加一圈亮色）
     */
    private static void drawGradientBorder(int x, int y, int width, int height, int color1, int color2, int thickness) {
        // 上边
        drawGradientRect(x, y, x + width, y + thickness, color1, color2);
        // 下边
        drawGradientRect(x, y + height - thickness, x + width, y + height, color2, color1);
        // 左边
        drawGradientRect(x, y + thickness, x + thickness, y + height - thickness, color1, color2);
        // 右边
        drawGradientRect(x + width - thickness, y + thickness, x + width, y + height - thickness, color2, color1);
    }

    /**
     * 绘制纹理矩形（用于显示装饰图片）
     */
    private static void drawTexturedRect(int x, int y, int width, int height) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(x, y + height, 0).tex(0, 1).endVertex();
        buffer.pos(x + width, y + height, 0).tex(1, 1).endVertex();
        buffer.pos(x + width, y, 0).tex(1, 0).endVertex();
        buffer.pos(x, y, 0).tex(0, 0).endVertex();
        tessellator.draw();
    }

    private static String tipMessage = "";
    private static int tipTimer = 0;
    private static final int TIP_DURATION = 60; // 显示持续 tick 数（2秒）

    /**
     * 显示提示消息
     */
    public static void showTip(String message) {
        tipMessage = message;
        tipTimer = TIP_DURATION;
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        if (tipTimer <= 0) return;

        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution res = event.getResolution();
        int screenWidth = res.getScaledWidth();
        int screenHeight = res.getScaledHeight();

        // 在快捷栏上方绘制（快捷栏通常在 y = height - 22，我们在上方留 16 像素）
        int y = screenHeight - 80;

        String text = tipMessage;
        ColorfulTextHelper font = ColorfulTextHelper.getFont();
        if (font != null) {
            int x = (screenWidth - font.getStringWidth(text)) / 2;
            // 使用彩色文字渲染
            font.drawStringWithShadow(text, x, y, 0xFFFFFFFF);
        } else {
            // 降级方案：使用原版字体（白色）
            int x = (screenWidth - mc.fontRenderer.getStringWidth(text)) / 2;
            mc.fontRenderer.drawStringWithShadow(text, x, y, 0xFFFFFF);
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onClientEvent(TickEvent.ClientTickEvent event){
        if (tipTimer <= 0) return;
        tipTimer--;
    }
}