package yc.ycqin.doth.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;


@SideOnly(Side.CLIENT)
public class DOTHKeyBind {
    public static KeyBinding OPEN_CONFIG_GUI;
    public static void register(FMLInitializationEvent event) {
        OPEN_CONFIG_GUI = new KeyBinding("key.opengui",Keyboard.KEY_C,"key.categories.doth");
        ClientRegistry.registerKeyBinding(OPEN_CONFIG_GUI);
        System.out.println("[DOTHKeyBind] Registered key binding");
    }
}
