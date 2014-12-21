package com.drtshock.playervaults;

import com.drtshock.playervaults.command.ConvertCommand;
import com.drtshock.playervaults.command.DeleteCommand;
import com.drtshock.playervaults.command.SignCommand;
import com.drtshock.playervaults.command.VaultCommand;
import com.drtshock.playervaults.command.WorkbenchCommand;
import com.drtshock.playervaults.util.Lang;
import com.drtshock.playervaults.util.Updater;
import com.drtshock.playervaults.util.VaultViewInfo;
import com.google.common.base.Throwables;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import javax.annotation.Nullable;

public final class PlayerVaults extends JavaPlugin {

    private static PlayerVaults instance;
    private VaultManager manager;
    private Method legacyGetOnlinePlayers;
    private boolean updateAvailable;
    private Economy economy = null;
    private YamlConfiguration langConfig;

    public PlayerVaults() {
        instance = this;
    }

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.reloadConfig();
        this.loadLang();
        this.setupEconomy();

        this.stupidGetOnlinePlayers();

        this.manager = new VaultManager(new File(this.getDataFolder(), "uuidvaults"));
        this.manager.loadConfig(this);

        this.getServer().getPluginManager().registerEvents(new VaultListener(this, this.manager), this);
        this.registerCommands();

        this.checkForUpdate();
    }

    private void registerCommands() {
        this.getCommand("pv").setExecutor(new VaultCommand());
        this.getCommand("pvdel").setExecutor(new DeleteCommand());
        this.getCommand("pvsign").setExecutor(new SignCommand());
        this.getCommand("workbench").setExecutor(new WorkbenchCommand());
        this.getCommand("pvconvert").setExecutor(new ConvertCommand());
    }

    @Override
    public void onDisable() {
        for (Player player : this.getOnlinePlayers()) {
            if (this.manager.getActiveVaults().containsKey(player.getName())) {
                Inventory inventory = player.getOpenInventory().getTopInventory();
                if (inventory.getViewers().size() == 1) {
                    VaultViewInfo info = this.manager.getActiveVaults().get(player.getName());
                    try {
                        this.manager.saveVault(player.getUniqueId(), inventory, info.getVaultId());
                    } catch (IOException e) {
                        // ignore
                    }

                    this.manager.getInventoryMap().remove(info.toString());
                }

                this.manager.getActiveVaults().remove(player.getName());
            }

            player.closeInventory();
        }

        this.manager.saveSigns();
        instance = null;
    }

    public static PlayerVaults get() {
        return instance;
    }

    public VaultManager getManager() {
        return this.manager;
    }

    public YamlConfiguration getLanguage() {
        return this.langConfig;
    }

    @Nullable
    public Economy getEconomy() {
        return this.economy;
    }

    public boolean isUpdateAvailable() {
        return this.updateAvailable;
    }

    private void loadLang() {
        File langFile = new File(this.getDataFolder(), "lang.yml");

        try (InputStream input = this.getResource("lang.yml")) {
            if (!langFile.exists()) {
                try {
                    this.getDataFolder().mkdir();
                    langFile.createNewFile();

                    if (input != null) {
                        try (OutputStream output = new FileOutputStream(langFile)) {
                            byte[] bytes = new byte[1024];

                            int read;
                            while ((read = input.read(bytes)) != -1) {
                                output.write(bytes, 0, read);
                            }
                        }

                        this.langConfig = YamlConfiguration.loadConfiguration(input);
                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace(); // So they notice
                    this.getLogger().severe("Failed to create lang.yml.");
                    this.getLogger().severe("Please report this error at https://github.com/drtshock/PlayerVaults.");
                    this.getServer().getPluginManager().disablePlugin(this);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        YamlConfiguration conf = YamlConfiguration.loadConfiguration(langFile);
        for (Lang value : Lang.values()) {
            if (conf.getString(value.getKey()) == null) {
                conf.set(value.getKey(), value.getFallback());
            }
        }

        this.langConfig = conf;
        try {
            conf.save(langFile);
        } catch (IOException e) {
            this.getLogger().severe("Failed to save lang.yml.");
            this.getLogger().severe("Please report this error at https://github.com/drtshock/PlayerVaults.");
            e.printStackTrace();
        }
    }

    private boolean setupEconomy() {
        if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> provider = getServer().getServicesManager().getRegistration(Economy.class);
        if (provider != null) {
            this.economy = provider.getProvider();
        }

        return this.economy != null;
    }

    private void checkForUpdate() {
        if (this.getConfig().getBoolean("check-update", false)) {
            final Updater updater = new Updater(this, 50123, this.getFile(), this.getConfig().getBoolean("download-update", true) ? Updater.UpdateType.DEFAULT : Updater.UpdateType.NO_DOWNLOAD, false);
            this.getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
                @Override
                public void run() {
                    PlayerVaults.this.updateAvailable = updater.getResult() == Updater.UpdateResult.UPDATE_AVAILABLE;
                    if (updater.getResult() == Updater.UpdateResult.SUCCESS) {
                        PlayerVaults.this.getLogger().info("PlayerVaults will update to version " + updater.getLatestName() + " on the next restart.");
                    } else {
                        PlayerVaults.this.getLogger().info("No update available.");
                    }
                }
            });
        }
    }

    private void stupidGetOnlinePlayers() {
        for (Method method : this.getServer().getClass().getDeclaredMethods()) {
            if (method.getName().endsWith("getOnlinePlayers") && method.getReturnType() == Player[].class) {
                this.legacyGetOnlinePlayers = method;
            }
        }
    }

    public Collection<Player> getOnlinePlayers() {
        try {
            return (Collection<Player>) this.getServer().getOnlinePlayers();
        } catch (NoSuchMethodError e) {
            try {
                return Arrays.asList((Player[]) this.legacyGetOnlinePlayers.invoke(this.getServer()));
            } catch (InvocationTargetException | IllegalAccessException fatal) {
                this.getLogger().severe("PlayerVaults encountered an issue while trying to get the current online players and is now disabling.");
                this.getServer().getPluginManager().disablePlugin(this);
                throw Throwables.propagate(fatal instanceof InvocationTargetException ? fatal.getCause() : fatal);
            }
        }
    }
}
