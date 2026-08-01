package yc.ycqin.doth.proxy;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import yc.ycqin.doth.common.item.ItemReg;
import yc.ycqin.doth.network.NetworkHandler;

public class CommonProxy {
    public void preInit(FMLPreInitializationEvent event){
        NetworkHandler.registerMessages();
        new ItemReg();
    }

    public void init(FMLInitializationEvent event){
    }
}
