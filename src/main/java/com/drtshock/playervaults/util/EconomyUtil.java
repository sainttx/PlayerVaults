package com.drtshock.playervaults.util;

import com.drtshock.playervaults.PlayerVaults;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public class EconomyUtil {

    public static boolean payToCreate(Player player) {
        Economy economy = getEconomy();
        if (economy == null || player.hasPermission("playervaults.free")) {
            return true;
        }

        double cost = PlayerVaults.get().getConfig().getDouble("economy.cost-to-create", 100);
        EconomyResponse response = economy.withdrawPlayer(player.getName(), cost);
        if (response.transactionSuccess()) {
            Lang.COST_TO_CREATE.send(player, "%price", cost);
            return true;
        }

        return false;
    }

    public static boolean payToOpen(CommandSender sender, int vaultId) {
        Economy economy = getEconomy();
        if (economy == null  || !(sender instanceof Player) || sender.hasPermission("playervaults.free")) {
            return true;
        }

        Player player = (Player) sender;
        if (PlayerVaults.get().getManager().vaultExists(player.getUniqueId(), vaultId)) {
            return payToCreate(player);
        } else {
            double cost = PlayerVaults.get().getConfig().getDouble("economy.cost-to-open", 10);
            EconomyResponse response = economy.withdrawPlayer(player.getName(), cost);
            if (response.transactionSuccess()) {
                Lang.COST_TO_OPEN.send(player, "%price", cost);
                return true;
            }
        }

        return false;
    }

    public static boolean refundOnDelete(Player player, int vaultId) {
        Economy economy = getEconomy();
        if (economy == null || player.hasPermission("playervaults.free")) {
            return true;
        }

        YamlConfiguration vaultData = PlayerVaults.get().getManager().getVaultFileFor(player.getUniqueId());
        if (vaultData != null) {
            if (vaultData.getString("vault" + vaultId) == null) {
                Lang.VAULT_DOES_NOT_EXIST.send(player);
                return false;
            }
        } else {
            Lang.VAULT_DOES_NOT_EXIST.send(player);
            return false;
        }

        double cost = PlayerVaults.get().getConfig().getDouble("economy.refund-on-delete");
        EconomyResponse response = economy.depositPlayer(player.getName(), cost);
        if (response.transactionSuccess()) {
            player.sendMessage(Lang.TITLE.toString() + Lang.REFUND_AMOUNT.toString().replaceAll("%price", String.valueOf(cost)));
            return true;
        }

        return false;
    }

    @Nullable
    private static Economy getEconomy() {
        return PlayerVaults.get().getEconomy();
    }
}
