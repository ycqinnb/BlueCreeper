package yc.ycqin.doth;

import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import org.apache.logging.log4j.Logger;
import yc.ycqin.doth.command.CommandAllreturn;
import yc.ycqin.doth.command.CommandRushDebug;
import yc.ycqin.doth.common.block.BlockReg;
import yc.ycqin.doth.common.entities.EntityCoin;
import yc.ycqin.doth.common.entities.EntityItemSwordHighlight;
import yc.ycqin.doth.common.entities.EntityRushPowerup;
import yc.ycqin.doth.common.item.ItemReg;
import yc.ycqin.doth.common.item.RecipeRushTicket;
import yc.ycqin.doth.core.ProtectHelper;
import yc.ycqin.doth.event.ArenaFriendlyFireHandler;
import yc.ycqin.doth.event.BlockHighlightHandler;
import yc.ycqin.doth.event.PhotoCleanupHandler;
import yc.ycqin.doth.event.SwordMagnetHandler;
import yc.ycqin.doth.proxy.CommonProxy;
import yc.ycqin.doth.world.ArenaManager;
import yc.ycqin.doth.world.RushManager;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import yc.ycqin.doth.core.DOTHConfig;
import yc.ycqin.doth.core.DOTHEventBus;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

@Mod(modid = DOTHMod.MODID, name = DOTHMod.NAME, version = DOTHMod.VERSION)
public class DOTHMod
{
    public static final String MODID = "bluecreepersword";
    public static final String NAME = "蓝C牌电子榨菜";
    public static final String VERSION = "1.2.0";

    public static Set<Entity> dead = new HashSet<>();

    @Mod.Instance(DOTHMod.MODID)
    public static DOTHMod instance;
    @SidedProxy
            (clientSide = "yc.ycqin.doth.proxy.ClientProxy",
                    serverSide = "yc.ycqin.doth.proxy.CommonProxy"
            )
    private static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        // 注册方块
        new BlockReg();
        // 注册闪光掉落物实体
        registerEntities();
        proxy.preInit(event);
    }

    private void registerEntities() {
        int eid = 0;
        EntityRegistry.registerModEntity(
                new ResourceLocation(MODID, "sword_highlight"),
                EntityItemSwordHighlight.class,
                "SwordHighlight", eid++,
                instance, 64, 20, true
        );
        EntityRegistry.registerModEntity(
                new ResourceLocation(MODID, "rush_coin"),
                EntityCoin.class,
                "RushCoin", eid++,
                instance, 64, 1, true
        );
        EntityRegistry.registerModEntity(
                new ResourceLocation(MODID, "rush_powerup"),
                EntityRushPowerup.class,
                "RushPowerup", eid++,
                instance, 64, 1, true
        );
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        DOTHConfig.reload();

        // 注册斗蛐蛐维度（ID 666，占用则 +100，最多 3 次）
        ArenaManager.registerDimension();

        // 注册秒挖 + 磁吸事件监听器（注册到我们自己的 EventBus 上）
        MinecraftForge.EVENT_BUS.register(new SwordMagnetHandler());
        // 注册方块高亮渲染
        MinecraftForge.EVENT_BUS.register(new BlockHighlightHandler());
        // 注册照片清理
        MinecraftForge.EVENT_BUS.register(new PhotoCleanupHandler());
        // 注册斗蛐蛐防友伤
        MinecraftForge.EVENT_BUS.register(new ArenaFriendlyFireHandler());
        // 注册斗蛐蛐每 tick 逻辑
        MinecraftForge.EVENT_BUS.register(new ArenaTickHandler());

        // ===== 生物快跑（不依赖 SRP；装载 SRP 时默认坐骑为虫灵） =====
        // 跟随注入开关：关闭时整个生物快跑不注册（入场券不显示）
        if (DOTHConfig.enableRushInjection) {
            // 维度
            RushManager.registerDimension();
            // 维度守卫事件（维度未注册时全部空转，无影响）
            MinecraftForge.EVENT_BUS.register(RushManager.class);
            // 每 tick 逻辑
            MinecraftForge.EVENT_BUS.register(new RushTickHandler());
        }
        proxy.init(event);
    }

        @EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandAllreturn());
        event.registerServerCommand(new CommandRushDebug());
    }

    /** 斗蛐蛐维度每 tick 逻辑 */
    public static class ArenaTickHandler {
        @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
        public void onServerTick(net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent event) {
            if (event.phase == net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END) {
                ArenaManager.onServerTick();
            }
        }
    }

    /** 生物快跑每 tick 逻辑 */
    public static class RushTickHandler {
        @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
        public void onServerTick(net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent event) {
            if (event.phase == net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END) {
                RushManager.onServerTick();
            }
        }
    }

    /** 配方注册（生物快跑入场券，不依赖 SRP；跟随注入开关） */
    @Mod.EventBusSubscriber(modid = DOTHMod.MODID)
    public static class RecipeRegistrar {
        @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
        public static void registerRecipes(net.minecraftforge.event.RegistryEvent.Register<net.minecraft.item.crafting.IRecipe> event) {
            if (DOTHConfig.enableRushInjection) {
                event.getRegistry().register(
                        new RecipeRushTicket().setRegistryName(new ResourceLocation(MODID, "rush_ticket")));
            }
        }
    }
}
