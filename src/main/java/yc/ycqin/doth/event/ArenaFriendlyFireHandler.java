package yc.ycqin.doth.event;

import net.minecraft.entity.Entity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import yc.ycqin.doth.world.ArenaManager;

/**
 * 防友伤：斗蛐蛐场地内，同队选手之间的攻击/伤害一律取消
 */
public class ArenaFriendlyFireHandler {

    @SubscribeEvent
    public void onAttack(LivingAttackEvent event) {
        if (cancelIfFriendly(event.getSource().getTrueSource(), event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        if (cancelIfFriendly(event.getSource().getTrueSource(), event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private boolean cancelIfFriendly(Entity source, Entity victim) {
        if (source == null || victim == null) return false;
        // 双方都是场地选手且同队 → 取消（防范围伤害波及队友）
        if (ArenaManager.isArenaFighter(source) && ArenaManager.isArenaFighter(victim)) {
            return ArenaManager.sameTeam(source, victim);
        }
        return false;
    }
}
