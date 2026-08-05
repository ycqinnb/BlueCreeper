package yc.ycqin.doth.core;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import yc.ycqin.doth.common.item.BlueCreeperSword;
import yc.ycqin.doth.common.item.ItemReg;
import yc.ycqin.doth.event.SwordMagnetHandler;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import yc.ycqin.doth.event.TooltipRenderer;
import yc.ycqin.doth.util.EnhancedAttackManager;
import yc.ycqin.doth.util.EntityDeletionHelper;
import yc.ycqin.doth.util.SwordConfigHelper;

import static yc.ycqin.doth.event.SwordMagnetHandler.hasActualSwordInInventory;

public class ProtectHelper {

    // ============ EventBus 持续保护 ============

    /** 首次替换 EVENT_BUS */
    public static void initEventBus() {
        if (MinecraftForge.EVENT_BUS instanceof DOTHEventBus) return;
        setEventBus(new DOTHEventBus());
        System.out.println("[DOTH] Replaced EVENT_BUS with DOTHEventBus");
    }

    /**
     * 设置 EVENT_BUS，保留所有已注册的监听器。
     */
    @SuppressWarnings("unchecked")
    private static void setEventBus(EventBus bus) {
        try {
            Field field = MinecraftForge.class.getDeclaredField("EVENT_BUS");
            field.setAccessible(true);
            EventBus old = (EventBus) field.get(null);
            if (old != null) {
                // 复制 busID 和 listeners → 新 bus 拥有旧 bus 的全部监听器
                Field listenersField = EventBus.class.getDeclaredField("listeners");
                listenersField.setAccessible(true);
                listenersField.set(bus, listenersField.get(old));
                Field busIdField = EventBus.class.getDeclaredField("busID");
                busIdField.setAccessible(true);
                busIdField.set(bus, busIdField.get(old));
            }
            field.set(null, bus);
        } catch (Exception e) {
            // AT 没生效或字段名不对：强行去 final 再试
            try {
                Field field = MinecraftForge.class.getDeclaredField("EVENT_BUS");
                Field mod = Field.class.getDeclaredField("modifiers");
                mod.setAccessible(true);
                mod.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                field.setAccessible(true);
                EventBus old = (EventBus) field.get(null);
                if (old != null) {
                    Field lf = EventBus.class.getDeclaredField("listeners");
                    lf.setAccessible(true);
                    lf.set(bus, lf.get(old));
                    Field bf = EventBus.class.getDeclaredField("busID");
                    bf.setAccessible(true);
                    bf.set(bus, bf.get(old));
                }
                field.set(null, bus);
            } catch (Exception e2) {
                System.err.println("[DOTH] Failed to set EVENT_BUS: " + e2);
            }
        }
    }

    /** 如果配置要求替换但实际不是我们的 EventBus，重新替换 */
    public static void ensureEventBus() {
        if (!DOTHConfig.replaceEventBus) return;
        if (MinecraftForge.EVENT_BUS instanceof DOTHEventBus) return;
        System.out.println("[DOTH] ensureEventBus: current bus is " + MinecraftForge.EVENT_BUS.getClass().getName() + ", re-replacing...");
        setEventBus(new DOTHEventBus());
        System.out.println("[DOTH] ensureEventBus: after replace, bus is " + MinecraftForge.EVENT_BUS.getClass().getName());
    }

    /** 每 tick 确保所有世界的 loadedEntityList 已替换为 DOTHEntityList */
    public static void ensureEntityList(net.minecraft.server.MinecraftServer server) {
        if (!DOTHConfig.replaceEntityList) return;
        for (World world : server.worlds) {
            if (world != null && !world.isRemote) {
                DOTHEntityList.replaceIfNeeded(world);
            }
        }
    }

    // ============ EventBus.post() 字节码替换用的安全方法 ============

    public static final java.lang.reflect.Field EVENTBUS_BUSID;
    static {
        java.lang.reflect.Field f = null;
        try { f = EventBus.class.getDeclaredField("busID"); f.setAccessible(true); }
        catch (Exception ignored) {}
        EVENTBUS_BUSID = f;
    }

    @SuppressWarnings("unchecked")
    public static boolean safePost(EventBus bus, Event event) {
        return DOTHEventBus.postnnn(bus, event);
    }

    // ============ onServerTick ============

    public static boolean onEntityJoinWorld(Entity entity) {
        if (entity == null || entity.world == null) return true;
        if (entity instanceof EntityPlayer) return true;
        if (EnhancedAttackManager.isClassMarkedUniversal(entity.getClass().getName())) return false;
        return true;
    }

    public static void onServerTick(MinecraftServer server) {
        ensureEventBus();
        ensureEntityList(server);
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            if (SwordMagnetHandler.hasSwordInInventory(player)) {
                player.isDead = false;
                player.deathTime = 0;
                player.hurtTime = 0;
                player.setHealth(player.getMaxHealth());
                if (!player.world.loadedEntityList.contains(player))
                    player.world.loadedEntityList.add(player);
                if (!player.world.playerEntities.contains(player))
                    player.world.playerEntities.add(player);
                if (AntiDisarmTracker.isProtected(player)) {
                    AntiDisarmTracker.ConfigSnapshot cfg = AntiDisarmTracker.getConfig(player);
                    if (cfg != null && cfg.autoRecreate && !hasActualSwordInInventory(player)) {
                        ItemStack newSword = new ItemStack(ItemReg.BLUE_CREEPER_SWORD);
                        SwordConfigHelper.applySnapshot(newSword, cfg, player.getUniqueID());
                        player.inventory.mainInventory.set(player.inventory.currentItem, newSword);
                    }
                }
            }
        }
        if (!EnhancedAttackManager.isActive() || !EnhancedAttackManager.isIsAlwaysAttack()) return;
        for (World world : server.worlds) {
            if (world == null || world.isRemote) continue;
            for (Entity entity : new ArrayList<>(EnhancedAttackManager.getPendingEntities())) {
                if (entity != null && entity.world != null)
                    EntityDeletionHelper.deleteEntity(entity, null, false, null);
            }
        }
    }

    public static boolean onUpdate(EntityLivingBase base) {
        if (base instanceof EntityPlayer && SwordMagnetHandler.hasSwordInInventory((EntityPlayer) base)) {
            base.isDead = false;
            base.deathTime = 0;
            base.hurtTime = 0;
            EntityPlayer player = (EntityPlayer) base;
            if (AntiDisarmTracker.isProtected(player)) {
                AntiDisarmTracker.ConfigSnapshot cfg = AntiDisarmTracker.getConfig(player);
                if (cfg != null && cfg.autoRecreate && !hasActualSwordInInventory(player)) {
                    ItemStack newSword = new ItemStack(ItemReg.BLUE_CREEPER_SWORD);
                    SwordConfigHelper.applySnapshot(newSword, cfg, player.getUniqueID());
                    player.inventory.mainInventory.set(player.inventory.currentItem, newSword);
                }
            }
            return true;
        }
        return !EnhancedAttackManager.getPendingEntities().contains(base);
    }

    public static boolean isEntityAlive(EntityLivingBase base) {
        if (base instanceof EntityPlayer && SwordMagnetHandler.hasSwordInInventory((EntityPlayer) base)) return true;
        if (EnhancedAttackManager.getPendingEntities().contains(base)) return false;
        return !base.isDead && base.getHealth() > 0.0F;
    }

    public static float getMaxHealth(EntityLivingBase base) {
        if (base instanceof EntityPlayer && SwordMagnetHandler.hasSwordInInventory((EntityPlayer) base)) {
            base.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).removeAllModifiers();
            return 20;
        }
        if (EnhancedAttackManager.getPendingEntities().contains(base)) return 0;
        return (float) base.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getAttributeValue();
    }

    public static float getHealth(EntityLivingBase self) {
        if (self instanceof EntityPlayer && SwordMagnetHandler.hasSwordInInventory((EntityPlayer) self)) return 20;
        if (EnhancedAttackManager.getPendingEntities().contains(self)) return 0;
        try {
            Field f = EntityLivingBase.class.getDeclaredField("field_184632_c");
            f.setAccessible(true);
            return self.getDataManager().get((DataParameter<Float>) f.get(null));
        } catch (Exception e) {
            return self.getHealth();
        }
    }

    public static void setDead(EntityPlayer player) {
        if (SwordMagnetHandler.hasSwordInInventory(player)) {
            player.isDead = false;
            player.deathTime = 0;
            player.hurtTime = 0;
            player.setHealth(player.getMaxHealth());
            return;
        }
        player.isDead = true;
        player.inventoryContainer.onContainerClosed(player);
        if (player.openContainer != null) player.openContainer.onContainerClosed(player);
    }

    public static boolean onItemUpdate(ItemStack itemStack) {
        return !AllreturnConfig.isEnabled() || itemStack.getItem() instanceof BlueCreeperSword;
    }

    public static boolean onRemoveEntity(Entity entity) {
        if (!(entity instanceof EntityPlayer)) return true;
        EntityPlayer p = (EntityPlayer) entity;
        AntiDisarmTracker.ConfigSnapshot cfg = AntiDisarmTracker.getConfig(p);
        if (cfg != null) return !cfg.preventRemove;
        if (p.inventory == null) return true;
        for (ItemStack s : p.inventory.mainInventory)
            if (s.getItem() instanceof BlueCreeperSword && SwordConfigHelper.isPreventRemove(s)) return false;
        return true;
    }

    public static void onEntityUpdatePre(Entity entity) {
        if (entity instanceof EntityPlayer && SwordMagnetHandler.hasSwordInInventory((EntityPlayer) entity)) {
            entity.isDead = false;
            ((EntityPlayer) entity).deathTime = 0;
            ((EntityPlayer) entity).hurtTime = 0;
        }
    }

    public static void onEntityUpdatePost(Entity entity) {
        if (entity instanceof EntityPlayer && SwordMagnetHandler.hasSwordInInventory((EntityPlayer) entity) && entity.isDead) {
            entity.isDead = false;
            ((EntityPlayer) entity).deathTime = 0;
            ((EntityPlayer) entity).hurtTime = 0;
        }
    }

    public static void onLivingUpdatePost(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer && SwordMagnetHandler.hasSwordInInventory((EntityPlayer) entity) && entity.isDead) {
            entity.isDead = false;
            ((EntityPlayer) entity).deathTime = 0;
            ((EntityPlayer) entity).hurtTime = 0;
        }
    }

    public static void onWorldUpdatePre(World world) {
        for (Entity e : world.loadedEntityList)
            if (e instanceof EntityPlayer && SwordMagnetHandler.hasSwordInInventory((EntityPlayer) e)) e.isDead = false;
    }

    // ============ 防缴械增强：防 clear ============

    /**
     * 返回 true → 阻止清空背包
     */
    public static boolean shouldPreventClear(EntityPlayer player) {
        if (AntiDisarmTracker.isProtected(player))
            return true;
        for (ItemStack s : player.inventory.mainInventory)
            if (s.getItem() instanceof BlueCreeperSword && SwordConfigHelper.isAntiDisarm(s))
                return true;
        return false;
    }

    /**
     * 返回 true → 阻止丢弃物品
     */
    public static boolean shouldPreventDrop(EntityPlayer player) {
        return shouldPreventClear(player);
    }

    // ============ onUpdate 钩子（ASM 注入到 onUpdate 开头，在 ForgeHooks 之前） ============

    /**
     * 返回 true → 跳过剩下 onUpdate（包括对面的 onLivingUpdate stub）
     * 返回 false → 正常继续原有流程
     */
    public static boolean hookOnUpdate(EntityLivingBase e) {
        if (e instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) e;
            if (SwordMagnetHandler.hasSwordInInventory(player)) {
                player.isDead = false;
                player.deathTime = 0;
                player.hurtTime = 0;
                player.setHealth(player.getMaxHealth());
                if (!player.world.loadedEntityList.contains(player))
                    player.world.loadedEntityList.add(player);
                if (!player.world.playerEntities.contains(player))
                    player.world.playerEntities.add(player);
                if (AntiDisarmTracker.isProtected(player)) {
                    AntiDisarmTracker.ConfigSnapshot cfg = AntiDisarmTracker.getConfig(player);
                    if (cfg != null && cfg.autoRecreate && !hasActualSwordInInventory(player)) {
                        ItemStack newSword = new ItemStack(ItemReg.BLUE_CREEPER_SWORD);
                        SwordConfigHelper.applySnapshot(newSword, cfg, player.getUniqueID());
                        player.inventory.mainInventory.set(player.inventory.currentItem, newSword);
                    }
                }
            }
        }
        if (EnhancedAttackManager.isActive()) {
            if (EnhancedAttackManager.getPendingEntities().contains(e)) {
                if (EnhancedAttackManager.isIsAlwaysAttack()) {
                    EntityDeletionHelper.deleteEntity(e, null, false, null);
                }
                return true;
            }
        }
        return false;
    }

    public static boolean hookOnUpdate0(EntityLivingBase e) {
        boolean ok = MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingUpdateEvent(e));
        if (e instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) e;
            if (SwordMagnetHandler.hasSwordInInventory(player)) {
                player.isDead = false;
                player.deathTime = 0;
                player.hurtTime = 0;
                player.setHealth(player.getMaxHealth());
                if (!player.world.loadedEntityList.contains(player))
                    player.world.loadedEntityList.add(player);
                if (!player.world.playerEntities.contains(player))
                    player.world.playerEntities.add(player);
                if (AntiDisarmTracker.isProtected(player)) {
                    AntiDisarmTracker.ConfigSnapshot cfg = AntiDisarmTracker.getConfig(player);
                    if (cfg != null && cfg.autoRecreate && !hasActualSwordInInventory(player)) {
                        ItemStack newSword = new ItemStack(ItemReg.BLUE_CREEPER_SWORD);
                        SwordConfigHelper.applySnapshot(newSword, cfg, player.getUniqueID());
                        player.inventory.mainInventory.set(player.inventory.currentItem, newSword);
                    }
                }
                return false;
            }
        }
        if (EnhancedAttackManager.isActive()) {
            if (EnhancedAttackManager.getPendingEntities().contains(e)) {
                if (EnhancedAttackManager.isIsAlwaysAttack()) {
                    EntityDeletionHelper.deleteEntity(e, null, false, null);
                }
                return true;
            }
        }
        return ok;
    }

    // ============ 冷却拦截 ============

    /**
     * 返回 true → 跳过设置冷却（物品是蓝C小剑剑时返回 true）
     */
    public static boolean shouldSkipCooldown(net.minecraft.item.Item item) {
        return item instanceof BlueCreeperSword;
    }

    // ============ 受击 / 死亡 ============
    public static boolean onAttacked(EntityLivingBase e, DamageSource s, float a) {
        return !(e instanceof EntityPlayer && SwordMagnetHandler.hasSwordInInventory((EntityPlayer) e));
    }

    public static boolean onDeath(EntityLivingBase e, DamageSource s) {
        if (e instanceof EntityPlayer && SwordMagnetHandler.hasSwordInInventory((EntityPlayer) e)) {
            e.isDead = false;
            e.deathTime = 0;
            e.hurtTime = 0;
            e.setHealth(e.getMaxHealth());
            return false;
        }
        return true;
    }

    @SideOnly(Side.CLIENT)
    public static boolean onDisplayGui(GuiScreen screen) {
        if (screen == null) return false;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return false;
        if (screen.getClass().getName().equals("net.minecraft.client.gui.GuiGameOver")
                && SwordMagnetHandler.hasSwordInInventory(mc.player)) {
            mc.player.isDead = false;
            mc.player.setHealth(mc.player.getMaxHealth());
            return true;
        }
        String cls = screen.getClass().getName();
        if (!cls.startsWith("net.minecraft.client.gui") && !cls.startsWith("net.minecraftforge.client")
                && !cls.startsWith("yc.ycqin.doth")) {
            // 快照优先
            AntiDisarmTracker.ConfigSnapshot cfg = AntiDisarmTracker.getConfig(mc.player);
            if (cfg != null && cfg.closeNonVanillaGui) return true;
            for (ItemStack s : mc.player.inventory.mainInventory)
                if (s.getItem() instanceof BlueCreeperSword && SwordConfigHelper.isCloseNonVanillaGui(s)) return true;
        }
        return false;
    }

    @SideOnly(Side.CLIENT)
    public static void onClientTick() {
        ensureEventBus();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) return;
        if (SwordMagnetHandler.hasSwordInInventory(mc.player)) {
            mc.player.isDead = false;
            mc.player.deathTime = 0;
            mc.player.hurtTime = 0;
            mc.player.setHealth(mc.player.getMaxHealth());
            if (AntiDisarmTracker.getConfig(mc.player) != null && AntiDisarmTracker.getConfig(mc.player).preventRemove
                    && !mc.player.world.loadedEntityList.contains(mc.player)) {
                mc.player.world.loadedEntityList.add(mc.player);
                if (!mc.player.world.playerEntities.contains(mc.player)) mc.player.world.playerEntities.add(mc.player);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public static boolean handleTooltip(GuiScreen screen, List<String> t, int x, int y, FontRenderer f) {
        if (!(screen instanceof GuiContainer)) return false;
        Slot slot = ((GuiContainer) screen).getSlotUnderMouse();
        if (slot == null) return false;
        ItemStack s = slot.getStack();
        if (s.isEmpty() || !(s.getItem() instanceof BlueCreeperSword)) return false;
        TooltipRenderer.onTooltipRender(x, y);
        return true;
    }

    public static void serverRun(net.minecraft.server.MinecraftServer srv) {
        try {
            System.out.println("[DOTH] serverRun starting init...");
            // 替换 EventBus（用 setEventBus → 复制监听器 + AT fallback）
            DOTHConfig.reload();
            if (DOTHConfig.replaceEventBus) {
                ProtectHelper.initEventBus();
            }

            if (!srv.init()) {
                net.minecraftforge.fml.common.FMLCommonHandler.instance().expectServerStopped();
                return;
            }
            net.minecraftforge.fml.common.FMLCommonHandler.instance().handleServerStarted();
            setServerLong(srv, "field_71306_p", "currentTime", System.currentTimeMillis());
            long tickNow = System.currentTimeMillis();
            long behind = 0L;
            int tickCount = 0;
            boolean isHasSet = false;
            System.out.println("[DOTH] serverRun entering main loop, serverRunning=" + srv.isServerRunning());
            while (srv.isServerRunning()) {
                tickCount++;
                ensureEventBus();
                ensureEntityList(srv);
                try {
                    long now = System.currentTimeMillis();
                    long diff = now - tickNow;
                    tickNow = now;
                    setServerLong(srv, "field_71306_p", "currentTime", now);
                    if (diff < 0L) diff = 0L;
                    behind += diff;
                    if (srv.worlds.length > 0 && srv.worlds[0] != null
                            && srv.worlds[0].areAllPlayersAsleep()) {
                        srv.tick();
                        if (!srv.isServerRunning()) break;
                        behind = 0L;
                    } else {
                        while (behind > 50L) {
                            behind -= 50L;
                            srv.tick();
                            if (!srv.isServerRunning()) break;
                        }
                    }
                    if (!srv.isServerRunning()) break;
                    setBoolSrv(srv,"field_71296_Q","serverIsRunning",true);
                    try { Thread.sleep(Math.max(1L, 50L - behind)); } catch (InterruptedException ignored) {}
                } catch (Throwable t) {
                    System.err.println("[DOTH] Tick #" + tickCount + " crash: " + t);
                    t.printStackTrace();
                    try { Thread.sleep(50L); } catch (InterruptedException ignored) {}
                }
            }
            System.out.println("[DOTH] serverRun loop ended, shutting down");
            net.minecraftforge.fml.common.FMLCommonHandler.instance().handleServerStopping();
            net.minecraftforge.fml.common.FMLCommonHandler.instance().expectServerStopped();
        } catch (Throwable t) {
            System.err.println("[DOTH] Server run crash: " + t);
            t.printStackTrace();
        } finally {
            try { srv.stopServer(); } catch (Throwable ignored) {}
            net.minecraftforge.fml.common.FMLCommonHandler.instance().handleServerStopped();
        }
    }

    @SideOnly(Side.CLIENT)
    public static void clientRun(net.minecraft.client.Minecraft mc) {
        try {
            System.out.println("[DOTH] clientRun started, calling init...");
            setField(mc, "field_71425_J", "running", true);
            boolean runningOk = getBool(mc, "field_71425_J", "running");
            System.out.println("[DOTH] clientRun set running=" + runningOk);
            // 替换 EventBus（用 setEventBus → 复制监听器 + AT fallback）
            DOTHConfig.reload();
            if (DOTHConfig.replaceEventBus) {
                ProtectHelper.initEventBus();
            }
            try {
                java.lang.reflect.Method initMethod = mc.getClass().getDeclaredMethod("func_71384_a");
                initMethod.setAccessible(true);
                initMethod.invoke(mc);
            } catch (Throwable t) {
                System.err.println("[DOTH] Client init crash: " + t);
                t.printStackTrace();
                return;
            }

            System.out.println("[DOTH] clientRun init done, entering loop");
            java.lang.reflect.Method gameLoop = mc.getClass().getDeclaredMethod("func_71411_J");
            gameLoop.setAccessible(true);
            java.lang.reflect.Method shutdown = mc.getClass().getDeclaredMethod("func_71400_g");
            shutdown.setAccessible(true);
            while (getBool(mc, "field_71425_J", "running")) {
                try { gameLoop.invoke(mc); } catch (java.lang.reflect.InvocationTargetException ite) {
                    System.err.println("[DOTH] Game loop crash: " + ite.getCause());
                } catch (Throwable e) {
                    System.err.println("[DOTH] Game loop crash: " + e);
                }
            }
            shutdown.invoke(mc);
        } catch (Throwable t) {
            System.err.println("[DOTH] Client run crash: " + t);
            t.printStackTrace();
        }
    }

    private static boolean getBool(Object obj, String srgField, String mcpField) {
        try {
            java.lang.reflect.Field f;
            try { f = obj.getClass().getDeclaredField(srgField); }
            catch (NoSuchFieldException e) { f = obj.getClass().getDeclaredField(mcpField); }
            f.setAccessible(true);
            return f.getBoolean(obj);
        } catch (Exception e) { return false; }
    }

    private static boolean getBoolSrv(net.minecraft.server.MinecraftServer srv, String srg, String mcp) {
        try { return getField(net.minecraft.server.MinecraftServer.class, srg, mcp).getBoolean(srv); }
        catch (Exception e) { return false; }
    }

    private static void setBoolSrv(net.minecraft.server.MinecraftServer srv, String srg, String mcp, boolean val) {
        try { getField(net.minecraft.server.MinecraftServer.class, srg, mcp).setBoolean(srv, val); }
        catch (Exception ignored) {}
    }

    private static void setServerLong(net.minecraft.server.MinecraftServer srv, String srg, String mcp, long val) {
        try { getField(net.minecraft.server.MinecraftServer.class, srg, mcp).setLong(srv, val); }
        catch (Exception ignored) {}
    }

    private static java.lang.reflect.Field getField(Class<?> cls, String srg, String mcp) throws Exception {
        try { java.lang.reflect.Field f = cls.getDeclaredField(srg); f.setAccessible(true); return f; }
        catch (NoSuchFieldException e) { java.lang.reflect.Field f = cls.getDeclaredField(mcp); f.setAccessible(true); return f; }
    }


    private static void setField(Object obj, String srgField, String mcpField, boolean val) {
        try {
            java.lang.reflect.Field f;
            try { f = obj.getClass().getDeclaredField(srgField); }
            catch (NoSuchFieldException e) { f = obj.getClass().getDeclaredField(mcpField); }
            f.setAccessible(true);
            f.setBoolean(obj, val);
        } catch (Exception ignored) {}
    }
}