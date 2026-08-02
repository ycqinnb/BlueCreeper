package yc.ycqin.doth.common.block;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class BlockReg {

    public static final BlockArena BLOCK_ARENA = new BlockArena();
    public static final BlockFighter BLOCK_FIGHTER = new BlockFighter();

    public BlockReg() {
        MinecraftForge.EVENT_BUS.register(this);
        GameRegistry.registerTileEntity(TileEntityFighter.class, "bluecreepersword:fighter");
    }

    @SubscribeEvent
    public void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(
                BLOCK_ARENA,
                BLOCK_FIGHTER
        );
    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
                new ItemBlockArena(BLOCK_ARENA).setRegistryName(BLOCK_ARENA.getRegistryName()),
                new ItemBlockFighter(BLOCK_FIGHTER).setRegistryName(BLOCK_FIGHTER.getRegistryName())
        );
    }
}
