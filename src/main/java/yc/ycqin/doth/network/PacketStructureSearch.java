package yc.ycqin.doth.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import yc.ycqin.doth.client.gui.SwordConfigGui;

public class PacketStructureSearch implements IMessage {
    private String name, structName;
    private int resultX, resultZ;
    private boolean found, isResponse, isListEntry;

    public PacketStructureSearch() {}
    public PacketStructureSearch(String name) { this.name = name; }
    public PacketStructureSearch(int x, int z, boolean f) { resultX=x; resultZ=z; found=f; isResponse=true; }
    public PacketStructureSearch(String name, boolean list) { structName=name; isResponse=true; isListEntry=list; }

    @Override public void fromBytes(ByteBuf buf) {
        isResponse=buf.readBoolean(); isListEntry=buf.readBoolean();
        if(isResponse){ if(isListEntry)structName=ByteBufUtils.readUTF8String(buf); else{resultX=buf.readInt();resultZ=buf.readInt();found=buf.readBoolean();} }
        else name=ByteBufUtils.readUTF8String(buf);
    }
    @Override public void toBytes(ByteBuf buf) {
        buf.writeBoolean(isResponse); buf.writeBoolean(isListEntry);
        if(isResponse){ if(isListEntry)ByteBufUtils.writeUTF8String(buf,structName); else{buf.writeInt(resultX);buf.writeInt(resultZ);buf.writeBoolean(found);} }
        else ByteBufUtils.writeUTF8String(buf,name);
    }

    public boolean isResponse(){return isResponse;} public boolean isFound(){return found;}
    public int getX(){return resultX;} public int getZ(){return resultZ;}
    public boolean isListEntry(){return isListEntry;} public String getStructName(){return structName;}

    public static class Handler implements IMessageHandler<PacketStructureSearch,IMessage> {
        @Override public IMessage onMessage(PacketStructureSearch msg, MessageContext ctx) {
            if(msg.isResponse && ctx.side==Side.CLIENT) {
                Minecraft.getMinecraft().addScheduledTask(()->{
                    if(msg.isListEntry) SwordConfigGui.addStructOption(msg.getStructName());
                    else SwordConfigGui.handleStructResult(msg.getX(),msg.getZ(),msg.isFound());
                }); return null;
            }
            if(msg.isResponse)return null;
            EntityPlayerMP p=ctx.getServerHandler().player;
            p.getServerWorld().addScheduledTask(()->{
                if("$LIST".equals(msg.name)){
                    try {
                        java.lang.reflect.Field f=net.minecraft.world.gen.structure.MapGenStructureIO.class.getDeclaredField("field_143040_a");
                        f.setAccessible(true);
                        for(String s:((java.util.Map<String,Class<?>>)f.get(null)).keySet())
                            NetworkHandler.INSTANCE.sendTo(new PacketStructureSearch(s,true),p);
                    }catch(Exception e){e.printStackTrace();}
                    return;
                }
                BlockPos pos=p.world.findNearestStructure(msg.name,p.getPosition(),false);
                NetworkHandler.INSTANCE.sendTo(new PacketStructureSearch(pos!=null?pos.getX():0,pos!=null?pos.getZ():0,pos!=null),p);
            }); return null;
        }
    }
}
