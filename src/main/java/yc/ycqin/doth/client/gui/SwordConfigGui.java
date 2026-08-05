package yc.ycqin.doth.client.gui;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import yc.ycqin.doth.common.item.BlueCreeperSword;
import yc.ycqin.doth.core.AllreturnConfig;
import yc.ycqin.doth.network.NetworkHandler;
import yc.ycqin.doth.network.PacketFindAllStructures;
import yc.ycqin.doth.network.PacketStructureSearch;
import yc.ycqin.doth.network.PacketSwordConfig;
import yc.ycqin.doth.network.PacketTeleport;
import yc.ycqin.doth.util.BlockHighlightConfig;
import yc.ycqin.doth.util.EntityHighlightConfig;
import yc.ycqin.doth.util.SwordConfigHelper;

import java.io.IOException;
import java.util.*;

@SideOnly(Side.CLIENT)
public class SwordConfigGui extends GuiScreen {
    private final ItemStack swordStack;
    private int panelX, panelY, panelW, panelH, sidebarW=90, contentX, contentY, contentW, contentH, scrollOffset=0, maxScroll=0;
    private boolean attackPassive, attackPlayers, attackAllEntities, enableEnhanced, allReturn, alwaysAttack, tryDropItems, instantMine, magnetDrops, closeNonVanillaGui, enableBuffs, preventRemove, antiDisarm, autoRecreate, collectEntityDrops, rayTrace, purgeNBT, resetBooleans, resetLists;
    private int activeTab = 0; private boolean needsRefresh = true;
    private static SwordConfigGui activeInstance;
    private final List<Row> rows = new ArrayList<>();
    private static final ResourceLocation SWORD_TEX = new ResourceLocation("bluecreepersword","textures/shader/blue_creeper.png");
    private final Random rng = new Random(42);
    // 输入框
    private GuiTextField fRange, fFreq, fRadius, eRange, eFreq, eRadius;
    // 结构查找
    private final List<String> structIds = new ArrayList<>(Arrays.asList("Village","Temple","Temple","Witch_Hut","Igloo","Stronghold","Monument","EndCity","Fortress","Mansion","Mineshaft"));
    private final List<String> structNames = new ArrayList<>(Arrays.asList("村庄","沙漠神殿","丛林神庙","沼泽小屋","雪屋","要塞","海底神殿","末地城","下界要塞","林地府邸","废弃矿井"));
    private boolean structListLoaded;
    private int structFoundX, structFoundZ, lastSearchedStruct = -1; private String structStatus = "§7选择结构或输入自定义名";
    private boolean structFound;

    public SwordConfigGui(ItemStack s) {
        swordStack=s;
        attackPassive=SwordConfigHelper.isAttackPassive(s); attackPlayers=SwordConfigHelper.isAttackPlayers(s); attackAllEntities=SwordConfigHelper.isAttackAllEntities(s);
        enableEnhanced=SwordConfigHelper.isEnhancedEnabled(s); allReturn=SwordConfigHelper.isAllReturnEnabled(s); alwaysAttack=SwordConfigHelper.isAlwaysAttack(s);
        tryDropItems=SwordConfigHelper.isTryDropItems(s); instantMine=SwordConfigHelper.isInstantMine(s); magnetDrops=SwordConfigHelper.isMagnetDrops(s);
        closeNonVanillaGui=SwordConfigHelper.isCloseNonVanillaGui(s); enableBuffs=SwordConfigHelper.isEnableBuffs(s); preventRemove=SwordConfigHelper.isPreventRemove(s); antiDisarm=SwordConfigHelper.isAntiDisarm(s); autoRecreate=SwordConfigHelper.isAutoRecreate(s); collectEntityDrops=SwordConfigHelper.isCollectEntityDrops(s); rayTrace=SwordConfigHelper.isRayTrace(s); purgeNBT=SwordConfigHelper.isPurgeNBT(s); resetBooleans=SwordConfigHelper.isResetBooleans(s); resetLists=SwordConfigHelper.isResetLists(s);
    }

    public void initGui() {
        panelW=Math.min(420,width-40); panelH=Math.min(300,height-60); panelX=(width-panelW)/2; panelY=(height-panelH)/2-5;
        contentX=panelX+sidebarW; contentY=panelY+22; contentW=panelW-sidebarW; contentH=panelH-22;
        buttonList.clear(); addButton(new GuiButton(100,panelX+panelW/2-56,panelY+panelH-24,52,18,"§a✓ 确认")); addButton(new GuiButton(101,panelX+panelW/2+4,panelY+panelH-24,52,18,"§c✗ 取消"));
        fRange = new GuiTextField(0, fontRenderer, 0, 0, 50, 16);
        fFreq = new GuiTextField(1, fontRenderer, 0, 0, 50, 16);
        fRadius = new GuiTextField(2, fontRenderer, 0, 0, 50, 16);
        eRange = new GuiTextField(3, fontRenderer, 0, 0, 50, 16);
        eFreq = new GuiTextField(4, fontRenderer, 0, 0, 50, 16);
        eRadius = new GuiTextField(5, fontRenderer, 0, 0, 50, 16);
        rebuild(); }

    private void rebuild() {
        rows.clear();
        if(activeTab==0){rows.add(new Row("攻击非敌对",attackPassive));rows.add(new Row("攻击玩家",attackPlayers));rows.add(new Row("攻击所有实体",attackAllEntities));rows.add(new Row("增强攻击",enableEnhanced));rows.add(new Row("射线追踪",rayTrace));rows.add(new Row("持续攻击",alwaysAttack));rows.add(new Row("清理世界NBT",purgeNBT));rows.add(new Row("重置全局布尔值",resetBooleans));rows.add(new Row("重置全局列表",resetLists));rows.add(new Row("Allreturn",allReturn));rows.add(new Row("阻止实体移除",preventRemove));}
        else if(activeTab==1){rows.add(new Row("尝试掉东西",tryDropItems));rows.add(new Row("秒挖任何方块",instantMine));rows.add(new Row("掉落直进包",magnetDrops));rows.add(new Row("收集掉落物",collectEntityDrops));rows.add(new Row("关闭mod画面",closeNonVanillaGui));rows.add(new Row("防缴械",antiDisarm));rows.add(new Row("快照补剑",autoRecreate));rows.add(new Row("§d✦ Buff增强",enableBuffs));}
        else if(activeTab==2){rows.add(new Row("启用高亮 "+(BlockHighlightConfig.enabled?"§aON":"§8OFF")));rows.add(new Row("一键挖掘 "+(BlockHighlightConfig.mineAllEnabled?"§aON":"§8OFF")));rows.add(new Row("搜索范围", "f_range"));rows.add(new Row("更新频率", "f_freq"));rows.add(new Row("挖掘半径", "f_radius"));rows.add(new Row("§d✦ 管理方块列表","",""));rows.add(new Row("§c清空高亮列表"));}
        else if(activeTab==3){for(int i=0;i<structIds.size();i++)rows.add(new Row((structFound && i==lastSearchedStruct?"§a▶ ":"")+structNames.get(i)));}
        else if(activeTab==4){rows.add(new Row("启用高亮 "+(EntityHighlightConfig.enabled?"§aON":"§8OFF")));rows.add(new Row("一键攻击 "+(EntityHighlightConfig.attackAllEnabled?"§aON":"§8OFF")));rows.add(new Row("搜索范围", "e_range"));rows.add(new Row("更新频率", "e_freq"));rows.add(new Row("攻击半径", "e_radius"));rows.add(new Row("§d✦ 管理实体列表","",""));rows.add(new Row("§c清空高亮列表"));rows.add(new Row("§6✦ 管理攻击白名单 §7(" + EntityHighlightConfig.attackWhitelist.size() + ")","",""));rows.add(new Row("§c清空白名单"));}
        int th=rows.size()*24; maxScroll=Math.max(0,th-contentH+10); if(scrollOffset>maxScroll)scrollOffset=maxScroll; if(scrollOffset<0)scrollOffset=0;
    }

    public void updateScreen() {
        fRange.updateCursorCounter(); fFreq.updateCursorCounter(); fRadius.updateCursorCounter();
        eRange.updateCursorCounter(); eFreq.updateCursorCounter(); eRadius.updateCursorCounter();
    }

    protected void keyTyped(char c, int key) throws IOException {
        if (activeTab == 2) {
            if (fRange.isFocused()) { fRange.textboxKeyTyped(c, key); if (key==28) syncField("f_range"); return; }
            if (fFreq.isFocused()) { fFreq.textboxKeyTyped(c, key); if (key==28) syncField("f_freq"); return; }
            if (fRadius.isFocused()) { fRadius.textboxKeyTyped(c, key); if (key==28) syncField("f_radius"); return; }
        }
        if (activeTab == 4) {
            if (eRange.isFocused()) { eRange.textboxKeyTyped(c, key); if (key==28) syncField("e_range"); return; }
            if (eFreq.isFocused()) { eFreq.textboxKeyTyped(c, key); if (key==28) syncField("e_freq"); return; }
            if (eRadius.isFocused()) { eRadius.textboxKeyTyped(c, key); if (key==28) syncField("e_radius"); return; }
        }
        if (key == 1) { mc.displayGuiScreen(null); return; }
        super.keyTyped(c, key);
    }

    private void syncField(String id) {
        try {
            if ("f_range".equals(id)) BlockHighlightConfig.searchRange = Math.max(1, Math.min(128, Integer.parseInt(fRange.getText())));
            else if ("f_freq".equals(id)) BlockHighlightConfig.updateFrequency = Math.max(1, Math.min(200, Integer.parseInt(fFreq.getText())));
            else if ("f_radius".equals(id)) BlockHighlightConfig.mineAllRadius = Math.max(1, Math.min(128, Integer.parseInt(fRadius.getText())));
            else if ("e_range".equals(id)) EntityHighlightConfig.searchRange = Math.max(1, Math.min(128, Integer.parseInt(eRange.getText())));
            else if ("e_freq".equals(id)) EntityHighlightConfig.updateFrequency = Math.max(1, Math.min(200, Integer.parseInt(eFreq.getText())));
            else if ("e_radius".equals(id)) EntityHighlightConfig.attackRadius = Math.max(1, Math.min(128, Integer.parseInt(eRadius.getText())));
        } catch (NumberFormatException ignored) {}
    }

    public void handleMouseInput() throws IOException {
        int w=org.lwjgl.input.Mouse.getDWheel(); if(w!=0){scrollOffset-=Integer.signum(w)*13;if(scrollOffset<0)scrollOffset=0;if(scrollOffset>maxScroll)scrollOffset=maxScroll;}
        super.handleMouseInput();
    }

    protected void mouseClicked(int mx,int my,int btn) throws IOException {
        if (activeTab == 2) {
            fRange.mouseClicked(mx, my, btn); fFreq.mouseClicked(mx, my, btn); fRadius.mouseClicked(mx, my, btn);
            fRange.setFocused(false); fFreq.setFocused(false); fRadius.setFocused(false);
        }
        if (activeTab == 4) {
            eRange.mouseClicked(mx, my, btn); eFreq.mouseClicked(mx, my, btn); eRadius.mouseClicked(mx, my, btn);
            eRange.setFocused(false); eFreq.setFocused(false); eRadius.setFocused(false);
        }
        super.mouseClicked(mx,my,btn);
        // 标题栏传送按钮（在 contentY 上方）
        if (activeTab == 3 && structFound) {
            int bx = contentX + contentW - 46, by = panelY + 4;
            if (mx >= bx && my >= by && mx < bx + 40 && my < by + 16) {
                NetworkHandler.INSTANCE.sendToServer(new PacketTeleport(structFoundX, 128, structFoundZ));
                return;
            }
        }
        if(mx>=panelX+4&&mx<panelX+sidebarW){for(int i=0;i<5;i++){int ty=panelY+4+i*26;if(my>=ty&&my<ty+22){if(activeTab!=i){activeTab=i;rebuild();if(!structListLoaded){structListLoaded=true;NetworkHandler.INSTANCE.sendToServer(new PacketStructureSearch("$LIST"));}}}}return;}
        if(mx>=contentX&&mx<contentX+contentW&&my>=contentY&&my<contentY+contentH){
            int idx=(my-contentY+scrollOffset)/24; if(idx<0||idx>=rows.size())return;
            Row r=rows.get(idx);
            if (activeTab == 2 && r.fieldId != null) {
                // click on input field row
                int fieldX = contentX + contentW - 60;
                int fieldY = contentY + idx * 24 - scrollOffset;
                if (mx >= fieldX && mx < fieldX + 50) {
                    GuiTextField target = null;
                    if ("f_range".equals(r.fieldId)) target = fRange;
                    else if ("f_freq".equals(r.fieldId)) target = fFreq;
                    else if ("f_radius".equals(r.fieldId)) target = fRadius;
                    if (target != null) {
                        target.setFocused(true);
                        target.x = fieldX; target.y = fieldY + 3;
                    }
                }
                return;
            }
            if (activeTab == 4 && r.fieldId != null) {
                int fieldX = contentX + contentW - 60;
                int fieldY = contentY + idx * 24 - scrollOffset;
                if (mx >= fieldX && mx < fieldX + 50) {
                    GuiTextField target = null;
                    if ("e_range".equals(r.fieldId)) target = eRange;
                    else if ("e_freq".equals(r.fieldId)) target = eFreq;
                    else if ("e_radius".equals(r.fieldId)) target = eRadius;
                    if (target != null) {
                        target.setFocused(true);
                        target.x = fieldX; target.y = fieldY + 3;
                    }
                }
                return;
            }
            if(activeTab==0){
                r.on = !r.on;
                switch(idx){
                    case 0:
                        attackPassive=r.on;
                        break;
                    case 1:
                        attackPlayers=r.on;
                        break;
                    case 2:
                        attackAllEntities=r.on;
                        break;
                    case 3:
                        enableEnhanced=r.on;
                        break;
                    case 4:
                        rayTrace=r.on;
                        break;
                    case 5:
                        alwaysAttack=r.on;
                        break;
                    case 6:
                        purgeNBT=r.on;
                        break;
                    case 7:
                        resetBooleans=r.on;
                        break;
                    case 8:
                        resetLists=r.on;
                        break;
                    case 9:
                        allReturn=r.on;
                        break;
                    case 10:
                        preventRemove=r.on;
                        break;
                }
            } else if(activeTab==1){
                r.on=!r.on;
                switch(idx){
                case 0:
                    tryDropItems=r.on;
                    break;
                case 1:
                    instantMine=r.on;
                    break;
                case 2:
                    magnetDrops=r.on;
                    break;
                case 3:
                    collectEntityDrops=r.on;
                    break;
                case 4:
                    closeNonVanillaGui=r.on;
                    break;
                case 5:
                    antiDisarm=r.on;
                    break;
                case 6:
                    autoRecreate=r.on;
                    break;
                case 7:
                    mc.displayGuiScreen(new BuffConfigGui(this,swordStack));
                    break;
                }
            } else if (activeTab == 2) {
                if (idx==0) {BlockHighlightConfig.toggle();rebuild();}
                else if (idx==1) {BlockHighlightConfig.mineAllEnabled=!BlockHighlightConfig.mineAllEnabled;rebuild();}
                else if (idx==5) {mc.displayGuiScreen(new BlockSearchGui(this));}
                else if (idx==6) {BlockHighlightConfig.highlightBlocks.clear();rebuild();}
            } else if (activeTab == 4) {
                if (idx==0) {EntityHighlightConfig.toggle();rebuild();}
                else if (idx==1) {EntityHighlightConfig.attackAllEnabled=!EntityHighlightConfig.attackAllEnabled;rebuild();}
                else if (idx==5) {mc.displayGuiScreen(new EntitySearchGui(this));}
                else if (idx==6) {EntityHighlightConfig.highlightEntities.clear();rebuild();}
                else if (idx==7) {mc.displayGuiScreen(new EntitySearchGui(this, true));}
                else if (idx==8) {EntityHighlightConfig.attackWhitelist.clear();rebuild();}
            } else if (activeTab == 3) {
                int si = idx;
                if (si >= 0 && si < structIds.size()) {
                    if (btn == 1) {
                        structStatus = "§e全搜索: " + structNames.get(si) + "...";
                        NetworkHandler.INSTANCE.sendToServer(new PacketFindAllStructures(structIds.get(si)));
                    } else {
                        lastSearchedStruct = si;
                        structStatus = "§e搜索: " + structNames.get(si) + "...";
                        NetworkHandler.INSTANCE.sendToServer(new PacketStructureSearch(structIds.get(si)));
                    }
                }
                rebuild();
            }
        }
    }

    protected void actionPerformed(GuiButton b) throws IOException {
        if(b.id==100){NetworkHandler.INSTANCE.sendToServer(new PacketSwordConfig(attackPassive,attackPlayers,attackAllEntities,enableEnhanced,allReturn,alwaysAttack,tryDropItems,instantMine,magnetDrops,closeNonVanillaGui,enableBuffs,preventRemove,antiDisarm,autoRecreate,collectEntityDrops,rayTrace,purgeNBT,resetBooleans,resetLists));mc.displayGuiScreen(null);}
        else if(b.id==101)mc.displayGuiScreen(null);
    }

    public void drawScreen(int mx,int my,float pt){
        activeInstance = this;
        drawDefaultBackground();if(needsRefresh){rebuild();needsRefresh=false;}

        // sync field texts
        if (!fRange.isFocused()) fRange.setText(String.valueOf(BlockHighlightConfig.searchRange));
        if (!fFreq.isFocused()) fFreq.setText(String.valueOf(BlockHighlightConfig.updateFrequency));
        if (!fRadius.isFocused()) fRadius.setText(String.valueOf(BlockHighlightConfig.mineAllRadius));
        if (!eRange.isFocused()) eRange.setText(String.valueOf(EntityHighlightConfig.searchRange));
        if (!eFreq.isFocused()) eFreq.setText(String.valueOf(EntityHighlightConfig.updateFrequency));
        if (!eRadius.isFocused()) eRadius.setText(String.valueOf(EntityHighlightConfig.attackRadius));

        GlStateManager.enableBlend();
        drawRect(panelX+1,panelY,panelX+panelW-1,panelY+panelH,0xE0101525);drawRect(panelX,panelY+1,panelX+panelW,panelY+panelH-1,0xE0101525);
        drawRect(panelX,panelY,panelX+panelW,panelY+2,0xFF5B6EE1);drawRect(panelX,panelY+panelH-1,panelX+panelW,panelY+panelH,0xFF2A2D40);
        drawBorderGlow(); drawCenteredString(fontRenderer,"§b§l蓝 C 的 小 剑 剑  §7⚙ 配置",panelX+panelW/2,panelY-18,0xFFFFFF);
        drawRect(panelX,panelY+2,panelX+sidebarW,panelY+panelH-1,0xC00D1120);drawVerticalLine(panelX+sidebarW,panelY+2,panelY+panelH-1,0xFF2A2D40);
        drawTab("⚔ 攻击设置",0,activeTab==0);drawTab("✦ 功能设置",1,activeTab==1);drawTab("◈ 方块高亮",2,activeTab==2);drawTab("⌂ 结构查找",3,activeTab==3);drawTab("◉ 实体高亮",4,activeTab==4);
        drawStarfield(contentX,contentY,contentW,contentH);drawRect(contentX,panelY,contentX+contentW,panelY+22,0xC0141830);
        String hdr = "§e⌂ 结构查找";
        if (activeTab==0) hdr = "§c⚔ 攻击设置"; else if (activeTab==1) hdr = "§b✦ 功能设置"; else if (activeTab==2) hdr = "§d◈ 方块高亮";
        else if (activeTab==3 && structFound) hdr += "  §7→§a X:"+structFoundX+" Z:"+structFoundZ;
        if (activeTab==4) hdr = "§5◉ 实体高亮";
        drawString(fontRenderer,hdr,contentX+10,panelY+7,0xFFFFFF);
        int rY=contentY-scrollOffset;
        for(int i=0;i<rows.size();i++){
            int rowY=rY+i*24; if(rowY+24<contentY||rowY>contentY+contentH) continue;
            Row r=rows.get(i);
            if (activeTab == 2 && r.fieldId != null) {
                // draw label
                drawString(fontRenderer, "§f"+r.label, contentX+10, rowY+7, 0xEEEEFF);
                // draw text field
                int fx = contentX + contentW - 60;
                GuiTextField tf = null;
                if ("f_range".equals(r.fieldId)) tf = fRange;
                else if ("f_freq".equals(r.fieldId)) tf = fFreq;
                else if ("f_radius".equals(r.fieldId)) tf = fRadius;
                if (tf != null && !tf.isFocused()) { tf.x = fx; tf.y = rowY + 3; }
                if (tf != null && tf.x == fx) tf.drawTextBox();
            } else if (activeTab == 4 && r.fieldId != null) {
                drawString(fontRenderer, "§f"+r.label, contentX+10, rowY+7, 0xEEEEFF);
                int fx = contentX + contentW - 60;
                GuiTextField tf = null;
                if ("e_range".equals(r.fieldId)) tf = eRange;
                else if ("e_freq".equals(r.fieldId)) tf = eFreq;
                else if ("e_radius".equals(r.fieldId)) tf = eRadius;
                if (tf != null && !tf.isFocused()) { tf.x = fx; tf.y = rowY + 3; }
                if (tf != null && tf.x == fx) tf.drawTextBox();
            } else {
                drawRow(r,contentX,rowY,contentW,mx,my);
            }
        }
        if(maxScroll>0){int sbX=contentX+contentW-4,sbH=(int)((float)contentH/(maxScroll+contentH)*contentH),sbY=contentY+(int)((float)scrollOffset/maxScroll*(contentH-sbH));drawRect(sbX,sbY,sbX+3,sbY+sbH,0x805B6EE1);}
        // 结构传送按钮（标题栏右侧）
        if (activeTab == 3 && structFound) {
            int bx = contentX + contentW - 46, by = panelY + 4;
            if (mx >= bx && my >= by && mx < bx + 40 && my < by + 16)
                drawRect(bx, by, bx + 40, by + 16, 0xC03040A0);
            drawCenteredString(fontRenderer, "§6✦传送", bx + 20, by + 3, 0xFFFF55);
        }
        drawCenteredString(fontRenderer,"§7"+AllreturnConfig.getStatusDescription(),panelX+panelW/2,panelY+panelH+6,0x888888);
        EntityPlayer p=mc.player; boolean hs=false; for(ItemStack s:p.inventory.mainInventory)if(s.getItem()instanceof BlueCreeperSword){hs=true;break;}
        drawCenteredString(fontRenderer,hs?"§a✦ 飞行已激活":"§8飞行（需剑在背包）",panelX+panelW/2,panelY+panelH+16,0xAAAAAA);

        super.drawScreen(mx,my,pt);
    }

    private void drawBorderGlow(){
        long t=System.nanoTime()/50000000L; float hue=0.55f+0.2f*(float)Math.abs(Math.sin(t*0.015));
        int rgb=MathHelper.hsvToRGB(hue,0.7f,0.95f); int r=(rgb>>16)&0xFF,g=(rgb>>8)&0xFF,b=rgb&0xFF;
        GlStateManager.disableTexture2D();GlStateManager.enableBlend();GlStateManager.blendFunc(770,1);
        int[][] sp={{panelX+4,panelY+4},{panelX+panelW/2,panelY},{panelX+panelW-4,panelY+4},{panelX+4,panelY+panelH/2},{panelX+panelW-4,panelY+panelH/2},{panelX+4,panelY+panelH-4},{panelX+panelW/2,panelY+panelH},{panelX+panelW-4,panelY+panelH-4}};
        float[][] dr={{-1,-1},{0,-1},{1,-1},{-1,0},{1,0},{-1,1},{0,1},{1,1}};
        for(int i=0;i<sp.length;i++) for(int j=0;j<3;j++){rng.setSeed(i*100L+j*37L+t);float l=15+rng.nextFloat()*40,ex=sp[i][0]+dr[i][0]*l+(rng.nextFloat()-0.5f)*20,ey=sp[i][1]+dr[i][1]*l+(rng.nextFloat()-0.5f)*20;
            int a=(int)(40*(0.4f+0.6f*(float)Math.abs(Math.sin((t+i)*0.08)))); Tessellator tes=Tessellator.getInstance(); BufferBuilder vb=tes.getBuffer();
            vb.begin(GL11.GL_TRIANGLE_FAN,DefaultVertexFormats.POSITION_COLOR); vb.pos(sp[i][0],sp[i][1],0).color(r,g,b,a).endVertex(); vb.pos(ex-2,ey-2,0).color(r,g,b,0).endVertex(); vb.pos(ex+2,ey-2,0).color(r,g,b,0).endVertex(); vb.pos(ex+2,ey+2,0).color(r,g,b,0).endVertex(); vb.pos(ex-2,ey+2,0).color(r,g,b,0).endVertex(); tes.draw();
        } GlStateManager.depthMask(true);GlStateManager.blendFunc(770,771);GlStateManager.disableBlend();GlStateManager.enableTexture2D();
    }

    private void drawStarfield(int x,int y,int w,int h){
        drawRect(x,y,x+w,y+h,0xFF020418);long t=System.nanoTime()/40000000L;Random rng2=new Random();GlStateManager.disableTexture2D();GlStateManager.enableBlend();GlStateManager.blendFunc(770,1);
        for(int l=0;l<5;l++){int c=20-l*3;float hue=0.55f+0.2f*(float)Math.abs(Math.sin((t+l*80)*0.015));int rgb=MathHelper.hsvToRGB(hue,0.7f,0.95f);
            for(int i=0;i<c;i++){rng2.setSeed((l*1000L+i*137L)^(t/(5+l)));int sx=x+Math.abs(rng2.nextInt())%w,sy=y+Math.abs(rng2.nextInt())%h;
            float tw=0.2f+0.8f*(float)Math.abs(Math.sin((t+i*3+l*50)*(0.02+l*0.005)));int a=(int)((0.25f+l*0.12f)*255*tw);if(a<5)a=5;drawRect(sx,sy,sx+1,sy+1,(a<<24)|((rgb>>16)&0xFF)<<16|((rgb>>8)&0xFF)<<8|(rgb&0xFF));}}
        GlStateManager.blendFunc(770,771);GlStateManager.disableBlend();GlStateManager.enableTexture2D();
        mc.getTextureManager().bindTexture(SWORD_TEX);GlStateManager.enableBlend();GlStateManager.color(1F,1F,1F,0.12f);
        Tessellator tes=Tessellator.getInstance();BufferBuilder vb=tes.getBuffer();vb.begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_TEX);vb.pos(x,y+h,0).tex(0,1).endVertex();vb.pos(x+w,y+h,0).tex(1,1).endVertex();vb.pos(x+w,y,0).tex(1,0).endVertex();vb.pos(x,y,0).tex(0,0).endVertex();tes.draw();
        GlStateManager.color(1F,1F,1F,1F);GlStateManager.disableBlend();
    }

    private void drawTab(String l,int i,boolean a){int tx=panelX+4,ty=panelY+4+i*26,tw=sidebarW-8,th=22;if(a){drawGradientRect(tx,ty,tx+tw,ty+th,0xA05B6EE1,0x40304080);drawRect(tx,ty,tx+2,ty+th,0xFF5B6EE1);}drawString(fontRenderer,l,tx+8,ty+7,a?0xFFFFFF:0x666688);}
    private void drawRow(Row r,int rx,int ry,int rw,int mx,int my){boolean h=mx>=rx&&my>=ry&&mx<rx+rw&&my<ry+24;if(h)drawRect(rx,ry,rx+rw,ry+24,0x20202050);drawString(fontRenderer,(r.toggle?(r.on?"  §a● ON ":"  §8○ OFF"):"")+"§f"+r.label,rx+10,ry+7,0xEEEEFF);drawRect(rx+6,ry+23,rx+rw-6,ry+24,0x101A1A40);}
    public boolean doesGuiPauseGame(){return false;}
    
    public static void handleStructResult(int x, int z, boolean f) {
        if (activeInstance != null) {
            activeInstance.structFoundX = x; activeInstance.structFoundZ = z;
            activeInstance.structFound = f;
            activeInstance.structStatus = f ? ("§aX:" + x + " Z:" + z) : "§c未找到";
            activeInstance.rebuild();
        }
    }

    public static void addStructOption(String id) {
        if (activeInstance != null && !activeInstance.structIds.contains(id)) {
            activeInstance.structIds.add(id);
            activeInstance.structNames.add(id);
            activeInstance.rebuild();
        }
    }
    private static class Row{String label,fieldId;boolean on,toggle;Row(String l,boolean o){label=l;on=o;toggle=true;}Row(String l){label=l;on=false;toggle=false;}Row(String l,String a,String b){this(l);}Row(String l,String fieldId){label=l;this.fieldId=fieldId;on=false;toggle=false;}}
}
