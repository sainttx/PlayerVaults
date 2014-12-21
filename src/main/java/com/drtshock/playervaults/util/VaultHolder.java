package com.drtshock.playervaults.util;

import com.drtshock.playervaults.PlayerVaults;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Assists in the detection of PlayerVaults from other plugins.
 */
public final class VaultHolder implements InventoryHolder {

    private Inventory inventory;
    private int vaultId = 0;

    public VaultHolder(int vaultId) {
        this.vaultId = vaultId;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public int getVaultId() {
        return this.vaultId;
    }

    public void setVaultId(int vaultId) {
        this.vaultId = vaultId;
    }

    public static Inventory wrapInventory(int vaultId, String vaultName, int inventorySize) {
        VaultHolder holder = new VaultHolder(vaultId);
        Inventory inventory = PlayerVaults.get().getServer().createInventory(holder, inventorySize, vaultName);
        holder.setInventory(inventory);

        return inventory;
    }
}
