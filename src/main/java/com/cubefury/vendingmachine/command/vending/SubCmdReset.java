package com.cubefury.vendingmachine.command.vending;

import java.util.List;
import java.util.UUID;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;

import com.cubefury.vendingmachine.blocks.gui.WalletMode;
import com.cubefury.vendingmachine.command.Utils;
import com.cubefury.vendingmachine.storage.NameCache;
import com.cubefury.vendingmachine.trade.CurrencyType;
import com.cubefury.vendingmachine.trade.TradeManager;
import com.cubefury.vendingmachine.util.Wallet;
import com.gtnewhorizon.gtnhlib.util.CommandUtils;

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
        Wallet wallet = TradeManager.INSTANCE.getWallet(playerId, WalletMode.PERSONAL);
        if (wallet == null) {
            CommandUtils.error(sender, "No wallet found");
            return;
        }
        if (allCurrency) {
            wallet.resetAllCount();
            sender.addChatMessage(
                new ChatComponentText(String.format("Reset all coins for %s", target.getDisplayName())));
        } else {
            wallet.resetCount(type);
            sender.addChatMessage(
                new ChatComponentText(String.format("Reset %s for %s", type.id, target.getDisplayName())));
        }
        TradeManager.INSTANCE.saveTeamData(playerId);
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
