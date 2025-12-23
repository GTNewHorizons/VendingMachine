package com.cubefury.vendingmachine.command.vending;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;

import com.cubefury.vendingmachine.command.Utils;
import com.cubefury.vendingmachine.storage.NameCache;
import com.cubefury.vendingmachine.trade.CurrencyType;
import com.cubefury.vendingmachine.trade.TradeManager;

public class SubCmdReset implements IVendingSubcommand {

    @Override
    public String getName() {
        return "reset";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/vending reset [player] <coin type>";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP target = null;
        boolean allCurrency = false;
        CurrencyType type = null;

        switch (args.length) {
            case 1: {
                target = CommandBase.getCommandSenderAsPlayer(sender);
                allCurrency = args[0].equals("all");
                type = CurrencyType.getTypeFromId(args[0]);
                break;
            }
            case 2: {
                try {
                    target = CommandBase.getPlayer(sender, args[0]);
                    allCurrency = args[1].equals("all");
                    type = CurrencyType.getTypeFromId(args[1]);
                } catch (PlayerNotFoundException ignored) {}
                break;
            }
            default:
        }
        boolean validCurrency = allCurrency || type != null;
        if (target == null || !validCurrency) {
            sender.addChatMessage(new ChatComponentText("Usage: " + getUsage(sender)));
            return;
        }

        UUID playerId = NameCache.INSTANCE.getUUIDFromPlayer(target);
        TradeManager.INSTANCE.playerCurrency.putIfAbsent(playerId, new HashMap<>());
        Map<CurrencyType, Integer> coinInventory = TradeManager.INSTANCE.playerCurrency.get(playerId);
        if (allCurrency) {
            coinInventory.clear();
            sender.addChatMessage(
                new ChatComponentText(String.format("Reset all coins for %s", target.getDisplayName())));
        } else {
            coinInventory.remove(type);
            sender.addChatMessage(
                new ChatComponentText(String.format("Reset %s for %s", type.id, target.getDisplayName())));
        }
    }

    @Override
    public List<String> tabComplete(ICommandSender sender, String[] args) {
        switch (args.length) {
            case 0: {
                return Utils.getPlayersAndCoinTypesOrAll();
            }
            case 1: {
                return CommandBase
                    .getListOfStringsFromIterableMatchingLastWord(args, Utils.getPlayersAndCoinTypesOrAll());
            }
            case 2: {
                if (
                    Utils.getCurrentPlayers()
                        .contains(args[0])
                ) {
                    return CommandBase.getListOfStringsFromIterableMatchingLastWord(args, Utils.getCoinTypesOrAll());
                }
            }
            default: {
                return null;
            }
        }
    }
}
