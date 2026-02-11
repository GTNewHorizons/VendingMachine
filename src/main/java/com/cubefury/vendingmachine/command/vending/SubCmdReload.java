package com.cubefury.vendingmachine.command.vending;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import com.cubefury.vendingmachine.handlers.SaveLoadHandler;
import com.cubefury.vendingmachine.network.handlers.NetTradeDbSync;
import com.cubefury.vendingmachine.storage.NameCache;
import com.cubefury.vendingmachine.trade.TradeManager;

public class SubCmdReload implements IVendingSubcommand {

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/vending reload database, /vending reload tradestate [player]";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 1 && args[0].equals("database")) {
            SaveLoadHandler.INSTANCE.reloadDatabase();
            NetTradeDbSync.sendDatabase(null, false);

            sender.addChatMessage(new ChatComponentText("Reloaded Trade Database"));
        } else if ((args.length == 1 || args.length == 2) && args[0].equals("tradestate")) {
            UUID target = args.length == 1
                ? NameCache.INSTANCE.getUUIDFromPlayer(CommandBase.getCommandSenderAsPlayer(sender))
                : NameCache.INSTANCE.getUUID(args[1]);
            if (target == null) {
                sender.addChatMessage(new ChatComponentText("Could not resolve UUID of player."));
                return;
            }
            TradeManager.INSTANCE.clearTradeState(target);
            SaveLoadHandler.INSTANCE.loadTradeState(target);
            sender.addChatMessage(
                new ChatComponentText("Reloaded trade state for " + NameCache.INSTANCE.getName(target)));
        } else {
            sender.addChatMessage(new ChatComponentText("Usage: " + getUsage(sender)));
        }
    }

    @Override
    public List<String> tabComplete(ICommandSender sender, String[] args) {
        switch (args.length) {
            case 1: {
                return CommandBase
                    .getListOfStringsFromIterableMatchingLastWord(args, Arrays.asList("database", "tradestate"));
            }
            case 2: {
                return args[0].equals("tradestate")
                    ? CommandBase.getListOfStringsFromIterableMatchingLastWord(args, NameCache.INSTANCE.getAllNames())
                    : null;
            }
            default: {
                return null;
            }
        }
    }
}
