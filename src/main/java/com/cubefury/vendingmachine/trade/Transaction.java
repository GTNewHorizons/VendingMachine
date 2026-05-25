package com.cubefury.vendingmachine.trade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.cubefury.vendingmachine.blocks.MTEVendingMachine;
import com.cubefury.vendingmachine.blocks.MTEVendingUplinkHatch;
import com.cubefury.vendingmachine.util.BigItemStack;
import com.cubefury.vendingmachine.util.Wallet;

import appeng.api.storage.data.IAEItemStack;

public class Transaction {

    private final MTEVendingMachine vm;
    private final TradeRequest tradeRequest;
    private final Trade trade;

    private final Wallet preWallet;
    private final Wallet postWallet;
    private final Map<MTEVendingUplinkHatch, List<CurrencyItem>> pulledCoins = new HashMap<>();
    private final Map<MTEVendingUplinkHatch, List<CurrencyItem>> changeCoins = new HashMap<>();
    private final Map<MTEVendingUplinkHatch, List<IAEItemStack>> pulledStacks = new HashMap<>();
    private final ItemStack[] postInputSlots;

    public Transaction(MTEVendingMachine vm, TradeRequest tradeRequest) {
        this.vm = vm;
        this.tradeRequest = tradeRequest;
        this.trade = TradeDatabase.INSTANCE.getTradeGroupFromId(tradeRequest.tradeGroup)
            .getTrades()
            .get(tradeRequest.tradeGroupOrder);
        this.preWallet = TradeManager.INSTANCE.getWallet(tradeRequest.playerID, tradeRequest.walletMode);
        this.postWallet = Wallet.copyOf(this.preWallet);
        this.postInputSlots = vm.getCopyOfInputSlotItems();
    }

    public boolean validate() {
        if (
            !TradeManager.INSTANCE.canExecuteTrade(
                tradeRequest.playerID,
                TradeDatabase.INSTANCE.getTradeGroupFromId(tradeRequest.tradeGroup))
        ) return false;

        if (this.preWallet == null) return false;

        if (
            !vm.inputCurrencySatisfied(trade.fromCurrency, tradeRequest.playerID, tradeRequest.walletMode)
                || !vm.inputItemsSatisfied(trade.fromItems)
                || !vm.inputItemsSatisfied(trade.nonConsumedItems)
        ) return false;

        if (!trackedExtractCoinsFromWalletAndME()) {
            revert();
            return false;
        }

        for (BigItemStack stack : trade.fromItems) {
            if (!trackedExtractItemStackFromInputsAndME(stack)) {
                revert();
                return false;
            }
        }

        return true;
    }

    private void depositCoins(Map<MTEVendingUplinkHatch, List<CurrencyItem>> depositMap) {
        depositMap.forEach((hatch, ciList) -> { ciList.forEach(hatch::injectCoins); });
    }

    private void depositPulledItems() {
        pulledStacks.forEach(MTEVendingUplinkHatch::injectItems);
    }

    private boolean trackedExtractCoinsFromWalletAndME() {
        Map<CurrencyType, Integer> remaining = new HashMap<>();
        postWallet.performTrade(trade.fromCurrency)
            .forEach(ci -> { if (ci.value > 0) remaining.put(ci.type, ci.value); });
        if (remaining.isEmpty()) return true;
        vm.executeOnMeUplinkHatchIfPresent(hatch -> this.executeTrackedMECoinPull(hatch, remaining));
        return remaining.values()
            .stream()
            .anyMatch(v -> v > 0);
    }

    private void executeTrackedMECoinPull(MTEVendingUplinkHatch hatch, Map<CurrencyType, Integer> remaining) {
        List<CurrencyItem> pulled = hatch.performTrade(remaining);
        pulledCoins.put(hatch, pulled);

        List<CurrencyItem> change = new ArrayList<>();
        for (CurrencyItem pulledCurrency : pulled) {
            if (pulledCurrency.value - remaining.get(pulledCurrency.type) > 0) {
                change.add(
                    new CurrencyItem(pulledCurrency.type, pulledCurrency.value - remaining.get(pulledCurrency.type)));
            }
            remaining.put(pulledCurrency.type, remaining.get(pulledCurrency.type) - pulledCurrency.value);
        }
        changeCoins.put(hatch, change);
    }

    private boolean trackedExtractItemStackFromInputsAndME(BigItemStack stack) {
        ItemStack requiredStack = stack.getBaseStack();
        int requiredAmount = stack.stackSize;
        requiredAmount = extractItemStackFromSlots(requiredStack, requiredAmount, null);
        if (requiredAmount > 0 && stack.hasOreDict()) {
            requiredAmount = extractItemStackFromSlots(requiredStack, requiredAmount, stack.getOreDict());
        }

        requiredStack.stackSize = requiredAmount;
        return requiredAmount == 0 || vm.trackedFetchItemStackFromAE(
            requiredStack,
            false,
            stack.hasOreDict() ? stack.getOreDict() : null,
            tracker -> {
                if (tracker.getLeft() == null || tracker.getRight() == null) return;
                this.pulledStacks.putIfAbsent(tracker.getLeft(), new ArrayList<>());
                for (IAEItemStack existingStack : this.pulledStacks.get(tracker.getLeft())) {
                    if (
                        existingStack.getItem()
                            .equals(
                                tracker.getRight()
                                    .getItem())
                    ) {
                        existingStack.setStackSize(
                            existingStack.getStackSize() + tracker.getRight()
                                .getStackSize());
                        return;
                    }
                }
                this.pulledStacks.get(tracker.getLeft())
                    .add(
                        tracker.getRight()
                            .copy());
            });
    }

    private int extractItemStackFromSlots(ItemStack requiredStack, int requiredAmount, String oreDict) {
        for (int i = postInputSlots.length - 1; i >= 0 && requiredAmount > 0; i--) {
            if (postInputSlots[i] == null) continue;
            if (
                oreDict == null && requiredStack.isItemEqual(postInputSlots[i])
                    || IntStream.of(OreDictionary.getOreIDs(postInputSlots[i]))
                        .mapToObj(OreDictionary::getOreName)
                        .anyMatch(s -> s.equals(oreDict))
            ) {

                if (
                    requiredStack.isItemStackDamageable()
                        && requiredStack.getItemDamage() != postInputSlots[i].getItemDamage()
                ) {
                    continue;
                }

                if (requiredAmount >= postInputSlots[i].stackSize) {
                    requiredAmount -= postInputSlots[i].stackSize;
                    postInputSlots[i] = null;
                } else {
                    postInputSlots[i].stackSize -= requiredAmount;
                    requiredAmount = 0;
                }
            }
        }
        return requiredAmount;
    }

    public void revert() {
        depositCoins(pulledCoins);
        depositPulledItems();
    }

    public void commit(Consumer<ItemStack[]> consumer) {
        preWallet.resetAllCount();
        preWallet.merge(postWallet);
        depositCoins(changeCoins);
        TradeManager.INSTANCE.saveTeamData(tradeRequest.playerID);
        consumer.accept(postInputSlots);
    }

    public Trade getTrade() {
        return trade;
    }
}
