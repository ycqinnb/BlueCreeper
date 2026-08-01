package yc.ycqin.doth.common.entities;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import yc.ycqin.doth.util.SwordConfigHelper;

import java.util.UUID;

public class EntityItemSwordHighlight extends EntityItem {

    public EntityItemSwordHighlight(World worldIn) {
        super(worldIn);
    }

    public EntityItemSwordHighlight(World worldIn, double x, double y, double z, ItemStack stack) {
        super(worldIn, x, y, z, stack);
    }

    @Override
    public void onUpdate() {
        // 不消失：防自然过期 + 防外部 setDead
        this.setNoDespawn();
        this.isDead = false;

        ItemStack stack = this.getItem();
        if (SwordConfigHelper.isAntiDisarm(stack)){
            UUID ownerUUID = SwordConfigHelper.getOwnerUUID(stack);
            if (ownerUUID != null) {
                EntityPlayer owner = this.world.getPlayerEntityByUUID(ownerUUID);
                if (owner != null) {
                    this.setPosition(owner.posX, owner.posY + 1, owner.posZ);
                    this.setPickupDelay(0);
                }
            }
        }
        super.onUpdate();
    }
}
