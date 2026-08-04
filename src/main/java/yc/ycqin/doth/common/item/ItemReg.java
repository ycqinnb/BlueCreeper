package yc.ycqin.doth.common.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ItemReg {
    public static final CreativeTabs DOTH_TABLE = new CreativeTabs("doth") {
        @Override
        public ItemStack getTabIconItem() {
            return new ItemStack(BLUE_CREEPER_SWORD);
        }
    };

    public static final BlueCreeperSword BLUE_CREEPER_SWORD = new BlueCreeperSword();
    public static final CameraItem CAMERA = new CameraItem();
    public static final BioPhotoItem BIO_PHOTO = new BioPhotoItem();
    public static final CompressedClipItem COMPRESSED_CLIP = new CompressedClipItem();
    public static final ItemRushTicket RUSH_TICKET = new ItemRushTicket();

    public ItemReg() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
                BLUE_CREEPER_SWORD,
                CAMERA,
                BIO_PHOTO,
                COMPRESSED_CLIP
        );
        // 生物快跑入场券：仅在注入开关开启时注册（关闭则不显示）
        if (yc.ycqin.doth.core.DOTHConfig.enableRushInjection) {
            event.getRegistry().register(RUSH_TICKET);
        }
    }
}
