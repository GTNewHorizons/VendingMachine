package com.cubefury.vendingmachine.trade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import com.cubefury.vendingmachine.VendingMachine;
import com.cubefury.vendingmachine.api.trade.ICondition;
import com.cubefury.vendingmachine.handlers.SaveLoadHandler;
import com.cubefury.vendingmachine.integration.betterquesting.BqAdapter;
import com.cubefury.vendingmachine.integration.betterquesting.BqCondition;
import com.cubefury.vendingmachine.util.NBTConverter;

import cpw.mods.fml.common.Optional;

public class TradeGroup {

    private UUID id = new UUID(0, 0); // placeholder UUID
    private final List<Trade> trades = new ArrayList<>();
    public int cooldown = -1;
    public int maxTrades = -1;
    private TradeCategory category = TradeCategory.UNKNOWN;
    private String original_category_str = "";
    public final Set<ICondition> requirementSet = new HashSet<>();

    public TradeGroup() {}

    public UUID getId() {
        return this.id;
    }

    public String toString() {
        return this.id.toString();
    }

    public boolean hasNoConditions() {
        return this.requirementSet.isEmpty();
    }

    public List<Trade> getTrades() {
        return trades;
    }

    public TradeCategory getCategory() {
        return category;
    }

    public List<ICondition> getRequirements() {
        return new ArrayList<>(requirementSet);
    }

    public boolean readFromNBT(NBTTagCompound nbt) {
        this.trades.clear();
        this.requirementSet.clear();

        boolean generatedMetadata = false;
        if (nbt.hasKey("id")) {
            this.id = NBTConverter.UuidValueType.TRADEGROUP.readId(nbt.getCompoundTag("id"));
        } else {
            this.id = UUID.randomUUID();
            generatedMetadata = true;
        }
        if (nbt.hasKey("category")) {
            this.original_category_str = nbt.getString("category");
            this.category = TradeCategory.ofString(original_category_str);
        } else {
            this.original_category_str = TradeCategory.UNKNOWN.getUnlocalized_name();
            this.category = TradeCategory.UNKNOWN;
            generatedMetadata = true;
        }
        this.cooldown = nbt.getInteger("cooldown");
        this.maxTrades = nbt.getInteger("maxTrades");
        NBTTagList tradeList = nbt.getTagList("trades", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < tradeList.tagCount(); i++) {
            NBTTagCompound trade = tradeList.getCompoundTagAt(i);
            Trade newTrade = new Trade();
            newTrade.readFromNBT(trade);
            this.trades.add(newTrade);
        }
        NBTTagList reqList = nbt.getTagList("requirements", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < reqList.tagCount(); i++) {
            ICondition condition = ConditionParser.getConditionFromNBT(reqList.getCompoundTagAt(i));
            requirementSet.add(condition);
            if (VendingMachine.isBqLoaded && condition instanceof BqCondition) {
                BqCondition bqc = (BqCondition) condition;
                BqAdapter.INSTANCE.addQuestTrigger(bqc.getQuestId(), this);
            }
        }
        return generatedMetadata;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setTag("id", NBTConverter.UuidValueType.TRADEGROUP.writeId(this.id));
        nbt.setInteger("cooldown", this.cooldown);
        nbt.setInteger("maxTrades", this.maxTrades);
        nbt.setString("category", this.category.getKey());
        NBTTagList tList = new NBTTagList();
        for (Trade t : trades) {
            tList.appendTag(t.writeToNBT(new NBTTagCompound()));
        }
        nbt.setTag("trades", tList);
        NBTTagList cList = new NBTTagList();
        for (ICondition ic : requirementSet) {
            cList.appendTag(ic.writeToNBT(new NBTTagCompound()));
        }
        nbt.setTag("requirements", cList);
        return nbt;
    }

}
