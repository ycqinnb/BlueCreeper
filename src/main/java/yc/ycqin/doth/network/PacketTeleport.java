package yc.ycqin.doth.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketTeleport implements IMessage {
    private int x, y, z;

    public PacketTeleport() {}
    public PacketTeleport(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }

    @Override public void fromBytes(ByteBuf buf) { x = buf.readInt(); y = buf.readInt(); z = buf.readInt(); }
    @Override public void toBytes(ByteBuf buf) { buf.writeInt(x); buf.writeInt(y); buf.writeInt(z); }

    public static class Handler implements IMessageHandler<PacketTeleport, IMessage> {
        @Override public IMessage onMessage(PacketTeleport msg, MessageContext ctx) {
            EntityPlayerMP p = ctx.getServerHandler().player;
            p.getServerWorld().addScheduledTask(() -> {
                // 找安全Y坐标
                int safeY = msg.y;
                while (safeY > 0 && p.world.getBlockState(
                        new net.minecraft.util.math.BlockPos(msg.x, safeY, msg.z)).getMaterial().isSolid())
                    safeY++;
                p.connection.setPlayerLocation(msg.x + 0.5, safeY, msg.z + 0.5, p.rotationYaw, p.rotationPitch);
            });
            return null;
        }
    }
}
