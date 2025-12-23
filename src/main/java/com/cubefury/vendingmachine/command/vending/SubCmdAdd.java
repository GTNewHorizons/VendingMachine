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

import com.cubefury.vendingmachine.VendingMachine;
import com.cubefury.vendingmachine.command.Utils;
import com.cubefury.vendingmachine.storage.NameCache;
import com.cubefury.vendingmachine.trade.CurrencyType;
import com.cubefury.vendingmachine.trade.TradeManager;

public class SubCmdAdd implements IVendingSubcommand {

    @Override
    public String getName() {
        return "add";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/vending add [player] <coin type> <amount>";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP target = null;
        boolean allCurrency = false;
        CurrencyType type = null;
        int amount = 0;
        VendingMachine.LOG.info(args.length);

        switch (args.length) {
            case 2: {
                target = CommandBase.getCommandSenderAsPlayer(sender);
                allCurrency = args[0].equals("all");
                type = CurrencyType.getTypeFromId(args[0]);
                amount = Utils.parseAmount(args[1]);
                break;
            }
            case 3: {
                try {
                    target = CommandBase.getPlayer(sender, args[0]);
                    allCurrency = args[1].equals("all");
                    type = CurrencyType.getTypeFromId(args[1]);
                    amount = Utils.parseAmount(args[2]);
                } catch (PlayerNotFoundException ignored) {}
                break;
            }
            default:
        }
        boolean validCurrency = allCurrency || type != null;
        if (target == null || !validCurrency || amount == 0) {
            sender.addChatMessage(new ChatComponentText("Usage: " + getUsage(sender)));
            return;
        }

        UUID playerId = NameCache.INSTANCE.getUUIDFromPlayer(target);
        TradeManager.INSTANCE.playerCurrency.putIfAbsent(playerId, new HashMap<>());
        Map<CurrencyType, Integer> coinInventory = TradeManager.INSTANCE.playerCurrency.get(playerId);
        if (allCurrency) {
            for (CurrencyType cur : CurrencyType.values()) {
                coinInventory.put(cur, coinInventory.getOrDefault(cur, 0) + amount);
            }
            sender.addChatMessage(
                new ChatComponentText(String.format("Added %dx all coins for %s", amount, target.getDisplayName())));
        } else {
            coinInventory.put(type, coinInventory.getOrDefault(type, 0) + amount);
            sender.addChatMessage(
                new ChatComponentText(String.format("Added %dx %s for %s", amount, type.id, target.getDisplayName())));
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
