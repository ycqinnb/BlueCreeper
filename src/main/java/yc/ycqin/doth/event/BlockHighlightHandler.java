package yc.ycqin.doth.event;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import yc.ycqin.doth.network.NetworkHandler;
import yc.ycqin.doth.network.PacketMineAll;
import yc.ycqin.doth.util.BlockHighlightConfig;

import java.util.*;

@SideOnly(Side.CLIENT)
public class BlockHighlightHandler {

    private static final List<BlockPos> highlightedBlocks = new ArrayList<>();
    private static int tickCounter = 0;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!BlockHighlightConfig.enabled) return;
        tickCounter++;
        if (tickCounter % BlockHighlightConfig.updateFrequency != 0) return;
        tickCounter = 0;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer p = mc.player;
        if (p == null || p.world == null) return;

        highlightedBlocks.clear();
        int range = BlockHighlightConfig.searchRange;
        BlockPos playerPos = p.getPosition();
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    BlockPos pos = playerPos.add(dx, dy, dz);
                    IBlockState state = p.world.getBlockState(pos);
                    if (BlockHighlightConfig.isHighlighted(state) && !p.world.isAirBlock(pos)) {
                        highlightedBlocks.add(pos);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!BlockHighlightConfig.enabled || highlightedBlocks.isEmpty()) return;

        EntityPlayer p = Minecraft.getMinecraft().player;
        if (p == null) return;
        double px = p.lastTickPosX + (p.posX - p.lastTickPosX) * event.getPartialTicks();
        double py = p.lastTickPosY + (p.posY - p.lastTickPosY) * event.getPartialTicks();
        double pz = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * event.getPartialTicks();

        long t = System.nanoTime() / 30000000L;
        float hue = 0.55f + 0.2f * (float)Math.abs(Math.sin(t * 0.02));
        int rgb = MathHelper.hsvToRGB(hue, 0.7f, 0.95f);
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;

        GlStateManager.pushMatrix();
        GlStateManager.translate(-px, -py, -pz);
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        for (BlockPos pos : highlightedBlocks) {
            double x = pos.getX(), y = pos.getY(), z = pos.getZ();
            AxisAlignedBB box = new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1).grow(0.002);

            buf.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
            buf.pos(box.minX, box.minY, box.minZ).color(r, g, b, 0.6f).endVertex();
            buf.pos(box.maxX, box.minY, box.minZ).color(r, g, b, 0.6f).endVertex();
            buf.pos(box.maxX, box.minY, box.maxZ).color(r, g, b, 0.6f).endVertex();
            buf.pos(box.minX, box.minY, box.maxZ).color(r, g, b, 0.6f).endVertex();
            tess.draw();
            buf.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
            buf.pos(box.minX, box.maxY, box.minZ).color(r, g, b, 0.8f).endVertex();
            buf.pos(box.maxX, box.maxY, box.minZ).color(r, g, b, 0.8f).endVertex();
            buf.pos(box.maxX, box.maxY, box.maxZ).color(r, g, b, 0.8f).endVertex();
            buf.pos(box.minX, box.maxY, box.maxZ).color(r, g, b, 0.8f).endVertex();
            tess.draw();
            buf.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            for (int corner = 0; corner < 4; corner++) {
                double cx = (corner & 1) == 0 ? box.minX : box.maxX;
                double cz = (corner & 2) == 0 ? box.minZ : box.maxZ;
                buf.pos(cx, box.minY, cz).color(r, g, b, 0.4f).endVertex();
                buf.pos(cx, box.maxY, cz).color(r, g, b, 0.4f).endVertex();
            }
            tess.draw();
        }

        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (!BlockHighlightConfig.enabled || !BlockHighlightConfig.mineAllEnabled) return;
        if (event.getButton() != 0 || !event.isButtonstate()) return;
        if (!org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT)
                && !org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RSHIFT)) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;

        int r = BlockHighlightConfig.mineAllRadius;
        BlockPos center = mc.player.getPosition();
        List<BlockPos> toBreak = new ArrayList<>();
        for (BlockPos pos : highlightedBlocks) {
            if (Math.abs(pos.getX() - center.getX()) <= r
                    && Math.abs(pos.getY() - center.getY()) <= r
                    && Math.abs(pos.getZ() - center.getZ()) <= r) {
                toBreak.add(pos);
            }
        }
        if (!toBreak.isEmpty()) {
            NetworkHandler.INSTANCE.sendToServer(new PacketMineAll(toBreak));
            event.setCanceled(true);
        }
    }
}
