package com.cubefury.vendingmachine.command.vending;

import static com.cubefury.vendingmachine.command.Utils.getCurrencyListFormattedWithHighlights;

import java.util.List;
import java.util.UUID;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import com.cubefury.vendingmachine.blocks.gui.WalletMode;
import com.cubefury.vendingmachine.command.Utils;
import com.cubefury.vendingmachine.storage.NameCache;
import com.cubefury.vendingmachine.trade.CurrencyType;
import com.cubefury.vendingmachine.trade.TradeManager;
import com.cubefury.vendingmachine.util.Wallet;
import com.gtnewhorizon.gtnhlib.chat.customcomponents.ChatComponentNumber;
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
        EntityPlayerMP target;
        boolean allCurrency;
        CurrencyType type;
        int amount;
        String userEnteredNumber = "";
        String userEnteredCurrency;

        try {
            switch (args.length) {
                case 2:
                    target = CommandBase.getCommandSenderAsPlayer(sender);
                    userEnteredCurrency = args[0];
                    allCurrency = args[0].equalsIgnoreCase("all");
                    type = CurrencyType.getTypeFromId(args[0]);
                    userEnteredNumber = args[1];
                    amount = Integer.parseInt(userEnteredNumber);
                    break;

                case 3:
                    target = CommandBase.getPlayer(sender, args[0]);
                    userEnteredCurrency = args[1];
                    allCurrency = args[1].equalsIgnoreCase("all");
                    type = CurrencyType.getTypeFromId(args[1]);
                    userEnteredNumber = args[2];
                    amount = Integer.parseInt(userEnteredNumber);
                    break;

                default:
                    sender
                        .addChatMessage(new ChatComponentTranslation("vendingmachine.command.usage", getUsage(sender)));
                    return;
            }

        } catch (NumberFormatException e) {
            IChatComponent error = new ChatComponentTranslation(
                "vendingmachine.command.error.integer_format",
                userEnteredNumber);

            error.getChatStyle()
                .setColor(EnumChatFormatting.RED);

            sender.addChatMessage(error);
            return;

        } catch (PlayerNotFoundException e) {
            IChatComponent error = new ChatComponentTranslation(
                "vendingmachine.command.error.player_not_found",
                args[0]);
            error.getChatStyle()
                .setColor(EnumChatFormatting.RED);
            sender.addChatMessage(error);
            return;
        }

        boolean validCurrency = allCurrency || type != null;

        if (!validCurrency) {
            IChatComponent error = new ChatComponentTranslation(
                "vendingmachine.command.error.invalid_currency_with_list",
                userEnteredCurrency,
                getCurrencyListFormattedWithHighlights());

            error.getChatStyle()
                .setColor(EnumChatFormatting.RED);

            sender.addChatMessage(error);
            return;
        }

        UUID playerId = NameCache.INSTANCE.getUUIDFromPlayer(target);
        Wallet wallet = TradeManager.INSTANCE.getWallet(playerId, WalletMode.PERSONAL);

        if (wallet == null) {
            CommandUtils.error(sender, "vendingmachine.command.no_wallet");
            return;
        }

        if (allCurrency) {
            for (CurrencyType cur : CurrencyType.values()) {
                wallet.setCount(cur, amount);
            }

            sender.addChatMessage(
                new ChatComponentTranslation(
                    "vendingmachine.command.set.all_currency",
                    new ChatComponentNumber(amount),
                    target.getDisplayName()));

        } else {
            wallet.setCount(type, amount);

            IChatComponent translatedCoinName = Utils.getLocalisedCoinNameWithHover(type.id);

            sender.addChatMessage(
                new ChatComponentTranslation(
                    "vendingmachine.command.set.specific_currency",
                    translatedCoinName,
                    new ChatComponentNumber(amount),
                    target.getDisplayName()));
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
