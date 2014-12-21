package com.drtshock.playervaults.command;

import com.drtshock.playervaults.PlayerVaults;
import com.drtshock.playervaults.converter.BackpackConverter;
import com.drtshock.playervaults.converter.Converter;
import com.drtshock.playervaults.util.Lang;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.turt2live.uuid.CachingServiceProvider;
import com.turt2live.uuid.ServiceProvider;
import com.turt2live.uuid.turt2live.v2.ApiV2Service;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ConvertCommand implements CommandExecutor {

    private List<Converter> converters = ImmutableList.<Converter>builder()
        .add(new BackpackConverter())
        .build();
    private ServiceProvider uniqueIdProvider;

    public ConvertCommand() {
        CachingServiceProvider cachedUuidProvider = new CachingServiceProvider(new ApiV2Service());
        Map<UUID, String> seed = new HashMap<>();

        for (OfflinePlayer player : PlayerVaults.get().getServer().getOfflinePlayers()) {
            if (player.hasPlayedBefore()) {
                seed.put(player.getUniqueId(), player.getName());
            }
        }

        cachedUuidProvider.seedLoad(seed, 6 * 60 * 60); // 6 hour cache time
        this.uniqueIdProvider = cachedUuidProvider;
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("playervaults.convert")) {
            Lang.NO_PERMS.send(sender);
        } else {
            if (args.length == 0) {
                Lang.USAGE_CONVERT.send(sender, "%alias", alias);
            } else {
                String name = args[0];

                final List<Converter> converters = Lists.newArrayList();

                if (name.equalsIgnoreCase("all")) {
                    converters.addAll(this.converters);
                } else {
                    for (Converter converter : this.converters) {
                        if (converter.getName().equalsIgnoreCase(name)) {
                            converters.add(converter);
                        }
                    }
                }

                if (converters.size() <= 0) {
                    Lang.CONVERT_PLUGIN_NOT_FOUND.send(sender);
                } else {
                    // Fork into background
                    Lang.CONVERT_BACKGROUND.send(sender);
                    PlayerVaults.get().getServer().getScheduler().runTaskLaterAsynchronously(PlayerVaults.get(), new Runnable() {
                        @Override
                        public void run() {
                            int converted = 0;
                            PlayerVaults.get().getManager().setLocked(true);
                            for (Converter converter : converters) {
                                if (converter.canConvert()) {
                                    converted += converter.run(sender, ConvertCommand.this.uniqueIdProvider);
                                }
                            }

                            PlayerVaults.get().getManager().setLocked(false);
                            Lang.CONVERT_COMPLETE.send(sender, "%converted", converted);
                        }
                    }, 5); // This comment is to annoy evilmidget38
                }
            }
        }

        return true;
    }
}
