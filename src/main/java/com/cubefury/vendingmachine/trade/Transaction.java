package com.cubefury.vendingmachine.trade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import com.cubefury.vendingmachine.blocks.MTEVendingUplinkHatch;
import com.cubefury.vendingmachine.util.BigItemStack;

import appeng.api.storage.data.IAEItemStack;

public class Transaction {

    private final MTEVendingUplinkHatch hatch;
    private final List<BigItemStack> nonConsumedItems;
    private final List<BigItemStack> fromItems;
    private final List<CurrencyItem> fromCurrency;
    private final boolean simulate;

    private final List<CurrencyItem> pulledCoins = new ArrayList<>();
    private final List<CurrencyItem> changeCoins = new ArrayList<>();
    private final List<IAEItemStack> pulledStacks = new ArrayList<>();

    public Transaction(MTEVendingUplinkHatch hatch, List<BigItemStack> nonConsumedItems, List<BigItemStack> fromItems,
        List<CurrencyItem> fromCurrency, boolean simulate) {
        this.hatch = hatch;
        this.nonConsumedItems = nonConsumedItems;
        this.fromItems = fromItems;
        this.fromCurrency = fromCurrency;
        this.simulate = simulate;
    }

    public boolean validate() {
        if (simulate) {
            if (!hatch.checkSufficientCurrency(fromCurrency)) return false;
        } else if (!trackedExtractCoinsFromME()) {
            revert();
            return false;
        }

        for (BigItemStack stack : nonConsumedItems) {
            if (!trackedExtractItemStackFromME(stack, true)) {
                revert();
                return false;
            }
        }
        for (BigItemStack stack : fromItems) {
            if (!trackedExtractItemStackFromME(stack, simulate)) {
                revert();
                return false;
            }
        }
        return true;
    }

    private void depositCoins(List<CurrencyItem> deposit) {
        deposit.forEach(ci -> hatch.injectCoins(ci));
    }

    private void depositPulledItems() {
        hatch.injectItems(pulledStacks);
    }

    private boolean trackedExtractCoinsFromME() {
        Map<CurrencyType, Integer> remaining = new HashMap<>();
        fromCurrency.forEach(ci -> remaining.put(ci.type, ci.value));
        executeTrackedMECoinPull(remaining);
        return remaining.values()
            .stream()
            .allMatch(v -> v <= 0);
    }

    private void executeTrackedMECoinPull(Map<CurrencyType, Integer> remaining) {
        List<CurrencyItem> pulled = hatch.removeCoins(remaining);
        pulledCoins.addAll(pulled);

        List<CurrencyItem> change = new ArrayList<>();
        for (CurrencyItem pulledCurrency : pulled) {
            if (pulledCurrency.value - remaining.get(pulledCurrency.type) > 0) {
                change.add(
                    new CurrencyItem(pulledCurrency.type, pulledCurrency.value - remaining.get(pulledCurrency.type)));
            }
            remaining.put(pulledCurrency.type, remaining.get(pulledCurrency.type) - pulledCurrency.value);
        }
        changeCoins.addAll(change);
    }

    private boolean trackedExtractItemStackFromME(BigItemStack stack, boolean simulate) {
        if (stack.stackSize == 0) return true;

        ItemStack requiredStack = stack.getBaseStack();
        requiredStack.stackSize = stack.stackSize;

        return hatch.removeItem(
            requiredStack,
            simulate,
            stack.hasOreDict() ? stack.getOreDict() : null,
            simulate ? ignoreSimulatedPull -> {} : aePulledStack -> {
                if (aePulledStack == null) return;
                for (IAEItemStack existingStack : this.pulledStacks) {
                    if (
                        existingStack.getItem()
                            .equals(aePulledStack.getItem())
                    ) {
                        existingStack.setStackSize(existingStack.getStackSize() + aePulledStack.getStackSize());
                        return;
                    }
                }
                this.pulledStacks.add(aePulledStack.copy());
            }) == 0;
    }

    public void revert() {
        depositCoins(pulledCoins);
        depositPulledItems();
    }

    public void commit() {
        depositCoins(changeCoins);
    }
}
