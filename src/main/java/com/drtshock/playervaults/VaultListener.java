package com.drtshock.playervaults;

import com.drtshock.playervaults.util.Lang;
import com.drtshock.playervaults.util.SignSetInfo;
import com.drtshock.playervaults.util.VaultUtil;
import com.drtshock.playervaults.util.VaultViewInfo;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;

import java.io.IOException;

public class VaultListener implements Listener {

    private final PlayerVaults plugin;
    private final VaultManager manager;

    public VaultListener(PlayerVaults plugin, VaultManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void handleTeleport(final PlayerTeleportEvent event) {
        this.saveVault(event.getPlayer());
    }

    @EventHandler
    public void handleJoin(final PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (this.plugin.isUpdateAvailable() && (player.isOp() || player.hasPermission("playervaults.notify"))) {
            Lang.UPDATE_NOTIFY.send(player);
        }
    }

    @EventHandler
    public void handleQuit(final PlayerQuitEvent event) {
        this.saveVault(event.getPlayer());
    }

    @EventHandler
    public void handleDeath(final PlayerDeathEvent event) {
        this.saveVault(event.getEntity());
    }

    @EventHandler
    public void handleInventoryClose(final InventoryCloseEvent event) {
        HumanEntity human = event.getPlayer();
        if (human instanceof Player) {
            this.saveVault((Player) human);
        }
    }

    private void saveVault(Player player) {
        if (this.manager.getActiveVaults().containsKey(player.getName())) {
            Inventory inventory = player.getOpenInventory().getTopInventory();
            if (inventory.getViewers().size() == 1) {
                VaultViewInfo viewInfo = this.manager.getActiveVaults().get(player.getName());
                try {
                    this.manager.saveVault(player.getUniqueId(), inventory, viewInfo.getVaultId());
                } catch (IOException e) {
                    // ignore
                }

                this.manager.getInventoryMap().remove(viewInfo.toString());
            }

            this.manager.getActiveVaults().remove(player.getName());
        }
    }

    @EventHandler
    public void thisIsSoMessy(final PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (this.manager.getActiveVaults().containsKey(player.getName())) {
                Block block = event.getClickedBlock();
                // Different inventories that we don't want the player to open.
                if (block.getType() == Material.CHEST
                    || block.getType() == Material.TRAPPED_CHEST
                    || block.getType() == Material.ENDER_CHEST
                    || block.getType() == Material.FURNACE
                    || block.getType() == Material.BURNING_FURNACE
                    || block.getType() == Material.BREWING_STAND
                    || block.getType() == Material.ENCHANTMENT_TABLE
                    || block.getType() == Material.BEACON) {
                    event.setCancelled(true);
                }
            }
        }

        if (this.manager.getSigns().containsKey(player.getName())) {
            SignSetInfo info = this.manager.getSigns().get(player.getName());
            int vaultId = info.getVaultId();
            boolean self = info.isSelf();
            String owner = null;
            if (!self) {
                owner = info.getOwner();
            }

            this.manager.getSigns().remove(player.getName());
            event.setCancelled(true);

            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (event.getClickedBlock().getType() == Material.WALL_SIGN || event.getClickedBlock().getType() == Material.SIGN_POST) {
                    Sign sign = (Sign) event.getClickedBlock().getState();
                    Location location = sign.getLocation();
                    String worldName = location.getWorld().getName();
                    int x = location.getBlockX();
                    int y = location.getBlockY();
                    int z = location.getBlockZ();

                    if (self) {
                        this.manager.getSignConfig().set(worldName + ";;" + x + ";;" + y + ";;" + z + ".self", true);
                    } else {
                        this.manager.getSignConfig().set(worldName + ";;" + x + ";;" + y + ";;" + z + ".owner", owner);
                    }

                    this.manager.getSignConfig().set(worldName + ";;" + x + ";;" + y + ";;" + z + ".chest", vaultId);
                    this.manager.queueSignsForSave();

                    Lang.SET_SIGN.send(player);
                } else {
                    Lang.NOT_A_SIGN.send(player);
                }
            } else {
                Lang.NOT_A_SIGN.send(player);
            }

            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (clickedBlock.getType() == Material.WALL_SIGN || clickedBlock.getType() == Material.SIGN_POST) {
                Location location = clickedBlock.getLocation();
                String world = location.getWorld().getName();
                int x = location.getBlockX();
                int y = location.getBlockY();
                int z = location.getBlockZ();

                if (this.manager.getSignConfig().getKeys(false).contains(world + ";;" + x + ";;" + y + ";;" + z)) {
                    int vaultId = this.manager.getSignConfig().getInt(world + ";;" + x + ";;" + y + ";;" + z + ".chest");

                    if ((player.hasPermission("playervaults.signs.use") && (player.hasPermission("playervaults.signs.bypass") || VaultUtil.checkPerms(player, 99)))) {
                        boolean self = this.manager.getSignConfig().getBoolean(world + ";;" + x + ";;" + y + ";;" + z + ".self", false);
                        String owner = null;
                        if (!self) {
                            owner = this.manager.getSignConfig().getString(world + ";;" + x + ";;" + y + ";;" + z + ".owner");
                        }

                        OfflinePlayer offlinePlayer = this.plugin.getServer().getOfflinePlayer(owner != null ? owner : event.getPlayer().getName()); // Not best way but :\
                        if (offlinePlayer == null) {
                            Lang.VAULT_DOES_NOT_EXIST.send(player);
                            return;
                        }

                        Inventory inventory;
                        if (self) {
                            inventory = this.manager.loadVaultFor(player.getUniqueId(), vaultId, VaultUtil.getMaxVaultSize(player), player);
                        } else {
                            inventory = this.manager.loadVaultFor(offlinePlayer.getUniqueId(), vaultId, VaultUtil.getMaxVaultSize(offlinePlayer), player);
                        }

                        player.openInventory(inventory);

                        this.manager.getActiveVaults().put(player.getName(), new VaultViewInfo((self) ? player.getName() : owner, vaultId));
                        event.setCancelled(true);
                        Lang.OPEN_WITH_SIGN.send(player, "%v", vaultId, "%p", self ? player.getName() : owner);
                    } else {
                        Lang.NO_PERMS.send(player);
                    }
                }
            }
        }
    }

    @EventHandler
    public void handlePhysics(BlockPhysicsEvent event) {
        this.blockChangeCheck(event.getBlock().getLocation());
    }

    @EventHandler
    public void handleBlockChange(EntityChangeBlockEvent event) {
        this.blockChangeCheck(event.getBlock().getLocation());
    }

    @EventHandler
    public void handleBlockBreak(BlockBreakEvent event) {
        this.blockChangeCheck(event.getBlock().getLocation());
    }

    /**
     * Check if the location given is a sign, and if so, remove it from the signs.yml file
     *
     * @param location The location to check
     */
    private void blockChangeCheck(Location location) {
        String worldName = location.getWorld().getName();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        if (this.manager.getSignConfig().getKeys(false).contains(worldName + ";;" + x + ";;" + y + ";;" + z)) {
            this.manager.getSignConfig().set(worldName + ";;" + x + ";;" + y + ";;" + z, null);
            this.manager.queueSignsForSave();
        }
    }

    @EventHandler
    public void handleInteract(final PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        EntityType type = event.getRightClicked().getType();
        if ((type == EntityType.VILLAGER || type == EntityType.MINECART) && this.manager.getActiveVaults().containsKey(player.getName())) {
            event.setCancelled(true);
        }
    }
}
