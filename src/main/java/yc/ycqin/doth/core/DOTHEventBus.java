package yc.ycqin.doth.core;

import com.google.common.base.Throwables;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.IEventExceptionHandler;
import net.minecraftforge.fml.common.eventhandler.IEventListener;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import yc.ycqin.doth.common.item.BlueCreeperSword;
import yc.ycqin.doth.event.SwordMagnetHandler;
import yc.ycqin.doth.util.EnhancedAttackManager;
import yc.ycqin.doth.util.SwordConfigHelper;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static yc.ycqin.doth.core.ProtectHelper.EVENTBUS_BUSID;

/**
 * 替代 MinecraftForge.EVENT_BUS。
 *
 * 集中处理：
 * 1. 攻防保护——不死、不受伤（背包有剑）
 * 2. 禁生成——拦截标记实体类名（EnhancedAttackManager）
 * 3. 持续攻击——WorldTick 处理待删除实体
 * 4. 渲染拦截——关 mod GUI/HUD（closeNonVanillaGui + 有剑）
 * 5. 防缴械——CleanItem、丢弃物品拦截
 * 6. 飞行保持——每 tick 确保 allowFlying
 * 7. Buff 增强——每 tick 应用配置的 Buff
 */
public class DOTHEventBus extends EventBus implements IEventExceptionHandler {

    private static int maxID = 0;
    private ConcurrentHashMap<Object, ArrayList<IEventListener>> listeners;
    private Map<Object, ModContainer> listenerOwners;
    private int busID;
    private IEventExceptionHandler exceptionHandler;
    private boolean shutdown;

    public DOTHEventBus() {
        this.exceptionHandler = this;
    }

    @Override
    public void handleException(EventBus bus, Event event, IEventListener[] listeners, int index, Throwable throwable) {
        System.err.println("[DOTH] EventBus suppressed exception in "
            + event.getClass().getSimpleName() + " from listener #" + index + ": " + throwable);
    }

    @Override
    public boolean post(Event event) {
        return postnnn(this,event);
    }

    public static boolean postnnn(EventBus bus,Event event) {
        try {
        // ===== 禁生成——拦截标记实体 =====
        if (event instanceof EntityJoinWorldEvent) {
            EntityJoinWorldEvent e = (EntityJoinWorldEvent) event;
            if (!e.getWorld().isRemote && !(e.getEntity() instanceof EntityPlayer)) {
                if (EnhancedAttackManager.isClassMarked(e.getEntity().getClass().getName())) {
                    e.setCanceled(true);
                    return true;
                }
            }
        }

        // ===== 攻防保护 =====
        if (event instanceof LivingAttackEvent) {
            LivingAttackEvent e = (LivingAttackEvent) event;
            if (hasSword(e.getEntityLiving())) { e.setCanceled(true); return true; }
        }
        if (event instanceof LivingHurtEvent) {
            LivingHurtEvent e = (LivingHurtEvent) event;
            if (hasSword(e.getEntityLiving())) { e.setAmount(0); e.setCanceled(true); return true; }
        }
        if (event instanceof LivingDamageEvent) {
            LivingDamageEvent e = (LivingDamageEvent) event;
            if (hasSword(e.getEntityLiving())) { e.setCanceled(true); return true; }
        }
        if (event instanceof LivingDeathEvent) {
            LivingDeathEvent e = (LivingDeathEvent) event;
            if (hasSword(e.getEntityLiving())) {
                keepAlive(e.getEntityLiving());
                e.setCanceled(true);
                return true;
            }
        }
        if (event instanceof LivingEvent.LivingUpdateEvent) {
            LivingEvent.LivingUpdateEvent e = (LivingEvent.LivingUpdateEvent) event;
            EntityLivingBase ent = e.getEntityLiving();
            // 保活：有剑的玩家
            if (hasSword(ent)) {
                keepAlive(ent);
                return false;
            }
            // 持续攻击：标记实体 → 删除 + 阻止 onUpdate 继续
            if (EnhancedAttackManager.isActive()
                    && EnhancedAttackManager.getPendingEntities().contains(ent)) {
                if (EnhancedAttackManager.isIsAlwaysAttack()) {
                    yc.ycqin.doth.util.EntityDeletionHelper.deleteEntity(ent, null, false, null);
                }
                return true; // 取消事件 → 对应 ASM 里 hookOnUpdate 返回 true 跳过 onUpdate
            }
        }

        // ===== 持续攻击——WorldTick 处理待删除实体 =====
        if (event instanceof TickEvent.WorldTickEvent) {
            TickEvent.WorldTickEvent e = (TickEvent.WorldTickEvent) event;
            if (!e.world.isRemote && e.phase == TickEvent.Phase.END
                    && EnhancedAttackManager.isActive()
                    && EnhancedAttackManager.isIsAlwaysAttack()) {
                // 允许原监听器继续处理，DOTHEventBus 不做实际删除
                // EnhancedAttackManager.onWorldTick 会做实际工作
            }
        }

        // ===== 玩家 Tick——飞行 + Buff + 保活 =====
        if (event instanceof TickEvent.PlayerTickEvent) {
            TickEvent.PlayerTickEvent e = (TickEvent.PlayerTickEvent) event;
            if (e.phase == TickEvent.Phase.END && hasSword(e.player)) {
                // 飞行
                if (!e.player.capabilities.allowFlying) {
                    e.player.capabilities.allowFlying = true;
                    e.player.sendPlayerAbilities();
                }
                keepAlive(e.player);
            }
        }

        // ===== 渲染拦截 =====
        if (shouldBlockModOverlays() && isRenderEvent(event)) {
            return handleRenderBlock(event);
        }
            try {
                int busID = EVENTBUS_BUSID != null ? EVENTBUS_BUSID.getInt(bus) : 0;
                IEventListener[] listeners = event.getListenerList().getListeners(busID);
                for (int i = 0; i < listeners.length; i++) {
                    if (shouldSkip(event, listeners[i])) continue;
                    try {
                        listeners[i].invoke(event);
                    } catch (Throwable t) {
                        System.err.println("[DOTH] EventBus suppressed exception in "
                                + event.getClass().getSimpleName() + " from listener #" + i + ": " + t);
                    }
                }
                return event.isCancelable() && event.isCanceled();
            } catch (Throwable t) {
                System.err.println("[DOTH] EventBus safePost error: " + t);
                return false;
            }
        } catch (Throwable t) {
            System.err.println("[DOTH] EventBus caught exception in "
                + event.getClass().getSimpleName() + ": " + t);
            return false;
        }
    }

    // ========== 辅助 ==========

    /** 注册相关事件必须放行，否则 mod 的方块/物品/实体等无法注册 */
    private static boolean isRegistrationEvent(Event event) {
        String name = event.getClass().getName();
        return name.contains("RegistryEvent")       // 物品/方块/实体/附魔/药水/生物群系等
            || name.contains("FMLConstructionEvent")
            || name.contains("FMLStateEvent")        // preInit/init/postInit 等
            || name.contains("FMLLoadCompleteEvent")
            || name.contains("ModelRegistryEvent")   // 模型注册
            || name.contains("TextureStitchEvent")   // 纹理
            || name.contains("ColorHandlerEvent")    // 颜色
            || name.contains("ModelBakeEvent")
            || name.contains("FMLInitializationEvent");       // 模型烘焙
    }

    /**
     * 从 ASMEventHandler 包装里提取实际归属。
     * 用 owner（ModContainer.modId）或 readable 字符串判断。
     */
    private static String getActualHandlerName(IEventListener listener) {
        String clsName = listener.getClass().getName();
        if (!clsName.contains("ASMEventHandler")) return clsName;
        try {
            java.lang.reflect.Field f = listener.getClass().getDeclaredField("owner");
            f.setAccessible(true);
            Object owner = f.get(listener);
            if (owner != null) {
                String modId = (String) owner.getClass().getMethod("getModId").invoke(owner);
                if (modId != null && !modId.isEmpty()) return modId;
            }
        } catch (Exception ignored) {}
        try {
            java.lang.reflect.Field f = listener.getClass().getDeclaredField("readable");
            f.setAccessible(true);
            String readable = (String) f.get(listener);
            if (readable != null) return readable;
        } catch (Exception ignored) {}
        return null;
    }

    private static boolean shouldSkip(Event event, IEventListener listener) {
        if (isRegistrationEvent(event)) return false;
        String name = getActualHandlerName(listener);
        if (name == null) return false;
        boolean isOurs = name.equals("bluecreepersword") || name.equals("codechickenlib")
                || name.contains("yc.ycqin") || name.contains("codechicken");
        if (!isOurs && DOTHConfig.replaceEventBus && DOTHConfig.blockModEvents) {
            return true;
        }
        return false;
    }

    private static boolean hasSword(EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) return false;
        return SwordMagnetHandler.hasSwordInInventory((EntityPlayer) entity);
    }

    private static void keepAlive(EntityLivingBase ent) {
        ent.isDead = false;
        ent.deathTime = 0;
        ent.hurtTime = 0;
        ent.setHealth(ent.getMaxHealth());
        if (!ent.world.loadedEntityList.contains(ent))
            ent.world.loadedEntityList.add(ent);
        if (ent instanceof EntityPlayer
                && !ent.world.playerEntities.contains(ent))
            ent.world.playerEntities.add((EntityPlayer) ent);
    }

    // ========== 渲染拦截 ==========

    private static boolean shouldBlockModOverlays() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return false;
        if (!SwordMagnetHandler.hasSwordInInventory(mc.player)) return false;
        AntiDisarmTracker.ConfigSnapshot cfg = AntiDisarmTracker.getConfig(mc.player);
        if (cfg != null && cfg.closeNonVanillaGui) return true;
        for (ItemStack s : mc.player.inventory.mainInventory) {
            if (s.getItem() instanceof BlueCreeperSword
                    && SwordConfigHelper.isCloseNonVanillaGui(s)) return true;
        }
        return false;
    }

    private static boolean isRenderEvent(Event event) {
        String name = event.getClass().getName();
        return name.startsWith("net.minecraftforge.client.event.Render")
                || name.contains("GuiScreenEvent")
                || name.contains("GuiContainerEvent")
                || name.contains("DrawScreenEvent")
                || event instanceof GuiOpenEvent;
    }

    private static boolean handleRenderBlock(Event event) {
        if (event instanceof GuiOpenEvent) {
            GuiOpenEvent goe = (GuiOpenEvent) event;
            if (goe.getGui() != null
                    && goe.getGui().getClass().getName().equals("net.minecraft.client.gui.GuiGameOver")) {
                Minecraft.getMinecraft().player.isDead = false;
                Minecraft.getMinecraft().player.setHealth(Minecraft.getMinecraft().player.getMaxHealth());
                return true;
            }
            if (goe.getGui() != null) {
                String cls = goe.getGui().getClass().getName();
                if (!cls.startsWith("net.minecraft.client.gui")
                        && !cls.startsWith("net.minecraftforge.client")
                        && !cls.contains("yc.ycqin.doth")) {
                    return true;
                }
            }
            return false;
        }
        if (event instanceof RenderGameOverlayEvent) {
            if (event instanceof RenderGameOverlayEvent.Pre) {
                RenderGameOverlayEvent.ElementType type = ((RenderGameOverlayEvent.Pre) event).getType();
                if (type == RenderGameOverlayEvent.ElementType.TEXT
                        || type == RenderGameOverlayEvent.ElementType.HELMET
                        || type == RenderGameOverlayEvent.ElementType.PORTAL) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }
}
