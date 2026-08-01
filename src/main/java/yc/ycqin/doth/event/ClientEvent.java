package yc.ycqin.doth.event;

import codechicken.lib.util.TransformUtils;
import com.dhanantry.scapeandrunparasites.entity.ai.misc.EntityParasiteBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.*;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;
import yc.ycqin.doth.DOTHMod;
import yc.ycqin.doth.client.render.item.CosmicItemRender;
import yc.ycqin.doth.client.render.shader.CosmicShaderHelper;

import java.nio.FloatBuffer;

@SideOnly(Side.CLIENT)
public class ClientEvent {
    public static FloatBuffer cosmicUVs = BufferUtils.createFloatBuffer(4 * 10);

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onModelBake(ModelBakeEvent event) {
        System.out.println("onModelBake");
        ModelResourceLocation swordLocation = new ModelResourceLocation(
                new ResourceLocation(DOTHMod.MODID, "blue_creeper_sword"),
                "inventory"
        );

        // 2. 从注册表中获取原版模型（普通物品模型）
        IBakedModel originalModel = event.getModelRegistry().getObject(swordLocation);
        if (originalModel == null) {
            // 如果没找到，可能是模型还没加载，或者注册名错了
            System.err.println("Failed to find model for blue_creeper_sword!");
            return;
        }

        // 3. 用 CosmicItemRender 包装原模型
        //    参数1：IModelState（CCL 提供的默认转换工具）
        //    参数2：原始模型
        CosmicItemRender cosmicModel = new CosmicItemRender(
                TransformUtils.DEFAULT_ITEM,  // 来自 CCL，定义物品的视角变换
                originalModel
        );

        // 4. 把包装后的模型放回注册表，覆盖原模型
        event.getModelRegistry().putObject(swordLocation, cosmicModel);

        System.out.println("Blue Creeper Sword model replaced with CosmicItemRender!");
    }


    @SubscribeEvent
    public void onDrawScreenPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        CosmicShaderHelper.inventoryRender = true;
    }

    @SubscribeEvent
    public void onDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        CosmicShaderHelper.inventoryRender = false;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        // 只在每帧开始时更新一次
        if (event.phase != TickEvent.Phase.START) return;

        // 确保纹理已加载（防止空指针）
        if (COSMIC_SLOTS == null || COSMIC_SLOTS[0] == null) {
            System.out.println("[DOTH] NEBULA texture is null!");
            return;
        }
        // 创建缓冲区：10 个纹理 × 每组 4 个浮点数（minU, minV, maxU, maxV）
        cosmicUVs = BufferUtils.createFloatBuffer(4 * COSMIC_SLOTS.length);

        // 循环填充（因为 10 个槽位都是同一张图，UV 都一样）
        for (TextureAtlasSprite sprite : COSMIC_SLOTS) {
            cosmicUVs.put(sprite.getMinU());
            cosmicUVs.put(sprite.getMinV());
            cosmicUVs.put(sprite.getMaxU());
            cosmicUVs.put(sprite.getMaxV());
        }

        // 翻转缓冲区，准备读取
        cosmicUVs.flip();
    }

    public static TextureAtlasSprite NEBULA;

    public static TextureAtlasSprite[] COSMIC_SLOTS = new TextureAtlasSprite[10];

    @SubscribeEvent
    public void onTextureStitch(TextureStitchEvent.Pre event) {

        System.out.println("[doth] Registering textures");

        event.getMap().registerSprite(new ResourceLocation("bluecreepersword:items/blue_creeper_sword_mask"));

        NEBULA = event.getMap().registerSprite(new ResourceLocation("bluecreepersword:shader/blue_creeper"));

        // 用同一张纹理填满 10 个槽位
        for (int i = 0; i < 10; i++) {
            COSMIC_SLOTS[i] = NEBULA;
        }
    }
}
