package com.drtshock.playervaults;

import com.drtshock.playervaults.util.EconomyUtil;
import com.drtshock.playervaults.util.Lang;
import com.drtshock.playervaults.util.SignSetInfo;
import com.drtshock.playervaults.util.VaultHolder;
import com.drtshock.playervaults.util.VaultUtil;
import com.drtshock.playervaults.util.VaultViewInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

public class VaultManager {

    private final File storageDir;
    private final File backupDir;
    private final AtomicBoolean lock = new AtomicBoolean(false);
    private final Map<String, VaultViewInfo> activeVaults = Maps.newHashMap();
    private final Map<String, Inventory> inventoryMap = Maps.newHashMap();
    private final Map<String, SignSetInfo> signs = Maps.newHashMap();
    private boolean backupsEnabled;
    private YamlConfiguration signConfig;
    private File signFile;
    private boolean signSaveQueued = false;

    public VaultManager(File storageDir) {
        this.storageDir = storageDir;
        this.backupDir = new File(this.storageDir, "backups");
    }

    public void loadConfig(PlayerVaults plugin) {
        this.backupsEnabled = plugin.getConfig().getBoolean("backups.enabled", false);

        File signFile = new File(plugin.getDataFolder(), "signs.yml");
        if (!signFile.exists()) {
            try {
                signFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("PlayerVaults has encountered a fatal error trying to load the signs file.");
                plugin.getLogger().severe("Please report this error at https://github.com/drtshock/PlayerVaults.");
                e.printStackTrace();
            }
        }

        this.signFile = signFile;
        this.signConfig = YamlConfiguration.loadConfiguration(this.signFile);
        plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (VaultManager.this.signSaveQueued) {
                    VaultManager.this.saveSigns();
                }
            }
        }, 20, 20);
    }

    public File getStorageDir() {
        return this.storageDir;
    }

    public File getBackupDir() {
        return this.backupDir;
    }

    public boolean isLocked() {
        return this.lock.get();
    }

    public void setLocked(boolean locked) {
        this.lock.set(locked);

        if (locked) {
            for (Player player : PlayerVaults.get().getOnlinePlayers()) {
                if (player.getOpenInventory() != null) {
                    InventoryView view = player.getOpenInventory();
                    if (view.getTopInventory().getHolder() instanceof VaultHolder) {
                        player.closeInventory();
                        Lang.LOCKED.send(player);
                    }
                }
            }
        }
    }

    public Map<String, VaultViewInfo> getActiveVaults() {
        return this.activeVaults;
    }

    public Map<String, Inventory> getInventoryMap() {
        return this.inventoryMap;
    }

    public Map<String, SignSetInfo> getSigns() {
        return this.signs;
    }

    public YamlConfiguration getSignConfig() {
        return this.signConfig;
    }

    public void queueSignsForSave() {
        this.signSaveQueued = true;
    }

    public boolean vaultExists(UUID uniqueId, int vaultId) {
        return this.getVaultFileFor(uniqueId).contains("vault" + vaultId);
    }

    public Inventory getVault(UUID uniqueId, int vaultId) {
        OfflinePlayer player = PlayerVaults.get().getServer().getOfflinePlayer(uniqueId);
        if (player == null) {
            return null;
        }

        List<String> data = this.getVaultFileFor(uniqueId).getStringList("vault" + vaultId);
        int maxVaultSize = VaultUtil.getMaxVaultSize(player);
        if (data == null) {
            return VaultHolder.wrapInventory(vaultId, Lang.VAULT_TITLE.format("%number", vaultId, "%p", player.getName()), maxVaultSize);
        } else {
            return SerializationUtil.toInventory(vaultId, maxVaultSize, data);
        }
    }

    private Inventory getVault(YamlConfiguration vaultFile, int vaultId, int size) {
        List<String> data = Lists.newArrayList();

        for (int i = 0; i < size; i++) {
            String line = vaultFile.getString("vault" + vaultId + "." + i);
            if (line != null) {
                data.add(line);
            } else {
                data.add("null");
            }
        }

        return SerializationUtil.toInventory(vaultId, size, data);
    }

    public Inventory loadVaultFor(UUID uniqueId, int vaultId, int size, @Nullable Player viewer) {
        if (size % 9 != 0) {
            size = 54;
        }

        String viewInfo = new VaultViewInfo(uniqueId.toString(), vaultId).toString();

        Inventory inventory;
        if (this.inventoryMap.containsKey(viewInfo)) {
            inventory = this.inventoryMap.get(viewInfo);
        } else {
            YamlConfiguration vaultFile = this.getVaultFileFor(uniqueId);
            if (vaultFile.getConfigurationSection("vault" + vaultId) == null) {
                if (viewer != null) {
                    if (EconomyUtil.payToCreate(viewer)) {
                        inventory = VaultHolder.wrapInventory(vaultId, Lang.VAULT_TITLE.format("%number", vaultId, "%p", viewer.getName()), size);
                    } else {
                        Lang.INSUFFICIENT_FUNDS.send(viewer);
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                inventory = this.getVault(vaultFile, vaultId, size);
                if (inventory == null) {
                    return null;
                }
            }

            this.inventoryMap.put(viewInfo, inventory);
        }

        return inventory;
    }

    public void saveVault(UUID uniqueId, Inventory inventory, int number) throws IOException {
        int size = inventory.getSize();

        YamlConfiguration data = this.getVaultFileFor(uniqueId);
        if (size == 54) {
            data.set("vault" + number, null);
        } else {
            for (int i = 0; i < size; i++) {
                data.set("vault" + number + "." + i, null);
            }
        }

        List<String> serialized = SerializationUtil.toString(inventory);
        String[] asArray = serialized.toArray(new String[serialized.size()]);
        for (int x = 0; x < asArray.length; x++) {
            if (!asArray[x].equalsIgnoreCase("null")) {
                data.set("vault" + number + "." + x, asArray[x]);
            }
        }

        this.saveFile(uniqueId, data);
    }

    public void saveFile(UUID uniqueId, YamlConfiguration vaultData) throws IOException {
        File vaultFile = new File(this.storageDir, uniqueId.toString() + ".yml");
        if (vaultFile.exists() && this.backupsEnabled) {
            vaultFile.renameTo(new File(this.backupDir, uniqueId.toString() + ".yml"));
        }

        vaultData.save(vaultFile);
    }

    public YamlConfiguration getVaultFileFor(UUID uniqueId) {
        File vaultFile = new File(this.storageDir, uniqueId.toString() + ".yml");
        if (!vaultFile.exists()) {
            try {
                vaultFile.createNewFile();
            } catch (IOException e) {
                // ignore
            }
        }

        return YamlConfiguration.loadConfiguration(vaultFile);
    }

    public void deleteVault(CommandSender actor, Player player, int vaultId) throws IOException {
        UUID uniqueId = player.getUniqueId();
        YamlConfiguration playerFile = this.getVaultFileFor(uniqueId);
        playerFile.set("vault" + vaultId, null);
        this.saveFile(uniqueId, playerFile);

        if (actor.getName().equalsIgnoreCase(player.getName())) {
            Lang.DELETE_VAULT.send(actor, "%v", vaultId);
        } else {
            Lang.DELETE_OTHER_VAULT.send(actor, "%v", vaultId, "%o", player.getName());
        }

        this.inventoryMap.remove(new VaultViewInfo(uniqueId.toString(), vaultId).toString());
    }

    public void saveSigns() {
        this.signSaveQueued = false;
        try {
            this.signConfig.save(this.signFile);
        } catch (IOException e) {
            PlayerVaults.get().getLogger().severe("PlayerVaults has encountered an error trying to save the signs file.");
            PlayerVaults.get().getLogger().severe("Please report this error at https://github.com/drtshock/PlayerVaults.");
            e.printStackTrace();
        }
    }
}
