package yc.ycqin.doth.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import yc.ycqin.doth.DOTHMod;

import java.util.*;

@Mod.EventBusSubscriber(modid = DOTHMod.MODID)
public class EnhancedAttackManager {
    // ========== 服务端列表 ==========
    private static final List<Entity> PENDING_ENTITIES = new ArrayList<>();
    private static final List<String> MARKED_CLASS_NAMES = new ArrayList<>();
    private static boolean isAlwaysAttack = false;
    public static boolean resetBooleans = false;
    public static boolean resetLists = false;
    // ========== 客户端缓存 ==========
    @SideOnly(Side.CLIENT)
    private static boolean clientEnhancedEnabled = false;
    @SideOnly(Side.CLIENT)
    private static List<String> clientMarkedClasses = new ArrayList<>();

    // ========== 服务端方法 ==========
    public static void addTarget(Entity target) {
        if (target == null || target.world == null) return;

        if (!PENDING_ENTITIES.contains(target)) {
            PENDING_ENTITIES.add(target);
        }

        String className = target.getClass().getName();
        if (!MARKED_CLASS_NAMES.contains(className)) {
            MARKED_CLASS_NAMES.add(className);
        }
    }

    public static void setIsAlwaysAttack(boolean alwaysAttack){
        isAlwaysAttack = alwaysAttack;
    }

    public static List<Entity> getPendingEntities() {
        return PENDING_ENTITIES;
    }

    public static List<String> getMarkedClassNames() {
        return MARKED_CLASS_NAMES;
    }

    public static boolean isClassMarked(String className) {
        return MARKED_CLASS_NAMES.contains(className);
    }

    public static boolean isIsAlwaysAttack(){
        return isAlwaysAttack;
    }

    public static void clearAll() {
        PENDING_ENTITIES.clear();
        MARKED_CLASS_NAMES.clear();
    }

    /**
     * 遍历所有已加载的 mod 类，将所有非白名单包的 static boolean 非 final 字段设为 false。
     * 白名单参考 Allreturn 的 shouldApplyAllReturn。
     */
    @SuppressWarnings("unchecked")
    public static void resetAllModBooleans() {
        try {
            ClassLoader cl = EnhancedAttackManager.class.getClassLoader();
            if (!(cl instanceof net.minecraft.launchwrapper.LaunchClassLoader)) return;
            net.minecraft.launchwrapper.LaunchClassLoader lcl = (net.minecraft.launchwrapper.LaunchClassLoader) cl;
            java.lang.reflect.Field cachedField = net.minecraft.launchwrapper.LaunchClassLoader.class.getDeclaredField("cachedClasses");
            cachedField.setAccessible(true);
            java.util.Map<String, Class<?>> cachedClasses = (java.util.Map<String, Class<?>>) cachedField.get(lcl);
            int count = 0;
            for (Class<?> cls : cachedClasses.values()) {
                String name = cls.getName();
                if (isWhitelisted(name)) continue;
                try {
                    for (java.lang.reflect.Field f : cls.getDeclaredFields()) {
                        int mod = f.getModifiers();
                        if (!java.lang.reflect.Modifier.isStatic(mod)) continue;
                        if (!java.lang.reflect.Modifier.isFinal(mod) && f.getType() == Boolean.TYPE) {
                            f.setAccessible(true);
                            f.setBoolean(null, false);
                            System.out.println("[DOTH] Reset" + f.getName() +" to false");
                            count++;
                        }
                    }
                } catch (NoClassDefFoundError | Exception ignored) {}
            }
            System.out.println("[DOTH] Reset " + count + " static boolean fields to false");
        } catch (Throwable t) {
            System.err.println("[DOTH] resetAllModBooleans failed: " + t);
        }
    }

    /**
     * 遍历所有已加载的 mod 类，将所有非白名单包的 static Collection/Map 字段清空。
     */
    @SuppressWarnings("unchecked")
    public static void resetAllModLists() {
        try {
            ClassLoader cl = EnhancedAttackManager.class.getClassLoader();
            if (!(cl instanceof net.minecraft.launchwrapper.LaunchClassLoader)) return;
            net.minecraft.launchwrapper.LaunchClassLoader lcl = (net.minecraft.launchwrapper.LaunchClassLoader) cl;
            java.lang.reflect.Field cachedField = net.minecraft.launchwrapper.LaunchClassLoader.class.getDeclaredField("cachedClasses");
            cachedField.setAccessible(true);
            java.util.Map<String, Class<?>> cachedClasses = (java.util.Map<String, Class<?>>) cachedField.get(lcl);
            int count = 0;
            for (Class<?> cls : cachedClasses.values()) {
                String name = cls.getName();
                if (isWhitelisted(name)) continue;
                try {
                    for (java.lang.reflect.Field f : cls.getDeclaredFields()) {
                        int mod = f.getModifiers();
                        if (!java.lang.reflect.Modifier.isStatic(mod)) continue;
                        if (java.lang.reflect.Modifier.isFinal(mod)) continue;
                        Class<?> ft = f.getType();
                        if (Collection.class.isAssignableFrom(ft) || Map.class.isAssignableFrom(ft)) {
                            f.setAccessible(true);
                            Object obj = f.get(null);
                            if (obj instanceof Collection) { ((Collection<?>) obj).clear(); count++; }
                            else if (obj instanceof Map) { ((Map<?,?>) obj).clear(); count++; }
                            System.out.println("[DOTH] Reset " + f.getName() + "to empty");
                        }
                    }
                } catch (NoClassDefFoundError | Exception ignored) {}
            }
            System.out.println("[DOTH] Reset " + count + " static Collection/Map fields to empty");
        } catch (Throwable t) {
            System.err.println("[DOTH] resetAllModLists failed: " + t);
        }
    }

    private static boolean isWhitelisted(String className) {
        return className.contains("net.minecraft.advancements.") ||
            className.contains("net.minecraft.client.") ||
            className.contains("net.minecraft.block.") ||
            className.contains("net.minecraft.command.") ||
            className.contains("net.minecraft.crash.") ||
            className.contains("net.minecraft.creativetab.") ||
            className.contains("net.minecraft.dispenser.") ||
            className.contains("net.minecraft.enchantment.") ||
            className.contains("net.minecraft.entity.") ||
            className.contains("net.minecraft.init.") ||
            className.contains("net.minecraft.inventory.") ||
            className.contains("net.minecraft.item.") ||
            className.contains("net.minecraft.nbt.") ||
            className.contains("net.minecraft.network.") ||
            className.contains("net.minecraft.pathfinding.") ||
            className.contains("net.minecraft.potion.") ||
            className.contains("net.minecraft.profiler.") ||
            className.contains("net.minecraft.realms.") ||
            className.contains("net.minecraft.scoreboard.") ||
            className.contains("net.minecraft.server.") ||
            className.contains("net.minecraft.stats.") ||
            className.contains("net.minecraft.tileentity.") ||
            className.contains("net.minecraft.util.") ||
            className.contains("net.minecraft.village.") ||
            className.contains("net.minecraft.world.") ||
            className.contains("net.minecraftforge.") ||
            className.contains("net.optifine.") ||
            className.contains("yc.ycqin.") ||
            className.contains("codechicken.lib.") ||
            className.contains("goblinbob.mobends.") ||
            className.contains("com.replaymod.") ||
            className.contains("com.dhanantry.scapeandrunparasites.") ||
            className.contains("alexiy.secure.") ||
            className.contains("com.chaoswither.") ||
            className.startsWith("java.") ||
            className.startsWith("javax.") ||
            className.startsWith("sun.") ||
            className.startsWith("com.sun.") ||
            className.startsWith("jdk.") ||
            className.contains("org.apache.") ||
            className.contains("org.lwjgl.") ||
            className.contains("com.google.") ||
            className.contains("com.mojang.") ||
            className.contains("io.netty.") ||
            className.contains("it.unimi.dsi.fastutil.") ||
            className.contains("org.objectweb.asm.") ||
            className.contains("joptsimple.") ||
            className.contains("paulscode.") ||
            className.contains("oshi.") ||
            className.contains("jline.") ||
            className.contains("gnu.") ||
            className.contains("scala.") ||
            className.contains("clojure.") ||
            className.contains("com.jcraft.") ||
            className.contains("cpw.mods.") ||
            className.contains("libraries.") ||
            className.contains("org.jetbrains.") ||
            className.contains("org.intellij.") ||
            className.contains("com.intellij.") ||
            className.contains("org.slf4j.") ||
            className.contains("com.ibm.") ||
            className.contains("org.w3c.") ||
            className.contains("org.xml.") ||
            className.contains("kotlin.") ||
            className.contains("groovy.") ||
            className.contains("freemarker.");
    }

    public static boolean isActive() {
        return !PENDING_ENTITIES.isEmpty() || !MARKED_CLASS_NAMES.isEmpty();
    }

    // ========== 客户端方法（由网络包更新） ==========
    @SideOnly(Side.CLIENT)
    public static void setClientState(boolean enabled, List<String> classNames) {
        clientEnhancedEnabled = enabled;
        clientMarkedClasses = new ArrayList<>(classNames);
    }

    @SideOnly(Side.CLIENT)
    public static boolean isClientEnhancedEnabled() {
        return clientEnhancedEnabled;
    }

    @SideOnly(Side.CLIENT)
    public static boolean isClientClassMarked(String className) {
        return clientMarkedClasses.contains(className);
    }

    // ========== 通用方法（自动判断客户端/服务端） ==========
    public static boolean isClassMarkedUniversal(String className) {
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            return isClientClassMarked(className);
        }
        return isClassMarked(className);
    }

    /**
     * 每 Tick 处理列表1中的实体
     */
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        // 只在服务端处理，且只在 Tick 结束阶段执行
        if (event.world.isRemote) return;
        if (event.phase != TickEvent.Phase.END) return;
        if (PENDING_ENTITIES.isEmpty()) return;
        if (!isAlwaysAttack) return;
        Iterator<Entity> iterator = PENDING_ENTITIES.iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            // 如果实体还存在（未死亡且在世界中），执行抹除
            if (entity != null && entity.world != null) {
                EntityDeletionHelper.deleteEntity(entity,null,false,null);
            }
        }
    }

    /**
     * 拦截新实体加入世界（替代 ASM 注入实体加入世界方法）
     * 如果该实体的类名在列表2中，则阻止其加载并抹除
     */
    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote) return;
        if (MARKED_CLASS_NAMES.isEmpty()) return;

        Entity entity = event.getEntity();
        if (entity == null) return;
        if (entity instanceof EntityPlayer) return; // 保护玩家
        String className = entity.getClass().getName();
        if (MARKED_CLASS_NAMES.contains(className)) {
            // 阻止实体加入世界
            event.setCanceled(true);
        }
    }
}
