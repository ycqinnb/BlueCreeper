package yc.ycqin.doth.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import yc.ycqin.doth.common.item.CameraItem;
import yc.ycqin.doth.util.EnhancedAttackManager;
import yc.ycqin.doth.util.EntityDeletionHelper;
import yc.ycqin.doth.util.ReflectionHelper;

/**
 * 客户端 → 服务端：相机拍摄后，杀死取景框内所有可见生物
 */
public class PacketPhotoKill implements IMessage {

    private int[] entityIds;

    public PacketPhotoKill() {}

    public PacketPhotoKill(int[] entityIds) {
        this.entityIds = entityIds;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int count = buf.readInt();
        entityIds = new int[count];
        for (int i = 0; i < count; i++) {
            entityIds[i] = buf.readInt();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityIds.length);
        for (int id : entityIds) {
            buf.writeInt(id);
        }
    }

    public static class Handler implements IMessageHandler<PacketPhotoKill, IMessage> {
        @Override
        public IMessage onMessage(PacketPhotoKill message, MessageContext ctx) {
            ctx.getServerHandler().player.getServerWorld().addScheduledTask(() -> {
                EntityPlayer player = ctx.getServerHandler().player;
                ItemStack held = player.getHeldItemMainhand();
                if (!(held.getItem() instanceof CameraItem)) return;

                for (int id : message.entityIds) {
                    Entity entity = player.world.getEntityByID(id);
                    if (entity instanceof EntityLivingBase && !entity.isDead && entity != player) {
                        //EntityDeletionHelper.deleteEntity(entity, player, false, null);
                        try {
                            ReflectionHelper.nbSetHealth((EntityLivingBase) entity,0);
                            entity.world.removeEntityDangerously(entity);
                            entity.isDead = true;
                        } catch (Throwable e){
                            System.out.println(e.getMessage());
                        }

                    }
                }
            });
            return null;
        }
    }
}
