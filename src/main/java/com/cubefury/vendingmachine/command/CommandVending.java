package com.cubefury.vendingmachine.command;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;

import com.cubefury.vendingmachine.storage.NameCache;
import com.cubefury.vendingmachine.trade.CurrencyType;
import com.cubefury.vendingmachine.trade.TradeManager;
import com.cubefury.vendingmachine.util.Translator;

public class CommandVending extends CommandBase {

    @Override
    public String getCommandName() {
        return "vending";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/vending <set|add|reset> [player] <coin_type|all> <amount>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return sender.canCommandSenderUseCommand(getRequiredPermissionLevel(), getCommandName());
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        switch (args.length) {
            case 1: {
                return getListOfStringsMatchingLastWord(args, "set", "add", "reset");
            }
            case 2: {
                List<String> suggestions = getListOfStringsMatchingLastWord(
                    args,
                    MinecraftServer.getServer()
                        .getAllUsernames());
                suggestions.addAll(
                    Arrays.stream(CurrencyType.values())
                        .map(c -> c.id)
                        .collect(Collectors.toList()));
                suggestions.add("all");
                return suggestions;
            }
            case 3: {
                List<String> suggestions = Arrays.stream(CurrencyType.values())
                    .map(c -> c.id)
                    .collect(Collectors.toList());
                suggestions.add("all");
                if (
                    suggestions.stream()
                        .anyMatch(s -> s.equals(args[2]))
                ) {
                    return null;
                }
                return suggestions;
            }
            default:
                return null;
        }
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.addChatMessage(
                new ChatComponentText(
                    Translator.translate("vendingmachine.command.usage") + " " + getCommandUsage(sender)));
            return;
        }

        String action = args[0];
        EntityPlayerMP target = null;

        if (args.length >= 2) {
            target = getPlayer(sender, args[1]);
        } else if (sender instanceof EntityPlayer) {
            target = getCommandSenderAsPlayer(sender);
        }

        if (target == null) {
            sender.addChatMessage(new ChatComponentText(Translator.translate("vendingmachine.command.invalid_player")));
            return;
        }

        if (action.equals("set") || action.equals("add")) {
            int typeOffset = args.length >= 3 ? 2 : 1;
            CurrencyType type = CurrencyType.getTypeFromId(args[typeOffset]);
            if (type == null && !args[typeOffset].equals("all")) {
                sender.addChatMessage(
                    new ChatComponentText(
                        Translator.translate("vendingmachine.command.unknown_currency") + " " + args[typeOffset]));
                return;
            }
            try {
                int amount = Integer.parseInt(args[typeOffset + 1]);
                UUID playerId = NameCache.INSTANCE.getUUIDFromPlayer(target);
                TradeManager.INSTANCE.playerCurrency.putIfAbsent(playerId, new HashMap<>());
                Map<CurrencyType, Integer> coinInventory = TradeManager.INSTANCE.playerCurrency.get(playerId);
                if (args[typeOffset].equals("all")) {
                    for (CurrencyType cur : CurrencyType.values()) {
                        coinInventory.putIfAbsent(cur, 0);

                        coinInventory.put(cur, action.equals("add") ? coinInventory.get(cur) + amount : amount);
                    }
                } else {
                    coinInventory.putIfAbsent(type, 0);
                    coinInventory.put(type, action.equals("add") ? coinInventory.get(type) + amount : amount);
                }
            } catch (NumberFormatException e) {
                sender.addChatMessage(
                    new ChatComponentText(
                        Translator.translate("vendingmachine.command.usage") + " "
                            + getAddOrSetUsage(sender, args[0])));
            }
        } else if (action.equals("reset")) {
            int typeOffset = args.length >= 2 ? 2 : 1;
            CurrencyType type = CurrencyType.getTypeFromId(args[typeOffset]);
            if (type == null && !args[typeOffset].equals("all")) {
                sender.addChatMessage(
                    new ChatComponentText(
                        Translator.translate("vendingmachine.command.unknown_currency") + " " + args[typeOffset]));
                return;
            }
            UUID playerId = NameCache.INSTANCE.getUUIDFromPlayer(target);
            TradeManager.INSTANCE.playerCurrency.putIfAbsent(playerId, new HashMap<>());
            Map<CurrencyType, Integer> coinInventory = TradeManager.INSTANCE.playerCurrency.get(playerId);
            if (args[typeOffset].equals("all")) {
                coinInventory.clear();
            } else {
                coinInventory.remove(type);
            }
        } else {
            sender.addChatMessage(
                new ChatComponentText(Translator.translate("vendingmachine.command.unknown_action") + " " + action));
        }

    }

    private String getAddOrSetUsage(ICommandSender sender, String arg) {
        if (!arg.equals("add") && !arg.equals("set")) {
            return getCommandUsage(sender);
        }
        return "/vending " + arg + " [player] <coin_type|all> <amount>";
    }
}
