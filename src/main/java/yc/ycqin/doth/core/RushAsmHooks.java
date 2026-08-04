package yc.ycqin.doth.core;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import yc.ycqin.doth.world.RushManager;

import java.util.HashMap;
import java.util.Map;

/**
 * ASM 注入辅助（EntityLivingBase 全局注入，所有生物通用）：
 * 1. EntityLodo.growStage() 开头注入 → 虫灵快跑维度内不进化
 * 2. EntityLodo.func_82167_n()（onCollideWithPlayer）→ 不叠 COTH_E 药水
 * 3. EntityLivingBase 注入：canBeRidden（快跑维度才可骑）/ canBeSteered / travel / entityInit（注册 DataWatcher）
 */
public class RushAsmHooks {

    /** 记录 travel hook 最近一次接管移动的服务端 tick（用于 travel 被覆写的生物兜底） */
    private static final Map<Integer, Integer> lastRushMoveTick = new HashMap<>();

    /**
     * 是否阻止虫灵进化：所在维度是虫灵快跑维度 → true（growStage 直接 return）
     */
    public static boolean shouldBlockLodoGrow(Entity entity) {
        return entity != null && entity.world != null
                && entity.world.provider.getDimension() == RushManager.RUSH_DIM_ID;
    }

    // ===== 骑乘 =====

    /** 只有快跑维度内的生物可被骑乘（避免全游戏都能骑所有生物） */
    public static boolean canBeRidden(EntityLivingBase self) {
        return self != null && RushManager.isRushDimension(self.world);
    }

    public static boolean canBeSteered(EntityLivingBase self) {
        return self.isBeingRidden();
    }

    /** 在 entityInit 里注册 DataWatcher 状态位（双方都跑，0=正常） */
    public static void registerRushState(EntityLivingBase self) {
        self.getDataManager().register(RushManager.getRushStateKey(), (byte) 0);
    }

    /**
     * travel 覆写：返回 true = 快跑模式已处理移动（调用方不再走 super.travel）
     * 状态 1 = 倒数（原地不动）；状态 2 = 奔跑（恒定前进 + 骑手左右）；状态 0 = 走原版逻辑
     */
    public static boolean rushTravel(EntityLivingBase self, float strafe, float vertical, float forward) {
        byte state = self.getDataManager().get(RushManager.getRushStateKey());

        if (state == RushManager.STATE_HOLD) {
            self.motionX = 0.0D;
            self.motionY = 0.0D;
            self.motionZ = 0.0D;
            markRushMoved(self);
            return true;
        }

        if (state == RushManager.STATE_RUN) {
            Entity rider = self.getPassengers().isEmpty() ? null : self.getPassengers().get(0);
            if (rider instanceof EntityLivingBase) {
                EntityLivingBase r = (EntityLivingBase) rider;
                self.rotationYaw = 0.0F;
                self.prevRotationYaw = 0.0F;
                self.rotationPitch = 0.0F;
                float s = RushManager.steerFor(self, r.moveStrafing * 0.5F);
                float spd = RushManager.getRunSpeed(self); // 距离越远越快 + 加速道具
                self.motionX = s * spd;
                self.motionZ = spd;
                self.motionY = 0.0D;
                self.setNoGravity(true);
                // 原版物理移动（此 Forge 是 1.13 式 move(MoverType,...)，tracker 同步正常）
                self.move(net.minecraft.entity.MoverType.SELF, self.motionX, 0.0D, self.motionZ);
                // 手动走路动画（move() 不触发 limbSwing）
                self.prevLimbSwingAmount = self.limbSwingAmount;
                self.limbSwingAmount = Math.min(1.0F, self.limbSwingAmount + 0.35F);
                self.limbSwing += self.limbSwingAmount;
                markRushMoved(self);
                return true;
            }
            self.motionX = 0.0D;
            self.motionZ = 0.0D;
            markRushMoved(self);
            return true;
        }

        return false; // 正常状态 → 走 super.travel（原版逻辑）
    }

    /** 记录本次服务端 tick 已由 travel hook 接管移动 */
    public static void markRushMoved(EntityLivingBase self) {
        try {
            int tick = FMLCommonHandler.instance().getMinecraftServerInstance().getTickCounter();
            lastRushMoveTick.put(self.getEntityId(), tick);
        } catch (Exception ignored) {
        }
    }

    /**
     * 本服务端 tick travel hook 是否接管过该实体。
     * 若为 false → 该生物覆写了 travel（马/蝙蝠/史莱姆等），需要 RushManager 兜底推动。
     */
    public static boolean wasMovedThisTick(EntityLivingBase self) {
        try {
            int tick = FMLCommonHandler.instance().getMinecraftServerInstance().getTickCounter();
            Integer t = lastRushMoveTick.get(self.getEntityId());
            return t != null && t == tick;
        } catch (Exception e) {
            return true; // 取不到 tick 时保守起见不兜底
        }
    }

    /** 骑手 shift（潜行）下马兜底：由 RushManager tickRun 检测 isSneaking 实现，这里保留接口不用 */
    @SuppressWarnings("unused")
    public static boolean isSneakDismount(Entity rider) {
        return rider instanceof EntityPlayer && ((EntityPlayer) rider).isSneaking();
    }
}
