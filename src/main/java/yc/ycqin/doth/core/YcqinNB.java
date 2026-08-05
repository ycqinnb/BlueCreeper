package yc.ycqin.doth.core;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.common.asm.transformers.PatchingTransformer;
import net.minecraftforge.fml.relauncher.CoreModManager;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import yc.ycqin.doth.agent.Agent;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.Name("dothcore")
@IFMLLoadingPlugin.SortingIndex(Integer.MAX_VALUE)
@IFMLLoadingPlugin.TransformerExclusions("yc.ycqin.doth.")
public class YcqinNB implements IFMLLoadingPlugin {

    private static final String[] FORGE_PREFIXES = {
            "net.minecraftforge.fml.",
            "net.minecraftforge.classloading.",
            "net.minecraftforge.common.",
            "cpw.mods.fml.",
            "net.minecraftforge.registries.",
            "net.minecraftforge.event."
    };

    // 自己的包名前缀
    private static final String SELF_PREFIX = "yc.ycqin.doth.";



    public YcqinNB() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
       DOTHConfig.reload();
       if (DOTHConfig.enableAllReturn){
           clearAllButForgeAndSelf();
           List<IClassTransformer> qq = new ArrayList<>();
           qq.add((IClassTransformer) Launch.classLoader.loadClass(PatchingTransformer.class.getName()).newInstance());
           qq.add((IClassTransformer) Launch.classLoader.loadClass(ProtectClassTransformer.class.getName()).newInstance());
           qq.add((IClassTransformer) Launch.classLoader.loadClass(PlayerDeadTransformer.class.getName()).newInstance());
           setTransformersLast(qq);
       }
    }
    @Override public String getModContainerClass() { return null; }
    @Nullable
    @Override public String getSetupClass() { return null; }

    @Override
    public void injectData(Map<String, Object> map) {

    }

    private void clearAllButForgeAndSelf() {
        try {
            Field field = CoreModManager.class.getDeclaredField("loadPlugins");
            field.setAccessible(true);
            List<?> currentList = (List<?>) field.get(null);

            if (currentList == null || currentList.isEmpty()) {
                return;
            }

            List<Object> keepList = new ArrayList<>();
            int removedCount = 0;

            for (Object plugin : currentList) {
                if (isForgeCoreMod(plugin) || isSelfCoreMod(plugin)) {
                    keepList.add(plugin);
                } else {
                    removedCount++;
                    // 打印被移除的 CoreMod 信息（调试用）
                    String name = getCoreModClassName(plugin);
                    System.out.println("[DOTH] Removing third-party CoreMod: " + name);
                }
            }

            field.set(null, keepList);
            System.out.println("[DOTH] CoreMod list filtered. Kept: " + keepList.size() + ", Removed: " + removedCount);

        } catch (Exception e) {
            System.err.println("[DOTH] Failed to filter CoreMods:");
            e.printStackTrace();
        }
    }

    public static void setTransformersLast(List<IClassTransformer> myTransformers) {
        try {

            // 获取 LaunchClassLoader 的 transformers 字段
            Field transformersField = Launch.classLoader.getClass()
                    .getDeclaredField("transformers");
            transformersField.setAccessible(true);

            // 获取当前的 transformers 列表
            List<IClassTransformer> transformers =
                    (List<IClassTransformer>) transformersField.get(Launch.classLoader);
            // 清空列表
            transformers.clear();

            transformers.addAll(myTransformers);

            System.out.println("[DOTH] Transformers list has been cleared and replaced with custom transformers.");
            System.out.println("[DOTH] New transformer count: " + transformers.size());

        } catch (Exception e) {
            System.err.println("[DOTH] Failed to manipulate transformers list:");
            e.printStackTrace();
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{
                "yc.ycqin.doth.core.ProtectClassTransformer",
                "yc.ycqin.doth.core.PlayerDeadTransformer"
        };
    }

    /**
     * 判断是否是 Forge 自身的 CoreMod
     */
    private boolean isForgeCoreMod(Object plugin) {
        String className = getCoreModClassName(plugin);
        if (className == null) return false;

        for (String prefix : FORGE_PREFIXES) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否是自己的 CoreMod
     */
    private boolean isSelfCoreMod(Object plugin) {
        String className = getCoreModClassName(plugin);
        return className != null && className.startsWith(SELF_PREFIX);
    }

    /**
     * 获取 CoreMod 的实际类名（通过反射获取 coreModInstance）
     */
    private String getCoreModClassName(Object plugin) {
        try {
            // FMLPluginWrapper 内部有 coreModInstance 字段
            Field instanceField = plugin.getClass().getDeclaredField("coreModInstance");
            instanceField.setAccessible(true);
            Object instance = instanceField.get(plugin);
            if (instance != null) {
                return instance.getClass().getName();
            }
        } catch (Exception e) {
            // 如果反射失败，回退到 plugin 自身的类名
            return plugin.getClass().getName();
        }
        return plugin.getClass().getName();
    }


    @Override public String getAccessTransformerClass() { return null; }

}