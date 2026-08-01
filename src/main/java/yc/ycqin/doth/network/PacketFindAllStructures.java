package yc.ycqin.doth.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import yc.ycqin.doth.client.gui.StructResultsGui;

import java.util.*;

public class PacketFindAllStructures implements IMessage {
    private String name;
    private List<BlockPos> results = new ArrayList<>();
    private boolean isResponse;

    public PacketFindAllStructures() {}
    public PacketFindAllStructures(String name) { this.name = name; }
    public PacketFindAllStructures(List<BlockPos> r) { results = r; isResponse = true; }

    @Override public void fromBytes(ByteBuf buf) {
        isResponse = buf.readBoolean();
        if (isResponse) { int n = buf.readInt(); for (int i=0;i<n;i++) results.add(new BlockPos(buf.readInt(),buf.readInt(),buf.readInt())); }
        else name = ByteBufUtils.readUTF8String(buf);
    }
    @Override public void toBytes(ByteBuf buf) {
        buf.writeBoolean(isResponse);
        if (isResponse) { buf.writeInt(results.size()); for (BlockPos p:results) { buf.writeInt(p.getX()); buf.writeInt(p.getY()); buf.writeInt(p.getZ()); } }
        else ByteBufUtils.writeUTF8String(buf, name);
    }

    public boolean isResponse(){return isResponse;}
    public List<BlockPos> getResults(){return results;}

    public static class Handler implements IMessageHandler<PacketFindAllStructures,IMessage> {
        @Override public IMessage onMessage(PacketFindAllStructures msg, MessageContext ctx) {
            if (msg.isResponse && ctx.side==Side.CLIENT) {
                Minecraft.getMinecraft().addScheduledTask(()-> StructResultsGui.open(msg.getResults()));
                return null;
            }
            if (msg.isResponse) return null;
            EntityPlayerMP p=ctx.getServerHandler().player;
            p.getServerWorld().addScheduledTask(()->{
                Set<Long> found = new HashSet<>();
                List<BlockPos> results = new ArrayList<>();
                int r = 5000, step = 200;
                BlockPos center = p.getPosition();
                for (int dx=-r;dx<=r;dx+=step)
                    for (int dz=-r;dz<=r;dz+=step) {
                        BlockPos pos = p.world.findNearestStructure(msg.name, center.add(dx,0,dz), false);
                        if (pos != null) {
                            long key = ((long)pos.getX())<<32 | (pos.getZ()&0xffffffffL);
                            if (found.add(key)) results.add(pos);
                        }
                    }
                NetworkHandler.INSTANCE.sendTo(new PacketFindAllStructures(results), p);
            }); return null;
        }
    }
}
