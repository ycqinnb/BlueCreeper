package yc.ycqin.doth.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import yc.ycqin.doth.client.RushClientState;

/**
 * 虫灵快跑状态同步（S→C）：进入时激活 HUD，每秒更新分数，结束/离开时关闭。
 */
public class SPacketRushState implements IMessage {
    private int dimId;
    private int score;
    private int coins;
    private int distance;
    private boolean active;

    public SPacketRushState() {}

    public SPacketRushState(int dimId, int score, int coins, int distance, boolean active) {
        this.dimId = dimId;
        this.score = score;
        this.coins = coins;
        this.distance = distance;
        this.active = active;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        dimId = buf.readInt();
        score = buf.readInt();
        coins = buf.readInt();
        distance = buf.readInt();
        active = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dimId);
        buf.writeInt(score);
        buf.writeInt(coins);
        buf.writeInt(distance);
        buf.writeBoolean(active);
    }

    public static class Handler implements IMessageHandler<SPacketRushState, IMessage> {
        @SideOnly(Side.CLIENT)
        @Override
        public IMessage onMessage(SPacketRushState message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (message.active) {
                    RushClientState.active = true;
                    RushClientState.dimId = message.dimId;
                }
                RushClientState.score = message.score;
                RushClientState.coins = message.coins;
                RushClientState.distance = message.distance;
                if (!message.active) {
                    RushClientState.active = false;
                }
            });
            return null;
        }
    }
}
