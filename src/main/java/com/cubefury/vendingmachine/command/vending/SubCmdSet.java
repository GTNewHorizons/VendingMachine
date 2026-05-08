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

public class SubCmdSet implements IVendingSubcommand {

    @Override
    public String getName() {
        return "set";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/vending set [player] <coin type> <amount>";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP target = null;
        boolean allCurrency = false;
        CurrencyType type = null;
        int amount = 0;

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
        Wallet wallet = TradeManager.INSTANCE.getWallet(playerId, WalletMode.PERSONAL);
        if (wallet == null) {
            CommandUtils.error(sender, "No wallet found");
            return;
        }
        if (allCurrency) {
            for (CurrencyType cur : CurrencyType.values()) {
                wallet.setCount(cur, amount);
            }
            sender.addChatMessage(
                new ChatComponentText(String.format("Set all coins = %d for %s", amount, target.getDisplayName())));
        } else {
            wallet.setCount(type, amount);
            sender.addChatMessage(
                new ChatComponentText(String.format("Set %s = %d for %s", type.id, amount, target.getDisplayName())));
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
