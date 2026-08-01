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

    public static void toggle() { enabled = !enabled; }

    public static void addEntity(Class<? extends Entity> clazz) {
        highlightEntities.add(clazz.getName());
    }

    public static void removeEntity(Class<? extends Entity> clazz) {
        highlightEntities.remove(clazz.getName());
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
