package yc.ycqin.doth.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import yc.ycqin.doth.core.AntiDisarmTracker;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SwordConfigHelper {

    // NBT 键名
    private static final String KEY_ATTACK_PASSIVE = "attackPassive";
    private static final String KEY_ATTACK_PLAYERS = "attackPlayers";
    private static final String KEY_ATTACK_ALL = "attackAllEntities";
    private static final String KEY_ENHANCED = "enableEnhanced";
    private static final String KEY_ALWAYS = "alwaysAttack";
    private static final String KEY_ALL_RETURN = "allReturn";
    private static final String KEY_TRY_DROP = "tryDropItems";
    private static final String KEY_INSTANT_MINE = "instantMine";
    private static final String KEY_MAGNET_DROPS = "magnetDrops";
    private static final String KEY_CLOSE_MOD_GUI = "closeNonVanillaGui";
    private static final String KEY_ENABLE_BUFFS = "enableBuffs";
    private static final String KEY_PREVENT_REMOVE = "preventRemove";
    private static final String KEY_ANTI_DISARM = "antiDisarm";
    private static final String KEY_AUTO_RECREATE = "autoRecreate";
    private static final String KEY_COLLECT_ENTITY_DROPS = "collectEntityDrops";
    private static final String KEY_RAY_TRACE = "rayTrace";
    private static final String KEY_PURGE_NBT = "purgeNBT";
    private static final String KEY_RESET_BOOLEANS = "resetBooleans";
    private static final String KEY_RESET_LISTS = "resetLists";
    private static final String KEY_OWNER_UUID = "ownerUUID";
    private static final String KEY_BUFF_LIST = "buffList";

    private static NBTTagCompound getConfigTag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (!tag.hasKey("swordConfig")) {
            tag.setTag("swordConfig", new NBTTagCompound());
        }
        return tag.getCompoundTag("swordConfig");
    }

    // ========== 读取配置 ==========
    public static boolean isAttackPassive(ItemStack stack) {
        return getConfigTag(stack).getBoolean(KEY_ATTACK_PASSIVE);
    }
    public static boolean isAttackPlayers(ItemStack stack) {
        return getConfigTag(stack).getBoolean(KEY_ATTACK_PLAYERS);
    }
    public static boolean isAttackAllEntities(ItemStack stack) {
        return getConfigTag(stack).getBoolean(KEY_ATTACK_ALL);
    }
    public static boolean isEnhancedEnabled(ItemStack stack) {
        return getConfigTag(stack).getBoolean(KEY_ENHANCED);
    }

    // ========== 玩家感知读取（快照优先，防 NBT 被清除） ==========
    public static boolean isInstantMine(EntityPlayer player, ItemStack stack) {
        AntiDisarmTracker.ConfigSnapshot cfg = AntiDisarmTracker.getConfig(player);
        if (cfg != null) return cfg.instantMine;
        return isInstantMine(stack);
    }
    public static boolean isEnhancedEnabled(EntityPlayer player, ItemStack stack) {
        AntiDisarmTracker.ConfigSnapshot cfg = AntiDisarmTracker.getConfig(player);
        if (cfg != null) return cfg.enableEnhanced;
        return isEnhancedEnabled(stack);
    }
    public static boolean isTryDropItems(EntityPlayer player, ItemStack stack) {
        AntiDisarmTracker.ConfigSnapshot cfg = AntiDisarmTracker.getConfig(player);
        if (cfg != null) return cfg.tryDropItems;
        return isTryDropItems(stack);
    }
    public static boolean isAttackPassive(EntityPlayer player, ItemStack stack) {
        AntiDisarmTracker.ConfigSnapshot cfg = AntiDisarmTracker.getConfig(player);
        if (cfg != null) return cfg.attackPassive;
        return isAttackPassive(stack);
    }
    public static boolean isAttackPlayers(EntityPlayer player, ItemStack stack) {
        AntiDisarmTracker.ConfigSnapshot cfg = AntiDisarmTracker.getConfig(player);
        if (cfg != null) return cfg.attackPlayers;
        return isAttackPlayers(stack);
    }
    public static boolean isAttackAllEntities(EntityPlayer player, ItemStack stack) {
        AntiDisarmTracker.ConfigSnapshot cfg = AntiDisarmTracker.getConfig(player);
        if (cfg != null) return cfg.attackAllEntities;
        return isAttackAllEntities(stack);
    }
    public static boolean isAntiDisarm(EntityPlayer player, ItemStack stack) {
        AntiDisarmTracker.ConfigSnapshot cfg = AntiDisarmTracker.getConfig(player);
        if (cfg != null) return cfg.antiDisarm;
        return isAntiDisarm(stack);
    }
    public static UUID getOwnerUUID(EntityPlayer player, ItemStack stack) {
        AntiDisarmTracker.ConfigSnapshot cfg = AntiDisarmTracker.getConfig(player);
        if (cfg != null) return player.getUniqueID();
        return getOwnerUUID(stack);
    }

    public static boolean isCollectEntityDrops(EntityPlayer player, ItemStack stack) {
        AntiDisarmTracker.ConfigSnapshot cfg = AntiDisarmTracker.getConfig(player);
        if (cfg != null) return cfg.collectEntityDrops;
        return isCollectEntityDrops(stack);
    }

    public static boolean isRayTrace(EntityPlayer player, ItemStack stack) {
        AntiDisarmTracker.ConfigSnapshot cfg = AntiDisarmTracker.getConfig(player);
        if (cfg != null) return cfg.rayTrace;
        return isRayTrace(stack);
    }
    public static boolean isPurgeNBT(EntityPlayer player, ItemStack stack) {
        AntiDisarmTracker.ConfigSnapshot cfg = AntiDisarmTracker.getConfig(player);
        if (cfg != null) return cfg.purgeNBT;
        return isPurgeNBT(stack);
    }
    public static boolean isResetBooleans(EntityPlayer player, ItemStack stack) {
        AntiDisarmTracker.ConfigSnapshot cfg = AntiDisarmTracker.getConfig(player);
        if (cfg != null) return cfg.resetBooleans;
        return isResetBooleans(stack);
    }
    public static boolean isResetLists(EntityPlayer player, ItemStack stack) {
        AntiDisarmTracker.ConfigSnapshot cfg = AntiDisarmTracker.getConfig(player);
        if (cfg != null) return cfg.resetLists;
        return isResetLists(stack);
    }
    public static boolean isAllReturnEnabled(ItemStack stack) {
        return getConfigTag(stack).getBoolean(KEY_ALL_RETURN);
    }
    public static boolean isAlwaysAttack(ItemStack stack) {
        return getConfigTag(stack).getBoolean(KEY_ALWAYS);
    }
    public static boolean isTryDropItems(ItemStack stack) {
        return getConfigTag(stack).getBoolean(KEY_TRY_DROP);
    }
    public static boolean isInstantMine(ItemStack stack) {
        return getConfigTag(stack).getBoolean(KEY_INSTANT_MINE);
    }
    public static boolean isMagnetDrops(ItemStack stack) {
        return getConfigTag(stack).getBoolean(KEY_MAGNET_DROPS);
    }
    public static boolean isCloseNonVanillaGui(ItemStack stack) {
        return getConfigTag(stack).getBoolean(KEY_CLOSE_MOD_GUI);
    }
    public static boolean isEnableBuffs(ItemStack stack) {
        return true;
    }

    public static boolean isPreventRemove(ItemStack stack) {
        return getConfigTag(stack).getBoolean(KEY_PREVENT_REMOVE);
    }

    public static boolean isAntiDisarm(ItemStack stack) {
        return getConfigTag(stack).getBoolean(KEY_ANTI_DISARM);
    }

    public static boolean isAutoRecreate(ItemStack stack) {
        return getConfigTag(stack).getBoolean(KEY_AUTO_RECREATE);
    }

    public static boolean isCollectEntityDrops(ItemStack stack) {
        return getConfigTag(stack).getBoolean(KEY_COLLECT_ENTITY_DROPS);
    }

    public static boolean isRayTrace(ItemStack stack) {
        return getConfigTag(stack).getBoolean(KEY_RAY_TRACE);
    }

    public static boolean isPurgeNBT(ItemStack stack) {
        return getConfigTag(stack).getBoolean(KEY_PURGE_NBT);
    }

    public static boolean isResetBooleans(ItemStack stack) {
        return getConfigTag(stack).getBoolean(KEY_RESET_BOOLEANS);
    }

    public static boolean isResetLists(ItemStack stack) {
        return getConfigTag(stack).getBoolean(KEY_RESET_LISTS);
    }

    public static UUID getOwnerUUID(ItemStack stack) {
        String s = getConfigTag(stack).getString(KEY_OWNER_UUID);
        if (s.isEmpty()) return null;
        try { return UUID.fromString(s); } catch (Exception e) { return null; }
    }

    public static void setOwnerUUID(ItemStack stack, UUID uuid) {
        getConfigTag(stack).setString(KEY_OWNER_UUID, uuid != null ? uuid.toString() : "");
    }

    // ========== 设置配置 ==========
    public static void setAlwaysAttack(ItemStack stack, boolean value) {
        getConfigTag(stack).setBoolean(KEY_ALWAYS, value);
    }
    public static void setTryDropItems(ItemStack stack, boolean value) {
        getConfigTag(stack).setBoolean(KEY_TRY_DROP, value);
    }
    public static void setInstantMine(ItemStack stack, boolean value) {
        getConfigTag(stack).setBoolean(KEY_INSTANT_MINE, value);
    }
    public static void setMagnetDrops(ItemStack stack, boolean value) {
        getConfigTag(stack).setBoolean(KEY_MAGNET_DROPS, value);
    }
    public static void setCloseNonVanillaGui(ItemStack stack, boolean value) {
        getConfigTag(stack).setBoolean(KEY_CLOSE_MOD_GUI, value);
    }
    public static void setEnableBuffs(ItemStack stack, boolean value) {
        getConfigTag(stack).setBoolean(KEY_ENABLE_BUFFS, value);
    }

    public static void setPreventRemove(ItemStack stack, boolean value) {
        getConfigTag(stack).setBoolean(KEY_PREVENT_REMOVE, value);
    }

    public static void setAntiDisarm(ItemStack stack, boolean value) {
        getConfigTag(stack).setBoolean(KEY_ANTI_DISARM, value);
    }

    public static void setAutoRecreate(ItemStack stack, boolean value) {
        getConfigTag(stack).setBoolean(KEY_AUTO_RECREATE, value);
    }

    public static void setCollectEntityDrops(ItemStack stack, boolean value) {
        getConfigTag(stack).setBoolean(KEY_COLLECT_ENTITY_DROPS, value);
    }

    public static void setRayTrace(ItemStack stack, boolean value) {
        getConfigTag(stack).setBoolean(KEY_RAY_TRACE, value);
    }
    public static void setPurgeNBT(ItemStack stack, boolean value) {
        getConfigTag(stack).setBoolean(KEY_PURGE_NBT, value);
    }
    public static void setResetBooleans(ItemStack stack, boolean value) {
        getConfigTag(stack).setBoolean(KEY_RESET_BOOLEANS, value);
    }
    public static void setResetLists(ItemStack stack, boolean value) {
        getConfigTag(stack).setBoolean(KEY_RESET_LISTS, value);
    }
    public static void setAllReturn(ItemStack stack, boolean value) {
        getConfigTag(stack).setBoolean(KEY_ALL_RETURN, value);
    }
    public static void setAttackPassive(ItemStack stack, boolean value) {
        getConfigTag(stack).setBoolean(KEY_ATTACK_PASSIVE, value);
    }
    public static void setAttackPlayers(ItemStack stack, boolean value) {
        getConfigTag(stack).setBoolean(KEY_ATTACK_PLAYERS, value);
    }
    public static void setAttackAllEntities(ItemStack stack, boolean value) {
        getConfigTag(stack).setBoolean(KEY_ATTACK_ALL, value);
    }
    public static void setEnhancedEnabled(ItemStack stack, boolean value) {
        getConfigTag(stack).setBoolean(KEY_ENHANCED, value);
    }

    // ========== Buff 列表配置 ==========

    /** 读取 buff 配置：ResourceLocation → 等级(1-5)，不存在=0 */
    public static Map<String, Integer> getBuffConfig(ItemStack stack) {
        NBTTagCompound tag = getConfigTag(stack).getCompoundTag(KEY_BUFF_LIST);
        Map<String, Integer> map = new HashMap<>();
        for (String key : tag.getKeySet()) {
            map.put(key, tag.getInteger(key));
        }
        return map;
    }

    /** 写入 buff 配置 */
    public static void setBuffConfig(ItemStack stack, Map<String, Integer> buffs) {
        NBTTagCompound tag = new NBTTagCompound();
        for (Map.Entry<String, Integer> e : buffs.entrySet()) {
            if (e.getValue() > 0) tag.setInteger(e.getKey(), e.getValue());
        }
        getConfigTag(stack).setTag(KEY_BUFF_LIST, tag);
    }

    /** 把 ConfigSnapshot 的全部配置写回一个剑的 NBT */
    public static void applySnapshot(ItemStack stack, yc.ycqin.doth.core.AntiDisarmTracker.ConfigSnapshot cfg, UUID ownerUUID) {
        setAttackPassive(stack, cfg.attackPassive);
        setAttackPlayers(stack, cfg.attackPlayers);
        setAttackAllEntities(stack, cfg.attackAllEntities);
        setEnhancedEnabled(stack, cfg.enableEnhanced);
        setAllReturn(stack, cfg.allReturn);
        setAlwaysAttack(stack, cfg.alwaysAttack);
        setTryDropItems(stack, cfg.tryDropItems);
        setInstantMine(stack, cfg.instantMine);
        setMagnetDrops(stack, cfg.magnetDrops);
        setCloseNonVanillaGui(stack, cfg.closeNonVanillaGui);
        setEnableBuffs(stack, cfg.enableBuffs);
        setPreventRemove(stack, cfg.preventRemove);
        setAntiDisarm(stack, cfg.antiDisarm);
        setAutoRecreate(stack, cfg.autoRecreate);
        setCollectEntityDrops(stack, cfg.collectEntityDrops);
        setRayTrace(stack, cfg.rayTrace);
        setPurgeNBT(stack, cfg.purgeNBT);
        setResetBooleans(stack, cfg.resetBooleans);
        setResetLists(stack, cfg.resetLists);
        setOwnerUUID(stack, ownerUUID);
        if (!cfg.buffConfig.isEmpty()) setBuffConfig(stack, cfg.buffConfig);
    }

    public static String getConfigSummary(ItemStack stack) {
        return String.format("被动:%s 玩家:%s 全部:%s 增强:%s",
                isAttackPassive(stack), isAttackPlayers(stack),
                isAttackAllEntities(stack), isEnhancedEnabled(stack));
    }
}
