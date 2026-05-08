package com.cubefury.vendingmachine.util;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import com.cubefury.vendingmachine.VendingMachine;
import com.cubefury.vendingmachine.trade.CurrencyItem;
import com.cubefury.vendingmachine.trade.CurrencyType;

public class Wallet {

    private final Map<CurrencyType, Integer> currencies = new EnumMap<>(CurrencyType.class);
    private final List<NBTTagCompound> invalidCurrency = new ArrayList<>();

    public boolean hasEnough(List<CurrencyItem> currencyItems) {
        return currencyItems.stream()
            .allMatch(ci -> currencies.getOrDefault(ci.type, 0) >= ci.value);
    }

    public boolean performTrade(List<CurrencyItem> currencyItems) {
        Map<CurrencyType, Integer> newCoinInventory = new EnumMap<>(CurrencyType.class);

        // Check to make sure we have enough
        for (CurrencyItem ci : currencyItems) {
            int oldValue = currencies.get(ci.type);
            if (!currencies.containsKey(ci.type) || oldValue < ci.value) {
                return false;
            } else {
                newCoinInventory.put(ci.type, oldValue - ci.value);
            }
        }

        // Apply new coin inventory
        for (Map.Entry<CurrencyType, Integer> entry : newCoinInventory.entrySet()) {
            if (entry.getValue() == 0) {
                currencies.remove(entry.getKey());
            } else {
                currencies.put(entry.getKey(), entry.getValue());
            }
        }
        return true;
    }

    public void merge(Wallet other) {
        for (Entry<CurrencyType, Integer> entry : other.currencies.entrySet()) {
            addCount(entry.getKey(), entry.getValue());
        }
    }

    public int getCount(CurrencyType type) {
        return currencies.getOrDefault(type, 0);
    }

    public void setCount(CurrencyType type, int amount) {
        currencies.put(type, amount);
    }

    public void addCount(CurrencyType type, int amount) {
        currencies.put(type, currencies.getOrDefault(type, 0) + amount);
    }

    public void resetCount(CurrencyType type) {
        currencies.remove(type);
    }

    public void resetAllCount() {
        currencies.clear();
    }

    public void writeToNBT(NBTTagCompound tag) {
        if (currencies.isEmpty() && invalidCurrency.isEmpty()) {
            return;
        }
        NBTTagList nbtCurrencyList = new NBTTagList();
        for (Map.Entry<CurrencyType, Integer> entry : currencies.entrySet()) {
            if (entry.getValue() > 0) {
                NBTTagCompound currencyEntry = new NBTTagCompound();
                currencyEntry.setString("currency", entry.getKey().id);
                currencyEntry.setInteger("amount", entry.getValue());
                nbtCurrencyList.appendTag(currencyEntry);
            }
        }

        if (!invalidCurrency.isEmpty()) {
            for (NBTTagCompound invalidTag : invalidCurrency) {
                nbtCurrencyList.appendTag(invalidTag);
            }
        }
        tag.setTag("currency", nbtCurrencyList);
    }

    public void readFromNBT(NBTTagCompound tag, boolean merge) {
        if (!merge) {
            currencies.clear();
            invalidCurrency.clear();
        }

        if (!tag.hasKey("currency")) return;

        NBTTagList tagList = tag.getTagList("currency", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < tagList.tagCount(); i++) {
            NBTTagCompound currencyEntry = tagList.getCompoundTagAt(i);
            CurrencyType type = CurrencyType.getTypeFromId(currencyEntry.getString("currency"));
            if (type == null) {
                VendingMachine.LOG.warn("Unknown currency type found: {}", currencyEntry.getString("currency"));
                this.invalidCurrency.add(currencyEntry);
                continue;
            }
            int amount = currencyEntry.getInteger("amount");
            if (merge) {
                amount += currencies.getOrDefault(type, 0);
            }
            this.currencies.put(type, amount);
        }
    }

}
