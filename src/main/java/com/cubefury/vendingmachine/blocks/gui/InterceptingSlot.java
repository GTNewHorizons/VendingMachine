package com.cubefury.vendingmachine.blocks.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.utils.item.ItemStackHandler;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cubefury.vendingmachine.network.handlers.NetTradeStateSync;
import com.cubefury.vendingmachine.storage.NameCache;
import com.cubefury.vendingmachine.trade.CurrencyItem;
import com.cubefury.vendingmachine.trade.TradeManager;

public class InterceptingSlot extends ModularSlot {

    public InterceptingSlot(ItemStackHandler inputItems, int index) {
        super(inputItems, index);
    }

    // intercept item on both ends, but only do the post-intercept actions on server side
    public boolean intercept(ItemStack newItem, boolean client, EntityPlayer player) {
        CurrencyItem mapped = mapToCurrency(newItem);
        if (mapped != null) {
            if (!client) {
                TradeManager.INSTANCE.addCurrency(NameCache.INSTANCE.getUUIDFromPlayer(player), mapped, true);
                NetTradeStateSync.sendPlayerCurrency((EntityPlayerMP) player, mapped);
            } else {
                MTEVendingMachineGui.setForceRefresh();
            }
            this.putStack(null);
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
