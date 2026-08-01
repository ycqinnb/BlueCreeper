// 新增：同步增强攻击状态和类名黑名单
package yc.ycqin.doth.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import yc.ycqin.doth.core.AllreturnConfig;
import yc.ycqin.doth.util.EnhancedAttackManager;

import java.util.ArrayList;
import java.util.List;

public class PacketEnhancedSync implements IMessage {
    private boolean enhancedEnabled;
    private List<String> classNames;
    private boolean allReturnEnabled;

    public PacketEnhancedSync() {}

    public PacketEnhancedSync(boolean enhancedEnabled, List<String> classNames, boolean allReturnEnabled) {
        this.enhancedEnabled = enhancedEnabled;
        this.classNames = new ArrayList<>(classNames);
        this.allReturnEnabled = allReturnEnabled;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        enhancedEnabled = buf.readBoolean();
        allReturnEnabled = buf.readBoolean();
        int size = buf.readInt();
        classNames = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            classNames.add(ByteBufUtils.readUTF8String(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(enhancedEnabled);
        buf.writeBoolean(allReturnEnabled);
        buf.writeInt(classNames.size());
        for (String name : classNames) {
            ByteBufUtils.writeUTF8String(buf, name);
        }
    }

    public static class Handler implements IMessageHandler<PacketEnhancedSync, IMessage> {
        @Override
        public IMessage onMessage(PacketEnhancedSync message, MessageContext ctx) {
            // 客户端更新
            //EnhancedAttackManager.setClientState(
            //        message.enhancedEnabled,
            //        message.classNames
            //);
            AllreturnConfig.setClientCache(message.allReturnEnabled);
            return null;
        }
    }
}