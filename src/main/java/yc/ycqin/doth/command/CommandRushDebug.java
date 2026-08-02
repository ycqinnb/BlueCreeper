package yc.ycqin.doth.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import yc.ycqin.doth.core.AllreturnConfig;
import yc.ycqin.doth.world.RushManager;

/**
 * 虫灵快跑测试指令：/rushdebug invincible on|off
 */
public class CommandRushDebug extends CommandBase {

    @Override
    public String getName() {
        return "rushdebug";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/rushdebug invincible <on|off> - 开启/关闭虫灵无敌模式（仅特权玩家）";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayer)) {
            sender.sendMessage(new TextComponentString("§c该指令只能由玩家执行"));
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;
        if (!AllreturnConfig.isPrivileged(player.getName())) {
            sender.sendMessage(new TextComponentString("§c你没有权限执行此指令"));
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
                        "§e[虫灵快跑] " + player.getName() + " §a开启了虫灵无敌模式（测试用）"));
                break;
            case "off":
                RushManager.buglinInvincible = false;
                server.getPlayerList().sendMessage(new TextComponentString(
                        "§e[虫灵快跑] " + player.getName() + " §c关闭了虫灵无敌模式"));
                break;
            default:
                sender.sendMessage(new TextComponentString("§c参数错误，请使用 on / off"));
                return;
        }
        sender.sendMessage(new TextComponentString("§7当前虫灵无敌: " + (RushManager.buglinInvincible ? "§a开" : "§c关")));
    }
}
