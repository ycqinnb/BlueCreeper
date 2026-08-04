package yc.ycqin.doth.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import yc.ycqin.doth.world.RushManager;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 虫灵快跑无敌指令：/rushdebug invincible on|off（所有玩家可用）
 */
public class CommandRushDebug extends CommandBase {

    @Override
    public String getName() {
        return "rushdebug";
    }

    /** 所有玩家可用（无需 OP） */
    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/rushdebug invincible on - 开启无敌模式";
    }

    /** Tab 补全：第一参数 invincible/mob，第二参数 on/off */
    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "invincible", "mob");
        }
        if (args.length == 2 && "invincible".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "on", "off");
        }
        return super.getTabCompletions(server, sender, args, targetPos);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayer)) {
            sender.sendMessage(new TextComponentString("§c该指令只能由玩家执行"));
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;

        // 测试：/rushdebug mob <生物注册名> —— 直接骑指定生物进赛道
        if (args.length == 2 && "mob".equalsIgnoreCase(args[0])) {
            if (!(player instanceof EntityPlayerMP)) {
                sender.sendMessage(new TextComponentString("§c只能由玩家执行"));
                return;
            }
            if (RushManager.enterRush((EntityPlayerMP) player, args[1])) {
                // 测试指令不消耗入场券
            }
            return;
        }

        if (args.length != 2 || !"invincible".equalsIgnoreCase(args[0])) {
            sender.sendMessage(new TextComponentString("§c用法: " + getUsage(sender)));
            return;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "on":
                RushManager.buglinInvincible = true;
                server.getPlayerList().sendMessage(new TextComponentString(
                        "§e[虫灵快跑] " + player.getName() + " §a开启了无敌模式"));
                break;
            case "off":
                RushManager.buglinInvincible = false;
                server.getPlayerList().sendMessage(new TextComponentString(
                        "§e[虫灵快跑] " + player.getName() + " §c关闭了无敌模式"));
                break;
            default:
                sender.sendMessage(new TextComponentString("§c参数错误，请使用 on / off"));
                return;
        }
        sender.sendMessage(new TextComponentString("§7当前虫灵无敌: " + (RushManager.buglinInvincible ? "§a开" : "§c关")));
    }
}
