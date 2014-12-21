package com.drtshock.playervaults.task;

import com.drtshock.playervaults.PlayerVaults;
import com.drtshock.playervaults.VaultManager;
import com.google.common.io.Files;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Class to convert vaults by name to vaults by UUID.
 */
public final class UUIDConversion extends BukkitRunnable {

    @Override
    public void run() {
        PlayerVaults plugin = PlayerVaults.get();
        Logger logger = plugin.getLogger();
        VaultManager manager = plugin.getManager();

        if (!plugin.getConfig().getBoolean("conversion.do-uuid-conversion", false) && manager.getStorageDir().exists()) {
            logger.info("** Vaults have already been converted to UUIDs. See config.yml for more information.");
            return;
        }

        File oldVaults = new File(plugin.getDataFolder() + File.separator + "vaults");
        if (oldVaults.exists()) {
            logger.info("********** Starting conversion to UUIDs **********");
            logger.info("This might take awhile.");
            logger.info(oldVaults.toString() + " will remain as a backup.");

            for (File file : oldVaults.listFiles()) {
                if (file.isDirectory()) {
                    continue; // backups folder.
                }
                OfflinePlayer player = Bukkit.getOfflinePlayer(file.getName().replace(".yml", ""));
                if (player == null) {
                    logger.warning("Unable to convert file because player never joined the server: " + file.getName());
                    break;
                }

                File newFile = new File(manager.getStorageDir(), player.getUniqueId().toString() + ".yml");
                file.mkdirs();
                try {
                    Files.copy(file, newFile);
                    logger.info("Successfully converted vault file for " + player.getName());
                } catch (IOException e) {
                    logger.severe("Couldn't convert vault file for " + player.getName());
                }
            }

            logger.info("********** Conversion done ;D **********");
        }
    }
}
