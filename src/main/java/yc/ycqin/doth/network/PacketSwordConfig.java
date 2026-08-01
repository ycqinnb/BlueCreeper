package yc.ycqin.doth.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import yc.ycqin.doth.common.item.BlueCreeperSword;
import yc.ycqin.doth.core.AllreturnConfig;
import yc.ycqin.doth.core.AntiDisarmTracker;
import yc.ycqin.doth.util.EnhancedAttackManager;
import yc.ycqin.doth.util.SwordConfigHelper;

import java.util.ArrayList;
import java.util.List;

public class PacketSwordConfig implements IMessage {

    private boolean attackPassive;
    private boolean attackPlayers;
    private boolean attackAllEntities;
    private boolean enableEnhanced;
    private boolean allReturn;
    private boolean alwaysAttack;
    private boolean tryDropItems;
    private boolean instantMine;
    private boolean magnetDrops;
    private boolean closeNonVanillaGui;
    private boolean enableBuffs;
    private boolean preventRemove;
    private boolean antiDisarm;
    private boolean autoRecreate;
    private boolean collectEntityDrops;
    private boolean rayTrace;
    private boolean purgeNBT;
    private boolean resetBooleans;
    private boolean resetLists;

    public PacketSwordConfig() {}

    public PacketSwordConfig(boolean attackPassive, boolean attackPlayers,
                             boolean attackAllEntities, boolean enableEnhanced,
                             boolean allReturn,boolean alwaysAttack,
                             boolean tryDropItems,
                             boolean instantMine, boolean magnetDrops,
                             boolean closeNonVanillaGui,
                             boolean enableBuffs,
                             boolean preventRemove,
                             boolean antiDisarm,
                             boolean autoRecreate,
                             boolean collectEntityDrops,
                             boolean rayTrace,
                             boolean purgeNBT,
                             boolean resetBooleans,
                             boolean resetLists) {
        this.attackPassive = attackPassive;
        this.attackPlayers = attackPlayers;
        this.attackAllEntities = attackAllEntities;
        this.enableEnhanced = enableEnhanced;
        this.allReturn = allReturn;
        this.alwaysAttack = alwaysAttack;
        this.tryDropItems = tryDropItems;
        this.instantMine = instantMine;
        this.magnetDrops = magnetDrops;
        this.closeNonVanillaGui = closeNonVanillaGui;
        this.enableBuffs = enableBuffs;
        this.preventRemove = preventRemove;
        this.antiDisarm = antiDisarm;
        this.autoRecreate = autoRecreate;
        this.collectEntityDrops = collectEntityDrops;
        this.rayTrace = rayTrace;
        this.purgeNBT = purgeNBT;
        this.resetBooleans = resetBooleans;
        this.resetLists = resetLists;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        attackPassive = buf.readBoolean();
        attackPlayers = buf.readBoolean();
        attackAllEntities = buf.readBoolean();
        enableEnhanced = buf.readBoolean();
        allReturn = buf.readBoolean();
        alwaysAttack = buf.readBoolean();
        tryDropItems = buf.readBoolean();
        instantMine = buf.readBoolean();
        magnetDrops = buf.readBoolean();
        closeNonVanillaGui = buf.readBoolean();
        enableBuffs = buf.readBoolean();
        preventRemove = buf.readBoolean();
        antiDisarm = buf.readBoolean();
        autoRecreate = buf.readBoolean();
        collectEntityDrops = buf.readBoolean();
        rayTrace = buf.readBoolean();
        purgeNBT = buf.readBoolean();
        resetBooleans = buf.readBoolean();
        resetLists = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(attackPassive);
        buf.writeBoolean(attackPlayers);
        buf.writeBoolean(attackAllEntities);
        buf.writeBoolean(enableEnhanced);
        buf.writeBoolean(allReturn);
        buf.writeBoolean(alwaysAttack);
        buf.writeBoolean(tryDropItems);
        buf.writeBoolean(instantMine);
        buf.writeBoolean(magnetDrops);
        buf.writeBoolean(closeNonVanillaGui);
        buf.writeBoolean(enableBuffs);
        buf.writeBoolean(preventRemove);
        buf.writeBoolean(antiDisarm);
        buf.writeBoolean(autoRecreate);
        buf.writeBoolean(collectEntityDrops);
        buf.writeBoolean(rayTrace);
        buf.writeBoolean(purgeNBT);
        buf.writeBoolean(resetBooleans);
        buf.writeBoolean(resetLists);
    }

    public static class Handler implements IMessageHandler<PacketSwordConfig, IMessage> {
        @Override
        public IMessage onMessage(PacketSwordConfig message, MessageContext ctx) {
            // 在服务端处理
            ctx.getServerHandler().player.getServerWorld().addScheduledTask(() -> {
                EntityPlayer player = ctx.getServerHandler().player;
                ItemStack stack = player.getHeldItemMainhand();

                // 验证玩家手持的是蓝C之剑
                if (!(stack.getItem() instanceof BlueCreeperSword)) {
                    return;
                }

                // 更新服务端的物品 NBT
                SwordConfigHelper.setAttackPassive(stack, message.attackPassive);
                SwordConfigHelper.setAttackPlayers(stack, message.attackPlayers);
                SwordConfigHelper.setAttackAllEntities(stack, message.attackAllEntities);
                SwordConfigHelper.setEnhancedEnabled(stack, message.enableEnhanced);
                SwordConfigHelper.setAllReturn(stack, message.allReturn);
                SwordConfigHelper.setAlwaysAttack(stack,message.alwaysAttack);
                SwordConfigHelper.setTryDropItems(stack, message.tryDropItems);
                SwordConfigHelper.setInstantMine(stack, message.instantMine);
                SwordConfigHelper.setMagnetDrops(stack, message.magnetDrops);
                SwordConfigHelper.setCloseNonVanillaGui(stack, message.closeNonVanillaGui);
                SwordConfigHelper.setEnableBuffs(stack, message.enableBuffs);
                SwordConfigHelper.setPreventRemove(stack, message.preventRemove);
                SwordConfigHelper.setAntiDisarm(stack, message.antiDisarm);
                SwordConfigHelper.setAutoRecreate(stack, message.autoRecreate);
                SwordConfigHelper.setCollectEntityDrops(stack, message.collectEntityDrops);
                SwordConfigHelper.setRayTrace(stack, message.rayTrace);
                SwordConfigHelper.setPurgeNBT(stack, message.purgeNBT);
                SwordConfigHelper.setResetBooleans(stack, message.resetBooleans);
                SwordConfigHelper.setResetLists(stack, message.resetLists);
                if (message.antiDisarm) {
                    SwordConfigHelper.setOwnerUUID(stack, player.getUniqueID());
                    AntiDisarmTracker.protect(player, stack);
                } else {
                    SwordConfigHelper.setOwnerUUID(stack, null);
                    AntiDisarmTracker.unprotect(player);
                }
                // 同步到客户端（让客户端物品的 NBT 也更新）
                player.inventory.markDirty();
                // 刷新全局 Allreturn 状态
                AllreturnConfig.refresh();
                EnhancedAttackManager.setIsAlwaysAttack(message.alwaysAttack);
                EnhancedAttackManager.resetBooleans = message.resetBooleans;
                EnhancedAttackManager.resetLists = message.resetLists;
                if (!message.enableEnhanced) EnhancedAttackManager.clearAll();
                boolean enhanced = message.enableEnhanced;
                List<String> classNames = new ArrayList<>(EnhancedAttackManager.getMarkedClassNames());
                boolean allReturn = AllreturnConfig.isEnabled();

                NetworkHandler.INSTANCE.sendToAll(new PacketEnhancedSync(enhanced, classNames, allReturn));
            });
            return null;
        }
    }
}
