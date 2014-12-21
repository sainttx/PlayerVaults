package com.drtshock.playervaults.util;

/**
 * Stores information about a vault viewing including the viewer of the vault, and the vault number.
 */
public class VaultViewInfo {

    private final String viewer;
    private final int vaultId;

    public VaultViewInfo(String viewer, int vaultId) {
        this.viewer = viewer;
        this.vaultId = vaultId;
    }

    public String getViewer() {
        return viewer;
    }

    public int getVaultId() {
        return vaultId;
    }

    @Override
    public String toString() {
        return this.viewer + " " + this.vaultId;
    }
}
