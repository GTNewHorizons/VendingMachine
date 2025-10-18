package com.cubefury.vendingmachine.blocks.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.utils.item.ItemStackHandler;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cubefury.vendingmachine.blocks.MTEVendingMachine;
import com.cubefury.vendingmachine.storage.NameCache;
import com.cubefury.vendingmachine.trade.CurrencyItem;
import com.cubefury.vendingmachine.trade.TradeManager;

public class InterceptingSlot extends ModularSlot {

    private MTEVendingMachine vm;

    public InterceptingSlot(ItemStackHandler inputItems, int index, MTEVendingMachine vm) {
        super(inputItems, index);
        this.vm = vm;
    }

    // intercept item on both ends, but only do the post-intercept actions on server side
    public boolean intercept(ItemStack newItem, boolean client, EntityPlayer player) {
        if (vm == null || !vm.getActive()) {
            return false;
        }
        CurrencyItem mapped = mapToCurrency(newItem);
        if (mapped != null) {
            this.putStack(null);
            if (!client) {
                TradeManager.INSTANCE.addCurrency(NameCache.INSTANCE.getUUIDFromPlayer(player), mapped);
            }
            return true;
        }
        return false;
    }

    private CurrencyItem mapToCurrency(ItemStack newItem) {
        if (newItem == null) {
            return null;
        }
        return CurrencyItem.fromItemStack(newItem);
    }

}
