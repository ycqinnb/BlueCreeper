package yc.ycqin.doth.util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ColorfulTextHelper extends FontRenderer{

    private static ColorfulTextHelper font;

    public static ColorfulTextHelper getFont() {
        if (font == null) {
            Minecraft mc = Minecraft.getMinecraft();
            font = new ColorfulTextHelper(mc.gameSettings, new ResourceLocation("textures/font/ascii.png"), mc.renderEngine, false);
            if (mc.gameSettings.language != null)
            {
                font.setUnicodeFlag(mc.isUnicode());
                font.setBidiFlag(mc.getLanguageManager().isCurrentLanguageBidirectional());
            }
        }
        return font;
    }

    private ColorfulTextHelper(GameSettings gameSettingsIn, ResourceLocation location, TextureManager textureManagerIn, boolean unicode) {
        super(gameSettingsIn, location, textureManagerIn, unicode);
    }

    private static long milliTime() {
        return System.nanoTime() / 1000000L;
    }

    private static double rangeRemap(double value, double low1, double high1, double low2, double high2) {
        return low2 + (value - low1) * (high2 - low2) / (high1 - low1);
    }

    public int drawStringWithShadow(String text, float x, float y, int color) {
        float baseHue = 0.65f;
        float hueRange = 0.20f;
        float time = (float)milliTime() / 300.0f;
        float hueOffset = (float)Math.sin(time * Math.PI * 2) * (hueRange / 2);
        float huehuehue = baseHue + hueOffset;
        float huehuehueStep = 0.015f;
        float posX = x;
        String drawText = TextFormatting.getTextWithoutFormattingCodes(text);
        for (int i = 0; i < drawText.length(); i++) {
            int c = color & 0xFF000000 | MathHelper.hsvToRGB(huehuehue, 0.7f, 0.95f);
            posX = super.drawStringWithShadow(String.valueOf(drawText.charAt(i)), posX, y, c);
            huehuehue += huehuehueStep;
            if (huehuehue > 0.75f) huehuehue = 0.55f;
            if (huehuehue < 0.55f) huehuehue = 0.75f;
        }
        return (int)posX;
    }
}
