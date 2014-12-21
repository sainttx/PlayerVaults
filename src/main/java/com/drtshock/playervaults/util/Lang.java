/*
 * Copyright (C) 2013 drtshock
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.drtshock.playervaults.util;

import com.drtshock.playervaults.PlayerVaults;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Iterator;

/**
 * An enum for requesting strings from the language file.
 */
public enum Lang {
    TITLE("title-name", "&4[&fPlayerVaults&4]:"),
    OPEN_VAULT("open-vault", "&fOpening vault &a%v"),
    OPEN_OTHER_VAULT("open-other-vault", "&fOpening vault &a%v &fof &a%p"),
    OPEN_WORKBENCH("open-workbench", "&fOpening workbench"),
    INVALID_ARGS("invalid-args", "&cInvalid args!"),
    DELETE_VAULT("delete-vault", "&fDeleted vault &a%v"),
    DELETE_OTHER_VAULT("delete-other-vault", "&fDeleted vault &a%v &fof &a%p"),
    PLAYER_ONLY("player-only", "&cSorry but that can only be run by a player!"),
    MUST_BE_NUMBER("must-be-number", "&cYou need to specify a number between 1-99"),
    DELETE_VAULT_ERROR("delete-vault-error", "&cError deleting vault :("),
    NO_PERMS("no-permissions", "&cYou don''t have permission for that!"),
    INSUFFICIENT_FUNDS("insufficient-funds", "&cYou don''t have enough money for that!"),
    REFUND_AMOUNT("refund-amount", "&fYou were refunded &a%price &ffor deleting that vault."),
    COST_TO_CREATE("cost-to-create", "&fYou were charged &c%price &ffor creating a vault."),
    COST_TO_OPEN("cost-to-open", "&fYou were charged &c%price &ffor opening that vault."),
    VAULT_DOES_NOT_EXIST("vault-does-not-exist", "&cThat vault does not exist!"),
    CLICK_A_SIGN("click-a-sign", "&fNow click a sign!"),
    NOT_A_SIGN("not-a-sign", "&cYou must click a sign!"),
    SET_SIGN("set-sign-success", "&fYou have successfully set a PlayerVault access sign!"),
    EXISTING_VAULTS("existing-vaults", "&f%p has vaults: &a%v"),
    VAULT_TITLE("vault-title", "&4Vault #%number"),
    OPEN_WITH_SIGN("open-with-sign", "&fOpening vault &a%v &fof &a%p"),
    NO_PLAYER_FOUND("no-player-found", "&cCannot find player &a%p"),
    CONVERT_PLUGIN_NOT_FOUND("plugin-not-found", "&cNo converter found for that plugin"),
    CONVERT_COMPLETE("conversion-complete", "&aConverted %converted players to PlayerVaults"),
    CONVERT_BACKGROUND("conversion-background", "&fConversion has been forked to the background. See console for updates."),
    LOCKED("vaults-locked", "&cVaults are currently locked while conversion occurs. Please try again in a moment!"),

    ALREADY_VIEWING("already-viewing", "&cYou are already viewing a vault. If this is not the case, please re-login and try again."),

    USAGE_PV_NUMBER("usage.pv.number", "&f/%alias <number>"),
    USAGE_PV_OTHER("usage.pv.other", "&f/%alias <player> <number>"),
    USAGE_PVDEL_NUMBER("usage.pvdel.number", "&f/%alias <number>"),
    USAGE_PVDEL_OTHER("usage.pvdel.other", "&f/%alias <player> <number>"),
    USAGE_CONVERT("usage.convert", "&f/%alias <all | plugin name>"),
    USAGE_SIGN("usage.sign", "&f/%alias <owner> <#>"),

    UPDATE_NOTIFY("update.notify", "&aVersion %version of PlayerVaults is available for download");

    private final String key;
    private final String fallback;

    private Lang(String key, String fallback) {
        this.key = key;
        this.fallback = fallback;
    }

    /**
     * Sends a message.
     *
     * @param sender the target
     * @param args the args used to format the message
     */
    public void send(CommandSender sender, Object... args) {
        this.send(sender, true, args);
    }

    /**
     * Sends a message, prefixed with {@link #TITLE}.
     * @param sender the target
     * @param withTitle if the message should be prefixed with {@link #TITLE}
     * @param args the args used to format the message
     */
    public void send(CommandSender sender, boolean withTitle, Object... args) {
        sender.sendMessage(this.format(withTitle, args));
    }

    public String format(boolean withTitle, Object... args) {
        String message = this.toString();
        if (withTitle) {
            message = TITLE.toString() + " " + message;
        }

        Iterator<Object> iterator = Arrays.asList(args).iterator();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            String value = String.valueOf(iterator.next());
            message = message.replace(key, value);
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String format(Object... args) {
        return this.format(false, args);
    }

    @Override
    public String toString() {
        return PlayerVaults.get().getLanguage().getString(this.key, this.fallback);
    }

    /**
     * Get the default value of the key.
     *
     * @return The default value of the key.
     */
    public String getFallback() {
        return this.fallback;
    }

    /**
     * Get the key to the string.
     *
     * @return The key to the string.
     */
    public String getKey() {
        return this.key;
    }
}
