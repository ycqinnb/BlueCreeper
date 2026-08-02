package yc.ycqin.doth.world;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Biomes;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.play.server.SPacketTitle;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.DimensionType;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import yc.ycqin.doth.common.block.TileEntityFighter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 斗蛐蛐维度核心逻辑
 */
public class ArenaManager {

    private static final Logger log = LogManager.getLogger("DOTH-Arena");

    // ===== 维度注册 =====
    public static int ARENA_DIM_ID = -1;
    public static DimensionType DIMENSION_TYPE = null;
    private static boolean dimensionRegistered = false;

    // ===== 场地范围（随尺寸变化，默认 15×15 = 旧版尺寸） =====
    /** 地块厚度 5 格（固定） */
    public static final int Y1 = 50, Y2 = 54;
    /** 中间 7 格虚空连通区（固定） */
    public static final int GAP_X1 = -3, GAP_X2 = 3;
    /** 场地尺寸：平台边长（默认 15×15；手持战斗爽方块右键 +1 / shift+右键 -1） */
    private static int arenaSize = 15;
    private static final int ARENA_SIZE_MIN = 5;
    private static final int ARENA_SIZE_MAX = 100;
    /** 左地块 x 范围 */
    private static int leftX1 = -18, leftX2 = -4;
    /** 右地块 x 范围 */
    private static int rightX1 = 4, rightX2 = 18;
    /** 两地块 z 范围 */
    private static int z1 = 2, z2 = 16;

    /** 按尺寸重算场地范围（默认 15 时与旧版一致） */
    private static void applySize() {
        leftX1 = -4 - (arenaSize - 1);
        leftX2 = -4;
        rightX1 = 4;
        rightX2 = 4 + (arenaSize - 1);
        z1 = 2;
        z2 = 2 + (arenaSize - 1);
    }

    // ===== 战斗状态 =====
    private static boolean arenaBuilt = false;
    /** 场地是否就绪可进入（构建/重置后 true，战斗结束后 false → 下次右键需重置） */
    private static boolean arenaReady = false;
    private static boolean battleActive = false;
    private static int countdownTicks = -1;          // 倒数剩余 tick
    private static int battleTicks = 0;              // 战斗进行 tick
    private static int fireworkTicks = 0;            // 烟花剩余 tick
    private static int winnerSide = 0;               // 1=左, 2=右, 0=平局
    private static final int COUNTDOWN_TOTAL = 3 * 20;
    private static final int BATTLE_TIMEOUT = 10 * 60 * 20;   // 10分钟保底，超时按剩余血量判定胜负
    private static final int FIREWORK_TOTAL = 10 * 20;        // 10秒烟花

    // ===== Boss 血条（双方总血量） =====
    private static BossInfoServer leftBossBar;
    private static BossInfoServer rightBossBar;
    /** 当前血条显示名（用于检测名字变化重建血条） */
    private static String leftBarName = "";
    private static String rightBarName = "";
    /** 双方队伍名（铁砧自定义名，空 = 未命名） */
    private static String leftTeamName = "";
    private static String rightTeamName = "";

    // ===== 场地地板方块 & 生物群系（取自选手方块下方方块 / 所在生物群系） =====
    private static Block leftFloorBlock = Blocks.STONE;
    private static Block rightFloorBlock = Blocks.STONE;
    private static Biome leftFloorBiome = Biomes.PLAINS;
    private static Biome rightFloorBiome = Biomes.PLAINS;

    // ===== 选手方块注册表（运行时索引，不持久化，避免构建时全图遍历方块） =====
    /** 选手方块引用：维度 + 坐标 */
    public static class FighterRef {
        public final int dim;
        public final BlockPos pos;

        public FighterRef(int dim, BlockPos pos) {
            this.dim = dim;
            this.pos = pos;
        }

        public double distanceSq(BlockPos from) {
            return pos.distanceSq(from);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FighterRef)) return false;
            FighterRef f = (FighterRef) o;
            return dim == f.dim && pos.equals(f.pos);
        }

        @Override
        public int hashCode() {
            return dim * 31 + pos.hashCode();
        }
    }

    private static final Map<String, FighterRef> FIGHTERS = new HashMap<>();

    private static final String TAG_LEFT = "doth_arena_left";
    private static final String TAG_RIGHT = "doth_arena_right";

    private static final Random RNG = new Random();

    // ===== NBT 键 =====
    public static final String KEY_ORIG_DIM = "doth_orig_dim";
    public static final String KEY_ORIG_X = "doth_orig_x";
    public static final String KEY_ORIG_Y = "doth_orig_y";
    public static final String KEY_ORIG_Z = "doth_orig_z";
    public static final String KEY_ORIG_GT = "doth_orig_gametype";

    /**
     * 注册维度。ID 从 666 开始，占用则 +100，最多尝试 3 次。
     * @return 是否注册成功
     */
    public static boolean registerDimension() {
        if (dimensionRegistered) return true;
        int baseId = 666;
        for (int attempt = 0; attempt < 3; attempt++) {
            int id = baseId + attempt * 100;
            if (DimensionManager.isDimensionRegistered(id)) {
                log.warn("[DOTH] 维度ID {} 已被占用，尝试下一个", id);
                continue;
            }
            try {
                // keepLoaded=true：维度常驻，避免“无玩家→卸载→每tick又强制加载”的死循环
                DIMENSION_TYPE = DimensionType.register("DOTH_ARENA_" + id, "_arena" + id, id, ArenaWorldProvider.class, true);
                DimensionManager.registerDimension(id, DIMENSION_TYPE);
                ARENA_DIM_ID = id;
                dimensionRegistered = true;
                log.info("[DOTH] 斗蛐蛐维度注册成功，ID = {}", id);
                return true;
            } catch (Exception e) {
                log.error("[DOTH] 维度注册失败 ID={}", id, e);
            }
        }
        log.error("[DOTH] 斗蛐蛐维度注册失败：3次尝试均被占用");
        return false;
    }

    public static boolean isDimensionRegistered() {
        return dimensionRegistered;
    }

    public static boolean isBuilt() {
        return arenaBuilt;
    }

    /** 场地是否就绪可进入（true=可进入；false=需先重置） */
    public static boolean isArenaReady() {
        return arenaReady;
    }

    public static boolean isBattleActive() {
        return battleActive;
    }

    // ===== 构建场地 =====

    /**
     * 构建场地：清空 → 按倍率建左右两块地 → 生成双方选手（NoAI）
     * 地板方块 = 选手方块下方方块（除空气），生物群系 = 选手方块所在生物群系
     * @param left  左选手方块引用（X 小的一侧）
     * @param right 右选手方块引用
     */
    public static boolean buildArena(FighterRef left, FighterRef right) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || !dimensionRegistered) return false;

        WorldServer arenaWorld = server.getWorld(ARENA_DIM_ID);
        if (arenaWorld == null) return false;

        // 读取双方选手数据 + 地板方块 + 生物群系
        NBTTagCompound leftNbt = readFighterNbt(left);
        NBTTagCompound rightNbt = readFighterNbt(right);
        if (leftNbt == null || rightNbt == null) return false;
        Block leftFloor = getFloorBlock(left);
        Block rightFloor = getFloorBlock(right);
        Biome leftBiome = getBiomeAt(left);
        Biome rightBiome = getBiomeAt(right);

        // 清空旧场地（含生物）
        clearArena(arenaWorld);

        // 建左地块（方块 = 左选手下方方块）
        fillBox(arenaWorld, leftX1, Y1, z1, leftX2, Y2, z2, leftFloor);
        // 建右地块（方块 = 右选手下方方块）
        fillBox(arenaWorld, rightX1, Y1, z1, rightX2, Y2, z2, rightFloor);
        // 中间 7 格连通区保持虚空，战斗开始（startBattle）时才填平

        // 生成选手
        spawnFighterTeam(arenaWorld, leftNbt, leftX1, leftX2, TAG_LEFT);
        spawnFighterTeam(arenaWorld, rightNbt, rightX1, rightX2, TAG_RIGHT);

        // 场地生物群系 = 选手所在生物群系（provider 覆盖 + 区块数据双保险）
        leftFloorBlock = leftFloor;
        rightFloorBlock = rightFloor;
        leftFloorBiome = leftBiome;
        rightFloorBiome = rightBiome;
        applyPlatformBiomes(arenaWorld, leftBiome, rightBiome);

        arenaBuilt = true;
        arenaReady = true;
        battleActive = false;
        countdownTicks = -1;
        battleTicks = 0;
        fireworkTicks = 0;
        winnerSide = 0;
        leftTeamName = getTeamName(leftNbt);
        rightTeamName = getTeamName(rightNbt);
        initBossBars();
        log.info("[DOTH] 斗蛐蛐场地构建完成（{}×{}）", arenaSize, arenaSize);
        return true;
    }

    private static void spawnFighterTeam(World world, NBTTagCompound fighterNbt, int x1, int x2, String teamTag) {
        if (fighterNbt == null) return;
        NBTTagList idList = fighterNbt.getTagList("EntityIDs", 8);
        if (idList == null || idList.tagCount() == 0) return;

        for (int i = 0; i < idList.tagCount(); i++) {
            String entityId = idList.getStringTagAt(i);
            if (entityId.isEmpty()) continue;

            ResourceLocation rl = new ResourceLocation(entityId);
            Entity entity = EntityList.createEntityByIDFromName(rl, world);
            if (entity == null) {
                log.warn("[DOTH] 无法创建实体 {}", entityId);
                continue;
            }

            // 随机放在地块上（y = 地块顶 + 1）
            double x = x1 + 1 + RNG.nextDouble() * (x2 - x1 - 2);
            double z = z1 + 1 + RNG.nextDouble() * (z2 - z1 - 2);
            entity.setPosition(x, Y2 + 1, z);

            if (entity instanceof EntityLiving) {
                EntityLiving living = (EntityLiving) entity;
                living.setNoAI(true);
                living.setHealth(living.getMaxHealth());
                living.addTag(teamTag);
                // 应用药水效果
                applyPotionEffects(living, fighterNbt);
                world.spawnEntity(entity);
                log.info("[DOTH] 生成选手 {} -> {}", entityId, teamTag);
            }
        }
    }

    private static void applyPotionEffects(EntityLivingBase living, NBTTagCompound nbt) {
        if (nbt == null || !nbt.hasKey("PotionEffects")) return;
        NBTTagList list = nbt.getTagList("PotionEffects", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            PotionEffect effect = PotionEffect.readCustomPotionEffectFromNBT(list.getCompoundTagAt(i));
            if (effect != null) {
                living.addPotionEffect(effect);
            }
        }
    }

    private static void fillBox(World world, int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        for (int x = x1; x <= x2; x++)
            for (int y = y1; y <= y2; y++)
                for (int z = z1; z <= z2; z++)
                    world.setBlockState(new BlockPos(x, y, z), block.getDefaultState(), 2);
    }

    private static void clearBox(World world, int x1, int y1, int z1, int x2, int y2, int z2) {
        for (int x = x1; x <= x2; x++)
            for (int y = y1; y <= y2; y++)
                for (int z = z1; z <= z2; z++)
                    world.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState(), 2);
    }

    /** 清空整个场地区域 + 移除所有非玩家实体（选手、召唤物、残留物品） */
    public static void clearArena(WorldServer world) {
        clearBox(world, leftX1 - 3, Y1 - 3, z1 - 3, rightX2 + 3, Y2 + 6, z2 + 3);

        // 清掉所有非玩家实体（选手 + 选手召唤的仆从如恼鬼 + 残留掉落物）
        List<Entity> toRemove = new ArrayList<>();
        for (Entity e : world.loadedEntityList) {
            if (!(e instanceof EntityPlayer)) {
                toRemove.add(e);
            }
        }
        for (Entity e : toRemove) {
            e.isDead = true;
            world.removeEntity(e);
        }
    }

    // ===== 选手方块注册表（运行时索引） =====

    /** 选手方块 TE 每秒调用：把自己登记进注册表（不在列表就加） */
    public static void registerFighter(TileEntityFighter te) {
        if (te == null || te.getWorld() == null || te.getWorld().isRemote) return;
        FighterRef ref = new FighterRef(te.getWorld().provider.getDimension(), te.getPos());
        FIGHTERS.putIfAbsent(key(ref), ref);
    }

    /** 选手方块被拆掉时从注册表移除 */
    public static void unregisterFighter(World world, BlockPos pos) {
        if (world == null || world.isRemote) return;
        FIGHTERS.remove(key(world.provider.getDimension(), pos));
    }

    private static String key(FighterRef ref) {
        return ref.dim + ":" + ref.pos.getX() + "," + ref.pos.getY() + "," + ref.pos.getZ();
    }

    private static String key(int dim, BlockPos pos) {
        return dim + ":" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    /** 找最近的 N 个有效选手方块（同维度，顺带清理失效项） */
    public static List<FighterRef> findNearestFighters(BlockPos from, int dim, int count) {
        List<FighterRef> candidates = new ArrayList<>();
        Iterator<Map.Entry<String, FighterRef>> it = FIGHTERS.entrySet().iterator();
        while (it.hasNext()) {
            FighterRef ref = it.next().getValue();
            if (ref.dim != dim) continue;
            if (!isFighterValid(ref)) {
                it.remove();
                continue;
            }
            candidates.add(ref);
        }
        candidates.sort(Comparator.comparingDouble(r -> r.distanceSq(from)));
        List<FighterRef> result = new ArrayList<>();
        for (int i = 0; i < candidates.size() && i < count; i++) {
            result.add(candidates.get(i));
        }
        return result;
    }

    /** 选手方块是否仍有效（所在世界/区块加载且 TE 未失效） */
    private static boolean isFighterValid(FighterRef ref) {
        WorldServer w = DimensionManager.getWorld(ref.dim);
        if (w == null) return false;
        TileEntity te = w.getTileEntity(ref.pos);
        return te instanceof TileEntityFighter && !te.isInvalid();
    }

    /** 读取选手方块 NBT（含 EntityIDs 校验），无效返回 null */
    public static NBTTagCompound readFighterNbt(FighterRef ref) {
        if (ref == null) return null;
        WorldServer w = DimensionManager.getWorld(ref.dim);
        if (w == null) return null;
        TileEntity te = w.getTileEntity(ref.pos);
        if (te instanceof TileEntityFighter) {
            NBTTagCompound nbt = ((TileEntityFighter) te).getFighterNbt();
            if (nbt != null && nbt.hasKey("EntityIDs") && nbt.getTagList("EntityIDs", 8).tagCount() > 0) {
                return nbt;
            }
        }
        return null;
    }

    /** 选手方块下方方块（除空气；空气 → 默认石头） */
    private static Block getFloorBlock(FighterRef ref) {
        WorldServer w = DimensionManager.getWorld(ref.dim);
        if (w == null) return Blocks.STONE;
        IBlockState below = w.getBlockState(ref.pos.down());
        if (below.getBlock() == Blocks.AIR) return Blocks.STONE;
        return below.getBlock();
    }

    /** 选手方块所在生物群系 */
    private static Biome getBiomeAt(FighterRef ref) {
        WorldServer w = DimensionManager.getWorld(ref.dim);
        if (w == null) return Biomes.PLAINS;
        return w.getBiome(ref.pos);
    }

    /** 把左右地块的区块生物群系刷成选手所在群系（客户端颜色/F3 双保险） */
    private static void applyPlatformBiomes(WorldServer world, Biome left, Biome right) {
        setChunkBiomes(world, leftX1, z1, leftX2, z2, left);
        setChunkBiomes(world, rightX1, z1, rightX2, z2, right);
    }

    private static void setChunkBiomes(WorldServer world, int x1, int z1, int x2, int z2, Biome biome) {
        byte id = (byte) Biome.getIdForBiome(biome);
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                Chunk chunk = world.getChunkFromBlockCoords(new BlockPos(x, 64, z));
                byte[] arr = chunk.getBiomeArray();
                arr[(z & 15) << 4 | (x & 15)] = id;
                chunk.setBiomeArray(arr);
            }
        }
    }

    // ===== 场地范围调整（战斗爽方块手持右键） =====

    public static int getArenaSize() {
        return arenaSize;
    }

    /** 场地尺寸标签（如 15×15） */
    public static String sizeLabel() {
        return arenaSize + "×" + arenaSize;
    }

    /** 调整场地尺寸（每次 ±1，默认 15×15），战斗中仅提示、下一场生效 */
    public static void changeSize(EntityPlayerMP player, int delta) {
        int ns = arenaSize + delta;
        if (ns < ARENA_SIZE_MIN) ns = ARENA_SIZE_MIN;
        if (ns > ARENA_SIZE_MAX) ns = ARENA_SIZE_MAX;
        if (ns == arenaSize) {
            String bound = arenaSize >= ARENA_SIZE_MAX ? "（已达上限）" : "（已达下限）";
            player.sendMessage(new TextComponentString("§e[斗蛐蛐] 场地大小已是 " + arenaSize + "×" + arenaSize + bound));
            return;
        }
        arenaSize = ns;
        applySize();
        if (battleActive || countdownTicks > 0) {
            player.sendMessage(new TextComponentString("§e[斗蛐蛐] 场地大小已调整为 " + ns + "×" + ns + "，本场结束后重新构建生效"));
        } else {
            if (arenaBuilt) {
                arenaReady = false;
                player.sendMessage(new TextComponentString("§a[斗蛐蛐] 场地大小已调整为 " + ns + "×" + ns + "，再次右键战斗方块重建场地"));
            } else {
                player.sendMessage(new TextComponentString("§a[斗蛐蛐] 场地大小已调整为 " + ns + "×" + ns + "，构建场地时生效"));
            }
        }
    }

    /** 构建结果描述：队名（地板方块/生物群系） */
    public static String describeMatchup() {
        String l = teamDisplayName(TAG_LEFT) + "（" + leftFloorBlock.getRegistryName() + "/" + leftFloorBiome.getBiomeName() + "）";
        String r = teamDisplayName(TAG_RIGHT) + "（" + rightFloorBlock.getRegistryName() + "/" + rightFloorBiome.getBiomeName() + "）";
        return l + "  §7VS§r  " + r;
    }

    /** 场地生物群系（给 ArenaWorldProvider 覆盖用） */
    public static boolean isLeftPlatform(int x, int z) {
        return x >= leftX1 && x <= leftX2 && z >= z1 && z <= z2;
    }

    public static boolean isRightPlatform(int x, int z) {
        return x >= rightX1 && x <= rightX2 && z >= z1 && z <= z2;
    }

    public static Biome getLeftBiome() {
        return leftFloorBiome;
    }

    public static Biome getRightBiome() {
        return rightFloorBiome;
    }

    // ===== 玩家进入 =====

    /**
     * 玩家进入斗蛐蛐维度：记录原位置/模式 → 传送 0 75 0 → 旁观模式
     */
    public static void enterArena(EntityPlayerMP player) {
        if (!dimensionRegistered) {
            player.sendMessage(new TextComponentString("§c[斗蛐蛐] 维度未注册，无法进入"));
            return;
        }

        // 记录原状态
        NBTTagCompound data = player.getEntityData();
        data.setInteger(KEY_ORIG_DIM, player.dimension);
        data.setDouble(KEY_ORIG_X, player.posX);
        data.setDouble(KEY_ORIG_Y, player.posY);
        data.setDouble(KEY_ORIG_Z, player.posZ);
        data.setInteger(KEY_ORIG_GT, player.interactionManager.getGameType().getID());

        // 使用原版传送链（changeDimension）：先反射接管 arena 世界传送器，直接定位 0 60 0 并目视场地
        WorldServer target = player.getServer().getWorld(ARENA_DIM_ID);
        if (target == null) {
            player.sendMessage(new TextComponentString("§c[斗蛐蛐] 目标世界加载失败"));
            return;
        }
        ArenaTeleporter tp = installTeleporter(target);
        if (tp != null) {
            tp.setTarget(0, 60, 0);
        }
        player.rotationYaw = 0.0F;
        player.rotationPitch = 30.0F;
        player.changeDimension(ARENA_DIM_ID);
        player.connection.setPlayerLocation(0, 60, 0, 0.0F, 30.0F);
        player.setGameType(GameType.SPECTATOR);
        player.sendMessage(new TextComponentString("§e[斗蛐蛐] 已进入斗蛐蛐场地，观战中"));

        // 若战斗未开始，开始倒数
        if (!battleActive && countdownTicks < 0) {
            countdownTicks = COUNTDOWN_TOTAL;
            sendTitleToAll("§63", "");
        }
    }

    /** 传送所有在场玩家回原位置，恢复游戏模式 */
    public static void returnAllPlayers() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;

        for (EntityPlayerMP player : new ArrayList<>(server.getPlayerList().getPlayers())) {
            if (player.dimension == ARENA_DIM_ID) {
                returnPlayer(player);
            }
        }
    }

    private static void returnPlayer(EntityPlayerMP player) {
        NBTTagCompound data = player.getEntityData();
        int origDim = data.getInteger(KEY_ORIG_DIM);
        double x = data.getDouble(KEY_ORIG_X);
        double y = data.getDouble(KEY_ORIG_Y);
        double z = data.getDouble(KEY_ORIG_Z);
        int gt = data.getInteger(KEY_ORIG_GT);

        // 默认回主世界出生点（备份数据无效时）
        if (origDim == ARENA_DIM_ID || (origDim == 0 && x == 0 && z == 0)) {
            origDim = 0;
            BlockPos bed = player.getBedLocation(0);
            BlockPos spawn = player.world.getSpawnPoint();
            if (bed != null) {
                x = bed.getX() + 0.5; y = bed.getY(); z = bed.getZ() + 0.5;
            } else {
                x = spawn.getX() + 0.5; y = spawn.getY(); z = spawn.getZ() + 0.5;
            }
        }

        // 回程：临时接管目标世界传送器定位到原坐标 → changeDimension → 立即还原（不破坏地狱门/末地门）
        WorldServer target = player.getServer().getWorld(origDim);
        if (target != null) {
            net.minecraft.world.Teleporter original = target.getDefaultTeleporter();
            ArenaTeleporter tp = installTeleporter(target);
            if (tp != null) {
                tp.setTarget(x, y, z);
            }
            player.changeDimension(origDim);
            restoreTeleporter(target, original);
        } else {
            player.changeDimension(origDim);
        }
        player.connection.setPlayerLocation(x, y, z, player.rotationYaw, player.rotationPitch);
        player.setGameType(GameType.getByID(gt));
        player.sendMessage(new TextComponentString("§a[斗蛐蛐] 已传送回原位置"));
    }

    /**
     * 反射替换 WorldServer.worldTeleporter 字段，用 ArenaTeleporter 接管默认传送器。
     * 只对需要精确落点的世界（arena 维度 / 回程时的目标世界）临时调用。
     */
    private static ArenaTeleporter installTeleporter(WorldServer world) {
        if (world == null) return null;
        if (world.getDefaultTeleporter() instanceof ArenaTeleporter) {
            return (ArenaTeleporter) world.getDefaultTeleporter();
        }
        try {
            java.lang.reflect.Field f = null;
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
            log.error("[DOTH] 替换世界传送器失败", e);
            return null;
        }
    }

    /** 还原世界传送器为原版实例 */
    private static void restoreTeleporter(WorldServer world, net.minecraft.world.Teleporter original) {
        if (world == null || original == null) return;
        try {
            java.lang.reflect.Field f = null;
            try {
                f = WorldServer.class.getDeclaredField("worldTeleporter");
            } catch (NoSuchFieldException e) {
                f = WorldServer.class.getDeclaredField("field_85177_Q");
            }
            f.setAccessible(true);
            java.lang.reflect.Field mod = java.lang.reflect.Field.class.getDeclaredField("modifiers");
            mod.setAccessible(true);
            mod.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
            f.set(world, original);
        } catch (Exception e) {
            log.error("[DOTH] 还原世界传送器失败", e);
        }
    }

    // ===== 每 tick 逻辑（ServerTickEvent 调用） =====

    public static void onServerTick() {
        if (!dimensionRegistered || !arenaBuilt) return;
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;

        // 空闲守卫：无战斗/无倒数/无烟花/无玩家在维度 → 完全不处理，避免无谓加载
        boolean anyPlayerInArena = false;
        for (EntityPlayerMP p : server.getPlayerList().getPlayers()) {
            if (p.dimension == ARENA_DIM_ID) { anyPlayerInArena = true; break; }
        }
        if (!anyPlayerInArena && countdownTicks <= 0 && !battleActive && fireworkTicks <= 0) {
            return;
        }

        // 兜底：战斗/烟花已结束但玩家还困在 arena → 强制回程（防平局/异常漏传）
        if (!battleActive && countdownTicks <= 0 && fireworkTicks <= 0) {
            boolean stuck = false;
            for (EntityPlayerMP p : server.getPlayerList().getPlayers()) {
                if (p.dimension == ARENA_DIM_ID) { stuck = true; break; }
            }
            if (stuck) {
                clearBossBars();
                returnAllPlayers();
                return;
            }
        }

        WorldServer arenaWorld = server.getWorld(ARENA_DIM_ID);
        if (arenaWorld == null) return;

        // 固定为白日正午
        arenaWorld.setWorldTime(6000);

        // 虚空保护：把掉出世界的玩家拉回
        for (EntityPlayerMP p : new ArrayList<>(server.getPlayerList().getPlayers())) {
            if (p.dimension == ARENA_DIM_ID && p.posY < -50) {
                p.connection.setPlayerLocation(p.posX, 55, p.posZ, p.rotationYaw, p.rotationPitch);
            }
        }

        // Boss 血条同步：观战玩家进出 + 双方总血量实时更新
        syncBossBars(server, arenaWorld);

        // 倒数阶段
        if (countdownTicks > 0) {
            countdownTicks--;
            // 每秒更新标题
            if (countdownTicks % 20 == 0) {
                int sec = (countdownTicks + 19) / 20;
                if (sec >= 1 && sec <= 3) {
                    sendTitleToAll("§l§6" + sec, "");
                }
            }
            if (countdownTicks == 0) {
                startBattle(arenaWorld);
            }
            return;
        }

        // 战斗阶段
        if (battleActive) {
            battleTicks++;
            applyHateLock(arenaWorld);

            // 统计存活
            int leftAlive = countAlive(arenaWorld, TAG_LEFT);
            int rightAlive = countAlive(arenaWorld, TAG_RIGHT);

            // 胜负判定
            if (winnerSide == 0 && (leftAlive == 0 || rightAlive == 0)) {
                if (leftAlive == 0 && rightAlive == 0) {
                    winnerSide = 0; // 同归于尽
                } else if (leftAlive == 0) {
                    winnerSide = 2;
                } else {
                    winnerSide = 1;
                }
                fireworkTicks = FIREWORK_TOTAL;
                String winTitle;
                if (winnerSide == 1) {
                    winTitle = teamDisplayName(TAG_LEFT) + "胜利！";
                } else if (winnerSide == 2) {
                    winTitle = teamDisplayName(TAG_RIGHT) + "胜利！";
                } else {
                    winTitle = "平局！";
                }
                sendTitleToAll("§l§a" + winTitle, "");
            }

            // 超时判定：依据双方剩余总血量判定胜负（不再直接平局）
            if (battleTicks > BATTLE_TIMEOUT && fireworkTicks <= 0) {
                float[] frac = getTeamHpFraction(arenaWorld);
                if (frac[0] > frac[1]) {
                    winnerSide = 1;
                    sendTitleToAll("§l§e时间到！" + teamDisplayName(TAG_LEFT) + "剩余血量更高，获胜", "");
                } else if (frac[1] > frac[0]) {
                    winnerSide = 2;
                    sendTitleToAll("§l§e时间到！" + teamDisplayName(TAG_RIGHT) + "剩余血量更高，获胜", "");
                } else {
                    winnerSide = 0;
                    sendTitleToAll("§l§e时间到！双方剩余血量相同，平局", "");
                }
                fireworkTicks = FIREWORK_TOTAL;
                return;
            }

            // 烟花阶段
            if (fireworkTicks > 0) {
                fireworkTicks--;
                if (fireworkTicks % 5 == 0) {
                    spawnFirework(arenaWorld, winnerSide);
                }
                if (fireworkTicks == 0) {
                    finishBattle();
                }
            }
        }
    }

    /** 倒数结束 → 连通空地 + 解除 NoAI + 开始战斗 */
    private static void startBattle(WorldServer world) {
        // 填平中间 7 格，连通两地块
        fillBox(world, GAP_X1, Y1, z1, GAP_X2, Y2, z2, Blocks.STONE);

        // 解除所有选手的 NoAI
        for (Entity e : world.loadedEntityList) {
            if (e.getTags().contains(TAG_LEFT) || e.getTags().contains(TAG_RIGHT)) {
                if (e instanceof EntityLiving) {
                    ((EntityLiving) e).setNoAI(false);
                }
            }
        }

        battleActive = true;
        battleTicks = 0;
        fireworkTicks = 0;
        winnerSide = 0;
        sendTitleToAll("§l§c战斗开始！",
                "§9" + teamDisplayName(TAG_LEFT) + " §7VS §c" + teamDisplayName(TAG_RIGHT));
    }

    /** 强制仇恨：若生物当前仇恨已是对面存活生物则不干预，否则锁定对面最近目标 */
    private static void applyHateLock(WorldServer world) {
        List<EntityLiving> leftTeam = getTeam(world, TAG_LEFT);
        List<EntityLiving> rightTeam = getTeam(world, TAG_RIGHT);

        for (EntityLiving e : leftTeam) {
            // 仇恨已是对面存活生物 → 不重复设置
            if (hasEnemyTarget(e, TAG_RIGHT)) continue;
            EntityLivingBase target = findNearest(e, rightTeam);
            if (target != null) {
                e.setAttackTarget(target);
            }
        }
        for (EntityLiving e : rightTeam) {
            if (hasEnemyTarget(e, TAG_LEFT)) continue;
            EntityLivingBase target = findNearest(e, leftTeam);
            if (target != null) {
                e.setAttackTarget(target);
            }
        }
    }

    /** 判断生物当前仇恨是否已是对面队伍的存活生物 */
    private static boolean hasEnemyTarget(EntityLiving e, String enemyTeamTag) {
        EntityLivingBase target = e.getAttackTarget();
        return target != null && !target.isDead
                && target.getTags().contains(enemyTeamTag);
    }

    private static List<EntityLiving> getTeam(World world, String tag) {
        List<EntityLiving> list = new ArrayList<>();
        for (Entity e : world.loadedEntityList) {
            if (e instanceof EntityLiving && e.getTags().contains(tag) && !e.isDead) {
                list.add((EntityLiving) e);
            }
        }
        return list;
    }

    private static EntityLivingBase findNearest(Entity from, List<EntityLiving> team) {
        EntityLivingBase nearest = null;
        double best = Double.MAX_VALUE;
        for (EntityLiving e : team) {
            if (e.isDead) continue;
            double d = from.getDistanceSq(e);
            if (d < best) {
                best = d;
                nearest = e;
            }
        }
        return nearest;
    }

    private static int countAlive(World world, String tag) {
        int c = 0;
        for (Entity e : world.loadedEntityList) {
            if (e instanceof EntityLivingBase && e.getTags().contains(tag)
                    && !e.isDead && ((EntityLivingBase) e).getHealth() > 0) {
                c++;
            }
        }
        return c;
    }

    // ===== Boss 血条（双方总血量） =====

    /** 初始化/重建双方 Boss 血条（名字变化时重建，否则只重置血量） */
    private static void initBossBars() {
        String leftName = teamDisplayName(TAG_LEFT);
        String rightName = teamDisplayName(TAG_RIGHT);
        if (leftBossBar != null && rightBossBar != null
                && leftBarName.equals(leftName) && rightBarName.equals(rightName)) {
            leftBossBar.setPercent(1.0F);
            rightBossBar.setPercent(1.0F);
            return;
        }
        // 名字变化/首次创建 → 先清掉旧条再重建
        if (leftBossBar != null || rightBossBar != null) {
            clearBossBars();
        }
        leftBossBar = new BossInfoServer(new TextComponentString("§9" + leftName), BossInfo.Color.BLUE, BossInfo.Overlay.PROGRESS);
        rightBossBar = new BossInfoServer(new TextComponentString("§c" + rightName), BossInfo.Color.RED, BossInfo.Overlay.PROGRESS);
        leftBarName = leftName;
        rightBarName = rightName;
        leftBossBar.setPercent(1.0F);
        rightBossBar.setPercent(1.0F);
    }

    /** 读取选手方块自定义名（铁砧改名，存于 FighterNbt.TeamName），无则空串 */
    private static String getTeamName(NBTTagCompound nbt) {
        if (nbt == null || !nbt.hasKey("TeamName")) return "";
        return nbt.getString("TeamName").trim();
    }

    /** 队伍显示名：自定义名优先；未命名时统一按玩家视角（进场面向南方，左地块在屏幕右侧 → 显示"右方"） */
    private static String teamDisplayName(String tag) {
        String custom = tag.equals(TAG_LEFT) ? leftTeamName : rightTeamName;
        if (!custom.isEmpty()) return custom;
        return tag.equals(TAG_LEFT) ? "右方" : "左方";
    }

    /** 同步血条：观战玩家进出 + 每 tick 更新双方总血量比例 */
    private static void syncBossBars(MinecraftServer server, World world) {
        initBossBars();
        Set<EntityPlayerMP> inArena = new HashSet<>();
        for (EntityPlayerMP p : server.getPlayerList().getPlayers()) {
            if (p.dimension == ARENA_DIM_ID) {
                inArena.add(p);
            }
        }
        for (EntityPlayerMP p : inArena) {
            leftBossBar.addPlayer(p);
            rightBossBar.addPlayer(p);
        }
        for (EntityPlayerMP p : new ArrayList<>(leftBossBar.getPlayers())) {
            if (!inArena.contains(p)) leftBossBar.removePlayer(p);
        }
        for (EntityPlayerMP p : new ArrayList<>(rightBossBar.getPlayers())) {
            if (!inArena.contains(p)) rightBossBar.removePlayer(p);
        }
        float[] frac = getTeamHpFraction(world);
        leftBossBar.setPercent(frac[0]);
        rightBossBar.setPercent(frac[1]);
    }

    /** 清空血条（战斗结束/兜底回程时移除所有观战者） */
    private static void clearBossBars() {
        if (leftBossBar != null) {
            for (EntityPlayerMP p : new ArrayList<>(leftBossBar.getPlayers())) {
                leftBossBar.removePlayer(p);
            }
        }
        if (rightBossBar != null) {
            for (EntityPlayerMP p : new ArrayList<>(rightBossBar.getPlayers())) {
                rightBossBar.removePlayer(p);
            }
        }
    }

    /** 双方剩余总血量比例（0.0 ~ 1.0），只统计带队伍标签的选手本体 */
    private static float[] getTeamHpFraction(World world) {
        float leftHp = 0F, leftMax = 0F, rightHp = 0F, rightMax = 0F;
        for (Entity e : world.loadedEntityList) {
            if (!(e instanceof EntityLivingBase)) continue;
            EntityLivingBase lb = (EntityLivingBase) e;
            if (lb.isDead || lb.getHealth() <= 0F) continue;
            if (e.getTags().contains(TAG_LEFT)) {
                leftHp += lb.getHealth();
                leftMax += lb.getMaxHealth();
            } else if (e.getTags().contains(TAG_RIGHT)) {
                rightHp += lb.getHealth();
                rightMax += lb.getMaxHealth();
            }
        }
        float leftFrac = leftMax > 0F ? leftHp / leftMax : 0F;
        float rightFrac = rightMax > 0F ? rightHp / rightMax : 0F;
        return new float[]{leftFrac, rightFrac};
    }

    /** 在胜利方地板上空放烟花 */
    private static void spawnFirework(WorldServer world, int side) {
        int x1 = side == 1 ? leftX1 : rightX1;
        int x2 = side == 1 ? leftX2 : rightX2;
        double x = x1 + 1 + RNG.nextDouble() * (x2 - x1 - 2);
        double z = z1 + 1 + RNG.nextDouble() * (z2 - z1 - 2);

        NBTTagCompound explosion = new NBTTagCompound();
        explosion.setIntArray("Colors", new int[]{side == 1 ? 0x00AAFF : 0xFF5555});
        explosion.setByte("Type", (byte) RNG.nextInt(4));
        NBTTagList explosions = new NBTTagList();
        explosions.appendTag(explosion);
        NBTTagCompound fireworks = new NBTTagCompound();
        fireworks.setTag("Explosions", explosions);
        NBTTagCompound stackNbt = new NBTTagCompound();
        stackNbt.setTag("Fireworks", fireworks);

        ItemStack rocketStack = new ItemStack(Items.FIREWORKS);
        rocketStack.setTagCompound(stackNbt);
        EntityFireworkRocket rocket = new EntityFireworkRocket(world, x, Y2 + 2, z, rocketStack);
        world.spawnEntity(rocket);
    }

    private static void finishBattle() {
        battleActive = false;
        countdownTicks = -1;
        // 场地不再就绪 → 下次右键需重置重建
        arenaReady = false;
        clearBossBars();
        returnAllPlayers();
        log.info("[DOTH] 斗蛐蛐战斗结束");
    }

    // ===== Title 工具 =====

    public static void sendTitleToAll(String title, String subtitle) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;
        for (EntityPlayerMP p : server.getPlayerList().getPlayers()) {
            if (p.dimension == ARENA_DIM_ID) {
                sendTitle(p, title, subtitle);
            }
        }
    }

    public static void sendTitle(EntityPlayerMP player, String title, String subtitle) {
        player.connection.sendPacket(new SPacketTitle(SPacketTitle.Type.TITLE,
                new TextComponentString(title)));
        if (subtitle != null && !subtitle.isEmpty()) {
            player.connection.sendPacket(new SPacketTitle(SPacketTitle.Type.SUBTITLE,
                    new TextComponentString(subtitle)));
        }
    }

    // ===== 队伍判断（防友伤用） =====

    public static boolean isArenaFighter(Entity e) {
        return e != null && (e.getTags().contains(TAG_LEFT) || e.getTags().contains(TAG_RIGHT));
    }

    public static boolean sameTeam(Entity a, Entity b) {
        if (!isArenaFighter(a) || !isArenaFighter(b)) return false;
        return (a.getTags().contains(TAG_LEFT) && b.getTags().contains(TAG_LEFT))
                || (a.getTags().contains(TAG_RIGHT) && b.getTags().contains(TAG_RIGHT));
    }
}
