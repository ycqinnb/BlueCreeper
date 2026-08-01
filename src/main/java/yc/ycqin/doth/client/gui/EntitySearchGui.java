package yc.ycqin.doth.client.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import yc.ycqin.doth.util.EntityHighlightConfig;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@SideOnly(Side.CLIENT)
public class EntitySearchGui extends GuiScreen {

    private final GuiScreen parent;
    private GuiTextField searchField;
    private final List<Class<? extends Entity>> allEntities;
    private List<Class<? extends Entity>> filtered;
    private int scrollOffset = 0, maxScroll = 0;
    private static final int ITEM_H = 16;
    private int topY, bottomY;

    @SuppressWarnings("unchecked")
    public EntitySearchGui(GuiScreen parent) {
        this.parent = parent;
        allEntities = new ArrayList<>();
        // 从 Forge 实体注册表获取所有注册的实体类型
        for (net.minecraftforge.fml.common.registry.EntityEntry entry : net.minecraftforge.fml.common.registry.ForgeRegistries.ENTITIES) {
            Class<? extends Entity> clazz = entry.getEntityClass();
            if (clazz != null) {
                allEntities.add(clazz);
            }
        }
        allEntities.sort(Comparator.comparing(Class::getSimpleName));
        filtered = new ArrayList<>(allEntities);
    }

    @Override
    public void initGui() {
        searchField = new GuiTextField(0, fontRenderer, width / 2 - 140, 20, 280, 18);
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
        filtered = allEntities.stream()
                .filter(c -> c.getSimpleName().toLowerCase().contains(q)
                          || c.getName().toLowerCase().contains(q))
                .collect(Collectors.toList());
        scrollOffset = 0;
        maxScroll = Math.max(0, filtered.size() * ITEM_H - (bottomY - topY));
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        if (btn == 0 && my >= topY && my < bottomY) {
            int idx = (my - topY + scrollOffset) / ITEM_H;
            if (idx >= 0 && idx < filtered.size()) {
                Class<? extends Entity> clazz = filtered.get(idx);
                if (EntityHighlightConfig.highlightEntities.contains(clazz.getName()))
                    EntityHighlightConfig.removeEntity(clazz);
                else
                    EntityHighlightConfig.addEntity(clazz);
            }
        }
        searchField.mouseClicked(mx, my, btn);
    }

    @Override
    public void handleMouseInput() throws IOException {
        int w = org.lwjgl.input.Mouse.getDWheel();
        if (w != 0) {
            scrollOffset -= Integer.signum(w) * 16;
            if (scrollOffset < 0) scrollOffset = 0;
            if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        }
        super.handleMouseInput();
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();
        maxScroll = Math.max(0, filtered.size() * ITEM_H - (bottomY - topY));
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        GlStateManager.enableBlend();

        for (int i = 0; i < filtered.size(); i++) {
            int y = topY + i * ITEM_H - scrollOffset;
            if (y + ITEM_H < topY || y > bottomY) continue;

            Class<? extends Entity> clazz = filtered.get(i);
            boolean sel = EntityHighlightConfig.highlightEntities.contains(clazz.getName());
            boolean hover = mx >= 8 && my >= y && mx < width - 16 && my < y + ITEM_H;
            drawRect(8, y, width - 8, y + ITEM_H, hover ? 0x80303050 : (sel ? 0x40102030 : 0x40101020));
            if (sel) drawRect(8, y, 10, y + ITEM_H, 0xFFFF6B6B);
            String name = clazz.getSimpleName();
            drawString(fontRenderer, (sel ? "§c● " : "§8○ ") + name, 16, y + 4, sel ? 0xFFCCCC : 0x999999);
        }

        drawRect(0, 0, width, topY, 0xEE101010);
        drawCenteredString(fontRenderer, "§5◉ 实体高亮管理  §7(已选: §e" + EntityHighlightConfig.highlightEntities.size() + "§7)", width / 2, 6, 0xFFFFFF);
        searchField.drawTextBox();
        if (searchField.getText().isEmpty())
            drawString(fontRenderer, "§8搜索实体名称...", width / 2 - 136, 26, 0x666666);

        if (maxScroll > 0) {
            int sbX = width - 6;
            int sbH = Math.max(20, (int)((float)(bottomY - topY) / (maxScroll + bottomY - topY) * (bottomY - topY)));
            int sbY = topY + (int)((float)scrollOffset / Math.max(1, maxScroll) * (bottomY - topY - sbH));
            drawRect(sbX, sbY, sbX + 4, sbY + sbH, 0x80FF6B6B);
        }

        drawCenteredString(fontRenderer, "§8§o" + filtered.size() + " 个实体类型", width / 2, bottomY + 6, 0x555555);
        super.drawScreen(mx, my, pt);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
