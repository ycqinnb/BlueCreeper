package yc.ycqin.doth.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import yc.ycqin.doth.common.item.BlueCreeperSword;
import yc.ycqin.doth.util.EnhancedAttackManager;
import yc.ycqin.doth.util.EntityDeletionHelper;
import yc.ycqin.doth.util.SwordConfigHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端 → 服务端：一键攻击高亮实体
 */
public class PacketAttackAll implements IMessage {

    private List<Integer> entityIds = new ArrayList<>();

    public PacketAttackAll() {}

    public PacketAttackAll(List<Integer> ids) {
        this.entityIds = ids;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            entityIds.add(buf.readInt());
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityIds.size());
        for (int id : entityIds) {
            buf.writeInt(id);
        }
    }

    public static class Handler implements IMessageHandler<PacketAttackAll, IMessage> {
        @Override
        public IMessage onMessage(PacketAttackAll msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            ItemStack stack = player.getHeldItemMainhand();
            if (!(stack.getItem() instanceof BlueCreeperSword)) return null;

            boolean tryDrop = SwordConfigHelper.isTryDropItems(player, stack);
            boolean enhanced = SwordConfigHelper.isEnhancedEnabled(player, stack);

            for (int id : msg.entityIds) {
                Entity target = player.world.getEntityByID(id);
                if (target == null || target.isDead) continue;
                if (enhanced) EnhancedAttackManager.addTarget(target);
                EntityDeletionHelper.deleteEntity(target, player, tryDrop, stack);
            }
            return null;
        }
    }
}
