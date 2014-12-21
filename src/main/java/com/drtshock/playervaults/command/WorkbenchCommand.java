package com.drtshock.playervaults.command;

import com.drtshock.playervaults.util.Lang;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WorkbenchCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender.hasPermission("playervaults.workbench")) {
            if (sender instanceof Player) {
                ((Player) sender).openWorkbench(null, true);
                Lang.OPEN_WORKBENCH.send(sender);
            } else {
                Lang.PLAYER_ONLY.send(sender);
            }
        } else {
            Lang.NO_PERMS.send(sender);
        }

        return true;
    }
}
