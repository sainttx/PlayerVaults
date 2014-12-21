package com.drtshock.playervaults.task;

import com.drtshock.playervaults.PlayerVaults;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.logging.Logger;

public class Cleanup extends BukkitRunnable {

    private long diff;

    public Cleanup(int diff) {
        this.diff = diff * 86400;
    }

    @Override
    public void run() {
        File directory = PlayerVaults.get().getManager().getStorageDir();
        if (!directory.exists()) {
            // folder doesn't exist, don't run
            return;
        }

        Logger logger = PlayerVaults.get().getLogger();
        long time = System.currentTimeMillis();

        for (File file : directory.listFiles()) {
            if (time - file.lastModified() > this.diff) {
                logger.info("Deleting vault file (cleanup): " + file.getName());
                file.delete();
            }
        }
    }
}
