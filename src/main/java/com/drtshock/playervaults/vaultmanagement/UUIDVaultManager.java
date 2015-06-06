package com.drtshock.playervaults.vaultmanagement;

import com.drtshock.playervaults.PlayerVaults;
import com.drtshock.playervaults.util.Lang;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Class to handle vault operations with new UUIDs.
 */
public class UUIDVaultManager {

    private static UUIDVaultManager instance;
    private static CachedVaults cachedVaults;

    public UUIDVaultManager() {
        instance = this;
        cachedVaults = new CachedVaults();
    }

    private final File directory = PlayerVaults.getInstance().getVaultData();

    public CachedVaults getCachedVaults() {
        return cachedVaults;
    }

    /**
     * Saves the inventory to the specified player and vault number.
     *
     * @param inventory The inventory to be saved.
     * @param player    The player of whose file to save to.
     * @param number    The vault number.
     *
     * @throws java.io.IOException Uh oh!
     */
    public void saveVault(Inventory inventory, UUID player, int number) throws IOException {
        YamlConfiguration yaml = getPlayerVaultFile(player);

        String base64 = Serialization.toBase64(inventory.getContents());
        yaml.set("vault" + number, base64);

        saveFile(player, yaml);
    }

    /**
     * Load the player's vault and return it.
     *
     * @param player The holder of the vault.
     * @param number The vault number.
     */
    public Inventory loadOwnVault(Player player, int number, int size) {
        if (cachedVaults.hasVaultCached(player.getUniqueId(), number)) {
            return cachedVaults.getCachedVault(player.getUniqueId(), number);
        }

        if (size % 9 != 0) {
            size = 54;
        }

        String title = Lang.VAULT_TITLE.toString().replace("%number", String.valueOf(number)).replace("%p", player.getName());
        VaultViewInfo info = new VaultViewInfo(player.getUniqueId(), number);
        Inventory inv;
        if (PlayerVaults.getInstance().getOpenInventories().containsKey(info.toString())) {
            inv = PlayerVaults.getInstance().getOpenInventories().get(info.toString());
        } else {
            YamlConfiguration playerFile = getPlayerVaultFile(player.getUniqueId());
            if (!playerFile.isString("vault" + number)) {
                VaultHolder vaultHolder = new VaultHolder(number);
                if (EconomyOperations.payToCreate(player)) {
                    inv = Bukkit.createInventory(vaultHolder, size, title);
                    vaultHolder.setInventory(inv);
                } else {
                    player.sendMessage(Lang.TITLE.toString() + Lang.INSUFFICIENT_FUNDS.toString());
                    return null;
                }
            } else {
                Inventory i = getInventory(playerFile, size, number, title);
                if (i == null) {
                    return null;
                } else {
                    inv = i;
                }
            }
            PlayerVaults.getInstance().getOpenInventories().put(info.toString(), inv);
        }

        cachedVaults.setCachedVault(player.getUniqueId(), number, inv);
        return inv;
    }

    /**
     * Load the player's vault and return it.
     *
     * @param holder The holder of the vault.
     * @param number The vault number.
     */
    public Inventory loadOtherVault(UUID holder, int number, int size) {
        if (cachedVaults.hasVaultCached(holder, number)) {
            return cachedVaults.getCachedVault(holder, number);
        }

        if (size % 9 != 0) {
            size = 54;
        }
        String title = Lang.VAULT_TITLE.toString().replace("%number", String.valueOf(number)).replace("%p", Bukkit.getOfflinePlayer(holder).getName());
        VaultViewInfo info = new VaultViewInfo(holder, number);
        Inventory inv;
        if (PlayerVaults.getInstance().getOpenInventories().containsKey(info.toString())) {
            inv = PlayerVaults.getInstance().getOpenInventories().get(info.toString());
        } else {
            YamlConfiguration playerFile = getPlayerVaultFile(holder);
            if (!playerFile.isString("vault" + number)) {
                return null;
            } else {
                Inventory i = getInventory(playerFile, size, number, title);
                if (i == null) {
                    return null;
                } else {
                    inv = i;
                }
            }
            PlayerVaults.getInstance().getOpenInventories().put(info.toString(), inv);
        }

        cachedVaults.setCachedVault(holder, number, inv);
        return inv;
    }

    /**
     * Get an inventory from file. Returns null if the inventory doesn't exist. SHOULD ONLY BE USED INTERNALLY
     *
     * @param playerFile the YamlConfiguration file.
     * @param size       the size of the vault.
     * @param number     the vault number.
     *
     * @return inventory if exists, otherwise null.
     */
    private Inventory getInventory(YamlConfiguration playerFile, int size, int number, String title) {
        String vault = playerFile.getString("vault" + number);
        return Serialization.toInventory(vault, number, size, title);
    }

    /**
     * Gets an inventory without storing references to it. Used for dropping a players inventories on death.
     *
     * @param holder The holder of the vault.
     * @param number The vault number.
     *
     * @return The inventory of the specified holder and vault number.
     */
    public Inventory getVault(UUID holder, int number) {
        YamlConfiguration playerFile = getPlayerVaultFile(holder);
        List<String> data = playerFile.getStringList("vault" + number);
        OfflinePlayer player = Bukkit.getOfflinePlayer(holder);
        if (player == null || !player.hasPlayedBefore()) {
            return null;
        }
        String title = Lang.VAULT_TITLE.toString().replace("%number", String.valueOf(number)).replace("%p", Bukkit.getOfflinePlayer(holder).getName());
        if (data == null) {
            VaultHolder vaultHolder = new VaultHolder(number);
            Inventory inv = Bukkit.createInventory(vaultHolder, VaultOperations.getMaxVaultSize(player), Lang.VAULT_TITLE.toString().replace("%number", String.valueOf(number)).replace("%p", player.getName()));
            vaultHolder.setInventory(inv);
            return inv;
        } else {
            return Serialization.toInventory(data, number, VaultOperations.getMaxVaultSize(player), title);
        }
    }

    public boolean vaultExists(UUID holder, int number) {
        return getPlayerVaultFile(holder).contains("vault" + number);
    }

    /**
     * Deletes a players vault.
     *
     * @param sender The sender of whom to send messages to.
     * @param holder The vault holder.
     * @param number The vault number.
     *
     * @throws IOException Uh oh!
     */
    public void deleteVault(CommandSender sender, UUID holder, int number) throws IOException {
        File file = new File(directory, holder.toString() + ".yml");
        if (!file.exists()) {
            return;
        }

        cachedVaults.clearVaultCache(holder);

        FileConfiguration playerFile = YamlConfiguration.loadConfiguration(file);
        if (file.exists()) {
            playerFile.set("vault" + number, null);
            playerFile.save(file);
        }

        OfflinePlayer player = Bukkit.getPlayer(holder);
        if (player != null && sender.getName().equalsIgnoreCase(player.getName())) {
            sender.sendMessage(Lang.TITLE.toString() + Lang.DELETE_VAULT.toString().replace("%v", String.valueOf(number)));
        } else {
            sender.sendMessage(Lang.TITLE.toString() + Lang.DELETE_OTHER_VAULT.toString().replace("%v", String.valueOf(number)).replaceAll("%p", player.getName()));
        }

        PlayerVaults.getInstance().getOpenInventories().remove(new VaultViewInfo(holder.toString(), number).toString());
    }

    /**
     * Get the holder's vault file. Create if doesn't exist.
     *
     * @param holder The vault holder.
     *
     * @return The holder's vault config file.
     */
    public YamlConfiguration getPlayerVaultFile(UUID holder) {
        if (!directory.exists()) {
            directory.mkdir();
        }
        File file = new File(directory, holder.toString() + ".yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                // Who cares?
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Save the players vault file.
     *
     * @param holder The vault holder of whose file to save.
     * @param yaml   The config to save.
     *
     * @throws IOException Uh oh!
     */
    public void saveFile(UUID holder, YamlConfiguration yaml) throws IOException {
        File file = new File(directory, holder.toString() + ".yml");
        if (file.exists() && PlayerVaults.getInstance().isBackupsEnabled()) {
            file.renameTo(new File(PlayerVaults.getInstance().getBackupsFolder(), holder.toString() + ".yml"));
        }
        yaml.save(file);
    }

    /**
     * Get the instance of this class.
     *
     * @return - instance of this class.
     */
    public static UUIDVaultManager getInstance() {
        return instance;
    }
}
