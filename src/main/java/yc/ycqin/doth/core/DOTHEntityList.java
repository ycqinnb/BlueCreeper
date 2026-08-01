package yc.ycqin.doth.core;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import yc.ycqin.doth.common.item.BlueCreeperSword;
import yc.ycqin.doth.util.EnhancedAttackManager;
import yc.ycqin.doth.util.SwordConfigHelper;

import java.util.*;

/**
 * 替换 World.loadedEntityList 的包装列表。
 * 在 add/addAll 方法中拦截被标记的实体类（禁生成名单），直接拒绝加入。
 */
public class DOTHEntityList extends AbstractList<Entity> implements List<Entity>, RandomAccess {

    private final List<Entity> delegate;

    public DOTHEntityList(List<Entity> delegate) {
        this.delegate = delegate;
    }

    private boolean shouldBlock(Entity entity) {
        if (entity == null) return false;
        if (entity instanceof EntityPlayer) return false;
        if (EnhancedAttackManager.isClassMarkedUniversal(entity.getClass().getName()) || EnhancedAttackManager.getPendingEntities().contains(entity)) {
            // 偷偷杀掉：不加入列表，直接标记死亡并从世界移除
            entity.isDead = true;
            if (entity.world != null) {
                entity.world.removeEntity(entity);
            }
            return true;
        }
        return false;
    }

    /** 防移除：有剑且开了 preventRemove 的玩家不能从列表中移除 */
    private boolean shouldKeep(Entity entity) {
        if (!(entity instanceof EntityPlayer)) return false;
        EntityPlayer p = (EntityPlayer) entity;
        AntiDisarmTracker.ConfigSnapshot cfg = AntiDisarmTracker.getConfig(p);
        if (cfg != null && cfg.preventRemove) return true;
        if (p.inventory == null) return false;
        for (ItemStack s : p.inventory.mainInventory)
            if (s.getItem() instanceof BlueCreeperSword && SwordConfigHelper.isPreventRemove(s)) return true;
        return false;
    }

    @Override
    public boolean add(Entity entity) {
        if (shouldBlock(entity)) return false;
        return delegate.add(entity);
    }

    @Override
    public void add(int index, Entity entity) {
        if (!shouldBlock(entity)) delegate.add(index, entity);
    }

    @Override
    public boolean addAll(Collection<? extends Entity> c) {
        boolean changed = false;
        for (Entity e : c) {
            if (!shouldBlock(e) && delegate.add(e)) changed = true;
        }
        return changed;
    }

    @Override
    public boolean addAll(int index, Collection<? extends Entity> c) {
        List<Entity> filtered = new ArrayList<>();
        for (Entity e : c) {
            if (!shouldBlock(e)) filtered.add(e);
        }
        return delegate.addAll(index, filtered);
    }

    // ===== 委托方法（读取） =====

    @Override public Entity get(int index) { return delegate.get(index); }
    @Override public int size() { return delegate.size(); }
    @Override public Entity set(int index, Entity element) {
        if (shouldBlock(element)) return element;
        return delegate.set(index, element);
    }
    @Override public Entity remove(int index) {
        Entity e = delegate.get(index);
        if (shouldKeep(e)) return null;
        return delegate.remove(index);
    }
    @Override public boolean remove(Object o) {
        if (o instanceof Entity && shouldKeep((Entity) o)) return false;
        return delegate.remove(o);
    }
    @Override public void clear() { delegate.clear(); }
    @Override public boolean contains(Object o) { return delegate.contains(o); }
    @Override public int indexOf(Object o) { return delegate.indexOf(o); }
    @Override public int lastIndexOf(Object o) { return delegate.lastIndexOf(o); }
    @Override public Iterator<Entity> iterator() { return delegate.iterator(); }
    @Override public ListIterator<Entity> listIterator() { return delegate.listIterator(); }
    @Override public ListIterator<Entity> listIterator(int index) { return delegate.listIterator(index); }

    /**
     * 包装已有列表，如果已是 DOTHEntityList 则直接返回。
     */
    @SuppressWarnings("unchecked")
    public static List<Entity> wrapIfNeeded(List<Entity> original) {
        if (original instanceof DOTHEntityList) return original;
        return new DOTHEntityList(original);
    }

    /**
     * 检查并替换指定 World 的 loadedEntityList。
     * 返回 true 表示执行了替换。
     */
    public static boolean replaceIfNeeded(net.minecraft.world.World world) {
        if (!DOTHConfig.replaceEntityList) return false;
        try {
            java.lang.reflect.Field f = net.minecraft.world.World.class.getDeclaredField("field_72996_f");
            f.setAccessible(true);
            List<Entity> current = (List<Entity>) f.get(world);
            if (current instanceof DOTHEntityList) return false;
            f.set(world, wrapIfNeeded(current));
            System.out.println("[DOTH] Replaced loadedEntityList for world " + world.provider.getDimension());
            return true;
        } catch (Exception e) {
            // try mcp name
            try {
                java.lang.reflect.Field f = net.minecraft.world.World.class.getDeclaredField("loadedEntityList");
                f.setAccessible(true);
                List<Entity> current = (List<Entity>) f.get(world);
                if (current instanceof DOTHEntityList) return false;
                f.set(world, wrapIfNeeded(current));
                System.out.println("[DOTH] Replaced loadedEntityList for world " + world.provider.getDimension());
                return true;
            } catch (Exception e2) {
                System.err.println("[DOTH] Failed to replace loadedEntityList: " + e2);
            }
        }
        return false;
    }
}
