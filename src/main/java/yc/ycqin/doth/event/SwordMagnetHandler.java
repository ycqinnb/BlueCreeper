package yc.ycqin.doth.event;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import yc.ycqin.doth.common.item.BlueCreeperSword;
import yc.ycqin.doth.common.item.ItemReg;
import yc.ycqin.doth.core.AntiDisarmTracker;
import yc.ycqin.doth.network.NetworkHandler;
import yc.ycqin.doth.network.PacketInstantBreak;
import yc.ycqin.doth.util.SwordConfigHelper;

import java.util.*;
import java.util.UUID;

/**
 * 蓝C小剑剑 — 秒挖 & 掉落磁吸事件处理器
 *
 * 秒挖：手上拿剑 + instantMine 启用 → 任何方块瞬挖
 * 磁吸：剑在背包任意位置 + magnetDrops 启用 → 掉落物直接进包
 */
public class SwordMagnetHandler {

    // ==================== 秒挖 ====================

    // 拦截 UI 打开（替代 ASM 注入 Minecraft.displayGuiScreen）
    @SubscribeEvent
    public void onGuiOpen(net.minecraftforge.client.event.GuiOpenEvent event) {
        if (event.getGui() == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || !hasSwordInInventory(mc.player)) return;

        // 死亡界面：直接关闭
        if (event.getGui().getClass().getName().equals("net.minecraft.client.gui.GuiGameOver")) {
            event.setCanceled(true);
            mc.player.isDead = false;
            mc.player.setHealth(mc.player.getMaxHealth());
            return;
        }

        // 非原版 UI：配置控制
        String cls = event.getGui().getClass().getName();
        if (!cls.startsWith("net.minecraft.client.gui")
                && !cls.startsWith("net.minecraftforge.client")
                && !cls.startsWith("yc.ycqin.doth")) {
            for (ItemStack stack : mc.player.inventory.mainInventory) {
                if (stack.getItem() instanceof BlueCreeperSword
                        && SwordConfigHelper.isCloseNonVanillaGui(stack)) {
                    event.setCanceled(true);
                    return;
                }
            }
            if (AntiDisarmTracker.isProtected(mc.player)) {
                AntiDisarmTracker.ConfigSnapshot cfg = AntiDisarmTracker.getConfig(mc.player);
                if (cfg != null && cfg.closeNonVanillaGui) {
                    event.setCanceled(true);
                }
            }
        }
    }

    // 拦截左键点击——在 hardness 检查之前触发，专治基岩等不可破坏方块
    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getWorld().isRemote) return; // 只在客户端拦截

        EntityPlayer player = event.getEntityPlayer();
        ItemStack held = player.getHeldItemMainhand();
        if (!(held.getItem() instanceof BlueCreeperSword) || !SwordConfigHelper.isInstantMine(player, held)) {
            return;
        }

        BlockPos pos = event.getPos();
        IBlockState state = event.getWorld().getBlockState(pos);
        if (state.getBlockHardness(event.getWorld(), pos) < 0) {
            event.setCanceled(true);
            event.getWorld().playEvent(2001, pos, Block.getStateId(state)); // 粒子
            NetworkHandler.INSTANCE.sendToServer(new PacketInstantBreak(pos)); // 通知服务端
        }
    }

    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        EntityPlayer player = event.getEntityPlayer();
        ItemStack held = player.getHeldItemMainhand();
        if (held.getItem() instanceof BlueCreeperSword && SwordConfigHelper.isInstantMine(player, held)) {
            event.setNewSpeed(Float.MAX_VALUE);
        }
    }

    // ==================== 飞行 ====================

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        EntityPlayer player = event.player;
        boolean hasSword = hasSwordInInventory(player);

        // 飞行
        if (hasSword) {
            if (!player.capabilities.allowFlying) {
                player.capabilities.allowFlying = true;
                player.sendPlayerAbilities();
            }
        }
        // Buff 替换（每 tick）
        if (hasSword) {
            applyConfiguredBuffs(player);
        }

        // 快照补剑：防缴械 + 快照补剑同时开启 → 背包无剑时用快照重建
        if (AntiDisarmTracker.isProtected(player)) {
            AntiDisarmTracker.ConfigSnapshot cfg = AntiDisarmTracker.getConfig(player);
            if (cfg != null && cfg.autoRecreate && !hasActualSwordInInventory(player)) {
                ItemStack newSword = new ItemStack(ItemReg.BLUE_CREEPER_SWORD);
                SwordConfigHelper.applySnapshot(newSword, cfg, player.getUniqueID());
                player.inventory.mainInventory.set(player.inventory.currentItem, newSword);
            }
        }
    }

    /** 检查背包中是否确实有蓝C剑（不受防缴械保护标记影响） */
    public static boolean hasActualSwordInInventory(EntityPlayer player) {
        if (player == null || player.inventory == null) return false;
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack.getItem() instanceof BlueCreeperSword) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasSwordInInventory(EntityPlayer player) {
        if (player == null || player.inventory == null) return false;
        if (AntiDisarmTracker.isProtected(player)) return true;
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack.getItem() instanceof BlueCreeperSword) {
                return true;
            }
        }
        return false;
    }

    // ==================== Buff 增强 ====================

    private static boolean hasBuffsEnabled(EntityPlayer player) {
        if (player == null || player.inventory == null) return false;
        AntiDisarmTracker.ConfigSnapshot cfg = AntiDisarmTracker.getConfig(player);
        if (cfg != null) return cfg.enableBuffs;
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack.getItem() instanceof BlueCreeperSword
                    && SwordConfigHelper.isEnableBuffs(stack)) {
                return true;
            }
        }
        return false;
    }

    private static void applyConfiguredBuffs(EntityPlayer player) {
        Map<String, Integer> buffMap = null;
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack.getItem() instanceof BlueCreeperSword
                    && SwordConfigHelper.isEnableBuffs(stack)) {
                buffMap = SwordConfigHelper.getBuffConfig(stack);
                break;
            }
        }
        if (buffMap == null) {
            AntiDisarmTracker.ConfigSnapshot cfg = AntiDisarmTracker.getConfig(player);
            if (cfg != null && cfg.enableBuffs) buffMap = cfg.buffConfig;
        }
        if (buffMap == null || buffMap.isEmpty()) return;

        // 清除不在列表中的 buff
        Iterator<PotionEffect> it = player.getActivePotionMap().values().iterator();
        while (it.hasNext()) {
            PotionEffect pe = it.next();
            String reg = pe.getPotion().getRegistryName().toString();
            int lvl = buffMap.getOrDefault(reg, -1);
            if (lvl <= 0) {
                it.remove();
            }
        }

        // 添加/刷新列表中的 buff
        for (Map.Entry<String, Integer> e : buffMap.entrySet()) {
            Potion p = Potion.getPotionFromResourceLocation(e.getKey());
            if (p == null || e.getValue() <= 0) continue;
            player.addPotionEffect(new PotionEffect(p, 400, e.getValue() - 1, false, false));
        }
    }

    // ==================== 防缴械：掉落物 TP ====================

    @SubscribeEvent
    public void onItemSpawn(net.minecraftforge.event.entity.EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote) return;
        if (!(event.getEntity() instanceof EntityItem)) return;
        EntityItem ei = (EntityItem) event.getEntity();
        ItemStack stack = ei.getItem();
        if (!(stack.getItem() instanceof BlueCreeperSword)
                || !SwordConfigHelper.isAntiDisarm(stack)) return;

        UUID ownerUUID = SwordConfigHelper.getOwnerUUID(stack);
        if (ownerUUID == null) return;
        EntityPlayer owner = event.getWorld().getPlayerEntityByUUID(ownerUUID);
        if (owner == null) return;

        ei.setPosition(owner.posX, owner.posY + 1, owner.posZ);
        ei.setPickupDelay(0);
    }

    // ==================== 磁吸：怪物掉落 ====================

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        if (player.world.isRemote) return;

        if (!hasMagnetSword(player)) return;

        event.getDrops().removeIf(item -> attemptPickup(player, item));
    }

    // ==================== 磁吸：方块掉落 ====================

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHarvestDrops(BlockEvent.HarvestDropsEvent event) {
        if (event.getWorld().isRemote) return;
        EntityPlayer player = event.getHarvester();
        if (player == null) return;
        if (!hasMagnetSword(player)) return;

        Iterator<ItemStack> it = event.getDrops().iterator();
        while (it.hasNext()) {
            ItemStack drop = it.next();
            // 模拟原版掉落几率（受时运影响）
            if (event.getWorld().rand.nextFloat() <= event.getDropChance()) {
                EntityItem dummy = new EntityItem(event.getWorld(), player.posX, player.posY, player.posZ, drop);
                if (attemptPickup(player, dummy)) {
                    it.remove();
                }
            } else {
                it.remove();
            }
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 检查玩家背包中是否有启用磁吸功能的蓝C剑
     */
    public static boolean hasMagnetSword(EntityPlayer player) {
        if (player == null || player.inventory == null) return false;
        AntiDisarmTracker.ConfigSnapshot cfg = AntiDisarmTracker.getConfig(player);
        if (cfg != null) return cfg.magnetDrops;
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack.getItem() instanceof BlueCreeperSword && SwordConfigHelper.isMagnetDrops(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 尝试将掉落物吸入玩家背包
     * @return true 表示吸进去了，false 表示背包满了留在原地
     */
    private static boolean attemptPickup(EntityPlayer player, EntityItem item) {
        if (item.getItem().isEmpty()) return false;
        item.setNoPickupDelay();
        try {
            item.onCollideWithPlayer(player);
        } catch (Exception ignored) {
            // 某些 mod 可能导致异常
        }
        if (!item.isDead) {
            item.setDefaultPickupDelay();
            return false;
        }
        return true;
    }
}
