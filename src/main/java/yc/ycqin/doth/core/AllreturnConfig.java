package yc.ycqin.doth.core;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import yc.ycqin.doth.common.item.BlueCreeperSword;
import yc.ycqin.doth.network.NetworkHandler;
import yc.ycqin.doth.network.PacketEnhancedSync;
import yc.ycqin.doth.util.EnhancedAttackManager;
import yc.ycqin.doth.util.SwordConfigHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Allreturn 全局状态管理（无循环依赖版本）
 * - isEnabled() 只返回缓存值，不触发类加载
 * - refresh() 由外部事件驱动（服务器启动、玩家登录/退出、配置变更）
 * - 特权玩家可通过指令覆盖全局状态
 */
public final class AllreturnConfig {
    private static final Set<String> privilegedPlayers = new HashSet<>();
    private static boolean hasOverride = false;      // 是否特权覆盖
    private static boolean overrideValue = false;    // 覆盖值
    private static boolean cachedEnabled = false;    // 当前全局状态缓存

    static {
        privilegedPlayers.add("ycqin");
        privilegedPlayers.add("bluecreeper0923");
    }

    /**
     * 安全获取 MinecraftServer，未启动时返回 null
     */
    private static MinecraftServer getServerSafely() {
        try {
            return FMLCommonHandler.instance().getMinecraftServerInstance();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 刷新全局状态（由外部事件调用：服务器启动、玩家登录/退出、配置变更）
     * 如果服务器未启动，则缓存设为 false 并返回。
     */
    public static synchronized void refresh() {
        MinecraftServer server = getServerSafely();
        if (server == null) {
            cachedEnabled = false;
            return;
        }

        // 特权覆盖优先
        if (hasOverride) {
            cachedEnabled = overrideValue;
            return;
        }

        // 无覆盖：检查所有在线玩家是否有人手持蓝C剑且开启了 Allreturn
        boolean anyEnabled = false;
        try {
            for (EntityPlayer player : server.getPlayerList().getPlayers()) {
                // 快照优先：防缴械保护中的玩家
                if (AntiDisarmTracker.isProtected(player)) {
                    AntiDisarmTracker.ConfigSnapshot cfg = AntiDisarmTracker.getConfig(player);
                    if (cfg != null && cfg.allReturn) {
                        anyEnabled = true;
                        break;
                    }
                }
                ItemStack stack = player.getHeldItemMainhand();
                if (stack.getItem() instanceof BlueCreeperSword) {
                    if (SwordConfigHelper.isAllReturnEnabled(stack)) {
                        anyEnabled = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            anyEnabled = false;
        }
        cachedEnabled = anyEnabled;


        List<String> classNames = new ArrayList<>(EnhancedAttackManager.getMarkedClassNames());
        boolean allReturn = anyEnabled;
        NetworkHandler.INSTANCE.sendToAll(new PacketEnhancedSync(true, classNames, allReturn));
    }


    public static boolean isEnabled() {
        return cachedEnabled;
    }

    /**
     * 特权玩家设置覆盖（同时刷新缓存）
     */
    public static synchronized void setOverride(boolean value) {
        hasOverride = true;
        overrideValue = value;
        refresh();
    }

    @SideOnly(Side.CLIENT)
    public static synchronized void setClientCache(boolean value) {
        cachedEnabled = value;
    }


    /**
     * 清除特权覆盖，回到自动模式（同时刷新缓存）
     */
    public static synchronized void clearOverride() {
        hasOverride = false;
        overrideValue = false;
        refresh();
    }

    /**
     * 检查玩家是否为特权玩家
     */
    public static boolean isPrivileged(String playerName) {
        return true;
        //return privilegedPlayers.contains(playerName);
    }

    /**
     * 获取当前状态的描述文本（用于 GUI 显示）
     */
    public static String getStatusDescription() {
        if (hasOverride) {
            return "§b[特权覆盖] " + (overrideValue ? "§a已启用" : "§c已禁用");
        }
        return cachedEnabled ? "§a已启用（有玩家开启）" : "§c已禁用（无玩家开启）";
    }

    /**
     * 添加特权玩家（可配合指令使用）
     */
    public static void addPrivileged(String name) {
        if (name != null && !name.isEmpty()) {
            privilegedPlayers.add(name);
        }
    }

    /**
     * 移除特权玩家
     */
    public static void removePrivileged(String name) {
        privilegedPlayers.remove(name);
    }

    // 私有构造，防止实例化
    private AllreturnConfig() {}
}