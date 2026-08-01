package yc.ycqin.doth.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import yc.ycqin.doth.network.NetworkHandler;
import yc.ycqin.doth.network.PacketTeleport;

import java.io.IOException;
import java.util.*;

@SideOnly(Side.CLIENT)
public class StructResultsGui extends GuiScreen {
    private static List<BlockPos> cachedResults = new ArrayList<>();
    private final GuiScreen parent;
    private final List<BlockPos> results;
    private int scrollOffset = 0, maxScroll;
    private static final int ITEM_H = 16;

    public StructResultsGui(GuiScreen p, List<BlockPos> r) { parent = p; results = r; }

    public static void open(List<BlockPos> r) {
        cachedResults = r;
        Minecraft.getMinecraft().displayGuiScreen(new StructResultsGui(null, r));
    }

    @Override
    public void initGui() {
        maxScroll = Math.max(0, results.size() * ITEM_H - (height - 50));
        buttonList.clear();
        addButton(new GuiButton(99, width / 2 - 60, height - 35, 120, 20, "§c返回"));
    }

    @Override
    public void handleMouseInput() throws IOException {
        int w = org.lwjgl.input.Mouse.getDWheel();
        if (w != 0) { scrollOffset -= Integer.signum(w) * 13; if (scrollOffset<0)scrollOffset=0; if (scrollOffset>maxScroll)scrollOffset=maxScroll; }
        super.handleMouseInput();
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        // 只在不点按钮时处理行点击
        if (my >= height - 40 || mx < width/2-120 || mx > width/2+120) return;
        int idx = (my - 40 + scrollOffset) / ITEM_H;
        if (idx >= 0 && idx < results.size()) {
            BlockPos p = results.get(idx);
            NetworkHandler.INSTANCE.sendToServer(new PacketTeleport(p.getX(), 128, p.getZ()));
        }
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, "§d◈ 结构搜索结果 §7(" + results.size() + "个)", width / 2, 6, 0xFFFFFF);
        int startY = 40;
        for (int i = 0; i < results.size(); i++) {
            int y = startY + i * ITEM_H - scrollOffset;
            if (y + ITEM_H < startY || y > height - 40) continue;
            BlockPos p = results.get(i);
            boolean hover = mx >= width/2-120 && my >= y && mx < width/2+120 && my < y + ITEM_H;
            drawRect(width/2-120, y, width/2+120, y+ITEM_H, hover ? 0x80303050 : 0x20101020);
            drawString(fontRenderer, "§a● §7#"+(i+1)+" §bX:"+p.getX()+" Y:"+p.getY()+" Z:"+p.getZ(), width/2-112, y+4, 0xCCFFCC);
        }
        if (maxScroll > 0) {
            int sbH = Math.max(20, (int)((float)(height-80)/(maxScroll+height-80)*(height-80)));
            int sbY = startY + (int)((float)scrollOffset/maxScroll*(height-80-sbH));
            drawRect(width-6, sbY, width-2, sbY+sbH, 0x805B6EE1);
        }
        super.drawScreen(mx, my, pt);
    }

    @Override public boolean doesGuiPauseGame() { return false; }
}
