package yc.ycqin.doth.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import yc.ycqin.doth.common.item.BlueCreeperSword;
import yc.ycqin.doth.util.SwordConfigHelper;

import java.util.*;

public class PacketMineAll implements IMessage {
    private List<BlockPos> positions = new ArrayList<>();

    public PacketMineAll() {}
    public PacketMineAll(List<BlockPos> pos) { positions = pos; }

    @Override public void fromBytes(ByteBuf buf) {
        int count = buf.readInt();
        for (int i = 0; i < count; i++)
            positions.add(new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()));
    }

    @Override public void toBytes(ByteBuf buf) {
        buf.writeInt(positions.size());
        for (BlockPos p : positions) { buf.writeInt(p.getX()); buf.writeInt(p.getY()); buf.writeInt(p.getZ()); }
    }

    public static class Handler implements IMessageHandler<PacketMineAll, IMessage> {
        @Override
        public IMessage onMessage(PacketMineAll msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServer().addScheduledTask(() -> {
                for (BlockPos pos : msg.positions) {
                    if (player.world.isAirBlock(pos)) continue;
                    IBlockState state = player.world.getBlockState(pos);
                    boolean unbreakable = state.getBlockHardness(player.world, pos) < 0;

                    if (unbreakable) {
                        // 不可破坏方块：跟 PacketInstantBreak 同样处理
                        if (!player.capabilities.isCreativeMode) {
                            state.getBlock().harvestBlock(player.world, player, pos, state,
                                    player.world.getTileEntity(pos), player.getHeldItemMainhand());
                            List<ItemStack> drops = state.getBlock().getDrops(player.world, pos, state, 0);
                            if (!drops.stream().anyMatch(s -> !s.isEmpty())) {
                                ItemStack blockStack = new ItemStack(state.getBlock(), 1,
                                        state.getBlock().damageDropped(state));
                                if (!blockStack.isEmpty()) giveOrDrop(player.world, pos, player, blockStack);
                            }
                        }
                        player.world.setBlockToAir(pos);
                    } else {
                        // 正常方块：tryHarvestBlock 触发 HarvestDropsEvent（磁吸用）
                        if (!player.interactionManager.tryHarvestBlock(pos)) {
                            player.world.destroyBlock(pos, true);
                        }
                    }
                }
            });
            return null;
        }

        private void giveOrDrop(World world, BlockPos pos, EntityPlayer player, ItemStack stack) {
            if (stack.isEmpty()) return;
            if (hasMagnetSword(player) && player.inventory.addItemStackToInventory(stack)) return;
            Block.spawnAsEntity(world, pos, stack);
        }

        private boolean hasMagnetSword(EntityPlayer player) {
            yc.ycqin.doth.core.AntiDisarmTracker.ConfigSnapshot cfg = yc.ycqin.doth.core.AntiDisarmTracker.getConfig(player);
            if (cfg != null) return cfg.magnetDrops;
            for (ItemStack s : player.inventory.mainInventory)
                if (s.getItem() instanceof BlueCreeperSword && SwordConfigHelper.isMagnetDrops(s)) return true;
            return false;
        }
    }
}
