package yc.ycqin.doth.core;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import yc.ycqin.doth.common.item.BlueCreeperSword;
import yc.ycqin.doth.util.SwordConfigHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 防缴械追踪器：维护开启了防缴械的玩家集合 + 配置快照。
 */
public class AntiDisarmTracker {

    public static final class ConfigSnapshot {
        public boolean attackPassive, attackPlayers, attackAllEntities, enableEnhanced, allReturn, alwaysAttack;
        public boolean tryDropItems, instantMine, magnetDrops, closeNonVanillaGui, enableBuffs, preventRemove, antiDisarm;
        public boolean autoRecreate;
        public boolean collectEntityDrops;
        public boolean rayTrace;
        public boolean purgeNBT;
        public boolean resetBooleans;
        public boolean resetLists;
        public Map<String, Integer> buffConfig = new HashMap<>();

        public static ConfigSnapshot fromSword(ItemStack stack) {
            ConfigSnapshot s = new ConfigSnapshot();
            s.attackPassive      = SwordConfigHelper.isAttackPassive(stack);
            s.attackPlayers      = SwordConfigHelper.isAttackPlayers(stack);
            s.attackAllEntities  = SwordConfigHelper.isAttackAllEntities(stack);
            s.enableEnhanced     = SwordConfigHelper.isEnhancedEnabled(stack);
            s.allReturn          = SwordConfigHelper.isAllReturnEnabled(stack);
            s.alwaysAttack       = SwordConfigHelper.isAlwaysAttack(stack);
            s.tryDropItems       = SwordConfigHelper.isTryDropItems(stack);
            s.instantMine        = SwordConfigHelper.isInstantMine(stack);
            s.magnetDrops        = SwordConfigHelper.isMagnetDrops(stack);
            s.closeNonVanillaGui = SwordConfigHelper.isCloseNonVanillaGui(stack);
            s.enableBuffs        = SwordConfigHelper.isEnableBuffs(stack);
            s.preventRemove      = SwordConfigHelper.isPreventRemove(stack);
            s.antiDisarm         = SwordConfigHelper.isAntiDisarm(stack);
            s.autoRecreate       = SwordConfigHelper.isAutoRecreate(stack);
            s.collectEntityDrops = SwordConfigHelper.isCollectEntityDrops(stack);
            s.rayTrace          = SwordConfigHelper.isRayTrace(stack);
            s.purgeNBT          = SwordConfigHelper.isPurgeNBT(stack);
            s.resetBooleans     = SwordConfigHelper.isResetBooleans(stack);
            s.resetLists        = SwordConfigHelper.isResetLists(stack);
            if (s.enableBuffs) s.buffConfig = new HashMap<>(SwordConfigHelper.getBuffConfig(stack));
            return s;
        }
    }

    private static final Map<UUID, ConfigSnapshot> CONFIGS = new HashMap<>();

    public static void protect(EntityPlayer player, ItemStack sword) {
        if (player == null) return;
        CONFIGS.put(player.getUniqueID(), ConfigSnapshot.fromSword(sword));
    }

    public static void unprotect(EntityPlayer player) {
        if (player != null) CONFIGS.remove(player.getUniqueID());
    }

    public static boolean isProtected(EntityPlayer player) {
        return player != null && CONFIGS.containsKey(player.getUniqueID());
    }

    public static ConfigSnapshot getConfig(EntityPlayer player) {
        return player != null ? CONFIGS.get(player.getUniqueID()) : null;
    }
}
