package yc.ycqin.doth.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import yc.ycqin.doth.network.NetworkHandler;
import yc.ycqin.doth.network.PacketBuffConfig;
import yc.ycqin.doth.util.SwordConfigHelper;

import java.io.IOException;
import java.util.*;

@SideOnly(Side.CLIENT)
public class BuffConfigGui extends GuiScreen {

    private final GuiScreen parent;
    private final ItemStack swordStack;

    // left: all potion entries
    private final List<PotionEntry> allBuffs = new ArrayList<>();
    // right: added entries (id→level)
    private final LinkedHashMap<String, AddedEntry> addedBuffs = new LinkedHashMap<>();

    private int scrollL, scrollR;
    private int itemH = 18;
    private GuiButton btnConfirm, btnCancel;

    public BuffConfigGui(GuiScreen parent, ItemStack stack) {
        this.parent = parent;
        this.swordStack = stack;

        Map<String, Integer> saved = SwordConfigHelper.getBuffConfig(stack);
        for (Potion p : Potion.REGISTRY) {
            if (p == null) continue;
            String id = p.getRegistryName() != null ? p.getRegistryName().toString() : "";
            if (id.isEmpty()) continue;
            String name = I18n.format(p.getName());
            int lvl = saved.getOrDefault(id, 0);
            if (lvl > 0) addedBuffs.put(id, new AddedEntry(id, name, lvl));
            else allBuffs.add(new PotionEntry(id, name));
        }
        allBuffs.sort(Comparator.comparing(e -> e.display));
    }

    @Override
    public void initGui() {
        int by = height - 30;
        btnConfirm = new GuiButton(100, width / 2 - 55, by, 50, 20, "确认");
        btnCancel  = new GuiButton(101, width / 2 + 5,  by, 50, 20, "返回");
        addButton(btnConfirm);
        addButton(btnCancel);

        // 创建每个已添加条目的输入框
        for (AddedEntry e : addedBuffs.values()) {
            e.field = new GuiTextField(0, fontRenderer, 0, 0, 30, 14);
            e.field.setText(String.valueOf(e.level));
            e.field.setMaxStringLength(5);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == btnConfirm) {
            Map<String, Integer> buffs = new LinkedHashMap<>();
            for (Map.Entry<String, AddedEntry> e : addedBuffs.entrySet()) {
                buffs.put(e.getKey(), e.getValue().level);
            }
            SwordConfigHelper.setBuffConfig(swordStack, buffs);
            NetworkHandler.INSTANCE.sendToServer(new PacketBuffConfig(buffs));
            mc.displayGuiScreen(parent);
        } else if (button == btnCancel) {
            mc.displayGuiScreen(parent);
        }
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();
        FontRenderer fr = fontRenderer;
        int panelW = (width - 40) / 2;
        int lx = 12, rx = lx + panelW + 16;
        int top = 38, bot = height - 38;

        drawCenteredString(fr, "§e✦ Buff 配置 ✦", width / 2, 8, 0x66DDFF);
        drawCenteredString(fr, "§7左键添加  |  输入等级  |  右键/✗ 删除", width / 2, 20, 0xAAAAAA);

        // 列标题
        drawString(fr, "§7◈ 全部 buff", lx + 4, top - 14, 0xEEEE88);
        drawString(fr, "§7◈ 已添加 (" + addedBuffs.size() + ")", rx + 4, top - 14, 0x88EEEE);

        // 面板背景
        drawRect(lx, top, lx + panelW, bot, 0x60000000);
        drawRect(rx, top, rx + panelW, bot, 0x60000000);

        // ---- 左侧：全部 buff ----
        drawScrollableList(lx, top, panelW, bot, allBuffs.size(), scrollL,
                (i, y) -> drawString(fr, "§7" + allBuffs.get(i).display, lx + 4, y + 5, 0xCCCCCC));

        // ---- 右侧：已添加 ----
        List<Map.Entry<String, AddedEntry>> added = new ArrayList<>(addedBuffs.entrySet());
        drawScrollableList(rx, top, panelW, bot, added.size(), scrollR, (i, y) -> {
            AddedEntry e = added.get(i).getValue();
            String name = e.display;
            // 截断长名字
            if (fr.getStringWidth(name) > panelW - 95) {
                while (fr.getStringWidth(name + "..") > panelW - 95 && name.length() > 1)
                    name = name.substring(0, name.length() - 1);
                name += "..";
            }
            drawString(fr, "§f" + name, rx + 4, y + 5, 0xFFFFFF);

            // 输入框
            e.field.x = rx + panelW - 70;
            e.field.y = y + 1;
            e.field.drawTextBox();

            // 删除 [✗]
            int dx = rx + panelW - 20;
            drawCenteredString(fr, "§c✗", dx, y + 5, 0xFF5555);
        });

        super.drawScreen(mx, my, pt);
    }

    /** 通用可滚动列表绘制 */
    private void drawScrollableList(int x, int top, int w, int bot, int total, int scroll, RowDrawer drawer) {
        int maxScroll = Math.max(0, total * itemH - (bot - top));
        if (total == 0) {
            drawCenteredString(fontRenderer, "§8(空)", x + w / 2, top + 20, 0x666666);
            return;
        }
        for (int i = 0; i < total; i++) {
            int y = top + i * itemH - scroll;
            if (y + itemH < top || y > bot) continue;
            drawer.draw(i, y);
        }
        if (maxScroll > 0) {
            int bh = Math.max(16, (bot - top) * (bot - top) / (total * itemH));
            int by = top + (bot - top - bh) * scroll / maxScroll;
            drawRect(x + w - 5, by, x + w - 2, by + bh, 0xAA666666);
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int mb) throws IOException {
        int panelW = (width - 40) / 2;
        int lx = 12, rx = lx + panelW + 16;
        int top = 38, bot = height - 38;

        // 左侧点击 → 添加到已添加
        if (mb == 0 && mx >= lx && mx < lx + panelW && my >= top && my < bot) {
            int idx = (my - top + scrollL) / itemH;
            if (idx >= 0 && idx < allBuffs.size()) {
                PotionEntry e = allBuffs.remove(idx);
                AddedEntry ae = new AddedEntry(e.id, e.display, 1);
                ae.field = new GuiTextField(0, fontRenderer, 0, 0, 30, 14);
                ae.field.setText("1");
                ae.field.setMaxStringLength(5);
                addedBuffs.put(e.id, ae);
                return;
            }
        }

        // 右侧交互
        if (mx >= rx && mx < rx + panelW && my >= top && my < bot) {
            List<Map.Entry<String, AddedEntry>> added = new ArrayList<>(addedBuffs.entrySet());
            int idx = (my - top + scrollR) / itemH;
            if (idx >= 0 && idx < added.size()) {
                AddedEntry e = added.get(idx).getValue();
                // 删除按钮
                int dx = rx + panelW - 20;
                if (mb == 1 || (mx >= dx && mx < dx + 20)) {
                    e.field.setFocused(false);
                    // 放回左侧列表
                    allBuffs.add(new PotionEntry(added.get(idx).getKey(), e.display));
                    allBuffs.sort(Comparator.comparing(a -> a.display));
                    addedBuffs.remove(added.get(idx).getKey());
                    return;
                }
                // 点击输入框
                e.field.mouseClicked(mx, my, mb);
                if (e.field.isFocused()) {
                    // 取消其他输入框的焦点
                    for (AddedEntry other : addedBuffs.values()) {
                        if (other != e) other.field.setFocused(false);
                    }
                }
                return;
            }
        }

        super.mouseClicked(mx, my, mb);
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        // 传递给当前聚焦的输入框
        for (AddedEntry e : addedBuffs.values()) {
            if (e.field.isFocused()) {
                e.field.textboxKeyTyped(c, key);
                try { e.level = Integer.parseInt(e.field.getText()); }
                catch (NumberFormatException ex) { e.level = 0; }
                return;
            }
        }
        super.keyTyped(c, key);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dW = org.lwjgl.input.Mouse.getDWheel();
        if (dW != 0) {
            int panelW = (width - 40) / 2;
            int lx = 12, rx = lx + panelW + 16;
            int top = 38, bot = height - 38;
            int mx = org.lwjgl.input.Mouse.getX() * width / mc.displayWidth;
            int my = height - org.lwjgl.input.Mouse.getY() * height / mc.displayHeight - 1;

            int step = dW / 3;
            if (mx < lx + panelW + 8) {
                int max = Math.max(0, allBuffs.size() * itemH - (bot - top));
                scrollL = Math.max(0, Math.min(scrollL - step, max));
            } else {
                int max = Math.max(0, addedBuffs.size() * itemH - (bot - top));
                scrollR = Math.max(0, Math.min(scrollR - step, max));
            }
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        for (AddedEntry e : addedBuffs.values()) {
            if (e.field != null) e.field.updateCursorCounter();
        }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    // ============================================================

    private static class PotionEntry {
        final String id, display;
        PotionEntry(String id, String display) { this.id = id; this.display = display; }
    }

    private static class AddedEntry {
        final String id, display;
        int level;
        GuiTextField field;
        AddedEntry(String id, String display, int level) {
            this.id = id; this.display = display; this.level = level;
        }
    }

    @FunctionalInterface
    private interface RowDrawer { void draw(int index, int y); }
}
