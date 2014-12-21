package com.drtshock.playervaults.util;

/**
 * Stores information about a vault viewing including the viewer of the vault, and the vault number.
 */
public class VaultViewInfo {

    private final String viewer;
    private final int vaultId;

    /**
     * Make a VaultViewObject
     *
     * @param viewer The viewer of the vault.
     * @param vaultId The vault number.
     */
    public VaultViewInfo(String viewer, int vaultId) {
        this.viewer = viewer;
        this.vaultId = vaultId;
    }

    /**
     * Get the viewer of the vault.
     *
     * @return The viewer of the vault.
     */
    public String getViewer() {
        return this.viewer;
    }

    /**
     * Get the vault id.
     *
     * @return The vault id.
     */
    public int getVaultId() {
        return this.vaultId;
    }

    @Override
    public String toString() {
        return this.viewer + " " + this.vaultId;
    }
}
