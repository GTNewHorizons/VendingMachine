package com.cubefury.vendingmachine.trade;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import com.cubefury.vendingmachine.integration.betterquesting.gui.PanelQBTrade;
import com.cubefury.vendingmachine.util.BigItemStack;
import com.cubefury.vendingmachine.util.ItemPlaceholder;
import com.cubefury.vendingmachine.util.JsonHelper;

import betterquesting.api2.client.gui.misc.IGuiRect;
import betterquesting.api2.client.gui.panels.IGuiPanel;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class Trade {

    public final List<CurrencyItem> fromCurrency = new ArrayList<>();
    public final List<BigItemStack> fromItems = new ArrayList<>();
    public final List<BigItemStack> nonConsumedItems = new ArrayList<>();
    public final List<BigItemStack> toItems = new ArrayList<>();
    public BigItemStack displayItem = new BigItemStack(ItemPlaceholder.placeholder);

    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setTag("displayItem", displayItem.writeToNBT(new NBTTagCompound()));

        if (!this.fromCurrency.isEmpty()) {
            NBTTagList fromCurrencyArray = new NBTTagList();
            for (CurrencyItem ci : this.fromCurrency) {
                fromCurrencyArray.appendTag(ci.writeToNBT(new NBTTagCompound()));
            }
            nbt.setTag("fromCurrency", fromCurrencyArray);
        }

        if (!this.fromItems.isEmpty()) {
            NBTTagList fromItemsArray = new NBTTagList();
            for (BigItemStack stack : this.fromItems) {
                fromItemsArray.appendTag(JsonHelper.ItemStackToJson(stack, new NBTTagCompound()));
            }
            nbt.setTag("fromItems", fromItemsArray);
        }

        if (!this.nonConsumedItems.isEmpty()) {
            NBTTagList ncArray = new NBTTagList();
            for (BigItemStack stack : this.nonConsumedItems) {
                ncArray.appendTag(JsonHelper.ItemStackToJson(stack, new NBTTagCompound()));
            }
            nbt.setTag("nonConsumedItems", ncArray);
        }

        if (!this.toItems.isEmpty()) {
            NBTTagList toItemsArray = new NBTTagList();
            for (BigItemStack stack : this.toItems) {
                toItemsArray.appendTag(JsonHelper.ItemStackToJson(stack, new NBTTagCompound()));
            }
            nbt.setTag("toItems", toItemsArray);
        }

        return nbt;
    }

    public void readFromNBT(NBTTagCompound nbt) {
        displayItem = BigItemStack.loadItemStackFromNBT(nbt.getCompoundTag("displayItem"));

        fromCurrency.clear();
        fromItems.clear();
        toItems.clear();

        NBTTagList currencyList = nbt.getTagList("fromCurrency", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < currencyList.tagCount(); i++) {
            fromCurrency.add(CurrencyItem.fromNBT(currencyList.getCompoundTagAt(i)));
        }

        NBTTagList fromList = nbt.getTagList("fromItems", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < fromList.tagCount(); i++) {
            fromItems.add(JsonHelper.JsonToItemStack(fromList.getCompoundTagAt(i)));
        }

        NBTTagList ncList = nbt.getTagList("nonConsumedItems", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < ncList.tagCount(); i++) {
            nonConsumedItems.add(JsonHelper.JsonToItemStack(ncList.getCompoundTagAt(i)));
        }

        NBTTagList toList = nbt.getTagList("toItems", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < toList.tagCount(); i++) {
            toItems.add(JsonHelper.JsonToItemStack(toList.getCompoundTagAt(i)));
        }
    }

    public ItemStack getDisplayItem() {
        return this.displayItem.getBaseStack()
            .isItemEqual(new ItemStack(ItemPlaceholder.placeholder))
                ? this.toItems.get(0)
                    .convertToItemStack()
                : this.displayItem.getBaseStack();
    }

    @Optional.Method(modid = "betterquesting")
    @SideOnly(Side.CLIENT)
    public IGuiPanel getTradeGui(IGuiRect rect) {
        return new PanelQBTrade(rect, this);
    }

}
