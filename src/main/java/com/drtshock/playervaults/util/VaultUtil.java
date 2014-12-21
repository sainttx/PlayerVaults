package com.drtshock.playervaults.util;

import com.drtshock.playervaults.PlayerVaults;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public class VaultUtil {

    private static boolean isLocked() {
        return PlayerVaults.get().getManager().isLocked();
    }

    public static boolean canOpenVault(CommandSender sender, @Nullable Player owner, int vaultId) {
        if (isLocked()) {
            return false;
        }

        if (vaultId < 1) {
            return false;
        }

        if (owner != null) {
            if (sender.hasPermission("playervaults.admin")) {
                return true;
            } else {
                Lang.NO_PERMS.send(sender);
                return false;
            }
        }

        if (checkPerms(sender, vaultId)) {
            if (EconomyUtil.payToOpen(sender, vaultId)) {
                return true;
            } else {
                Lang.INSUFFICIENT_FUNDS.send(sender);
            }
        } else {
            Lang.NO_PERMS.send(sender);
        }

        return false;
    }

    public static boolean canDeleteVault(CommandSender sender, @Nullable Player owner, int vaultId) {
        if (isLocked()) {
            return false;
        }

        if (vaultId < 1) {
            Lang.MUST_BE_NUMBER.send(sender);
            return false;
        }

        if (owner != null) {
            if (sender.hasPermission("playervaults.delete")) {
                return true;
            } else {
                Lang.NO_PERMS.send(sender);
                return false;
            }
        }

        return true;
    }

    public static boolean checkPerms(CommandSender sender, int vaultId) {
        if (sender.hasPermission("playervaults.amount." + String.valueOf(vaultId))) {
            return true;
        }

        for (int i = vaultId; i <= 99; i++) {
            if (sender.hasPermission("playervaults.amount." + String.valueOf(i))) {
                return true;
            }
        }

        return false;
    }

    public static int getMaxVaultSize(OfflinePlayer player) {
        if (player == null || !player.isOnline()) {
            return 54;
        }

        for (int i = 6; i != 0; i--) {
            if (player.getPlayer().hasPermission("playervaults.size." + i)) {
                return i * 9;
            }
        }

        return 54;
    }
}
