package yc.ycqin.doth.event;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.translation.LanguageMap;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Map;

/**
 * 客户端语言同步：
 * 1.12.2 的 Forge 只把 en_US 注入 util LanguageMap（TextComponentTranslation 用它渲染聊天），
 * 从不注入玩家当前语言 —— 导致聊天/Boss 血条里的翻译组件永远显示英文。
 * 这里把客户端当前语言（Locale）同步进 LanguageMap，语言切换时自动更新。
 * 对文本组件渲染零侵入，仅补全翻译表。
 */
@SideOnly(Side.CLIENT)
public class ClientLanguageSync {

    private static String lastLanguage = "";

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.getLanguageManager() == null) return;
        try {
            String cur = mc.getLanguageManager().getCurrentLanguage().getLanguageCode();
            if (cur != null && cur.equals(lastLanguage)) return;
            lastLanguage = cur;
            sync();
        } catch (Exception ignored) {
        }
    }

    private static void sync() {
        try {
            // 客户端当前语言的翻译表（Locale）
            java.lang.reflect.Field f1 = ReflectionHelper.findField(
                    net.minecraft.client.resources.I18n.class, "i18nLocale");
            f1.setAccessible(true);
            Object locale = f1.get(null);
            if (locale == null) return;
            java.lang.reflect.Field f2 = ReflectionHelper.findField(
                    net.minecraft.client.resources.Locale.class, "field_135032_a");
            f2.setAccessible(true);
            Map<?, ?> localeMap = (Map<?, ?>) f2.get(locale);
            if (localeMap == null) return;
            // util LanguageMap（TextComponentTranslation 的翻译源）
            java.lang.reflect.Field f3 = ReflectionHelper.findField(
                    LanguageMap.class, "field_74817_a");
            f3.setAccessible(true);
            Object lm = f3.get(null);
            if (lm == null) return;
            java.lang.reflect.Field f4 = ReflectionHelper.findField(
                    LanguageMap.class, "field_74816_c");
            f4.setAccessible(true);
            Map<Object, Object> lmMap = (Map<Object, Object>) f4.get(lm);
            if (lmMap == null) return;
            lmMap.putAll(localeMap);
        } catch (Throwable t) {
            System.out.println("[DOTH] LanguageMap sync failed: " + t);
        }
    }
}
