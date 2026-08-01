package yc.ycqin.doth.client.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import yc.ycqin.doth.common.item.BioPhotoItem;
import yc.ycqin.doth.network.NetworkHandler;
import yc.ycqin.doth.network.PacketPhotoKill;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiCamera extends GuiScreen {

    private static final File PHOTO_DIR = new File("doth_photos");

    public GuiCamera(EnumHand hand) {
        if (!PHOTO_DIR.exists()) {
            PHOTO_DIR.mkdirs();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawViewfinder();

        String hint1 = TextFormatting.WHITE + "右键拍摄";
        String hint2 = TextFormatting.GRAY + "ESC 退出取景";
        fontRenderer.drawStringWithShadow(hint1, width / 2 - fontRenderer.getStringWidth(hint1) / 2, height - 40, 0xFFFFFF);
        fontRenderer.drawStringWithShadow(hint2, width / 2 - fontRenderer.getStringWidth(hint2) / 2, height - 25, 0xAAAAAA);
    }

    private void drawViewfinder() {
        int cx = width / 2, cy = height / 2, len = 20, gap = 8, color = 0x80FFFFFF;
        drawHorizontalLine(cx - gap - len, cx - gap, cy - gap, color);
        drawVerticalLine(cx - gap, cy - gap - len, cy - gap, color);
        drawHorizontalLine(cx + gap, cx + gap + len, cy - gap, color);
        drawVerticalLine(cx + gap, cy - gap - len, cy - gap, color);
        drawHorizontalLine(cx - gap - len, cx - gap, cy + gap, color);
        drawVerticalLine(cx - gap, cy + gap, cy + gap + len, color);
        drawHorizontalLine(cx + gap, cx + gap + len, cy + gap, color);
        drawVerticalLine(cx + gap, cy + gap, cy + gap + len, color);
        drawHorizontalLine(cx - 6, cx + 6, cy, color);
        drawVerticalLine(cx, cy - 6, cy + 6, color);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 1) {
            // 关GUI → 藏HUD（不渲染手臂）→ 世界渲染后截图
            mc.displayGuiScreen(null);
            mc.gameSettings.hideGUI = true;
            PhotoTaker.afterWorldRender(this::takePhoto);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void takePhoto() {
        EntityPlayer player = mc.player;

        // 截图
        String filePath = captureFramebuffer();
        mc.gameSettings.hideGUI = false;

        if (filePath == null) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "[相机] 拍摄失败"));
            return;
        }

        // 生成照片
        ItemStack photo = BioPhotoItem.createPhoto(filePath);

        // 视锥体检测
        List<Integer> visibleEntityIds = getVisibleEntityIds(player);

        // 把可见生物的类型 ID 写入照片 NBT
        if (!visibleEntityIds.isEmpty()) {
            NBTTagList entityIds = new NBTTagList();
            for (int id : visibleEntityIds) {
                Entity e = mc.world.getEntityByID(id);
                if (e != null) {
                    ResourceLocation key = EntityList.getKey(e);
                    if (key != null) {
                        entityIds.appendTag(new net.minecraft.nbt.NBTTagString(key.toString()));
                    }
                }
            }
            if (entityIds.tagCount() > 0) {
                NBTTagCompound tag = photo.getTagCompound();
                if (tag == null) {
                    tag = new NBTTagCompound();
                    photo.setTagCompound(tag);
                }
                tag.setTag("EntityIDs", entityIds);
            }
        }

        // 发包杀生物
        if (!visibleEntityIds.isEmpty()) {
            int[] ids = new int[visibleEntityIds.size()];
            for (int i = 0; i < ids.length; i++) ids[i] = visibleEntityIds.get(i);
            NetworkHandler.INSTANCE.sendToServer(new PacketPhotoKill(ids));
        }

        // 给照片
        if (!player.inventory.addItemStackToInventory(photo)) {
            player.dropItem(photo, false);
        }

        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "📷 已拍摄！" +
                (visibleEntityIds.isEmpty() ? "" : " (" + visibleEntityIds.size() + " 个生物)")
        ));
    }

    private String captureFramebuffer() {
        try {
            int w = mc.displayWidth, h = mc.displayHeight;

            GL11.glReadBuffer(GL11.GL_FRONT);
            ByteBuffer buffer = BufferUtils.createByteBuffer(w * h * 4);
            GL11.glReadPixels(0, 0, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int i = ((h - 1 - y) * w + x) * 4;
                    int r = buffer.get(i) & 0xFF;
                    int g = buffer.get(i + 1) & 0xFF;
                    int b = buffer.get(i + 2) & 0xFF;
                    image.setRGB(x, y, (0xFF << 24) | (r << 16) | (g << 8) | b);
                }
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
            File out = new File(PHOTO_DIR, "photo_" + timestamp + ".png");
            ImageIO.write(image, "PNG", out);

            return out.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<Integer> getVisibleEntityIds(EntityPlayer player) {
        List<Integer> ids = new ArrayList<>();
        if (mc.world == null) return ids;

        Vec3d eye = player.getPositionEyes(1.0F);
        Vec3d look = player.getLookVec();
        double halfFovCos = Math.cos(Math.toRadians(mc.gameSettings.fovSetting / 2.0) * 1.15);
        double maxDist = 96.0D;

        for (Entity entity : mc.world.loadedEntityList) {
            if (entity == player || !(entity instanceof EntityLivingBase) || entity.isDead)
                continue;

            double dx = entity.posX - eye.x;
            double dy = (entity.posY + entity.height * 0.5) - eye.y;
            double dz = entity.posZ - eye.z;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq > maxDist * maxDist) continue;

            double dist = Math.sqrt(distSq);
            if ((look.x * dx + look.y * dy + look.z * dz) / dist < halfFovCos) continue;

            Vec3d target = new Vec3d(entity.posX, entity.posY + entity.height * 0.5, entity.posZ);
            if (isOccluded(eye, target)) continue;

            ids.add(entity.getEntityId());
        }
        return ids;
    }

    private boolean isOccluded(Vec3d from, Vec3d to) {
        Vec3d dir = to.subtract(from).normalize();
        double dist = from.distanceTo(to);
        for (double d = 0.5; d < dist - 0.5; d += 0.5) {
            Vec3d pos = new Vec3d(from.x + dir.x * d, from.y + dir.y * d, from.z + dir.z * d);
            if (mc.world.getBlockState(new net.minecraft.util.math.BlockPos(pos)).isOpaqueCube())
                return true;
        }
        return false;
    }
}
