package yc.ycqin.doth.client.gui;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import yc.ycqin.doth.util.BlockHighlightConfig;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@SideOnly(Side.CLIENT)
public class BlockSearchGui extends GuiScreen {

    private final GuiScreen parent;
    private GuiTextField searchField;
    private final List<Block> allBlocks;
    private List<Block> filtered;
    private int scrollOffset = 0, maxScroll = 0;
    private static final int COLS = 3, ITEM_H = 20;
    private int topY, bottomY;

    public BlockSearchGui(GuiScreen parent) {
        this.parent = parent;
        allBlocks = new ArrayList<>();
        Block.REGISTRY.forEach(allBlocks::add);
        allBlocks.sort(Comparator.comparing(b -> b.getLocalizedName().toLowerCase()));
        filtered = new ArrayList<>(allBlocks);
    }

    @Override
    public void initGui() {
        searchField = new GuiTextField(0, fontRenderer, width / 2 - 120, 20, 240, 18);
        searchField.setFocused(true);
        searchField.setCanLoseFocus(false);
        topY = 44;
        bottomY = height - 8;
    }

    @Override
    public void updateScreen() { searchField.updateCursorCounter(); }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == 1) { mc.displayGuiScreen(parent); return; }
        searchField.textboxKeyTyped(c, key);
        filterResults();
    }

    private void filterResults() {
        String q = searchField.getText().toLowerCase();
        filtered = allBlocks.stream()
                .filter(b -> b.getLocalizedName().toLowerCase().contains(q)
                         || Block.REGISTRY.getNameForObject(b).toString().toLowerCase().contains(q))
                .collect(Collectors.toList());
        scrollOffset = 0;
        updateMaxScroll();
    }

    private void updateMaxScroll() {
        int rows = (int) Math.ceil((double) filtered.size() / COLS);
        maxScroll = Math.max(0, rows * ITEM_H - (bottomY - topY));
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        if (btn == 0 && my >= topY && my < bottomY) {
            int colW = (width - 16) / COLS;
            int row = (my - topY + scrollOffset) / ITEM_H;
            int col = (mx - 8) / colW;
            if (col < 0) col = 0; if (col >= COLS) col = COLS - 1;
            int idx = row * COLS + col;
            if (idx >= 0 && idx < filtered.size()) {
                Block b = filtered.get(idx);
                if (BlockHighlightConfig.highlightBlocks.contains(b))
                    BlockHighlightConfig.removeBlock(b);
                else
                    BlockHighlightConfig.addBlock(b);
            }
        }
        searchField.mouseClicked(mx, my, btn);
    }

    @Override
    public void handleMouseInput() throws IOException {
        int w = org.lwjgl.input.Mouse.getDWheel();
        if (w != 0) {
            scrollOffset -= Integer.signum(w) * 20;
            if (scrollOffset < 0) scrollOffset = 0;
            if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        }
        super.handleMouseInput();
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();
        updateMaxScroll();

        GlStateManager.enableBlend();
        int colW = (width - 16) / COLS;

        for (int i = 0; i < filtered.size(); i++) {
            int row = i / COLS, col = i % COLS;
            int x = 8 + col * colW, y = topY + row * ITEM_H - scrollOffset;
            if (y + ITEM_H < topY || y > bottomY) continue;

            Block b = filtered.get(i);
            boolean sel = BlockHighlightConfig.highlightBlocks.contains(b);
            boolean hover = mx >= x && my >= y && mx < x + colW - 2 && my < y + ITEM_H;
            drawRect(x, y, x + colW - 2, y + ITEM_H, hover ? 0x80303050 : (sel ? 0x40102030 : 0x40101020));
            if (sel) drawRect(x, y, x + 2, y + ITEM_H, 0xFF5BEE6B);
            drawString(fontRenderer, (sel ? "§a● " : "§8○ ") + b.getLocalizedName(), x + 8, y + 6, sel ? 0xCCFFCC : 0x999999);
        }

        // 顶栏遮罩，防止列表滚动到搜索框上面
        drawRect(0, 0, width, topY, 0xEE101010);
        drawCenteredString(fontRenderer, "§d◈ 方块高亮管理  §7(已选: §e" + BlockHighlightConfig.highlightBlocks.size() + "§7)", width / 2, 6, 0xFFFFFF);
        searchField.drawTextBox();
        if (searchField.getText().isEmpty())
            drawString(fontRenderer, "§8搜索方块名称或ID...", width / 2 - 116, 26, 0x666666);

        // 右侧滚动条
        if (maxScroll > 0) {
            int sbX = width - 6;
            int sbH = Math.max(20, (int)((float)(bottomY - topY) / (maxScroll + bottomY - topY) * (bottomY - topY)));
            int sbY = topY + (int)((float)scrollOffset / Math.max(1, maxScroll) * (bottomY - topY - sbH));
            drawRect(sbX, sbY, sbX + 4, sbY + sbH, 0x805B6EE1);
        }

        drawCenteredString(fontRenderer, "§8§o" + filtered.size() + " 个结果", width / 2, bottomY + 6, 0x555555);
        super.drawScreen(mx, my, pt);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
