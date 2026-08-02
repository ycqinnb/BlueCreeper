package yc.ycqin.doth.core;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import yc.ycqin.doth.world.RushManager;

/**
 * ASM 注入辅助：
 * 1. EntityLodo.growStage() 开头注入 → 虫灵快跑维度内不进化
 * 2. EntityLodo.func_82167_n()（onCollideWithPlayer）→ 不叠 COTH_E 药水
 * 3. 新增骑乘覆写方法体（canBeRidden / canBeSteered / travel / entityInit）
 */
public class RushAsmHooks {

    /**
     * 是否阻止虫灵进化：所在维度是虫灵快跑维度 → true（growStage 直接 return）
     */
    public static boolean shouldBlockLodoGrow(Entity entity) {
        return entity != null && entity.world != null
                && entity.world.provider.getDimension() == RushManager.RUSH_DIM_ID;
    }

    // ===== 骑乘 =====

    public static boolean canBeRidden() {
        return true;
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
     * 状态 1 = 倒数（原地不动）；状态 2 = 奔跑（恒定前进 + 骑手左右）；状态 0 = 走 SRP 原生
     */
    public static boolean rushTravel(EntityLivingBase self, float strafe, float vertical, float forward) {
        byte state = self.getDataManager().get(RushManager.getRushStateKey());

        if (state == RushManager.STATE_HOLD) {
            self.motionX = 0.0D;
            self.motionY = 0.0D;
            self.motionZ = 0.0D;
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
                return true;
            }
            self.motionX = 0.0D;
            self.motionZ = 0.0D;
            return true;
        }

        return false; // 正常状态 → 走 super.travel（SRP 原生）
    }

    /** 骑手 shift（潜行）下马兜底：由 RushManager tickRun 检测 isSneaking 实现，这里保留接口不用 */
    @SuppressWarnings("unused")
    public static boolean isSneakDismount(Entity rider) {
        return rider instanceof EntityPlayer && ((EntityPlayer) rider).isSneaking();
    }
}
