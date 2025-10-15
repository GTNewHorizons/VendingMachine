package com.cubefury.vendingmachine.trade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class CurrencyItem {

    public CurrencyType type;
    public int value;
    public static final Map<String, CurrencyType> typeMap = new HashMap<>();
    private static final String[] coinSuffixes = new String[] { "IV", "III", "II", "I", "" };
    private static final int[] coinValues = new int[] { 10000, 1000, 100, 10, 1 };

    public static CurrencyItem fromNBT(NBTTagCompound nbt) {
        CurrencyType type = CurrencyType.getTypeFromId(nbt.getString("type"));
        if (type == null) {
            return null;
        }
        return new CurrencyItem(type, nbt.getInteger("value"));
    }

    public NBTTagCompound writeToNBT(NBTTagCompound payload) {
        payload.setString("type", this.type.id);
        payload.setInteger("value", this.value);
        return payload;
    }

    public List<ItemStack> itemize() {
        List<ItemStack> outputs = new ArrayList<>();
        if (this.type == null || this.value <= 0) {
            return outputs;
        }
        for (int i = 0; i < coinValues.length; i++) {
            while (this.value >= coinValues[i]) {
                Item outputItem = (Item) Item.itemRegistry.getObject(this.type.itemPrefix + coinSuffixes[i]);
                int stackSize = Math.min(this.value / coinValues[i], outputItem.getItemStackLimit());
                outputs.add(new ItemStack(outputItem, stackSize));
                this.value -= stackSize * coinValues[i];
            }
        }
        return outputs;
    }

    public ItemStack getItemRepresentation() {
        Item outputItem = (Item) Item.itemRegistry.getObject(this.type.itemPrefix);
        return new ItemStack(outputItem, value);
    }

    public CurrencyItem(CurrencyType type, int value) {
        this.type = type;
        this.value = value;
    }

    public static CurrencyItem fromItemStack(ItemStack newItem) {
        String itemName = Item.itemRegistry.getNameForObject(newItem.getItem());
        for (CurrencyType type : CurrencyType.values()) {
            if (itemName.startsWith(type.itemPrefix)) {
                int currencyValue = mapSuffixToValue(itemName.substring(type.itemPrefix.length()));
                if (currencyValue < 0) {
                    return null;
                }
                return new CurrencyItem(type, currencyValue * newItem.stackSize);
            }
        }
        return null;
    }

    private static int mapSuffixToValue(String suffix) {
        for (int i = 0; i < coinSuffixes.length; i++) {
            if (suffix.equals(coinSuffixes[i])) {
                return coinValues[i];
            }
        }
        return -1;
    }
}
