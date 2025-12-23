package com.cubefury.vendingmachine.command;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.server.MinecraftServer;

import com.cubefury.vendingmachine.trade.CurrencyType;

public class Utils {

    public static List<String> getCoinTypes() {
        return Arrays.stream(CurrencyType.values())
            .map(c -> c.id)
            .collect(Collectors.toList());
    }

    public static List<String> getCoinTypesOrAll() {
        List<String> types = getCoinTypes();
        types.add("all");
        return types;
    }

    public static List<String> getCurrentPlayers() {
        return Arrays.stream(
            MinecraftServer.getServer()
                .getAllUsernames())
            .collect(Collectors.toList());
    }

    public static List<String> getPlayersAndCoinTypesOrAll() {
        return Stream.concat(getCoinTypesOrAll().stream(), getCurrentPlayers().stream())
            .collect(Collectors.toList());
    }

    public static int parseAmount(String arg) {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException except) {
            return 0;
        }
    }
}
