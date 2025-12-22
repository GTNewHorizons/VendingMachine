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
    private final Set<ICondition> requirementSet = new HashSet<>();

    // List of completed conditions for each player
    // This is only updated server-side, since players only need to know what trades
    // they have and their status.
    private final Map<UUID, Set<ICondition>> playerDone = new HashMap<>();

    // List of players with trade history
    private final Map<UUID, TradeHistory> tradeState = new HashMap<>();

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

    public void addSatisfiedCondition(UUID player, ICondition c) {
        synchronized (playerDone) {
            playerDone.computeIfAbsent(player, k -> new HashSet<>());
            playerDone.get(player)
                .add(c);
            if (
                playerDone.get(player)
                    .equals(requirementSet)
            ) {
                TradeManager.INSTANCE.addTradeGroup(player, this.id);
            }
        }
    }

    public void removeSatisfiedCondition(UUID player, ICondition c) {
        synchronized (playerDone) {
            if (!playerDone.containsKey(player) || playerDone.get(player) == null) {
                return;
            }
            playerDone.get(player)
                .remove(c);
            if (
                !playerDone.get(player)
                    .equals(requirementSet)
            ) {
                TradeManager.INSTANCE.removeTradeGroup(player, this.id);
            }
        }
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

    public void clearTradeState(UUID player) {
        synchronized (tradeState) {
            if (player == null) {
                tradeState.clear();
            } else {
                tradeState.remove(player);
            }
        }
    }

    public boolean isUnlockedPlayer(UUID player) {
        return requirementSet.equals(playerDone.getOrDefault(player, new HashSet<>()));
    }

    public Set<UUID> getAllUnlockedPlayers() {
        Set<UUID> playerList = new HashSet<>();
        for (Map.Entry<UUID, Set<ICondition>> entry : playerDone.entrySet()) {
            if (
                entry.getValue()
                    .equals(requirementSet)
            ) {
                playerList.add(entry.getKey());
            }
        }
        return playerList;
    }

    public TradeHistory getTradeState(UUID player) {
        synchronized (tradeState) {
            if (!tradeState.containsKey(player) || tradeState.get(player) == null) {
                return new TradeHistory();
            }
            return tradeState.get(player);
        }
    }

    public void setTradeState(UUID player, TradeHistory history) {
        synchronized (tradeState) {
            tradeState.put(player, history);

        }
    }

    public boolean canExecuteTrade(UUID player) {
        List<TradeGroup> availableTrades = TradeManager.INSTANCE.getAvailableTradeGroups(player);
        long currentTimestamp = System.currentTimeMillis();
        long lastTradeTime = this.getTradeState(player).lastTrade;
        long tradeCount = this.getTradeState(player).tradeCount;
        long cooldownRemaining;
        if (this.cooldown != -1 && lastTradeTime != -1 && (currentTimestamp - lastTradeTime) / 1000 < this.cooldown) {
            cooldownRemaining = this.cooldown - (currentTimestamp - lastTradeTime) / 1000;
        } else {
            cooldownRemaining = -1;
        }

        boolean enabled = this.maxTrades == -1 || tradeCount < this.maxTrades;

        for (TradeGroup trade : availableTrades) {
            if (trade == null) { // shouldn't happen
                continue;
            }
            if (trade.id.equals(this.id) && enabled && cooldownRemaining < 0) {
                return true;
            }
        }
        return false;
    }

    public void executeTrade(UUID player) {
        TradeHistory newTradeHistory = getTradeState(player);
        newTradeHistory.executeTrade(maxTrades, this.cooldown != -1);
        setTradeState(player, newTradeHistory);
        SaveLoadHandler.INSTANCE.writeTradeState(Collections.singleton(player));
        if (newTradeHistory.notificationQueued) {
            TradeManager.INSTANCE.addNotification(player, this);
        }
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

    @Optional.Method(modid = "betterquesting")
    public void removeAllSatisfiedBqConditions(UUID player) {
        synchronized (tradeState) {
            if (player == null) {
                for (Map.Entry<UUID, Set<ICondition>> entry : playerDone.entrySet()) {
                    if (entry.getValue() == null) { // just in case
                        continue;
                    }
                    entry.getValue()
                        .removeIf((condition) -> condition instanceof BqCondition);
                }
            } else if (playerDone.get(player) != null) {
                playerDone.get(player)
                    .removeIf((condition) -> condition instanceof BqCondition);
            }
        }
    }
}
