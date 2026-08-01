package yc.ycqin.doth.world;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.play.server.SPacketTitle;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.DimensionType;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 斗蛐蛐维度核心逻辑
 */
public class ArenaManager {

    private static final Logger log = LogManager.getLogger("DOTH-Arena");

    // ===== 维度注册 =====
    public static int ARENA_DIM_ID = -1;
    public static DimensionType DIMENSION_TYPE = null;
    private static boolean dimensionRegistered = false;

    // ===== 场地常量 =====
    /** 左地块 x 范围 */
    public static final int LEFT_X1 = -18, LEFT_X2 = -4;
    /** 右地块 x 范围 */
    public static final int RIGHT_X1 = 4, RIGHT_X2 = 18;
    /** 两地块 z 范围（15 格） */
    public static final int Z1 = 2, Z2 = 16;
    /** 地块厚度 5 格 */
    public static final int Y1 = 50, Y2 = 54;
    /** 中间 7 格虚空（连通区） */
    public static final int GAP_X1 = -3, GAP_X2 = 3;

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
    private static final int BATTLE_TIMEOUT = 20 * 60 * 20;   // 20分钟
    private static final int FIREWORK_TOTAL = 10 * 20;        // 10秒烟花

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
     * 构建场地：清空 → 建左右两块地 → 生成双方选手（NoAI）
     * @param leftNbt  左选手方块 NBT
     * @param rightNbt 右选手方块 NBT
     */
    public static boolean buildArena(NBTTagCompound leftNbt, NBTTagCompound rightNbt) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || !dimensionRegistered) return false;

        WorldServer arenaWorld = server.getWorld(ARENA_DIM_ID);
        if (arenaWorld == null) return false;

        // 清空旧场地（含生物）
        clearArena(arenaWorld);

        // 建左地块
        fillBox(arenaWorld, LEFT_X1, Y1, Z1, LEFT_X2, Y2, Z2, Blocks.STONE);
        // 建右地块
        fillBox(arenaWorld, RIGHT_X1, Y1, Z1, RIGHT_X2, Y2, Z2, Blocks.STONE);

        // 生成选手
        spawnFighterTeam(arenaWorld, leftNbt, LEFT_X1, LEFT_X2, TAG_LEFT);
        spawnFighterTeam(arenaWorld, rightNbt, RIGHT_X1, RIGHT_X2, TAG_RIGHT);

        arenaBuilt = true;
        arenaReady = true;
        battleActive = false;
        countdownTicks = -1;
        battleTicks = 0;
        fireworkTicks = 0;
        winnerSide = 0;
        log.info("[DOTH] 斗蛐蛐场地构建完成");
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
            double z = Z1 + 1 + RNG.nextDouble() * (Z2 - Z1 - 2);
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
        clearBox(world, LEFT_X1 - 3, Y1 - 3, Z1 - 3, RIGHT_X2 + 3, Y2 + 6, Z2 + 3);

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
                sendTitleToAll("§l§a" + (winnerSide == 1 ? "左方胜利！" : winnerSide == 2 ? "右方胜利！" : "平局！"), "");
            }

            // 超时判定
            if (battleTicks > BATTLE_TIMEOUT && fireworkTicks <= 0) {
                sendTitleToAll("§l§e时间到，平局", "");
                finishBattle();
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
        fillBox(world, GAP_X1, Y1, Z1, GAP_X2, Y2, Z2, Blocks.STONE);

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
        sendTitleToAll("§l§c战斗开始！", "");
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

    /** 在胜利方地板上空放烟花 */
    private static void spawnFirework(WorldServer world, int side) {
        int x1 = side == 1 ? LEFT_X1 : RIGHT_X1;
        int x2 = side == 1 ? LEFT_X2 : RIGHT_X2;
        double x = x1 + 1 + RNG.nextDouble() * (x2 - x1 - 2);
        double z = Z1 + 1 + RNG.nextDouble() * (Z2 - Z1 - 2);

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
