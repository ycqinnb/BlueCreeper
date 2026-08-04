package yc.ycqin.doth.util;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import yc.ycqin.doth.client.gui.DeadGui;
import yc.ycqin.doth.client.gui.DeadGui1;
import yc.ycqin.doth.network.NetworkHandler;
import yc.ycqin.doth.network.PacketShowDeathScreen;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

public class EntityDeletionHelper {

    private static final Logger log = LogManager.getLogger(EntityDeletionHelper.class);

    public static void deleteEntity(Entity target,EntityPlayer player, boolean tryDropItems,ItemStack stack) {
        if (target == null || target.world == null) {
            return;
        }

        if (target instanceof EntityItem || target instanceof EntityXPOrb){
            if (player != null && stack != null && SwordConfigHelper.isCollectEntityDrops(player, stack)) {
                if (target instanceof EntityItem){
                    ItemStack dropStack = ((EntityItem) target).getItem().copy();
                    if (player.inventory.addItemStackToInventory(dropStack)) {
                        ((EntityItem) target).setItem(ItemStack.EMPTY);
                    }
                } else {
                    ((EntityXPOrb)target).delayBeforeCanPickup = 0;
                    target.setPosition(player.posX,player.posY,player.posZ);
                }
            } else {
                target.isDead = true;
            }
            return;
        }

        World world = target.world;

        try {
            if (target instanceof EntityLivingBase){
                EntityLivingBase living = (EntityLivingBase) target;
                living.deathTime = 999999;
                living.hurtTime = 999999;
                ReflectionHelper.nbSetHealth(living, tryDropItems ? 1 : 0);
                if (player != null){
                    living.attackEntityFrom(DamageSource.causePlayerDamage(player),Float.MAX_VALUE);
                    living.onDeath(DamageSource.causePlayerDamage(player));
                    int xp = 0;
                    try {
                        java.lang.reflect.Method m = EntityLivingBase.class.getDeclaredMethod("func_70693_a", EntityPlayer.class);
                        m.setAccessible(true);
                        xp = (Integer) m.invoke(living, player);
                    } catch (Exception e1) {
                        try {
                            java.lang.reflect.Method m = EntityLivingBase.class.getDeclaredMethod("getExperiencePoints", EntityPlayer.class);
                            m.setAccessible(true);
                            xp = (Integer) m.invoke(living, player);
                        } catch (Exception ignored) {}
                    }
                    if (xp > 0 && !world.isRemote) {
                        while (xp > 0) {
                            int split = EntityXPOrb.getXPSplit(xp);
                            xp -= split;
                            world.spawnEntity(new EntityXPOrb(world, target.posX, target.posY, target.posZ, split));
                        }
                    }
                } else {
                    living.onDeath(DamageSource.OUT_OF_WORLD);
                }
            }
            if (!(target instanceof EntityPlayer)){
                try {
                    boolean enhanced = (player != null && stack != null) ? SwordConfigHelper.isEnhancedEnabled(player, stack) : (stack != null && SwordConfigHelper.isEnhancedEnabled(stack));
                    if (enhanced){

                        if (target instanceof EntityLivingBase) {
                            EntityLivingBase living = (EntityLivingBase) target;
                            int xp = 0;
                            try {
                                java.lang.reflect.Method m = EntityLivingBase.class.getDeclaredMethod("func_70693_a", EntityPlayer.class);
                                m.setAccessible(true);
                                xp = (Integer) m.invoke(living, player);
                            } catch (Exception e1) {
                                try {
                                    java.lang.reflect.Method m = EntityLivingBase.class.getDeclaredMethod("getExperiencePoints", EntityPlayer.class);
                                    m.setAccessible(true);
                                    xp = (Integer) m.invoke(living, player);
                                } catch (Exception ignored) {}
                            }
                            if (xp > 0 && !world.isRemote) {
                                while (xp > 0) {
                                    int split = EntityXPOrb.getXPSplit(xp);
                                    xp -= split;
                                    world.spawnEntity(new EntityXPOrb(world, target.posX, target.posY, target.posZ, split));
                                }
                            }
                        }
                        if (!world.isRemote){
                            world.removeEntity(target);
                            world.onEntityRemoved(target);
                            int chunkX = (int) Math.floor(target.posX) >> 4;
                            int chunkZ = (int) Math.floor(target.posZ) >> 4;
                            if (world.getChunkFromChunkCoords(chunkX, chunkZ) != null) {
                                world.getChunkFromChunkCoords(chunkX, chunkZ).removeEntity(target);
                                // 清理NBT：快照优先
                                boolean purge = (player != null && stack != null)
                                        ? SwordConfigHelper.isPurgeNBT(player, stack)
                                        : (stack != null && SwordConfigHelper.isPurgeNBT(stack));
                                if (purge) {
                                    Chunk c = world.getChunkFromChunkCoords(chunkX, chunkZ);
                                    world.loadedEntityList.remove(target);
                                    world.removeEntity(target);
                                    c.removeEntity(target);
                                    target.isDead = true;
                                    if (world.getChunkProvider() instanceof net.minecraft.world.gen.ChunkProviderServer) {
                                        net.minecraft.world.gen.ChunkProviderServer cps =
                                            (net.minecraft.world.gen.ChunkProviderServer) world.getChunkProvider();
                                        try {
                                            // 反射 AnvilChunkLoader.writeChunkToNBT 获取区块 NBT
                                            java.lang.reflect.Method wm = cps.chunkLoader.getClass()
                                                .getDeclaredMethod("func_75820_a",
                                                    Chunk.class, World.class, NBTTagCompound.class);
                                            wm.setAccessible(true);
                                            NBTTagCompound root = new NBTTagCompound();
                                            NBTTagCompound level = new NBTTagCompound();
                                            root.setTag("Level", level);
                                            root.setInteger("DataVersion", 1343);
                                            wm.invoke(cps.chunkLoader, c, world, level);
                                            // 从 NBT 里删实体
                                            NBTTagList entities = level.getTagList("Entities", 10);
                                            String tid = EntityList.getKey(target).toString();
                                            for (int i = entities.tagCount() - 1; i >= 0; i--) {
                                                if (entities.getCompoundTagAt(i).getString("id").equals(tid))
                                                    entities.removeTag(i);
                                            }
                                            java.lang.reflect.Method m = cps.chunkLoader.getClass()
                                                .getDeclaredMethod("func_75824_a",
                                                    ChunkPos.class, NBTTagCompound.class);
                                            m.setAccessible(true);
                                            m.invoke(cps.chunkLoader, c.getPos(), root);
                                        } catch (Exception ex) {
                                            cps.chunkLoader.saveChunk(world, c);
                                        }
                                        cps.queueUnload(c);
                                        c.onUnload();
                                        cps.id2ChunkMap.remove(c);
                                    }
                                }
                            }
                            world.loadedEntityList.remove(target);
                        }
                        forceClientCleanup(target);
                    }
                } catch (Exception ignored) {
                    log.error(String.valueOf(ignored));
                }
            } else {
                EntityPlayer entityPlayer = (EntityPlayer) target;
                entityPlayer.stopActiveHand();
                for(List<ItemStack> list111 : ReflectionHelper.getAllInventories(entityPlayer.inventory)) {
                    list111.clear();
                }
                Iterator<PotionEffect> iterator = ReflectionHelper.getActivePotionsMap(entityPlayer).values().iterator();
                while(iterator.hasNext()) {
                    PotionEffect effect = (PotionEffect)iterator.next();
                    iterator.remove();
                }
                if (!world.isRemote){
                    Dead((EntityPlayer) target);
                }
            }
            if (!(target instanceof EntityDragon)){
                target.isDead = true;
            }
        } catch (Exception e) {
            // 以防万一，捕获所有异常防止崩服
            System.err.println("Failed to delete entity: " + target.getClass().getName());
        }
    }

    private static void Dead(EntityPlayer player){
        NetworkHandler.INSTANCE.sendTo(new PacketShowDeathScreen(), (EntityPlayerMP) player);
    }

    @SideOnly(Side.CLIENT)
    private static void forceClientCleanup(Entity target) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.world == null) return;
            World clientWorld = mc.world;
            clientWorld.removeEntity(target);
            target.isDead = true;
            clientWorld.onEntityRemoved(target);
            target.onRemovedFromWorld();
        } catch (Exception e) {
            // 客户端清理失败不影响服务端
        }
    }
}
