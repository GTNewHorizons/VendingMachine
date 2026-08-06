package com.cubefury.vendingmachine.command;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.event.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

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

    public static IChatComponent getCurrencyListFormattedWithHighlights() {
        ChatComponentText text = new ChatComponentText("");

        List<String> names = getCoinTypes();

        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                text.appendText(", ");
            }

            IChatComponent translated = getLocalisedCoinNameWithHover(names.get(i));
            translated.getChatStyle()
                .setColor(i % 2 == 0 ? EnumChatFormatting.AQUA : EnumChatFormatting.GREEN);
            text.appendSibling(translated);
        }

        return text;
    }

    public static IChatComponent getLocalisedCoinNameWithHover(String unlocalised) {

        ChatComponentTranslation translated = new ChatComponentTranslation("vendingmachine.coin." + unlocalised);

        translated.getChatStyle()
            .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(unlocalised)));

        return translated;
    }
}
