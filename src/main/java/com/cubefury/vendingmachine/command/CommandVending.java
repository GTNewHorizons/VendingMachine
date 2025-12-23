package com.cubefury.vendingmachine.command;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import com.cubefury.vendingmachine.command.vending.IVendingSubcommand;
import com.cubefury.vendingmachine.command.vending.SubCmdAdd;
import com.cubefury.vendingmachine.command.vending.SubCmdReset;
import com.cubefury.vendingmachine.command.vending.SubCmdSet;

public class CommandVending extends CommandBase {

    private static final Map<String, IVendingSubcommand> SUBCOMMAND_MAP = new HashMap<>();

    static {
        register(new SubCmdAdd());
        register(new SubCmdSet());
        register(new SubCmdReset());
    }

    private static void register(IVendingSubcommand cmd) {
        SUBCOMMAND_MAP.put(cmd.getName(), cmd);
    }

    @Override
    public String getCommandName() {
        return "vending";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/vending <" + String.join("|", SUBCOMMAND_MAP.keySet()) + "> [...] ";
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return sender.canCommandSenderUseCommand(getRequiredPermissionLevel(), getCommandName());
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsFromIterableMatchingLastWord(args, SUBCOMMAND_MAP.keySet());
        }
        if (args.length > 1) {
            IVendingSubcommand sub = SUBCOMMAND_MAP.get(args[0]);
            if (sub == null) {
                return null;
            }
            return sub.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
        }
        return null;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length > 0 && SUBCOMMAND_MAP.containsKey(args[0])) {
            SUBCOMMAND_MAP.get(args[0])
                .execute(sender, Arrays.copyOfRange(args, 1, args.length));
        } else {
            sender.addChatMessage(new ChatComponentText("Usage: " + getCommandUsage(sender)));
        }
    }

}
