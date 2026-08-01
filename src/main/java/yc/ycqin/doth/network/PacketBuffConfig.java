package yc.ycqin.doth.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import yc.ycqin.doth.common.item.BlueCreeperSword;
import yc.ycqin.doth.util.SwordConfigHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * 客户端 → 服务端：同步 Buff 配置表
 */
public class PacketBuffConfig implements IMessage {

    private Map<String, Integer> buffs = new HashMap<>();

    public PacketBuffConfig() {}

    public PacketBuffConfig(Map<String, Integer> buffs) {
        this.buffs = buffs;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            String name = readUTF(buf);
            int level = buf.readInt();
            if (level > 0) buffs.put(name, level);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(buffs.size());
        for (Map.Entry<String, Integer> e : buffs.entrySet()) {
            writeUTF(buf, e.getKey());
            buf.writeInt(e.getValue());
        }
    }

    // ByteBuf 没有 writeUTF/readUTF，手动实现
    private static void writeUTF(ByteBuf buf, String s) {
        byte[] bytes = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        buf.writeShort(bytes.length);
        buf.writeBytes(bytes);
    }

    private static String readUTF(ByteBuf buf) {
        int len = buf.readShort();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    public static class Handler implements IMessageHandler<PacketBuffConfig, IMessage> {
        @Override
        public IMessage onMessage(PacketBuffConfig message, MessageContext ctx) {
            ctx.getServerHandler().player.getServerWorld().addScheduledTask(() -> {
                EntityPlayer player = ctx.getServerHandler().player;
                ItemStack held = player.getHeldItemMainhand();
                if (!(held.getItem() instanceof BlueCreeperSword)) return;
                SwordConfigHelper.setBuffConfig(held, message.buffs);
                player.inventory.markDirty();
            });
            return null;
        }
    }
}
