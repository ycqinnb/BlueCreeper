package yc.ycqin.doth.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import yc.ycqin.doth.core.AllreturnConfig;

public class CommandAllreturn extends CommandBase {

    @Override
    public String getName() {
        return "allreturn";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/allreturn <on|off|clear> - 启用/禁用/清除覆盖（仅特权玩家）";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayer)) {
            sender.sendMessage(new TextComponentString("§c该指令只能由玩家执行"));
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;
        String playerName = player.getName();

        if (!AllreturnConfig.isPrivileged(playerName)) {
            sender.sendMessage(new TextComponentString("§c你没有权限执行此指令"));
            return;
        }

        if (args.length != 1) {
            sender.sendMessage(new TextComponentString("§c用法: " + getUsage(sender)));
            return;
        }

        String subCmd = args[0].toLowerCase();

        switch (subCmd) {
            case "on":
                AllreturnConfig.setOverride(true);
                server.getPlayerList().sendMessage(
                        new TextComponentString("§e[Allreturn] " + playerName + " §a强制启用全局Allreturn")
                );
                break;

            case "off":
                AllreturnConfig.setOverride(false);
                server.getPlayerList().sendMessage(
                        new TextComponentString("§e[Allreturn] " + playerName + " §c强制禁用全局Allreturn")
                );
                break;

            case "clear":
                AllreturnConfig.clearOverride();
                server.getPlayerList().sendMessage(
                        new TextComponentString("§e[Allreturn] " + playerName + " §7已清除覆盖，回到自动模式")
                );
                break;

            default:
                sender.sendMessage(new TextComponentString("§c参数错误，请使用 on / off / clear"));
                return;
        }

        // 发送当前状态给执行者
        sender.sendMessage(new TextComponentString("§7当前状态: " + AllreturnConfig.getStatusDescription()));
    }
}