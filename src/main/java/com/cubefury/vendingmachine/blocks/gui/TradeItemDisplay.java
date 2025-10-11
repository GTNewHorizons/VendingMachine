package com.cubefury.vendingmachine.blocks.gui;

import java.util.List;
import java.util.UUID;

import net.minecraft.item.ItemStack;

import com.cubefury.vendingmachine.trade.CurrencyItem;
import com.cubefury.vendingmachine.util.BigItemStack;

import codechicken.nei.api.ItemFilter;

public class TradeItemDisplay {

    public List<CurrencyItem> fromCurrency;
    public List<BigItemStack> fromItems;
    public List<BigItemStack> toItems;
    public ItemStack display;
    public UUID tgID;
    public int tradeGroupOrder;
    public long cooldown;
    public String cooldownText;
    public boolean hasCooldown;
    public boolean enabled;
    public boolean tradeableNow;

    public TradeItemDisplay(List<CurrencyItem> fromCurrency, List<BigItemStack> fromItems, List<BigItemStack> toItems,
        ItemStack display, UUID tgID, int tradeGroupOrder, long cooldown, String cooldownText, boolean hasCooldown,
        boolean enabled, boolean tradeableNow) {
        this.fromCurrency = fromCurrency;
        this.fromItems = fromItems;
        this.toItems = toItems;
        this.display = display;
        this.tgID = tgID;
        this.tradeGroupOrder = tradeGroupOrder;
        this.cooldown = cooldown;
        this.cooldownText = cooldownText;
        this.hasCooldown = hasCooldown;
        this.enabled = enabled;
        this.tradeableNow = tradeableNow;
    }

    public boolean satisfiesSearch(ItemFilter filter, String searchStringNoCase) {
        return filter.matches(this.display) || this.toItems.stream()
            .anyMatch(bis -> filter.matches(bis.getBaseStack()))
            || this.fromItems.stream()
                .anyMatch(bis -> filter.matches(bis.getBaseStack()));
    }
}
