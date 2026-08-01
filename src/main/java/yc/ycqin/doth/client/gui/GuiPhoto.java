package yc.ycqin.doth.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@SideOnly(Side.CLIENT)
public class GuiPhoto extends GuiScreen {

    private final String photoPath;
    private DynamicTexture dynamicTexture;
    private ResourceLocation textureLocation;
    private int imgWidth, imgHeight;
    private boolean loaded;
    private String errorMsg;

    public GuiPhoto(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        this.photoPath = tag != null ? tag.getString("PhotoPath") : null;
        this.loaded = false;
        this.errorMsg = null;
    }

    @Override
    public void initGui() {
        if (loaded) return;
        loaded = true;

        if (photoPath == null || photoPath.isEmpty()) {
            errorMsg = "无照片数据";
            return;
        }

        File file = new File(photoPath);
        if (!file.exists()) {
            errorMsg = "照片文件不存在: " + photoPath;
            return;
        }

        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                errorMsg = "无法读取照片文件";
                return;
            }
            imgWidth = image.getWidth();
            imgHeight = image.getHeight();
            dynamicTexture = new DynamicTexture(image);
            textureLocation = mc.getTextureManager().getDynamicTextureLocation("photo_" + photoPath.hashCode(), dynamicTexture);
        } catch (IOException e) {
            errorMsg = "读取照片出错: " + e.getMessage();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 暗色背景
        drawRect(0, 0, width, height, 0xCC000000);

        if (errorMsg != null) {
            drawCenteredString(fontRenderer, errorMsg, width / 2, height / 2, 0xFF5555);
            drawCenteredString(fontRenderer, "按 ESC 关闭", width / 2, height / 2 + 15, 0xAAAAAA);
            return;
        }

        if (textureLocation == null) {
            drawCenteredString(fontRenderer, "加载中...", width / 2, height / 2, 0xFFFFFF);
            return;
        }

        // 等比例缩放，适配屏幕
        int maxW = width - 40;
        int maxH = height - 40;
        double scale = Math.min((double) maxW / imgWidth, (double) maxH / imgHeight);
        int drawW = (int) (imgWidth * scale);
        int drawH = (int) (imgHeight * scale);
        int drawX = (width - drawW) / 2;
        int drawY = (height - drawH) / 2;

        // 绘制照片
        mc.getTextureManager().bindTexture(textureLocation);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        drawModalRectWithCustomSizedTexture(drawX, drawY, 0, 0, drawW, drawH, drawW, drawH);

        // 提示
        drawCenteredString(fontRenderer, "按 ESC 关闭", width / 2, height - 15, 0xAAAAAA);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) { // ESC
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onGuiClosed() {
        // DynamicTexture 由 TextureManager 管理，会自动清理
    }
}
