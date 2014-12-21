package com.drtshock.playervaults.command;

import com.drtshock.playervaults.PlayerVaults;
import com.drtshock.playervaults.VaultManager;
import com.drtshock.playervaults.util.Lang;
import com.drtshock.playervaults.util.VaultUtil;
import com.drtshock.playervaults.util.VaultViewInfo;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class VaultCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            Lang.PLAYER_ONLY.send(sender);
            return true;
        }

        VaultManager manager = PlayerVaults.get().getManager();

        if (manager.isLocked()) {
            Lang.LOCKED.send(sender);
            return true;
        }

        Player player = (Player) sender;

        if (manager.getActiveVaults().containsKey(player.getName())) {
            Lang.ALREADY_VIEWING.send(player);
            return true;
        }

        int vaultId;
        switch (args.length) {
            // view own vault
            case 1:
                try {
                    vaultId = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    Lang.INVALID_ARGS.send(player);
                    return true;
                }

                if (VaultUtil.canOpenVault(player, null, vaultId)) {
                    player.openInventory(manager.loadVaultFor(player.getUniqueId(), vaultId, VaultUtil.getMaxVaultSize(player), player));
                    manager.getActiveVaults().put(player.getName(), new VaultViewInfo(player.getName(), vaultId));
                    Lang.OPEN_VAULT.send(player, "%v", vaultId);
                } else if (player.hasPermission("playervaults.admin")) {
                    OfflinePlayer target = PlayerVaults.get().getServer().getOfflinePlayer(args[0]);
                    if (target == null) {
                        Lang.NO_PLAYER_FOUND.send(player, "%p", args[0]);
                        return true;
                    }

                    YamlConfiguration vaultFile = manager.getVaultFileFor(target.getUniqueId());
                    if (vaultFile == null) {
                        Lang.VAULT_DOES_NOT_EXIST.send(player);
                    } else {
                        StringBuilder builder = new StringBuilder();
                        for (String key : vaultFile.getKeys(false)) {
                            builder.append(key.replace("vault", "")).append(" ");
                        }

                        Lang.EXISTING_VAULTS.send(player, "%p", args[0], "%v", builder.toString().trim());
                    }
                }
                break;
            // view another player's vault
            case 2:
                Player target = PlayerVaults.get().getServer().getPlayer(args[0]);
                if (target == null) {
                    Lang.NO_PLAYER_FOUND.send(player, "%p", args[0]);
                    return true;
                }

                try {
                    vaultId = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    Lang.INVALID_ARGS.send(player);
                    return true;
                }

                if (VaultUtil.canOpenVault(player, target, vaultId)) {
                    player.openInventory(manager.loadVaultFor(player.getUniqueId(), vaultId, VaultUtil.getMaxVaultSize(target), player));
                    manager.getActiveVaults().put(player.getName(), new VaultViewInfo(args[0], vaultId));
                    Lang.OPEN_VAULT.send(player, "%v", vaultId);
                } else {
                    sender.sendMessage(Lang.TITLE.toString() + "Failed to open vault.");
                }
                break;
            default:
                Lang.USAGE_PV_NUMBER.send(player, "%alias", alias);
                Lang.USAGE_PV_OTHER.send(player, "%alias", alias);
                break;
        }

        return true;
    }
}
