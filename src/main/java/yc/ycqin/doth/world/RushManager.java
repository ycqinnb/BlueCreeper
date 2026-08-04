package yc.ycqin.doth.world;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SPacketTitle;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.DimensionType;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import yc.ycqin.doth.common.entities.EntityCoin;
import yc.ycqin.doth.common.entities.EntityRushPowerup;
import yc.ycqin.doth.network.NetworkHandler;
import yc.ycqin.doth.network.SPacketRushMount;
import yc.ycqin.doth.network.SPacketRushState;
import yc.ycqin.doth.util.ReflectionHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * 虫灵快跑：虫灵骑乘跑酷小游戏（仅装载 srparasites 模组时启用）。
 * - 每条赛道 = 一整块连续跑道（3 车道相连，中间不隔虚空）
 * - 服务端每 tick 控制虫灵前进，玩家 A/D 通过 CPacketRushInput 控制左右
 * - 障碍/金币/道具按段实时生成，跑过的清理
 * - 撞障碍 -2 血（原版虫灵 7 血），死亡或提前下马 → 结算返回
 */
public class RushManager {

    private static final Logger log = LogManager.getLogger("DOTH-Rush");

    // ===== 维度注册 =====
    public static int RUSH_DIM_ID = -1;
    public static DimensionType DIMENSION_TYPE = null;
    private static boolean dimensionRegistered = false;

    // ===== 赛道布局 =====
    /** 跑道方块层 y（5 格厚） */
    public static final int TRACK_Y1 = 60, TRACK_Y2 = 64;
    /** 跑道半宽（x 中心 ±6，宽 13 格，3 车道相连） */
    public static final int TRACK_HALF = 6;
    /** 相邻赛道中心间距 */
    public static final int TRACK_SPACING = 16;
    /** 车道中心（相对赛道中心） */
    private static final int[] LANES = {-4, 0, 4};
    /** 出生 z */
    public static final double START_Z = 2.0D;
    /** 基础前进速度（格/tick，0.15 = 3 格/秒），随距离递增 */
    public static final float RUN_SPEED_F = 0.15F;
    /** 速度递增：每 100 格 +0.15 */
    private static final double SPEED_RAMP = 0.05D / 100.0D;
    /** 速度上限（格/tick） */
    private static final float SPEED_MAX = 100.0F;
    /** 左右移动速度（格/tick） */
    public static final double STEER_SPEED = 0.16D;
    /** 相对赛道中心的 x 限位 */
    public static final double MAX_X = 5.5D;

    // ===== 虫灵状态（DataWatcher，自动同步客户端） =====
    public static final byte STATE_NORMAL = 0;   // SRP 原生行为
    public static final byte STATE_HOLD = 1;     // 倒数中：原地不动
    public static final byte STATE_RUN = 2;      // 奔跑中：恒定前进 + 左右
    /** 撞障碍伤害 */
    public static final int OBSTACLE_DAMAGE = 2;
    /** 障碍生成概率（每车道每段，基础） */
    private static final double OBSTACLE_CHANCE = 0.22D;
    /** 障碍密度递增：每 100 格 +0.05 */
    private static final double OBSTACLE_RAMP = 0.05D / 100.0D;
    /** 障碍概率上限 */
    private static final double OBSTACLE_MAX = 0.75D;
    /** 金币行生成概率 */
    private static final double COIN_CHANCE = 0.6D;
    /** 道具生成概率 */
    private static final double POWERUP_CHANCE = 0.08D;
    /** 每段生成间距（z 格） */
    private static final int SEGMENT_LEN = 8;
    /** 生成前瞻距离 */
    private static final double GEN_AHEAD = 48.0D;
    /** 身后清理距离 */
    private static final double CLEAN_BEHIND = 24.0D;

    /** 我方比赛实体标签（EntityJoinWorldEvent 白名单用） */
    public static final String TAG_RUSH = "doth_rush";

    /** 虫灵无敌模式（测试用，/rushdebug invincible on|off） */
    public static boolean buglinInvincible = false;

    // ===== NBT 键（原位置记录） =====
    private static final String KEY_ORIG_DIM = "doth_rush_orig_dim";
    private static final String KEY_ORIG_X = "doth_rush_orig_x";
    private static final String KEY_ORIG_Y = "doth_rush_orig_y";
    private static final String KEY_ORIG_Z = "doth_rush_orig_z";
    private static final String KEY_ORIG_GT = "doth_rush_orig_gametype";

    private static final Random RNG = new Random();

    // ===== 比赛状态 =====
    public static class RushRun {
        public EntityPlayerMP player;
        public int trackIndex;
        public double centerX;
        public String mobId;         // 当前坐骑生物注册名（如 minecraft:zombie / srparasites:buglin）
        public EntityLivingBase buglin;
        public BossInfoServer bossBar;
        public int state;            // 0=倒数 1=进行中 2=已结束
        public int countdownTicks;
        public int coins;
        public int shieldTicks;
        public double nextGenZ;
        public int syncTicks;
        public int mountSyncTicks;
        public int lastFillZ = 0;
        public List<BlockPos> obstacles = new ArrayList<>();
        public List<EntityCoin> coinEntities = new ArrayList<>();
        public List<EntityRushPowerup> powerupEntities = new ArrayList<>();
    }

    private static final Map<UUID, RushRun> RUNS = new LinkedHashMap<>();
    private static final Set<Integer> USED_TRACKS = new HashSet<>();

    // ===== 注册维度 =====
    public static boolean registerDimension() {
        if (dimensionRegistered) return true;
        int baseId = 668;
        for (int attempt = 0; attempt < 3; attempt++) {
            int id = baseId + attempt * 100;
            if (DimensionManager.isDimensionRegistered(id)) {
                log.warn("[RUSH] 维度ID {} 已被占用，尝试下一个", id);
                continue;
            }
            try {
                DIMENSION_TYPE = DimensionType.register("DOTH_RUSH_" + id, "_rush" + id, id, RushWorldProvider.class, true);
                DimensionManager.registerDimension(id, DIMENSION_TYPE);
                RUSH_DIM_ID = id;
                dimensionRegistered = true;
                log.info("[RUSH] 虫灵快跑维度注册成功，ID = {}", id);
                return true;
            } catch (Exception e) {
                log.error("[RUSH] 维度注册失败 ID={}", id, e);
            }
        }
        log.error("[RUSH] 虫灵快跑维度注册失败：3次尝试均被占用");
        return false;
    }

    public static boolean isDimensionRegistered() {
        return dimensionRegistered;
    }

    public static boolean isRushDimension(World world) {
        return dimensionRegistered && world != null && world.provider.getDimension() == RUSH_DIM_ID;
    }

    // ===== 进入 =====

    /**
     * 玩家右键入场券：分配赛道、传送、生成坐骑生物、骑乘、开始倒数。
     * @param mobId 坐骑生物注册名（如 minecraft:zombie / srparasites:buglin）
     * @return 是否成功进入
     */
    public static boolean enterRush(EntityPlayerMP player, String mobId) {
        if (!dimensionRegistered) {
            player.sendMessage(new TextComponentString("§c[虫灵快跑] 维度未注册"));
            return false;
        }
        if (player.dimension == RUSH_DIM_ID) {
            player.sendMessage(new TextComponentString("§c[虫灵快跑] 你已经在赛道上"));
            return false;
        }

        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return false;
        WorldServer rushWorld = server.getWorld(RUSH_DIM_ID);
        if (rushWorld == null) return false;

        // 按注册名生成生物（EntityList，不直接引用任何模组类）
        Entity mob = EntityList.createEntityByIDFromName(new ResourceLocation(mobId), rushWorld);
        if (!(mob instanceof EntityLivingBase)) {
            player.sendMessage(new TextComponentString("§c[虫灵快跑] 无法生成生物：" + mobId));
            return false;
        }

        // 分配赛道（先到先得，离开释放）
        int track = -1;
        for (int i = 0; i < 64; i++) {
            if (!USED_TRACKS.contains(i)) {
                track = i;
                break;
            }
        }
        if (track < 0) {
            player.sendMessage(new TextComponentString("§c[虫灵快跑] 赛道已满"));
            return false;
        }

        RushRun run = new RushRun();
        run.player = player;
        run.trackIndex = track;
        run.centerX = track * TRACK_SPACING;
        run.mobId = mobId;
        run.state = 0;
        run.countdownTicks = 3 * 20;
        run.nextGenZ = START_Z + 8;

        USED_TRACKS.add(track);
        RUNS.put(player.getUniqueID(), run);

        // 清理该赛道残留（跨会话防残留）
        clearTrackArea(rushWorld, run);
        // 补铺出生区跑道方块（该区块可能以前生成过但没有跑道）
        ensureTrackBlocks(rushWorld, run, 0, 96);

        // 记录原位置/模式
        NBTTagCompound data = player.getEntityData();
        data.setInteger(KEY_ORIG_DIM, player.dimension);
        data.setDouble(KEY_ORIG_X, player.posX);
        data.setDouble(KEY_ORIG_Y, player.posY);
        data.setDouble(KEY_ORIG_Z, player.posZ);
        data.setInteger(KEY_ORIG_GT, player.interactionManager.getGameType().getID());

        // 传送玩家到赛道起点
        ArenaTeleporter tp = installTeleporter(rushWorld);
        if (tp != null) tp.setTarget(run.centerX, TRACK_Y2 + 2, START_Z);
        player.changeDimension(RUSH_DIM_ID);
        player.connection.setPlayerLocation(run.centerX, TRACK_Y2 + 2, START_Z, 0.0F, 30.0F);
        player.setGameType(GameType.ADVENTURE);

        // 坐骑（ASM 已让 EntityLivingBase 全类可骑；移动由 travel hook 接管）
        EntityLivingBase mount = (EntityLivingBase) mob;
        // 清除原 AI（防乱跑/攻击/瞬移/自爆），快跑移动完全由 hook/tickRun 接管
        if (mount instanceof EntityLiving) {
            ((EntityLiving) mount).tasks.taskEntries.clear();
            ((EntityLiving) mount).targetTasks.taskEntries.clear();
            ((EntityLiving) mount).setNoAI(true);
        }
        mount.setNoGravity(true);
        mount.setHealth(mount.getMaxHealth());
        mount.addTag(TAG_RUSH);
        mount.setPosition(run.centerX, TRACK_Y2 + 1, START_Z);
        mount.rotationYaw = 0.0F;
        rushWorld.spawnEntity(mount);
        run.buglin = mount;
        setRushState(mount, STATE_HOLD); // 倒数中原地不动

        // 骑乘 + 手动补发乘客状态（保险）
        if (!player.startRiding(mount, true)) {
            player.sendMessage(new TextComponentString("§c[虫灵快跑] 骑乘失败"));
            endGame(run, "骑乘失败");
            return false;
        }
        player.connection.sendPacket(new net.minecraft.network.play.server.SPacketSetPassengers(mount));

        // Boss 血条（玩家名 + 生物名）
        TextComponentString bossTitle = new TextComponentString("§c" + player.getName() + " 的 ");
        bossTitle.appendSibling(mobNameComponent(mobId));
        run.bossBar = new BossInfoServer(bossTitle, BossInfo.Color.RED, BossInfo.Overlay.PROGRESS);
        run.bossBar.addPlayer(player);
        run.bossBar.setPercent(mount.getHealth() / Math.max(1.0F, mount.getMaxHealth()));

        // 通知客户端（维度 id + 激活 HUD）
        NetworkHandler.INSTANCE.sendTo(new SPacketRushState(RUSH_DIM_ID, 0, 0, 0, true), player);

        TextComponentString enterMsg = new TextComponentString("§e[虫灵快跑] 骑上");
        enterMsg.appendSibling(mobNameComponent(mobId));
        enterMsg.appendText("，3、2、1 后出发！A/D 左右移动，Shift 下区即弃权");
        player.sendMessage(enterMsg);
        sendTitle(player, "§l§6虫灵快跑", "§7" + player.getName());
        log.info("[RUSH] {} 进入赛道 {}，坐骑 {}，中心 x={}", player.getName(), track, mobId, run.centerX);
        return true;
    }

    /** 强制铺设指定 z 范围内的跑道方块（补以前生成过的空区块） */
    private static void ensureTrackBlocks(WorldServer world, RushRun run, int zStart, int zEnd) {
        int cx1 = (int) Math.floor(run.centerX - TRACK_HALF);
        int cx2 = (int) Math.floor(run.centerX + TRACK_HALF);
        for (int x = cx1; x <= cx2; x++) {
            for (int z = zStart; z <= zEnd; z++) {
                for (int y = TRACK_Y1; y <= TRACK_Y2; y++) {
                    world.setBlockState(new BlockPos(x, y, z), Blocks.QUARTZ_BLOCK.getDefaultState(), 2);
                }
            }
        }
    }

    /** 清理指定赛道范围内的残留（实体 + 障碍层方块），防跨会话残留 */
    private static void clearTrackArea(WorldServer world, RushRun run) {
        double cx1 = run.centerX - TRACK_HALF - 1;
        double cx2 = run.centerX + TRACK_HALF + 1;
        for (Entity e : new ArrayList<>(world.loadedEntityList)) {
            if (!(e instanceof EntityPlayer)) {
                AxisAlignedBB bb = e.getEntityBoundingBox();
                if (bb.minX >= cx1 && bb.maxX <= cx2) {
                    e.setDead();
                }
            }
        }
        for (Chunk chunk : world.getChunkProvider().getLoadedChunks()) {
            int cwx = chunk.x * 16;
            int cwz = chunk.z * 16;
            if (cwz + 15 < 0) continue;
            if (cwx + 15 < cx1 || cwx > cx2) continue;
            int startX = (int) Math.max(cx1, cwx);
            int endX = (int) Math.min(cx2, cwx + 15);
            int startZ = Math.max(0, cwz);
            int endZ = cwz + 15;
            for (int x = startX; x <= endX; x++) {
                for (int z = startZ; z <= endZ; z++) {
                    for (int y = TRACK_Y2 + 1; y <= TRACK_Y2 + 3; y++) {
                        world.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState(), 2);
                    }
                }
            }
        }
    }

    // ===== 传送器接管（复用斗蛐蛐的 ArenaTeleporter） =====
    private static ArenaTeleporter installTeleporter(WorldServer world) {
        if (world == null) return null;
        if (world.getDefaultTeleporter() instanceof ArenaTeleporter) {
            return (ArenaTeleporter) world.getDefaultTeleporter();
        }
        try {
            java.lang.reflect.Field f;
            try {
                f = WorldServer.class.getDeclaredField("worldTeleporter");
            } catch (NoSuchFieldException e) {
                f = WorldServer.class.getDeclaredField("field_85177_Q");
            }
            f.setAccessible(true);
            java.lang.reflect.Field mod = java.lang.reflect.Field.class.getDeclaredField("modifiers");
            mod.setAccessible(true);
            mod.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
            ArenaTeleporter tp = new ArenaTeleporter(world);
            f.set(world, tp);
            return tp;
        } catch (Exception e) {
            log.error("[RUSH] 替换世界传送器失败", e);
            return null;
        }
    }

    // ===== 每 tick 逻辑（ServerTickEvent 调用） =====
    public static void onServerTick() {
        if (!dimensionRegistered) return;
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;
        WorldServer rushWorld = server.getWorld(RUSH_DIM_ID);
        if (rushWorld == null) return;

        if (!RUNS.isEmpty()) {
            rushWorld.setWorldTime(6000);
        }

        // 兜底：在维度里但没有比赛的玩家（/tp 进来的）→ 送回
        for (EntityPlayerMP p : new ArrayList<>(server.getPlayerList().getPlayers())) {
            if (p.dimension == RUSH_DIM_ID && !RUNS.containsKey(p.getUniqueID())) {
                returnPlayer(p);
            }
        }

        // 虚空保护
        for (EntityPlayerMP p : new ArrayList<>(server.getPlayerList().getPlayers())) {
            if (p.dimension == RUSH_DIM_ID && p.posY < 40) {
                RushRun run = RUNS.get(p.getUniqueID());
                double x = run != null ? run.buglin.posX : 0;
                double z = run != null ? run.buglin.posZ : START_Z;
                p.connection.setPlayerLocation(x, TRACK_Y2 + 2, z, p.rotationYaw, p.rotationPitch);
            }
        }

        // 遍历副本：endGame 内部会直接 RUNS.remove，不能在迭代中再 remove（会 CME）
        for (RushRun run : new ArrayList<>(RUNS.values())) {
            if (run.state == 2) continue; // 已结束（可能是其他路径先结束了）
            tickRun(run);
        }
    }

    /** @return true = 比赛结束（从 RUNS 移除） */
    private static boolean tickRun(RushRun run) {
        EntityPlayerMP player = run.player;
        if (player == null || player.isDead || !player.connection.getNetworkManager().isChannelOpen()) {
            endGame(run, "玩家离线");
            return true;
        }
        if (run.buglin == null || run.buglin.isDead) {
            endGame(run, "虫灵死亡");
            return true;
        }
        // 提前下马（shift）→ 游戏结束
        if (player.getRidingEntity() != run.buglin) {
            endGame(run, "提前下区");
            return true;
        }
        // 骑手按 shift（潜行，服务端状态已同步）→ 主动下马 = 游戏结束
        if (player.isSneaking()) {
            player.dismountRidingEntity();
            endGame(run, "提前下区");
            return true;
        }

        // 血条同步
        if (run.bossBar != null) {
            run.bossBar.setPercent(run.buglin.getHealth() / Math.max(1.0F, run.buglin.getMaxHealth()));
        }

        // 倒数
        if (run.state == 0) {
            // 兜底：travel 被生物自身覆写（马/蝙蝠/史莱姆等）时，倒数期间锁回出生点
            if (!yc.ycqin.doth.core.RushAsmHooks.wasMovedThisTick(run.buglin)) {
                run.buglin.setPosition(run.centerX, TRACK_Y2 + 1, START_Z);
                run.buglin.motionX = 0.0D;
                run.buglin.motionY = 0.0D;
                run.buglin.motionZ = 0.0D;
            }
            run.countdownTicks--;
            if (run.countdownTicks % 20 == 0) {
                int sec = (run.countdownTicks + 19) / 20;
                if (sec >= 1 && sec <= 3) {
                    sendTitle(player, "§l§6" + sec, "");
                }
            }
            if (run.countdownTicks <= 0) {
                run.state = 1;
                setRushState(run.buglin, STATE_RUN);
                sendTitle(player, "§l§a出发！", "");
            }
            return false;
        }

        if (run.state != 1) return false;

        // ===== 虫灵由 Mixin travel() 驱动（恒定前进 + 左右），这里只做判定/生成 =====
        if (run.shieldTicks > 0) run.shieldTicks--;

        // 兜底：travel 被生物自身覆写（马/蝙蝠/史莱姆等）时，服务端直接推动
        if (!yc.ycqin.doth.core.RushAsmHooks.wasMovedThisTick(run.buglin)) {
            float strafe = run.player != null ? run.player.moveStrafing * 0.5F : 0.0F;
            float spd = getRunSpeed(run.buglin);
            float s = steerFor(run.buglin, strafe);
            run.buglin.setPosition(run.buglin.posX + s * spd, run.buglin.posY, run.buglin.posZ + spd);
            run.buglin.rotationYaw = 0.0F;
            run.buglin.prevRotationYaw = 0.0F;
            run.buglin.rotationPitch = 0.0F;
            run.buglin.prevLimbSwingAmount = run.buglin.limbSwingAmount;
            run.buglin.limbSwingAmount = Math.min(1.0F, run.buglin.limbSwingAmount + 0.35F);
            run.buglin.limbSwing += run.buglin.limbSwingAmount;
        }

        double nz = run.buglin.posZ;
        double nx = run.buglin.posX;

        // ===== 分段生成 =====
        while (run.nextGenZ < nz + GEN_AHEAD) {
            generateSegment(run, (int) run.nextGenZ);
            run.nextGenZ += SEGMENT_LEN;
        }

        // ===== 持续补铺跑道（防区块生成时机导致跑道缺块/一条一条） =====
        if (run.lastFillZ <= (int) nz + 40) {
            int fillTo = (int) nz + 40;
            int cx1 = (int) Math.floor(run.centerX - TRACK_HALF);
            int cx2 = (int) Math.floor(run.centerX + TRACK_HALF);
            WorldServer w = rushWorld(run);
            if (w != null) {
                for (int x = cx1; x <= cx2; x++) {
                    for (int z = run.lastFillZ; z <= fillTo; z++) {
                        for (int y = TRACK_Y1; y <= TRACK_Y2; y++) {
                            w.setBlockState(new BlockPos(x, y, z), Blocks.QUARTZ_BLOCK.getDefaultState(), 2);
                        }
                    }
                }
            }
            run.lastFillZ = fillTo + 1;
        }

        // ===== 障碍判定：提前移除靠近的障碍（物理碰撞前），撞上掉血并继续前进 =====
        BlockPos hitPos = findNearObstacle(run, nx, nz);
        if (hitPos != null) {
            if (run.shieldTicks > 0) {
                run.shieldTicks = 0;
                player.sendMessage(new TextComponentString("§b[虫灵快跑] 护盾抵挡了碰撞！"));
            } else if (buglinInvincible) {
                // 无敌模式：不掉血
                player.sendMessage(new TextComponentString("§7[虫灵快跑] 无敌模式，碰撞免疫"));
            } else {
                float hp = run.buglin.getHealth() - OBSTACLE_DAMAGE;
                ReflectionHelper.nbSetHealth(run.buglin,Math.max(0.0F, hp));
                player.sendMessage(new TextComponentString("§c[虫灵快跑] 撞上障碍 -" + OBSTACLE_DAMAGE + " 血"));
            }
            removeObstacleBlock(run, hitPos);
            if (run.buglin.getHealth() <= 0.0F) {
                endGame(run, "虫灵死亡");
                return true;
            }
        }

        // ===== 客户端同步（防 tracker 不可靠）：每 tick 强制虫灵位置 + 每 20 tick 重申骑乘 =====
        player.connection.sendPacket(new net.minecraft.network.play.server.SPacketEntityTeleport(run.buglin));
        if (++run.mountSyncTicks >= 20) {
            run.mountSyncTicks = 0;
            NetworkHandler.INSTANCE.sendTo(new SPacketRushMount(run.buglin.getEntityId(), true), player);
        }

        // ===== 金币/道具收集 =====
        collectEntities(run);

        // ===== 身后清理 =====
        cleanupBehind(run, nz);

        // ===== 分数同步（每秒） =====
        if (++run.syncTicks >= 20) {
            run.syncTicks = 0;
            int dist = (int) Math.max(0, nz - START_Z);
            int score = dist + run.coins * 10;
            NetworkHandler.INSTANCE.sendTo(new SPacketRushState(RUSH_DIM_ID, score, run.coins, dist, true), player);
        }
        return false;
    }

    private static WorldServer rushWorld(RushRun run) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        return server != null ? server.getWorld(RUSH_DIM_ID) : null;
    }

    // ===== 分段生成 =====
    private static void generateSegment(RushRun run, int segZ) {
        WorldServer world = rushWorld(run);
        if (world == null) return;
        double cx = run.centerX;

        // 越往后障碍越密集（随距离提升概率）
        double dist = Math.max(0.0D, segZ - START_Z);
        double obstacleChance = Math.min(OBSTACLE_MAX, OBSTACLE_CHANCE + dist * OBSTACLE_RAMP);

        // 障碍：随机分布在整条跑道（相对中心 -5~+5），数量随距离递增（1~3 个）
        int obstacleCount = 1;
        if (RNG.nextDouble() < obstacleChance) obstacleCount++;
        if (RNG.nextDouble() < obstacleChance * 0.5D) obstacleCount++;
        obstacleCount = Math.min(3, obstacleCount);
        Set<Integer> usedX = new HashSet<>();
        int placed = 0;
        int tries = 0;
        while (placed < obstacleCount && tries < 20) {
            tries++;
            int laneX = (int) cx + RNG.nextInt(11) - 5; // cx-5 .. cx+5
            if (!usedX.add(laneX)) continue;
            placed++;
            // 障碍：2 格高，坐在跑道上
            BlockPos p1 = new BlockPos(laneX, TRACK_Y2 + 1, segZ);
            BlockPos p2 = new BlockPos(laneX, TRACK_Y2 + 2, segZ);
            world.setBlockState(p1, Blocks.OBSIDIAN.getDefaultState(), 2);
            world.setBlockState(p2, Blocks.OBSIDIAN.getDefaultState(), 2);
            run.obstacles.add(p1);
            run.obstacles.add(p2);
        }

        // 金币行（随机 x，3 个）——y 与虫灵同层，确保能吃到
        if (RNG.nextDouble() < COIN_CHANCE) {
            int coinX = (int) cx + RNG.nextInt(11) - 5;
            for (int k = 1; k <= 3; k++) {
                EntityCoin coin = new EntityCoin(world);
                coin.addTag(TAG_RUSH);
                coin.setPosition(coinX, TRACK_Y2 + 1.5D, segZ + k);
                world.spawnEntity(coin);
                run.coinEntities.add(coin);
            }
        }

        // 道具（随机 x）
        if (RNG.nextDouble() < POWERUP_CHANCE) {
            EntityRushPowerup pu = new EntityRushPowerup(world);
            pu.addTag(TAG_RUSH);
            pu.setType(RNG.nextInt(3));
            pu.setPosition((int) cx + RNG.nextInt(11) - 5, TRACK_Y2 + 1.5D, segZ + 4);
            world.spawnEntity(pu);
            run.powerupEntities.add(pu);
        }
    }

    // ===== 障碍判定：用虫灵实际包围盒与障碍方块相交判定（提前移除，防物理卡住） =====
    // 判定盒四边各向外扩 0.05 格：物理碰撞会把虫灵挡在方块表面（进不了方块内部），
    // 判定盒必须比真实方块稍大，才能在撞上前一 tick 触发（删块 + 掉血）。
    // 1 格宽空隙（0.5 宽虫灵）居中仍可穿过。
    private static final double OBSTACLE_HIT_EXPAND = 0.05D;

    private static BlockPos findNearObstacle(RushRun run, double nx, double nz) {
        EntityLivingBase buglin = run.buglin;
        if (buglin == null) return null;
        AxisAlignedBB bugBox = buglin.getEntityBoundingBox();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : run.obstacles) {
            // 障碍实际占用 [x, x+1] x [z, z+1]，判定范围四边各向外扩 0.05 格
            double ox1 = p.getX() - OBSTACLE_HIT_EXPAND;
            double ox2 = p.getX() + 1.0D + OBSTACLE_HIT_EXPAND;
            double oz1 = p.getZ() - OBSTACLE_HIT_EXPAND;
            double oz2 = p.getZ() + 1.0D + OBSTACLE_HIT_EXPAND;
            if (bugBox.maxX <= ox1 || bugBox.minX >= ox2) continue;
            if (bugBox.maxZ <= oz1 || bugBox.minZ >= oz2) continue;
            double dx = p.getX() + 0.5D - nx;
            double d = dx * dx;
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    private static void removeObstacleBlock(RushRun run, BlockPos pos) {
        World world = rushWorld(run);
        if (world == null) return;
        world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
        // 同列的上下块一起清（2 格高障碍）
        world.setBlockState(pos.up(), Blocks.AIR.getDefaultState(), 2);
        world.setBlockState(pos.down(), Blocks.AIR.getDefaultState(), 2);
        run.obstacles.remove(pos);
        run.obstacles.remove(pos.up());
        run.obstacles.remove(pos.down());
    }

    private static void collectEntities(RushRun run) {
        World world = rushWorld(run);
        if (world == null) return;
        EntityLivingBase buglin = run.buglin;
        AxisAlignedBB box = buglin.getEntityBoundingBox().grow(0.5D, 0.5D, 0.5D);

        Iterator<EntityCoin> cit = run.coinEntities.iterator();
        while (cit.hasNext()) {
            EntityCoin coin = cit.next();
            if (coin.isDead) {
                cit.remove();
                continue;
            }
            if (coin.getEntityBoundingBox().intersects(box)) {
                coin.setDead();
                cit.remove();
                run.coins++;
                world.playSound(null, coin.getPosition(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                        SoundCategory.PLAYERS, 0.6F, 1.2F);
            }
        }

        Iterator<EntityRushPowerup> pit = run.powerupEntities.iterator();
        while (pit.hasNext()) {
            EntityRushPowerup pu = pit.next();
            if (pu.isDead) {
                pit.remove();
                continue;
            }
            if (pu.getEntityBoundingBox().intersects(box)) {
                pu.setDead();
                pit.remove();
                applyPowerup(run, pu.getType());
            }
        }
    }

    private static void applyPowerup(RushRun run, int type) {
        switch (type) {
            case 0:
                run.shieldTicks = 200;
                run.player.sendMessage(new TextComponentString("§b[虫灵快跑] 获得护盾！"));
                break;
            case 1:
                float hp = Math.min(run.buglin.getMaxHealth(), run.buglin.getHealth() + 3.0F);
                run.buglin.setHealth(hp);
                run.player.sendMessage(new TextComponentString("§a[虫灵快跑] 回复 3 点血！"));
                break;
            default:
                // 加速：给虫灵速度药水效果（原版移动会按效果加成）
                run.buglin.addPotionEffect(new net.minecraft.potion.PotionEffect(
                        net.minecraft.init.MobEffects.SPEED, 100, 0, false, false));
                run.player.sendMessage(new TextComponentString("§e[虫灵快跑] 加速！"));
                break;
        }
    }

    // ===== 身后清理 =====
    private static void cleanupBehind(RushRun run, double nz) {
        World world = rushWorld(run);
        if (world == null) return;
        double behind = nz - CLEAN_BEHIND;

        Iterator<BlockPos> oit = run.obstacles.iterator();
        while (oit.hasNext()) {
            BlockPos p = oit.next();
            if (p.getZ() < behind) {
                world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                oit.remove();
            }
        }
        Iterator<EntityCoin> cit = run.coinEntities.iterator();
        while (cit.hasNext()) {
            EntityCoin coin = cit.next();
            if (coin.posZ < behind) {
                coin.setDead();
                cit.remove();
            }
        }
        Iterator<EntityRushPowerup> pit = run.powerupEntities.iterator();
        while (pit.hasNext()) {
            EntityRushPowerup pu = pit.next();
            if (pu.posZ < behind) {
                pu.setDead();
                pit.remove();
            }
        }
    }

    // ===== 结束 =====
    private static void endGame(RushRun run, String reason) {
        if (run.state == 2) return;
        run.state = 2;

        int dist = (int) Math.max(0, run.buglin != null ? run.buglin.posZ - START_Z : 0);
        int score = dist + run.coins * 10;
        sendTitle(run.player, "§l§6游戏结束！", "§e得分 " + score + "  §7距离 " + dist + "m  金币 " + run.coins);
        run.player.sendMessage(new TextComponentString("§e[虫灵快跑] 结算：距离 " + dist + "m + 金币 " + run.coins + "×10 = §6得分 " + score));

        NetworkHandler.INSTANCE.sendTo(new SPacketRushState(RUSH_DIM_ID, score, run.coins, dist, false), run.player);

        // 清理：虫灵/金币/道具/障碍
        World world = rushWorld(run);
        if (world != null) {
            if (run.buglin != null) {
                run.buglin.setDead();
            }
            for (EntityCoin coin : run.coinEntities) {
                if (!coin.isDead) coin.setDead();
            }
            for (EntityRushPowerup pu : run.powerupEntities) {
                if (!pu.isDead) pu.setDead();
            }
            for (BlockPos p : run.obstacles) {
                world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
            }
        }
        if (run.bossBar != null) {
            run.bossBar.removePlayer(run.player);
            run.bossBar = null;
        }
        // 客户端解除骑乘
        NetworkHandler.INSTANCE.sendTo(new SPacketRushMount(-1, false), run.player);

        USED_TRACKS.remove(run.trackIndex);
        RUNS.remove(run.player.getUniqueID());
        returnPlayer(run.player);
        log.info("[RUSH] {} 比赛结束（{}），得分 {}", run.player.getName(), reason, score);
    }

    /** 玩家回原位置，恢复游戏模式 */
    private static void returnPlayer(EntityPlayerMP player) {
        if (player.dimension == RUSH_DIM_ID) {
            NBTTagCompound data = player.getEntityData();
            int origDim = data.getInteger(KEY_ORIG_DIM);
            double x = data.getDouble(KEY_ORIG_X);
            double y = data.getDouble(KEY_ORIG_Y);
            double z = data.getDouble(KEY_ORIG_Z);
            int gt = data.getInteger(KEY_ORIG_GT);

            if (origDim == RUSH_DIM_ID || (origDim == 0 && x == 0 && z == 0)) {
                origDim = 0;
                BlockPos spawn = player.world.getSpawnPoint();
                x = spawn.getX() + 0.5;
                y = spawn.getY();
                z = spawn.getZ() + 0.5;
            }
            WorldServer target = player.getServer().getWorld(origDim);
            if (target != null) {
                net.minecraft.world.Teleporter original = target.getDefaultTeleporter();
                ArenaTeleporter tp = installTeleporter(target);
                if (tp != null) tp.setTarget(x, y, z);
                player.changeDimension(origDim);
                if (target.getDefaultTeleporter() instanceof ArenaTeleporter) {
                    try {
                        java.lang.reflect.Field f;
                        try {
                            f = WorldServer.class.getDeclaredField("worldTeleporter");
                        } catch (NoSuchFieldException e) {
                            f = WorldServer.class.getDeclaredField("field_85177_Q");
                        }
                        f.setAccessible(true);
                        java.lang.reflect.Field mod = java.lang.reflect.Field.class.getDeclaredField("modifiers");
                        mod.setAccessible(true);
                        mod.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
                        f.set(target, original);
                    } catch (Exception ignored) {
                    }
                }
            } else {
                player.changeDimension(origDim);
            }
            player.connection.setPlayerLocation(x, y, z, player.rotationYaw, player.rotationPitch);
            player.setGameType(GameType.getByID(gt));
            player.sendMessage(new TextComponentString("§a[虫灵快跑] 已传送回原位置"));
        }
    }

    // ===== 坐骑状态（DataWatcher，自动同步客户端；ASM 在 EntityLivingBase.entityInit 里注册这个 key） =====
    // 所有生物共用 EntityLivingBase 作 key owner（1.12.2 不校验 owner，子类实例可正常注册）
    private static DataParameter<Byte> rushStateKey = null;

    public static DataParameter<Byte> getRushStateKey() {
        if (rushStateKey == null) {
            rushStateKey = EntityDataManager.createKey(EntityLivingBase.class, DataSerializers.BYTE);
        }
        return rushStateKey;
    }

    public static void setRushState(EntityLivingBase buglin, byte state) {
        if (buglin != null) {
            buglin.getDataManager().set(getRushStateKey(), state);
        }
    }

    // ===== 生物名 =====

    /** 生物注册名 → 翻译 key（EntityEntry 名，可能是翻译 key 或原版名如 Zombie） */
    public static String getMobTranslationKey(String mobId) {
        if (mobId == null || mobId.isEmpty()) return null;
        try {
            return EntityList.getTranslationName(new ResourceLocation(mobId));
        } catch (Exception e) {
            return null;
        }
    }

    /** 生物注册名 → 本地化显示名（客户端按当前语言，服务端用默认语言兜底） */
    public static String getMobDisplayName(String mobId) {
        String raw = getMobTranslationKey(mobId);
        String fallback = mobId != null && mobId.contains(":") ? mobId.substring(mobId.indexOf(':') + 1) : mobId;
        if (raw == null || raw.isEmpty()) return fallback;
        try {
            boolean client = net.minecraftforge.fml.common.FMLCommonHandler.instance().getEffectiveSide()
                    == net.minecraftforge.fml.relauncher.Side.CLIENT;
            String t = client
                    ? net.minecraft.client.resources.I18n.format(raw)
                    : net.minecraft.util.text.translation.I18n.translateToLocal(raw);
            if (t != null && !t.isEmpty() && !t.equals(raw)) return t;
            // 原版生物：EntityEntry 名是 Zombie 这类，翻译 key 是 entity.Zombie.name
            String k2 = "entity." + raw + ".name";
            String t2 = client
                    ? net.minecraft.client.resources.I18n.format(k2)
                    : net.minecraft.util.text.translation.I18n.translateToLocal(k2);
            if (t2 != null && !t2.isEmpty() && !t2.equals(k2)) return t2;
        } catch (Exception ignored) {
        }
        return fallback;
    }

    /** 选聊天组件用的翻译 key：raw 本身就是 key 就用 raw，否则按原版规则拼 entity.raw.name */
    private static String pickMobKey(String raw) {
        try {
            String t = net.minecraft.util.text.translation.I18n.translateToLocal(raw);
            if (t != null && !t.isEmpty() && !t.equals(raw)) return raw;
            String k2 = "entity." + raw + ".name";
            String t2 = net.minecraft.util.text.translation.I18n.translateToLocal(k2);
            if (t2 != null && !t2.isEmpty() && !t2.equals(k2)) return k2;
        } catch (Exception ignored) {
        }
        return raw;
    }

    /** 生物注册名 → 可本地化的文本组件（客户端渲染时按玩家语言显示） */
    public static ITextComponent mobNameComponent(String mobId) {
        String raw = getMobTranslationKey(mobId);
        if (raw != null && !raw.isEmpty()) {
            return new TextComponentTranslation(pickMobKey(raw));
        }
        String fallback = mobId != null && mobId.contains(":") ? mobId.substring(mobId.indexOf(':') + 1) : mobId;
        return new TextComponentString(fallback);
    }

    /** 当前奔跑速度：基础 + 距离递增（每 100 格 +0.05，上限 1.0），加速道具加成 */
    public static float getRunSpeed(EntityLivingBase buglin) {
        if (buglin == null) return RUN_SPEED_F;
        double dist = Math.max(0.0D, buglin.posZ - START_Z);
        float spd = (float) Math.min(SPEED_MAX, RUN_SPEED_F + dist * SPEED_RAMP);
        if (buglin.isPotionActive(net.minecraft.init.MobEffects.SPEED)) {
            spd *= 1.0F + 0.2F * (buglin.getActivePotionEffect(net.minecraft.init.MobEffects.SPEED).getAmplifier() + 1);
        }
        return spd;
    }

    /** 左右转向限位：虫灵不会跑出自己赛道范围（ASM travel 调用） */
    public static float steerFor(Entity buglin, float strafe) {
        for (RushRun run : RUNS.values()) {
            if (run.buglin == buglin) {
                double next = buglin.posX + strafe * getRunSpeed((EntityLivingBase) buglin);
                if (next < run.centerX - MAX_X || next > run.centerX + MAX_X) {
                    return 0.0F;
                }
                return strafe;
            }
        }
        return strafe;
    }

    // ===== 供区块生成器查询 =====
    public static List<double[]> getActiveTrackCenters() {
        List<double[]> list = new ArrayList<>();
        for (RushRun run : RUNS.values()) {
            list.add(new double[]{run.centerX, run.trackIndex});
        }
        return list;
    }

    // ===== Title =====
    public static void sendTitle(EntityPlayerMP player, String title, String subtitle) {
        player.connection.sendPacket(new SPacketTitle(SPacketTitle.Type.TITLE,
                new TextComponentString(title)));
        if (subtitle != null && !subtitle.isEmpty()) {
            player.connection.sendPacket(new SPacketTitle(SPacketTitle.Type.SUBTITLE,
                    new TextComponentString(subtitle)));
        }
    }

    // ===== 维度守卫事件 =====

    /** 维度内禁止非比赛实体生成（防 SRP 刷怪/进化产物/野怪）。只服务端生效——客户端实体 tag 不同步，会误拦虫灵 */
    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public static void onEntityJoinWorld(net.minecraftforge.event.entity.EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote) return; // 客户端不拦截（tag 不同步）
        if (!isRushDimension(event.getWorld())) return;
        Entity e = event.getEntity();
        if (e instanceof net.minecraft.entity.player.EntityPlayer) return;
        if (e.getTags().contains(TAG_RUSH)) return;
        event.setCanceled(true);
    }

    /** 维度内玩家免疫伤害；虫灵本体也免伤（防窒息等杂伤，比赛伤害走 setHealth） */
    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public static void onLivingHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (!isRushDimension(event.getEntity().world)) return;
        if (event.getEntity() instanceof EntityPlayerMP) {
            event.setCanceled(true);
        } else if (event.getEntity().getTags().contains(TAG_RUSH)) {
            event.setCanceled(true);
        }
    }

    /** 维度内玩家死亡（/kill 等）→ 取消并结束比赛 */
    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public static void onLivingDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof EntityPlayerMP && isRushDimension(event.getEntity().world)) {
            RushRun run = RUNS.get(event.getEntity().getUniqueID());
            if (run != null) {
                event.setCanceled(true);
                endGame(run, "玩家死亡");
            }
        }
    }

    /** 维度内禁止破坏/放置方块 */
    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public static void onBlockBreak(net.minecraftforge.event.world.BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof EntityPlayerMP && isRushDimension(event.getWorld())) {
            event.setCanceled(true);
        }
    }

    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public static void onBlockPlace(net.minecraftforge.event.world.BlockEvent.PlaceEvent event) {
        if (event.getPlayer() instanceof EntityPlayerMP && isRushDimension(event.getWorld())) {
            event.setCanceled(true);
        }
    }

    /** 玩家离线 → 结束比赛 */
    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public static void onPlayerLoggedOut(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            RushRun run = RUNS.get(event.player.getUniqueID());
            if (run != null) {
                endGame(run, "玩家离线");
            }
        }
    }
}
