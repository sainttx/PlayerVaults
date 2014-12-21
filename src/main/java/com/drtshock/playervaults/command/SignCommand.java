package com.drtshock.playervaults.command;

import com.drtshock.playervaults.PlayerVaults;
import com.drtshock.playervaults.util.Lang;
import com.drtshock.playervaults.util.SignSetInfo;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SignCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (sender.hasPermission("playervaults.signs.set")) {
            int vaultId;

            if (sender instanceof Player) {
                if (args.length == 1) {
                    try {
                        vaultId = Integer.parseInt(args[0]);
                    } catch (NumberFormatException e) {
                        Lang.MUST_BE_NUMBER.send(sender);
                        Lang.USAGE_SIGN.send(sender, "%alias", alias);
                        return true;
                    }

                    PlayerVaults.get().getManager().getSigns().put(sender.getName(), new SignSetInfo(vaultId));
                    Lang.CLICK_A_SIGN.send(sender);
                } else if (args.length >= 2) {
                    try {
                        vaultId = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        Lang.MUST_BE_NUMBER.send(sender);
                        Lang.USAGE_SIGN.send(sender, "%alias", alias);
                        return true;
                    }

                    PlayerVaults.get().getManager().getSigns().put(sender.getName(), new SignSetInfo(args[0].toLowerCase(), vaultId));
                    Lang.CLICK_A_SIGN.send(sender);
                } else {
                    Lang.INVALID_ARGS.send(sender);
                }
            } else {
                Lang.PLAYER_ONLY.send(sender);
            }
        } else {
            Lang.NO_PERMS.send(sender);
        }

        return true;
    }
}
