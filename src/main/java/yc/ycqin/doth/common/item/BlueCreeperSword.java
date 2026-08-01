package yc.ycqin.doth.common.item;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemSword;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import yc.ycqin.doth.DOTHMod;
import yc.ycqin.doth.client.render.item.ICosmicRenderItem;
import yc.ycqin.doth.core.AntiDisarmTracker;
import yc.ycqin.doth.event.TooltipRenderer;
import yc.ycqin.doth.network.NetworkHandler;
import yc.ycqin.doth.network.SPacketKillNumber;
import yc.ycqin.doth.common.entities.EntityItemSwordHighlight;
import yc.ycqin.doth.util.EnhancedAttackManager;
import yc.ycqin.doth.util.EntityDeletionHelper;
import yc.ycqin.doth.util.SwordConfigHelper;

import yc.ycqin.doth.network.NetworkHandler;
import yc.ycqin.doth.network.PacketInstantBreak;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.UUID;

import static yc.ycqin.doth.event.SwordMagnetHandler.hasMagnetSword;

public class BlueCreeperSword extends ItemSword implements ICosmicRenderItem {

    // 使用钻石材料（可换成你喜欢的）
    private static final ToolMaterial MATERIAL = ToolMaterial.DIAMOND;

    /** 射线追踪每玩家冷却 (world tick) */
    private static final Map<UUID, Long> rayTraceCooldown = new HashMap<>();

    public BlueCreeperSword() {
        super(MATERIAL);
        setUnlocalizedName("bluecreepersword.blue_creeper_sword");
        setRegistryName("blue_creeper_sword");
        setCreativeTab(ItemReg.DOTH_TABLE); // 或你自己的创造标签
    }

    @Override
    public void onUpdate(ItemStack stack, World p_77663_2_, Entity entity, int p_77663_4_, boolean p_77663_5_) {
        if (entity instanceof EntityPlayer){
            EntityPlayer player = (EntityPlayer)entity;
            if (SwordConfigHelper.isAntiDisarm(player, stack)){
                UUID owner = SwordConfigHelper.getOwnerUUID(player, stack);
                if (owner != null && !owner.equals(player.getUniqueID())){
                    stack.setCount(0);
                }
            }
            player.isDead = false;
            player.deathTime = 0;
            player.hurtTime = 0;
            player.setHealth(player.getMaxHealth());
        }

        super.onUpdate(stack, p_77663_2_, entity, p_77663_4_, p_77663_5_);
    }

    // 无限耐久（可选，如果不需要可删除这两个覆写）
    @Override
    public boolean isDamageable() {
        return false;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return 0;
    }

    // ========== 秒挖 ==========
    @Override
    public float getDestroySpeed(ItemStack stack, IBlockState state) {
        if (SwordConfigHelper.isInstantMine(stack)) {
            return Float.MAX_VALUE;
        }
        return super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean canHarvestBlock(IBlockState state, ItemStack stack) {
        if (SwordConfigHelper.isInstantMine(stack)) {
            return true;
        }
        return super.canHarvestBlock(state, stack);
    }

    // 处理硬度为负（无法正常挖掘）的方块，如基岩、命令方块等
    @Override
    public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, EntityPlayer player) {
        if (!SwordConfigHelper.isInstantMine(player, stack)) {
            return false; // 没开秒挖，走原版逻辑
        }

        World world = player.world;
        IBlockState state = world.getBlockState(pos);

        // 硬度 >= 0 的方块由 getDestroySpeed 覆盖处理，无需干预
        if (state.getBlockHardness(world, pos) >= 0) {
            return false;
        }

        // 客户端：播放粒子 + 发包给服务端
        if (world.isRemote) {
            world.playEvent(2001, pos, Block.getStateId(state));
            NetworkHandler.INSTANCE.sendToServer(new PacketInstantBreak(pos));
            return true;
        }

        return false; // 服务端：由 PacketInstantBreak 的 Handler 处理
    }

    // ========== 掉落物闪光特效 ==========
    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    @Override
    public Entity createEntity(World world, Entity location, ItemStack itemstack) {
        EntityItemSwordHighlight ei = new EntityItemSwordHighlight(world, location.posX, location.posY, location.posZ, itemstack);
        ei.setDefaultPickupDelay();
        ei.motionX = location.motionX;
        ei.motionY = location.motionY;
        ei.motionZ = location.motionZ;
        if (location instanceof EntityItem) {
            ei.setThrower(((EntityItem) location).getThrower());
            ei.setOwner(((EntityItem) location).getOwner());
        }
        return ei;
    }

    // -------- ICosmicRenderItem 接口实现 --------
    @Override
    @SideOnly(Side.CLIENT)
    public TextureAtlasSprite getMaskTexture(ItemStack stack, EntityLivingBase player) {
        return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(
                new ResourceLocation(DOTHMod.MODID, "items/blue_creeper_sword_mask").toString()
        );

    }

    @Override
    @SideOnly(Side.CLIENT)
    public float getMaskOpacity(ItemStack stack, EntityLivingBase player) {
        return 1.0F; // 完全不透明
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
        if (player.world.isRemote) {
            TooltipRenderer.showTip("已清除实体："+ entity.getName());
            return true; // 客户端不做逻辑，但阻止原版伤害
        }

        // 射线追踪模式：跳过单体攻击，交给 onEntitySwing 统一处理
        if (SwordConfigHelper.isRayTrace(player, stack)) {
            return true;
        }

        // 检查配置是否允许攻击该目标
        if (!canAttackEntity(player, entity)) {
            return false; // 不允许攻击，交给原版处理
        }

        // 执行单体删除
        boolean enhanced = SwordConfigHelper.isEnhancedEnabled(player, stack);
        if (enhanced){
            EnhancedAttackManager.addTarget(entity);
        }
        EntityDeletionHelper.deleteEntity(entity,player,SwordConfigHelper.isTryDropItems(player, stack),stack);
        return true;
    }

    private void giveOrDrop(World world, BlockPos pos, EntityPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (hasMagnetSword(player) && player.inventory.addItemStackToInventory(stack)) return;
        Block.spawnAsEntity(world, pos, stack);
    }

    /** 射线追踪：沿视线扫描实体并攻击，直到撞到方块或超出距离 */
    private void doRayTrace(EntityPlayer player, ItemStack stack) {
        Vec3d start = player.getPositionEyes(1.0F);
        Vec3d look = player.getLookVec();
        boolean instantMine = SwordConfigHelper.isInstantMine(player, stack);
        boolean tryDrop = SwordConfigHelper.isTryDropItems(player, stack);
        boolean enhanced = SwordConfigHelper.isEnhancedEnabled(player, stack);
        double maxDist = 128.0D;

        for (double d = 0.5D; d < maxDist; d += 0.5D) {
            Vec3d pos = new Vec3d(start.x + look.x * d, start.y + look.y * d, start.z + look.z * d);
            BlockPos blockPos = new BlockPos(pos);
            IBlockState state = player.world.getBlockState(blockPos);

            // 碰到非空气方块：秒挖开启则破坏，然后停止
            if (!state.getBlock().isAir(state, player.world, blockPos)) {
                if (instantMine) {
                    boolean unbreakable = state.getBlockHardness(player.world, blockPos) < 0;

                    if (unbreakable) {
                        // 不可破坏方块：跟 PacketInstantBreak 同样处理
                        if (!player.capabilities.isCreativeMode) {
                            state.getBlock().harvestBlock(player.world, player, blockPos, state,
                                    player.world.getTileEntity(blockPos), player.getHeldItemMainhand());
                            List<ItemStack> drops = state.getBlock().getDrops(player.world, blockPos, state, 0);
                            if (!drops.stream().anyMatch(s -> !s.isEmpty())) {
                                ItemStack blockStack = new ItemStack(state.getBlock(), 1,
                                        state.getBlock().damageDropped(state));
                                if (!blockStack.isEmpty()) giveOrDrop(player.world, blockPos, player, blockStack);
                            }
                        }
                        player.world.setBlockToAir(blockPos);
                    } else {
                        // 正常方块：tryHarvestBlock 触发 HarvestDropsEvent（磁吸用）
                        if (!((EntityPlayerMP)player).interactionManager.tryHarvestBlock(blockPos)) {
                            player.world.destroyBlock(blockPos, true);
                        }
                    }
                }
                return;
            }

            // 扫描该点的实体
            AxisAlignedBB box = new AxisAlignedBB(
                pos.x - 0.4D, pos.y - 0.4D, pos.z - 0.4D,
                pos.x + 0.4D, pos.y + 0.4D, pos.z + 0.4D
            );
            for (Entity e : player.world.getEntitiesWithinAABBExcludingEntity(player, box)) {
                if (e.isDead) continue;
                if (canAttackEntity(player, e)) {
                    if (enhanced) EnhancedAttackManager.addTarget(e);
                    EntityDeletionHelper.deleteEntity(e, player, tryDrop, stack);
                }
            }
        }
    }

    /**
     * 挥剑 → 射线追踪模式：沿视线扫描实体并攻击（带冷却防连续触发）
     */
    @Override
    public boolean onEntitySwing(EntityLivingBase entityLiving, ItemStack stack) {
        if (!(entityLiving instanceof EntityPlayer)) return super.onEntitySwing(entityLiving, stack);
        EntityPlayer player = (EntityPlayer) entityLiving;
        if (!SwordConfigHelper.isRayTrace(player, stack)) return super.onEntitySwing(entityLiving, stack);
        if (player.world.isRemote) return super.onEntitySwing(entityLiving, stack);

        // 冷却：每次挥剑至少间隔 3 tick
        long now = player.world.getTotalWorldTime();
        Long last = rayTraceCooldown.get(player.getUniqueID());
        if (last != null && now - last < 3) return super.onEntitySwing(entityLiving, stack);
        rayTraceCooldown.put(player.getUniqueID(), now);

        doRayTrace(player, stack);
        return super.onEntitySwing(entityLiving, stack);
    }

    /**
     * 右键使用 → 范围攻击 或 全局攻击
     */
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        // 判断是否按住 Shift（潜行）
        if (player.isSneaking()) {
            // Shift + 右键：攻击所有加载的实体（全局抹除）
            if (!world.isRemote){
                attackAllEntities(player,stack);
            } else {
                if (EnhancedAttackManager.resetBooleans) {
                    EnhancedAttackManager.resetAllModBooleans();
                }
                if (EnhancedAttackManager.resetLists) {
                    EnhancedAttackManager.resetAllModLists();
                }
            }
        } else {
            if (!world.isRemote){
                // 普通右键：10x10 范围攻击
                attackRangeEntities(player, 10.0D,stack);
            }
        }

        // 设置物品使用动作（格挡动画，看起来更帅）
        player.setActiveHand(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    // ============================================================
    //                    辅助方法
    // ============================================================

    /**
     * 判断当前配置是否允许攻击该目标实体
     */
    private boolean canAttackEntity(EntityPlayer player, Entity target) {
        ItemStack stack = player.getHeldItemMainhand();

        boolean attackPassive = SwordConfigHelper.isAttackPassive(player, stack);
        boolean attackPlayers = SwordConfigHelper.isAttackPlayers(player, stack);
        boolean attackAll = SwordConfigHelper.isAttackAllEntities(player, stack);
        if (target instanceof EntityMob){
            return true;
        }
        // 2. 攻击玩家
        if (target instanceof EntityPlayer) {
            // 不能攻击自己
            if (target == player) return false;
            return attackPlayers;
        }

        // 3. 攻击非敌对生物（动物、村民等）
        if (target instanceof EntityAnimal) {
            return attackPassive;
        }

        return attackAll;
    }

    /**
     * 10x10 范围攻击
     */
    private void attackRangeEntities(EntityPlayer player, double radius,ItemStack stack) {
        AxisAlignedBB aabb = player.getEntityBoundingBox().grow(radius);
        List<Entity> targets = player.world.getEntitiesWithinAABB(Entity.class, aabb,
                e -> e != player && e != player.getRidingEntity() && canAttackEntity(player, e)
        );
        int count = 0;
        for (Entity target : targets) {
            boolean enhanced = SwordConfigHelper.isEnhancedEnabled(player, stack);
            if (enhanced){
                EnhancedAttackManager.addTarget(target);
            }
            EntityDeletionHelper.deleteEntity(target,player,SwordConfigHelper.isTryDropItems(player, stack),stack);
            count++;
        }
        NetworkHandler.INSTANCE.sendTo(new SPacketKillNumber(count), (EntityPlayerMP) player);
    }

    /**
     * 攻击世界中所有加载的实体（Shift + 右键）
     * 注意：这个操作非常危险，会删除所有符合条件的实体
     */
    private void attackAllEntities(EntityPlayer player,ItemStack stack) {
        List<Entity> targets = new ArrayList<>(player.world.loadedEntityList);
        int count = 0;
        for (Entity target : targets) {
            if (target == player) {
                continue;
            }
            boolean enhanced = SwordConfigHelper.isEnhancedEnabled(player, stack);
            if (enhanced){
                EnhancedAttackManager.addTarget(target);
            }
            if (canAttackEntity(player, target)) {
                EntityDeletionHelper.deleteEntity(target,player,SwordConfigHelper.isTryDropItems(player, stack),stack);
            }
            count++;
        }
        NetworkHandler.INSTANCE.sendTo(new SPacketKillNumber(count), (EntityPlayerMP) player);
        if (EnhancedAttackManager.resetBooleans) {
            EnhancedAttackManager.resetAllModBooleans();
        }
        if (EnhancedAttackManager.resetLists) {
            EnhancedAttackManager.resetAllModLists();
        }
    }

    // ========== 右键动作设置（格挡动画） ==========
    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.BLOCK;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 72000;
    }
}