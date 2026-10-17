package moe.meowrealms.ysmsync.web

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/** One-time proof that the web account owner controls this in-game account. */
class WebPortalCommand : Command("ysmsyncweb", "Bind a game account to the YSM web portal", "/ysmsyncweb bind <code>", listOf()) {
    override fun execute(sender: CommandSender, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) { sender.sendMessage("该命令只能由玩家在游戏内执行。"); return true }
        if (args.size != 2 || !args[0].equals("bind", true)) { sender.sendMessage("用法：/ysmsyncweb bind <网页显示的绑定码>"); return true }
        sender.sendMessage(WebPortalManager.bindPlayer(sender, args[1]))
        return true
    }
}
