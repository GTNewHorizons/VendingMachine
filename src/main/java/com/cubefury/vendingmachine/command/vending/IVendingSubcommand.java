package com.cubefury.vendingmachine.command.vending;

import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

public interface IVendingSubcommand {

    String getName();

    String getUsage(ICommandSender sender);

    void execute(ICommandSender sender, String[] args) throws CommandException;

    List<String> tabComplete(ICommandSender sender, String[] args);
}
