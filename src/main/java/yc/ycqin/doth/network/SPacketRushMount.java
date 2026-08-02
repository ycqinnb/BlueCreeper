package yc.ycqin.doth.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import yc.ycqin.doth.client.RushClientState;

/**
 * 虫灵快跑骑乘强制同步（S→C，照寄群日记 SPacketMountSync 思路）：
 * 客户端直接 startRiding(mount, true) 建立骑乘关系，不依赖 SPacketSetPassengers。
 * 实体还没同步到客户端时挂起重试（RushClientState.pendingMountId）。
 */
public class SPacketRushMount implements IMessage {
    private int mountId;
    private boolean mounting;

    public SPacketRushMount() {}

    public SPacketRushMount(int mountId, boolean mounting) {
        this.mountId = mountId;
        this.mounting = mounting;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        mountId = buf.readInt();
        mounting = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(mountId);
        buf.writeBoolean(mounting);
    }

    public static class Handler implements IMessageHandler<SPacketRushMount, IMessage> {
        @SideOnly(Side.CLIENT)
        @Override
        public IMessage onMessage(SPacketRushMount message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (!message.mounting) {
                    RushClientState.pendingMountId = -1;
                    if (Minecraft.getMinecraft().player != null
                            && Minecraft.getMinecraft().player.isRiding()) {
                        Minecraft.getMinecraft().player.dismountRidingEntity();
                    }
                    return;
                }
                // 挂起重试：直到客户端有这个实体
                RushClientState.pendingMountId = message.mountId;
            });
            return null;
        }
    }
}
