package yc.ycqin.doth.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import yc.ycqin.doth.network.NetworkHandler;
import yc.ycqin.doth.network.PacketStructureSearch;
import yc.ycqin.doth.network.PacketTeleport;

import java.io.IOException;
import java.util.*;

@SideOnly(Side.CLIENT)
public class StructureFindGui extends GuiScreen {
    private static StructureFindGui activeInstance;
    private final GuiScreen parent;
    private final List<String> structureIds = new ArrayList<>();
    private final List<String> structureNames = new ArrayList<>();
    private GuiTextField customField;
    private int selected = -1, foundX, foundZ; boolean found;
    private String status = "§7选择一个结构或输入自定义名称";
    private static final int ITEM_H = 18;

    public StructureFindGui(GuiScreen p) {
        parent = p;
        add("Village","村庄"); add("Desert_Pyramid","沙漠神殿"); add("Jungle_Pyramid","丛林神庙");
        add("Swamp_Hut","沼泽小屋"); add("Igloo","雪屋"); add("Stronghold","要塞");
        add("Monument","海底神殿"); add("EndCity","末地城"); add("Fortress","下界要塞");
        add("Mansion","林地府邸"); add("Mineshaft","废弃矿井");
    }

    private void add(String id, String name) { structureIds.add(id); structureNames.add(name); }

    @Override
    public void updateScreen() { customField.updateCursorCounter(); }

    @Override
    public void initGui() {
        activeInstance = this;
        buttonList.clear();
        customField = new GuiTextField(0, fontRenderer, width/2-120, 28, 240, 18);

        int sy = 52, total = structureIds.size();
        for (int i = 0; i < total; i++) {
            boolean s = (i == selected);
            int y = sy + i * 20;
            addButton(new GuiButton(100 + i, width / 2 - 120, y, 160, ITEM_H, (s ? "§a▶ " : "") + structureNames.get(i)));
            if (found && i == selected) {
                addButton(new GuiButton(200 + i, width / 2 + 50, y, 80, ITEM_H, "§6✦ 传送"));
            }
        }
        int by = sy + Math.max(total, 1) * 20 + 8;
        addButton(new GuiButton(99, width / 2 - 60, by, 120, 20, "§c返回"));
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == 1) { mc.displayGuiScreen(parent); return; }
        customField.textboxKeyTyped(c, key);
        if (key == 28 && !customField.getText().isEmpty()) { // Enter
            search(customField.getText());
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        customField.mouseClicked(mx, my, btn);
        super.mouseClicked(mx, my, btn);
    }

    private void search(String name) {
        selected = -1; found = false;
        status = "§e搜索中: " + name + "...";
        NetworkHandler.INSTANCE.sendToServer(new PacketStructureSearch(name));
    }

    @Override
    protected void actionPerformed(GuiButton btn) throws IOException {
        if (btn.id == 99) { mc.displayGuiScreen(parent); return; }

        int selIdx = btn.id - 100;
        if (selIdx >= 0 && selIdx < structureIds.size()) {
            selected = selIdx;
            search(structureIds.get(selIdx));
        }
        int tpIdx = btn.id - 200;
        if (tpIdx >= 0 && tpIdx < structureIds.size() && found) {
            NetworkHandler.INSTANCE.sendToServer(new PacketTeleport(foundX, 128, foundZ));
        }
    }

    public static void handleResult(int x, int z, boolean f) {
        if (activeInstance != null) {
            activeInstance.found = f; activeInstance.foundX = x; activeInstance.foundZ = z;
            if (activeInstance.selected >= 0) {
                String name = activeInstance.structureNames.get(activeInstance.selected);
                activeInstance.status = f ? ("§a" + name + " §7→ §bX:" + x + " Z:" + z) : ("§c未找到 " + name);
            } else {
                activeInstance.status = f ? ("§a找到 §7→ §bX:" + x + " Z:" + z) : "§c未找到";
            }
            activeInstance.initGui();
        }
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, "§d◈ 结构查找器", width / 2, 6, 0xFFFFFF);
        drawCenteredString(fontRenderer, status, width / 2, 48, 0xCCCCCC);

        customField.drawTextBox();
        if (customField.getText().isEmpty())
            drawString(fontRenderer, "§8自定义结构名...", width / 2 - 116, 34, 0x666666);

        super.drawScreen(mx, my, pt);
    }

    @Override public boolean doesGuiPauseGame() { return false; }
}
