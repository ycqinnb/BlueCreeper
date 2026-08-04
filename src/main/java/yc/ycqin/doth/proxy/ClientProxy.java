package yc.ycqin.doth.proxy;


import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import yc.ycqin.doth.client.DOTHKeyBind;
import yc.ycqin.doth.client.render.entity.RenderEntityCoin;
import yc.ycqin.doth.client.render.entity.RenderEntityItemSwordHighlight;
import yc.ycqin.doth.client.render.entity.RenderEntityPowerup;
import yc.ycqin.doth.client.render.shader.ShaderHelper;
import yc.ycqin.doth.common.entities.EntityCoin;
import yc.ycqin.doth.common.entities.EntityItemSwordHighlight;
import yc.ycqin.doth.common.entities.EntityRushPowerup;
import yc.ycqin.doth.common.item.ModelReg;
import yc.ycqin.doth.event.ClientEvent;
import yc.ycqin.doth.event.ClientLanguageSync;
import yc.ycqin.doth.event.EntityHighlightHandler;
import yc.ycqin.doth.event.RushClientHandler;

import java.util.Map;


public class ClientProxy extends CommonProxy {

    public void preInit(FMLPreInitializationEvent event){
        super.preInit(event);
        // 注册闪光掉落物的客户端渲染器
        RenderingRegistry.registerEntityRenderingHandler(
                EntityItemSwordHighlight.class,
                new RenderEntityItemSwordHighlight.Factory()
        );
        // 虫灵快跑实体渲染
        RenderingRegistry.registerEntityRenderingHandler(
                EntityCoin.class,
                new RenderEntityCoin.Factory()
        );
        RenderingRegistry.registerEntityRenderingHandler(
                EntityRushPowerup.class,
                new RenderEntityPowerup.Factory()
        );
        MinecraftForge.EVENT_BUS.register(new ClientEvent());
        MinecraftForge.EVENT_BUS.register(new EntityHighlightHandler());
        MinecraftForge.EVENT_BUS.register(new RushClientHandler());
        // 客户端语言同步到 LanguageMap（修复 TextComponentTranslation 聊天/血条显示英文）
        MinecraftForge.EVENT_BUS.register(new ClientLanguageSync());
        new ModelReg();
        ShaderHelper.initShaders();
    }

    public void init(FMLInitializationEvent event){
        super.init(event);
        DOTHKeyBind.register(event);
    }

}
