package yc.ycqin.doth.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import yc.ycqin.doth.client.gui.DeadGui;
import yc.ycqin.doth.client.gui.DeadGui1;

public class PacketShowDeathScreen implements IMessage {

    public PacketShowDeathScreen() {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<PacketShowDeathScreen, IMessage> {
        @Override
        public IMessage onMessage(PacketShowDeathScreen message, MessageContext ctx) {
            // 在客户端执行
            Minecraft.getMinecraft().addScheduledTask(() -> {
                Minecraft mc = Minecraft.getMinecraft();
                Minecraft.getMinecraft().displayGuiScreen(new GuiGameOver(new TextComponentString("114514")));
                mc.displayGuiScreen(new DeadGui(new TextComponentString("114514")));
                mc.displayGuiScreen(new DeadGui1(new TextComponentString("114514")));
            });
            return null;
        }
    }
}