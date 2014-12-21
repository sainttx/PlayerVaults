package com.drtshock.playervaults.command;

import com.drtshock.playervaults.PlayerVaults;
import com.drtshock.playervaults.VaultManager;
import com.drtshock.playervaults.util.Lang;
import com.drtshock.playervaults.util.VaultUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;

public class DeleteCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        VaultManager manager = PlayerVaults.get().getManager();

        if (manager.isLocked()) {
            Lang.LOCKED.send(sender);
            return true;
        }

        int vaultId;
        switch (args.length) {
            case 1:
                if (sender instanceof Player) {
                    try {
                        vaultId = Integer.parseInt(args[0]);
                    } catch (NumberFormatException e) {
                        Lang.INVALID_ARGS.send(sender);
                        return true;
                    }

                    Player player = (Player) sender;
                    if (VaultUtil.canDeleteVault(player, null, vaultId)) {
                        try {
                            manager.deleteVault(player, player, vaultId);
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                } else {
                    Lang.PLAYER_ONLY.send(sender);
                }
                break;
            case 2:
                Player player = Bukkit.getPlayer(args[0]);
                if (player == null) {
                    Lang.NO_PLAYER_FOUND.send(sender, "%p", args[0]);
                    return true;
                }

                try {
                    vaultId = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    Lang.INVALID_ARGS.send(sender);
                    return true;
                }

                if (VaultUtil.canDeleteVault(sender, player, vaultId)) {
                    try {
                        manager.deleteVault(sender, player, vaultId);
                    } catch (IOException e) {
                        // ignore
                    }
                }
                break;
            default:
                Lang.USAGE_PVDEL_NUMBER.send(sender, "%alias", alias);
                Lang.USAGE_PVDEL_OTHER.send(sender, "%alias", alias);
                break;
        }

        return true;
    }
}
