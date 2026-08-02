package yc.ycqin.doth.client;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 虫灵快跑客户端状态（由 SPacketRushState 更新，HUD 读取）
 */
@SideOnly(Side.CLIENT)
public class RushClientState {
    public static boolean active = false;
    public static int dimId = -1;
    public static int score = 0;
    public static int coins = 0;
    public static int distance = 0;
    /** 待强制上马的实体 id（SPacketRushMount 挂起，实体同步到后重试） */
    public static int pendingMountId = -1;
}
