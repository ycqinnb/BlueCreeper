package yc.ycqin.doth.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import yc.ycqin.doth.common.item.BlueCreeperSword;
import yc.ycqin.doth.util.SwordConfigHelper;

import java.util.List;

/**
 * 客户端 → 服务端：秒挖不可破坏方块（基岩等）
 * 因为客户端不会为 hardness<0 的方块发挖掘包，所以需要这个自定义包
 */
public class PacketInstantBreak implements IMessage {

    private int x, y, z;

    public PacketInstantBreak() {}

    public PacketInstantBreak(BlockPos pos) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
    }

    public static class Handler implements IMessageHandler<PacketInstantBreak, IMessage> {
        @Override
        public IMessage onMessage(PacketInstantBreak message, MessageContext ctx) {
            ctx.getServerHandler().player.getServerWorld().addScheduledTask(() -> {
                EntityPlayer player = ctx.getServerHandler().player;
                BlockPos pos = new BlockPos(message.x, message.y, message.z);
                World world = player.world;

                // 二次验证：手持蓝C剑 + 秒挖开启
                ItemStack held = player.getHeldItemMainhand();
                if (!(held.getItem() instanceof BlueCreeperSword) ||
                        !SwordConfigHelper.isInstantMine(player, held)) {
                    return;
                }

                IBlockState state = world.getBlockState(pos);
                if (state.getBlockHardness(world, pos) < 0) {
                    // 不可破坏方块：强制掉落+清除
                    if (!player.capabilities.isCreativeMode) {
                        state.getBlock().harvestBlock(world, player, pos, state,
                                world.getTileEntity(pos), held);
                        // 基岩等方块 harvestBlock 不会掉落任何东西——手动掉落方块本身
                        List<ItemStack> drops = state.getBlock().getDrops(world, pos, state, 0);
                        boolean hasDrops = drops.stream().anyMatch(s -> !s.isEmpty());
                        if (!hasDrops) {
                            ItemStack blockStack = new ItemStack(state.getBlock(), 1,
                                    state.getBlock().damageDropped(state));
                            if (!blockStack.isEmpty()) {
                                // 磁吸：剑在背包→优先进包
                                giveOrDrop(world, pos, player, blockStack);
                            }
                        }
                    }
                    world.setBlockToAir(pos);
                }
            });
            return null;
        }

        // 如果玩家背包里有磁吸毒剑，优先放入背包，否则掉落到地面
        private void giveOrDrop(World world, BlockPos pos, EntityPlayer player, ItemStack stack) {
            if (stack.isEmpty()) return;
            if (hasMagnetSword(player) && player.inventory.addItemStackToInventory(stack)) {
                return; // 放进背包了
            }
            Block.spawnAsEntity(world, pos, stack); // 背包满 → 掉地上
        }

        private boolean hasMagnetSword(EntityPlayer player) {
            yc.ycqin.doth.core.AntiDisarmTracker.ConfigSnapshot cfg = yc.ycqin.doth.core.AntiDisarmTracker.getConfig(player);
            if (cfg != null) return cfg.magnetDrops;
            for (ItemStack invStack : player.inventory.mainInventory) {
                if (invStack.getItem() instanceof BlueCreeperSword
                        && SwordConfigHelper.isMagnetDrops(invStack)) {
                    return true;
                }
            }
            return false;
        }
    }
}
