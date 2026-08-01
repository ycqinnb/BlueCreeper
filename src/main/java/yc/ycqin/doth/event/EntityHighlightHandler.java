package yc.ycqin.doth.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import yc.ycqin.doth.common.item.BlueCreeperSword;
import yc.ycqin.doth.network.NetworkHandler;
import yc.ycqin.doth.network.PacketAttackAll;
import yc.ycqin.doth.util.EntityHighlightConfig;
import yc.ycqin.doth.util.SwordConfigHelper;

import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class EntityHighlightHandler {

    private static final List<Entity> highlightedEntities = new ArrayList<>();
    private static int tickCounter = 0;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!EntityHighlightConfig.enabled) return;
        tickCounter++;
        if (tickCounter % EntityHighlightConfig.updateFrequency != 0) return;
        tickCounter = 0;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer p = mc.player;
        if (p == null || p.world == null) return;

        // 读取剑的攻击配置，决定哪些实体高亮
        boolean attackPassive = false, attackPlayers = false, attackAll = false;
        for (ItemStack s : p.inventory.mainInventory) {
            if (s.getItem() instanceof BlueCreeperSword) {
                attackPassive = SwordConfigHelper.isAttackPassive(s);
                attackPlayers = SwordConfigHelper.isAttackPlayers(s);
                attackAll = SwordConfigHelper.isAttackAllEntities(s);
                break;
            }
        }

        highlightedEntities.clear();
        int range = EntityHighlightConfig.searchRange;
        for (Entity e : p.world.loadedEntityList) {
            if (e == p) continue;
            double dx = e.posX - p.posX;
            double dy = e.posY - p.posY;
            double dz = e.posZ - p.posZ;
            if (Math.abs(dx) > range || Math.abs(dy) > range || Math.abs(dz) > range) continue;
            if (EntityHighlightConfig.isHighlighted(e, attackPassive, attackPlayers, attackAll)) {
                highlightedEntities.add(e);
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!EntityHighlightConfig.enabled || highlightedEntities.isEmpty()) return;

        EntityPlayer p = Minecraft.getMinecraft().player;
        if (p == null) return;
        double px = p.lastTickPosX + (p.posX - p.lastTickPosX) * event.getPartialTicks();
        double py = p.lastTickPosY + (p.posY - p.lastTickPosY) * event.getPartialTicks();
        double pz = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * event.getPartialTicks();

        long t = System.nanoTime() / 30000000L;
        float hue = 0.05f + 0.2f * (float)Math.abs(Math.sin(t * 0.02)); // 红色调，区别于方块蓝色的
        int rgb = MathHelper.hsvToRGB(hue, 0.8f, 0.95f);
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

        for (Entity entity : highlightedEntities) {
            AxisAlignedBB box = entity.getEntityBoundingBox();
            if (box == null) continue;
            box = box.grow(0.1);

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
        if (!EntityHighlightConfig.enabled || !EntityHighlightConfig.attackAllEnabled) return;
        if (event.getButton() != 0 || !event.isButtonstate()) return;
        if (!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && !Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;

        int r = EntityHighlightConfig.attackRadius;
        List<Integer> toAttack = new ArrayList<>();
        for (Entity e : highlightedEntities) {
            double dx = Math.abs(e.posX - mc.player.posX);
            double dy = Math.abs(e.posY - mc.player.posY);
            double dz = Math.abs(e.posZ - mc.player.posZ);
            if (dx <= r && dy <= r && dz <= r) {
                toAttack.add(e.getEntityId());
            }
        }
        if (!toAttack.isEmpty()) {
            NetworkHandler.INSTANCE.sendToServer(new PacketAttackAll(toAttack));
            event.setCanceled(true);
        }
    }
}
