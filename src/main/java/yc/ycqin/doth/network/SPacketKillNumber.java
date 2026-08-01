package yc.ycqin.doth.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import yc.ycqin.doth.event.TooltipRenderer;

public class SPacketKillNumber implements IMessage {
    private int num;

    public SPacketKillNumber() {}

    public SPacketKillNumber(int num) {
        this.num = num;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        num = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(num);
    }

    public static class Handler implements IMessageHandler<SPacketKillNumber, IMessage> {
        @SideOnly(Side.CLIENT)
        @Override
        public IMessage onMessage(SPacketKillNumber message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                TooltipRenderer.showTip("已清除实体："+ message.num);
            });
            return null;
        }
    }
}
