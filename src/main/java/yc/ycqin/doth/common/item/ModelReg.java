package yc.ycqin.doth.common.item;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ModelReg {
    public ModelReg(){
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void registerModels(ModelRegistryEvent event) {
        registerModel(ItemReg.BLUE_CREEPER_SWORD);
        registerModel(ItemReg.CAMERA);
        registerModel(ItemReg.BIO_PHOTO);
        // 生物快跑入场券（跟随注入开关，关闭不注册则不显示）
        if (yc.ycqin.doth.core.DOTHConfig.enableRushInjection) {
            registerModel(ItemReg.RUSH_TICKET);
        }
        // compressed_clip 有 subtypes，需要逐个注册
        for (int i = 0; i <= CompressedClipItem.MAX_LEVEL; i++) {
            ModelLoader.setCustomModelResourceLocation(ItemReg.COMPRESSED_CLIP, i,
                    new ModelResourceLocation(ItemReg.COMPRESSED_CLIP.getRegistryName(), "inventory"));
        }
        // 方块模型
        registerModel(net.minecraft.item.Item.getItemFromBlock(yc.ycqin.doth.common.block.BlockReg.BLOCK_ARENA));
        registerModel(net.minecraft.item.Item.getItemFromBlock(yc.ycqin.doth.common.block.BlockReg.BLOCK_FIGHTER));
    }

    private void registerModel(Item item) {
        ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}
