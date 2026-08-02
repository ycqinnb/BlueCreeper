package yc.ycqin.doth.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import yc.ycqin.doth.DOTHMod;

public class NetworkHandler {
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(DOTHMod.MODID);
    private static int id = 0;

    public static void registerMessages() {
        INSTANCE.registerMessage(PacketSwordConfig.Handler.class, PacketSwordConfig.class, id++, Side.SERVER);
        INSTANCE.registerMessage(PacketEnhancedSync.Handler.class, PacketEnhancedSync.class, id++, Side.CLIENT);
        INSTANCE.registerMessage(PacketShowDeathScreen.Handler.class, PacketShowDeathScreen.class, id++,Side.CLIENT);
        INSTANCE.registerMessage(SPacketKillNumber.Handler.class, SPacketKillNumber.class, id++, Side.CLIENT);
        INSTANCE.registerMessage(PacketInstantBreak.Handler.class, PacketInstantBreak.class, id++, Side.SERVER);
        INSTANCE.registerMessage(PacketBuffConfig.Handler.class, PacketBuffConfig.class, id++, Side.SERVER);
        INSTANCE.registerMessage(PacketMineAll.Handler.class, PacketMineAll.class, id++, Side.SERVER);
        INSTANCE.registerMessage(PacketStructureSearch.Handler.class, PacketStructureSearch.class, id++, Side.SERVER);
        INSTANCE.registerMessage(PacketStructureSearch.Handler.class, PacketStructureSearch.class, id++, Side.CLIENT);
        INSTANCE.registerMessage(PacketTeleport.Handler.class, PacketTeleport.class, id++, Side.SERVER);
        INSTANCE.registerMessage(PacketFindAllStructures.Handler.class, PacketFindAllStructures.class, id++, Side.SERVER);
        INSTANCE.registerMessage(PacketFindAllStructures.Handler.class, PacketFindAllStructures.class, id++, Side.CLIENT);
        INSTANCE.registerMessage(PacketAttackAll.Handler.class, PacketAttackAll.class, id++, Side.SERVER);
        INSTANCE.registerMessage(PacketPhotoKill.Handler.class, PacketPhotoKill.class, id++, Side.SERVER);
        INSTANCE.registerMessage(SPacketRushState.Handler.class, SPacketRushState.class, id++, Side.CLIENT);
        INSTANCE.registerMessage(SPacketRushMount.Handler.class, SPacketRushMount.class, id++, Side.CLIENT);
    }
}