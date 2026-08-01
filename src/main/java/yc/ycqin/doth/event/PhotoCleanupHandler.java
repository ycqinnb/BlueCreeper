package yc.ycqin.doth.event;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import yc.ycqin.doth.common.block.ItemBlockFighter;
import yc.ycqin.doth.common.item.BioPhotoItem;

import java.io.File;
import java.util.List;

/**
 * 合成相关逻辑：
 * 1. 合成消耗照片时清理磁盘照片文件
 * 2. 合成选手方块时，把生物照片 NBT + 药水效果写入输出 NBT
 */
public class PhotoCleanupHandler {

    @SubscribeEvent
    public void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.player.world.isRemote) {
            cleanupPhotos(event);
        }
        transferFighterNbt(event);
    }

    /** 清理合成消耗的照片文件（仅客户端） */
    private void cleanupPhotos(PlayerEvent.ItemCraftedEvent event) {
        for (int i = 0; i < event.craftMatrix.getSizeInventory(); i++) {
            ItemStack stack = event.craftMatrix.getStackInSlot(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BioPhotoItem)) continue;

            NBTTagCompound tag = stack.getTagCompound();
            if (tag == null || !tag.hasKey("PhotoPath")) continue;

            String path = tag.getString("PhotoPath");
            File file = new File(path);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    /**
     * 选手方块合成：把输入生物照片的 EntityIDs + 药水瓶的效果复制到输出 NBT
     */
    private void transferFighterNbt(PlayerEvent.ItemCraftedEvent event) {
        ItemStack output = event.crafting;
        if (output.isEmpty() || !(output.getItem() instanceof ItemBlockFighter)) return;

        NBTTagList entityIds = new NBTTagList();
        NBTTagList potionEffects = new NBTTagList();

        for (int i = 0; i < event.craftMatrix.getSizeInventory(); i++) {
            ItemStack stack = event.craftMatrix.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            // 生物照片 → 复制 EntityIDs
            if (stack.getItem() instanceof BioPhotoItem && stack.hasTagCompound()) {
                NBTTagCompound tag = stack.getTagCompound();
                if (tag.hasKey("EntityIDs")) {
                    entityIds = tag.getTagList("EntityIDs", 8);
                }
            }

            // 药水瓶 → 读效果
            if (stack.getItem() instanceof net.minecraft.item.ItemPotion) {
                List<PotionEffect> effects = PotionUtils.getEffectsFromStack(stack);
                for (PotionEffect effect : effects) {
                    potionEffects.appendTag(effect.writeCustomPotionEffectToNBT(new NBTTagCompound()));
                }
            }
        }

        // 构造 FighterNbt
        NBTTagCompound fighterNbt = new NBTTagCompound();
        if (entityIds.tagCount() > 0) {
            fighterNbt.setTag("EntityIDs", entityIds);
        }
        if (potionEffects.tagCount() > 0) {
            fighterNbt.setTag("PotionEffects", potionEffects);
        }

        NBTTagCompound outer = new NBTTagCompound();
        outer.setTag("FighterNbt", fighterNbt);
        output.setTagCompound(outer);
    }
}
