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

public class CommandVending extends CommandBase {

    @Override
    public String getCommandName() {
        return "vending";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/vending <set|add|reset> [player] <coin_type|all> [amount]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 4;
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
        if (args.length < 2) {
            sender.addChatMessage(new ChatComponentText("Usage: " + getCommandUsage(sender)));
            return;
        }

        String action = args[0];
        EntityPlayerMP target = null;

        if (args.length > 2 && action.equals("reset") || args.length > 3) {
            target = getPlayer(sender, args[1]);
        } else if (sender instanceof EntityPlayer) {
            target = getCommandSenderAsPlayer(sender);
        }

        if (target == null) {
            sender.addChatMessage(new ChatComponentText("Could not identify player."));
            return;
        }

        if (action.equals("set") || action.equals("add")) {
            if (args.length < 3 || args.length > 4) {
                sender.addChatMessage(new ChatComponentText("Usage: " + getAddOrSetUsage(sender, args[0])));
                return;
            }

            int amount = 0;
            try {
                amount = Integer.parseInt(args[args.length - 1]);
            } catch (NumberFormatException e) {
                sender.addChatMessage(new ChatComponentText("Usage: " + getAddOrSetUsage(sender, args[0])));
                return;
            }

            int typeOffset = args.length > 3 ? 2 : 1;
            CurrencyType type = CurrencyType.getTypeFromId(args[typeOffset]);
            if (type == null && !args[typeOffset].equals("all")) {
                sender.addChatMessage(new ChatComponentText("Unknown Currency Type: " + args[typeOffset]));
                return;
            }

            UUID playerId = NameCache.INSTANCE.getUUIDFromPlayer(target);
            TradeManager.INSTANCE.playerCurrency.putIfAbsent(playerId, new HashMap<>());
            Map<CurrencyType, Integer> coinInventory = TradeManager.INSTANCE.playerCurrency.get(playerId);
            if (args[typeOffset].equals("all")) {
                for (CurrencyType cur : CurrencyType.values()) {
                    coinInventory.put(cur, action.equals("add") ? coinInventory.getOrDefault(cur, 0) + amount : amount);
                }
                sender.addChatMessage(
                    new ChatComponentText(
                        String.format(
                            "%s %dx all coins for %s",
                            action.equals("add") ? "Added" : "Set",
                            amount,
                            target.getDisplayName())));
            } else {
                coinInventory.put(type, action.equals("add") ? coinInventory.getOrDefault(type, 0) + amount : amount);
                sender.addChatMessage(
                    new ChatComponentText(
                        String.format(
                            "%s %dx %s coins for %s",
                            action.equals("add") ? "Added" : "Set",
                            amount,
                            type.id,
                            target.getDisplayName())));
            }
        } else if (action.equals("reset")) {
            if (args.length < 2 || args.length > 3) {
                sender.addChatMessage(new ChatComponentText("Usage: /vending reset [player] [coin_type|all]"));
                return;
            }
            int typeOffset = args.length > 2 ? 2 : 1;
            CurrencyType type = CurrencyType.getTypeFromId(args[typeOffset]);
            if (type == null && !args[typeOffset].equals("all")) {
                sender.addChatMessage(new ChatComponentText("Unknown Currency Type: " + args[typeOffset]));
                return;
            }
            UUID playerId = NameCache.INSTANCE.getUUIDFromPlayer(target);
            TradeManager.INSTANCE.playerCurrency.putIfAbsent(playerId, new HashMap<>());
            Map<CurrencyType, Integer> coinInventory = TradeManager.INSTANCE.playerCurrency.get(playerId);
            if (args[typeOffset].equals("all")) {
                coinInventory.clear();
                sender.addChatMessage(
                    new ChatComponentText(String.format("Reset all coins for %s", target.getDisplayName())));
            } else {
                coinInventory.remove(type);
                sender.addChatMessage(
                    new ChatComponentText(String.format("Reset %s coins for %s", type.id, target.getDisplayName())));
            }
        } else {
            sender.addChatMessage(new ChatComponentText("Unknown Action: " + action));
        }

    }

    private String getAddOrSetUsage(ICommandSender sender, String arg) {
        if (!arg.equals("add") && !arg.equals("set")) {
            return getCommandUsage(sender);
        }
        return "/vending " + arg + " [player] <coin_type|all> <amount>";
    }
}
