package yc.ycqin.doth.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;

import java.util.HashSet;
import java.util.Set;

/**
 * 实体高亮配置（与 BlockHighlightConfig 平行）
 */
public class EntityHighlightConfig {
    public static boolean enabled = false;
    public static int searchRange = 32;
    public static int updateFrequency = 10;
    public static boolean attackAllEnabled = false;
    public static int attackRadius = 4;

    /** 手动选择的实体类型（完整类名） */
    public static final Set<String> highlightEntities = new HashSet<>();

    /** 攻击白名单（完整类名）：名单内的生物永远不会被武器攻击/删除 */
    public static final Set<String> attackWhitelist = new HashSet<>();

    public static void toggle() { enabled = !enabled; }

    public static void addEntity(Class<? extends Entity> clazz) {
        highlightEntities.add(clazz.getName());
    }

    public static void removeEntity(Class<? extends Entity> clazz) {
        highlightEntities.remove(clazz.getName());
    }

    // ========== 攻击白名单（复用实体高亮的选择器风格） ==========

    public static void addAttackWhitelist(Class<? extends Entity> clazz) {
        attackWhitelist.add(clazz.getName());
    }

    public static void removeAttackWhitelist(Class<? extends Entity> clazz) {
        attackWhitelist.remove(clazz.getName());
    }

    public static void clearAttackWhitelist() {
        attackWhitelist.clear();
    }

    /** 判断实体是否在攻击白名单内（含父类匹配：白名单 EntityAnimal 可保护所有动物） */
    public static boolean isAttackWhitelisted(Entity entity) {
        if (entity == null) return false;
        Class<?> c = entity.getClass();
        while (c != null && c != Object.class) {
            if (attackWhitelist.contains(c.getName())) return true;
            c = c.getSuperclass();
        }
        return false;
    }

    /** 判断一个实体是否应该被高亮 */
    public static boolean isHighlighted(Entity entity, boolean attackPassive, boolean attackPlayers, boolean attackAll) {
        if (entity == null) return false;
        if (entity.isDead) return false;
        if (entity == net.minecraft.client.Minecraft.getMinecraft().player) return false;

        // 手动列表优先
        if (highlightEntities.contains(entity.getClass().getName())) return true;
        // 空列表时按攻击配置过滤
        if (highlightEntities.isEmpty()) {
            if (entity instanceof EntityPlayer) return attackPlayers;
            if (entity instanceof EntityAnimal) return attackPassive;
            if (entity instanceof IMob || entity instanceof EntityLivingBase) return true;
            if (entity instanceof EntityItem) return true;
            return attackAll;
        }
        return false;
    }
}
