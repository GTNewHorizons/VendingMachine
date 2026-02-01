package com.cubefury.vendingmachine.command.vending;

import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import com.cubefury.vendingmachine.handlers.SaveLoadHandler;
import com.cubefury.vendingmachine.network.handlers.NetTradeDbSync;

public class SubCmdReload implements IVendingSubcommand {

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/vending reload database";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 1 || args[0].compareTo("database") != 0) {
            sender.addChatMessage(new ChatComponentText("Usage: " + getUsage(sender)));
            return;
        }

        SaveLoadHandler.INSTANCE.reloadDatabase();
        NetTradeDbSync.sendDatabase(null, false);

        sender.addChatMessage(new ChatComponentText("Reloaded Trade Database"));
    }

    @Override
    public List<String> tabComplete(ICommandSender sender, String[] args) {
        switch (args.length) {
            case 1: {
                return CommandBase
                    .getListOfStringsFromIterableMatchingLastWord(args, Collections.singletonList("database"));
            }
            default: {
                return null;
            }
        }
    }
}
